package com.teixeirah.withdrawals.infrastructure.secondary.external;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        // You can customize this later with timeouts, interceptors, etc. if needed
        return new RestTemplate();
    }
}