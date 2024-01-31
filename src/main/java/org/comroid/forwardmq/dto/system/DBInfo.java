package org.comroid.forwardmq.dto.system;

import lombok.Value;

@Value
public class DBInfo {
    String url;
    String username;
    String password;
}
