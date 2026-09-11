package com.xiantu.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret = "xiantu-secret-key-please-change-in-production-1234567890";

    private long expirationMs = 86400000L;

}
