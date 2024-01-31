package org.comroid.forwardmq;

import lombok.Getter;
import lombok.Synchronized;
import lombok.Value;
import lombok.extern.java.Log;
import org.comroid.annotations.internal.Annotations;
import org.comroid.api.Polyfill;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.DataStructure;
import org.comroid.api.func.exc.ThrowingFunction;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.Event;
import org.comroid.api.func.util.Streams;
import org.comroid.api.tree.Component;
import org.comroid.forwardmq.data.ProtoImplementation;
import org.comroid.forwardmq.data.adapter.DataAdapter;
import org.comroid.forwardmq.data.processor.DataProcessor;
import org.comroid.forwardmq.data.processor.internal.DataProcessor$Internal$Aurion2Discord;
import org.comroid.forwardmq.data.processor.internal.DataProcessor$Internal$Discord2Aurion;
import org.comroid.forwardmq.entity.DataFlow;
import org.comroid.forwardmq.entity.proto.Proto;
import org.comroid.forwardmq.entity.proto.adapter.ProtoAdapter;
import org.comroid.forwardmq.entity.proto.processor.ProtoProcessor;
import org.comroid.forwardmq.entity.proto.processor.ProtoProcessor$Internal;
import org.comroid.forwardmq.model.IDataProcessor;
import org.comroid.forwardmq.repo.*;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.stream.Stream;

@Log
@Getter
@Service
public class DataFlowManager extends Component.Base implements ApplicationRunner {
    public static final ThreadGroup mThreadGroup = new ThreadGroup("DataFlowManager");
    private final Map<ProtoAdapter, DataAdapter<?>> adapterCache = new ConcurrentHashMap<>();
    private final Map<ProtoProcessor, IDataProcessor<?>> processorCache = new ConcurrentHashMap<>();
    private final Map<DataFlow, DataFlowRunner> runnerCache = new ConcurrentHashMap<>();
    private final List<IDataProcessor<ProtoProcessor$Internal>> internalProcessorCache = List.of(
            new DataProcessor$Internal$Aurion2Discord(),
            new DataProcessor$Internal$Discord2Aurion()
    );

    private @Autowired ForwardMQ fmq;
    private @Autowired ProtoAdapter$DiscordChannel$Repo adapterRepo_dc;
    private @Autowired ProtoAdapter$DiscordWebhook$Repo adapterRepo_dw;
    private @Autowired ProtoAdapter$Rabbit$Repo adapterRepo_mq;
    private @Autowired ProtoProcessor$Internal$Repo processorRepo_$$;
    private @Autowired ProtoProcessor$JavaScript$Repo processorRepo_js;
    private @Autowired DataFlow$Repo flowRepo;
    private @Autowired ScheduledExecutorService executor;

    @Override
    public void run(ApplicationArguments args) {
        var added = new HashSet<ProtoProcessor$Internal>();
        for (var idp : internalProcessorCache) {
            var proto = idp.getProto();
            if (processorRepo_$$.findByName(proto.getName()).isEmpty()) {
                processorRepo_$$.save(proto);
                added.add(proto);
            }
        }
        log.info("Added " + added.size() + " new internal processors to DB");
        log.fine(Arrays.toString(added.toArray()));

        execute(executor, Duration.ofMinutes(1));
    }

