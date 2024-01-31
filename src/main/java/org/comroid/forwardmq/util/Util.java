package org.comroid.forwardmq.util;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.stream.Collectors;

@UtilityClass
public class Util {
    public static String componentString(Component comp) {
        if (comp instanceof TextComponent text)
            return text.content() + comp.children().stream()
                    .map(Util::componentString)
                    .collect(Collectors.joining());
        return comp.toString();
    }
}
