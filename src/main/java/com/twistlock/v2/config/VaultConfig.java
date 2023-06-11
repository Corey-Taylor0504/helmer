package com.twistlock.v2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class VaultConfig {
    @Bean
    public RestTemplate getVaultTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate;
    }
}
