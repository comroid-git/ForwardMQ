package org.comroid.forwardmq.dto;

import lombok.Value;

@Value
public class Config {
    OAuth2Info oAuth;
    DBInfo database;
    String discordToken;
}
