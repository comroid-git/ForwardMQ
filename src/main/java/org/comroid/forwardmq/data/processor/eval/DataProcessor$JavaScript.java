package org.comroid.forwardmq.data.processor.eval;

import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.java.Log;
import org.comroid.api.data.seri.DataNode;
import org.comroid.forwardmq.data.processor.DataProcessor;
import org.comroid.forwardmq.entity.proto.processor.eval.ProtoProcessor$JavaScript;
import org.intellij.lang.annotations.Language;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

@Log
@Value
public class DataProcessor$JavaScript extends DataProcessor<ProtoProcessor$JavaScript> {
    public static final ScriptEngine ENGINE;

    static {
        ENGINE = new ScriptEngineManager().getEngineByName("JavaScript");
    }

    public DataProcessor$JavaScript(ProtoProcessor$JavaScript proto) {
        super(proto);
    }

    @Override
    @SneakyThrows
    public DataNode apply(DataNode data) {
        var bindings = ENGINE.createBindings();
        bindings.put("data", data);

        @Language("JavaScript") var init = """
let json = JSON.parse(data.json().toString());
""";
        var result = ENGINE.eval(init+'\n'+getProto().getScript(), bindings);
        return (DataNode) result;
    }
}
