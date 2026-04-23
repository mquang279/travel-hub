package edu.uet.travel_hub.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AiClientConfig {
    @Bean
    WebClient aiWebClient(@Value("${ai.base-url:http://localhost:8888}") String aiBaseUrl) {
        return WebClient.builder()
                .baseUrl(aiBaseUrl)
                .build();
    }
}
