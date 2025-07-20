// src/main/java/com/bauhaus/livingbrushbackendapi/config/RestTemplateConfig.java
package com.bauhaus.livingbrushbackendapi.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    @Qualifier("metaRestTemplate")
    public RestTemplate metaRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    @Qualifier("googleRestTemplate")
    public RestTemplate googleRestTemplate() {
        return new RestTemplate();
    }
}