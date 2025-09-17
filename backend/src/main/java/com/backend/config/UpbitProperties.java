package com.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "upbit")
public class UpbitProperties {

    private String baseUrl;
    private String websocketUrl;
    private String accessKey;
    private String secretKey;

}


