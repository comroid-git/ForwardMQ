package org.comroid.forwardmq.dto.system;

import lombok.Value;

@Value
public class OAuth2Info {
    String name;
    String clientId;
    String secret;
    String scope;
    String redirectUrl;
    String agentRedirectUrl;
    String authorizationUrl;
    String tokenUrl;
    String userInfoUrl;
    String userNameAttributeName;
}