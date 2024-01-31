package org.comroid.forwardmq.dto.system;

import lombok.Value;

@Value
public class Config {
    OAuth2Info oAuth;
    DBInfo database;
    String discordToken;
}
