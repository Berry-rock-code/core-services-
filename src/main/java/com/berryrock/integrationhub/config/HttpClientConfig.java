package com.berryrock.integrationhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HttpClientConfig
{
    @Bean
    public WebClient.Builder webClientBuilder()
    {
        return WebClient.builder();
    }

    @Bean
    public WebClient buildiumWebClient(WebClient.Builder builder, BuildiumProperties properties)
    {
        return builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("x-buildium-client-id", properties.getClientId())
                .defaultHeader("x-buildium-client-secret", properties.getClientSecret())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }
}