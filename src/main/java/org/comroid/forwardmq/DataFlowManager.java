package org.comroid.forwardmq;

import emoji4j.EmojiUtils;
import lombok.Getter;
import lombok.Synchronized;
import lombok.Value;
import lombok.extern.java.Log;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.comroid.annotations.internal.Annotations;
import org.comroid.api.Polyfill;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.DataStructure;
import org.comroid.api.func.exc.ThrowingFunction;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.Event;
import org.comroid.api.func.util.Streams;
import org.comroid.api.info.Constraint;
import org.comroid.api.tree.Component;
import org.comroid.forwardmq.data.ProtoImplementation;
import org.comroid.forwardmq.data.adapter.DataAdapter;
import org.comroid.forwardmq.data.processor.DataProcessor;
import org.comroid.forwardmq.entity.DataFlow;
import org.comroid.forwardmq.entity.proto.Proto;
import org.comroid.forwardmq.entity.proto.adapter.ProtoAdapter;
import org.comroid.forwardmq.entity.proto.processor.ProtoProcessor;
import org.comroid.forwardmq.entity.proto.processor.ProtoProcessor$Internal;
import org.comroid.forwardmq.model.AurionChat;
import org.comroid.forwardmq.model.IDataProcessor;
import org.comroid.forwardmq.repo.*;
import org.comroid.forwardmq.util.Util;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson;

@Log
@Getter
@Service
public class DataFlowManager extends Component.Base implements ApplicationRunner {
    public static final ThreadGroup mThreadGroup = new ThreadGroup("DataFlowManager");
    public static final UUID InternalProcessorId_Aurion2Discord = UUID.fromString("a593c1f6-7073-4f72-b360-c767d284ba12");
    public static final UUID InternalProcessorId_Discord2Aurion = UUID.fromString("a16b03f3-807e-4c06-8571-b3dc18db8ac3");
    private final Map<ProtoAdapter, DataAdapter<?>> adapterCache = new ConcurrentHashMap<>();
    private final Map<ProtoProcessor, IDataProcessor> processorCache = new ConcurrentHashMap<>();
    private final Map<DataFlow, DataFlowRunner> runnerCache = new ConcurrentHashMap<>();
    private final List<IDataProcessor> internalProcessorCache = List.of(
            new IDataProcessor() {
                final ProtoProcessor$Internal proto = new ProtoProcessor$Internal() {{
                    setId(InternalProcessorId_Aurion2Discord);
                    setName("aurion2discord");
                    setDisplayName("AurionChat to Discord message converter");
                }};

                @Override
                public ProtoProcessor$Internal getProto() {
                    return proto;
                }

                @Override
                public DataNode apply(DataNode aurionMsg) {
                    var event = aurionMsg.asObject().convert(AurionChat.Message.class);
                    Constraint.notNull(event, "event").run();

                    var component = gson().deserialize(event.getMessage());

                    return DiscordAdapter.Message.builder()
                            .content(Util.componentString(component)
                                    .replaceAll("[§&]\\w", ""))
                            .build();
                }
            },
            new IDataProcessor() {
                final ProtoProcessor$Internal proto = new ProtoProcessor$Internal() {{
                    setId(InternalProcessorId_Discord2Aurion);
                    setName("discord2aurion");
                    setDisplayName("Discord to AurionChat message converter");
                }};

                @Override
                public ProtoProcessor$Internal getProto() {
                    return proto;
                }

                @Override
                public DataNode apply(DataNode dcMsg) {
                    var message = dcMsg.asObject().convert(DiscordAdapter.Message.class);
                    Constraint.notNull(message, "message").run();
                    var author = message.getAuthor();
                    Constraint.notNull(author, "message.author").run();

                    var str = message.getContent() + message.getAttachmentUrls().stream()
                            .collect(Collectors.joining(" ", " ", ""))
                            .trim();
                    var base = text("DISCORD ", TextColor.color(86, 98, 246));
                    //base.clickEvent(ClickEvent.openUrl()); todo: add meta config for discord invite url
                    var color = author.getColor();
                    var component = base
                            .append(text(EmojiUtils.removeAllEmojis(author.getEffectiveName()).trim(),
                                    TextColor.color(color != null ? color.getRGB() : 0xFF_FF_FF)))
                            .append(text(": " + str, TextColor.color(0xFF_FF_FF)));

                    return AurionChat.Message.builder()
                            .type("chat")
                            .source("discord")
                            .channel("global")
                            .message(gson().serializeToTree(component).toString())
                            .build();
                }
            }
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
        for (var idp : internalProcessorCache) {
            var added = new HashSet<ProtoProcessor$Internal>();
            var proto = idp.getProto();
            if (processorRepo_$$.findByName(proto.getName()).isEmpty()) {
                processorRepo_$$.save(proto);
                added.add(proto);
            }
            log.info("Added " + added.size() + " new internal processors to DB");
            log.fine(Arrays.toString(added.toArray()));
        }

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

    @Value
    public class DataFlowRunner extends Thread {
        Queue<DataNode> queue = new LinkedBlockingQueue<>();
        DataFlow flow;
        Event.Listener<DataNode> source;
        List<? extends IDataProcessor> processors;
        DataAdapter<?> destination;

        public DataFlowRunner(DataFlow flow) {
            super(mThreadGroup, $toString(flow));
            this.flow = flow;
            this.source = adapterCache.get(flow.getSource())
                    .getSource().listen()
                    .subscribeData(this::push);
            this.processors = flow.getProcessors().stream()
                    .map(proto -> processorCache.getOrDefault(proto, null))
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
