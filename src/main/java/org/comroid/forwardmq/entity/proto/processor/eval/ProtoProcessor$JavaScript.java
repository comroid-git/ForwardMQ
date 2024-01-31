package org.comroid.forwardmq.entity.proto.processor.eval;

import jakarta.persistence.Basic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.comroid.forwardmq.entity.proto.processor.ProtoProcessor;
import org.intellij.lang.annotations.Language;

@Log
@Getter
@NoArgsConstructor
//@AllArgsConstructor
@jakarta.persistence.Entity
public class ProtoProcessor$JavaScript extends ProtoProcessor {
    @Basic String script;

    public ProtoProcessor$JavaScript(@Language(value = "JavaScript",prefix = "function(data,json){\n",suffix = "\n}") String script) {
        this.script = script;
    }
}