    @Override
    protected void $initialize() {
        var helper = new Object() {
            <R extends Proto.Repo<? extends P>, P extends Proto, I extends ProtoImplementation<P>> long initFromProto(
                    Stream<? extends R> repos, Class<? super P> proto, Class<? super I> impl, Map<? super P, ? super I> map) {
                return repos.map(CrudRepository::findAll)
                        .flatMap(Streams::of)
                        .flatMap(it -> Annotations.related(it.getClass())
                                .map(DataStructure::of)
                                .flatMap(struct -> struct.getConstructors().stream())
                                .filter(ctor -> ctor.getArgs().size() == 1 && proto.isAssignableFrom(ctor.getArgs().get(0).getType()))
                                .map(DataStructure.Constructor::getCtor)
                                .map(ThrowingFunction.fallback(ctor -> ctor.invoke(null, it), Wrap.empty()))
                                .filter(impl::isInstance)
                                .map(Polyfill::<I>uncheckedCast))
                        .peek(i -> map.put(i.getProto(), i))
                        .count();
            }
        };
        var adapterCount = helper.<ProtoAdapter.Repo<? extends ProtoAdapter>, ProtoAdapter, DataAdapter<ProtoAdapter>>initFromProto(
                Stream.of(adapterRepo_dc, adapterRepo_dw, adapterRepo_mq),
                ProtoAdapter.class, DataAdapter.class, adapterCache);
        var processorCount = helper.<ProtoProcessor.Repo<? extends ProtoProcessor>, ProtoProcessor, DataProcessor<ProtoProcessor>>initFromProto(
                Stream.of(processorRepo_js),
                ProtoProcessor.class, DataProcessor.class, processorCache);
        var flowCount = Streams.of(flowRepo.findAll())
                .peek(this::init)
                .count();

        log.info("Initialized with %d adapters; %d processors and %d flows"
                .formatted(adapterCount, processorCount, flowCount));
    }

    @Override
    protected void $terminate() {
        runnerCache.forEach((flow, runner) -> runner.interrupt());
        runnerCache.clear();
    }

    public void init(DataFlow flow) {
        runnerCache.computeIfAbsent(flow, it -> {
            var runner = new DataFlowRunner(it);
            runner.start();
            return runner;
        });
    }

    public Optional<IDataProcessor<ProtoProcessor$Internal>> getInternalProcessor(final @Nullable String name) {
        return internalProcessorCache.stream()
                .filter(internal -> Objects.equals(name, internal.getProto().getName()))
                .findAny();
    }

    @Value
    public class DataFlowRunner extends Thread {
        Queue<DataNode> queue = new LinkedBlockingQueue<>();
        DataFlow flow;
        Event.Listener<DataNode> source;
        List<? extends IDataProcessor<?>> processors;
        DataAdapter<?> destination;

        public DataFlowRunner(DataFlow flow) {
            super(mThreadGroup, $toString(flow));
            this.flow = flow;
            this.source = adapterCache.get(flow.getSource())
                    .getSource().listen()
                    .subscribeData(this::push);
            this.processors = flow.getProcessors().stream()
                    .flatMap(proto -> proto instanceof ProtoProcessor$Internal
                            ? getInternalProcessor(proto.getName()).stream()
                            : Stream.of(processorCache.getOrDefault(proto, null)))
                    .filter(Objects::nonNull)
                    .toList();
            this.destination = adapterCache.get(flow.getDestination());
        }

        @Synchronized("queue")
        public void push(DataNode data) {
            queue.add(data);
            queue.notify();
        }

        @Override
        @SuppressWarnings("InfiniteLoopStatement")
        public void run() {
            DataNode data = null;
            try {
                while (true) {
                    // input
                    synchronized (queue) {
                        while (queue.isEmpty())
                            queue.wait();
                        data = queue.poll();
                    }

                    // processing
                    for (var proc : processors)
                        try {
                            data = proc.apply(data);
                        } catch (Throwable t) {
                            log.log(Level.WARNING, "An error occurred during processing step " + proc, t);
                        }

                    // output
                    destination.accept(data);
                }
            } catch (Throwable t) {
                log.log(Level.WARNING, "Exception occurred in " + this + "; data = " + data, t);
            }
        }

        @Override
        public String toString() {
            return $toString(flow);
        }

        private static String $toString(DataFlow flow) {
            return "DataFlowRunner#" + flow.getId().toString();
        }

        @Override
        public void interrupt() {
            source.close();
            super.interrupt();
        }
    }
}
