package com.berryrock.integrationhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Spring configuration for reactive HTTP clients.
 *
 * Part of the config package — defines the {@link WebClient} beans used by the
 * integration clients. Separating HTTP client construction here keeps credential
 * injection (via {@link BuildiumProperties}) away from the client implementation
 * classes themselves, which receive a pre-configured client via constructor injection.
 *
 * The in-memory codec limit is raised to 16 MB on the Buildium client to accommodate
 * large paginated responses from the rentals and leases endpoints.
 */
@Configuration
public class HttpClientConfig
{
    /**
     * Creates a new instance; Spring manages the lifecycle of this configuration class.
     */
    public HttpClientConfig()
    {
    }

    /**
     * Provides a shared {@link WebClient.Builder} that other beans can use as a
     * starting point when they need to create their own configured client instance.
     *
     * @return a default, unconfigured {@link WebClient.Builder}
     */
    @Bean
    public WebClient.Builder webClientBuilder()
    {
        return WebClient.builder();
    }

    /**
     * Creates the pre-configured {@link WebClient} for all calls to the Buildium API.
     *
     * Sets the base URL and injects the Buildium client ID and secret as default headers
     * on every request, eliminating the need for the implementation class to repeat this
     * boilerplate. The codec buffer is expanded to 16 MB to handle full-page responses
     * from the rentals/units endpoints.
     *
     * @param builder    the shared {@link WebClient.Builder} bean
     * @param properties Buildium connection settings, including credentials and base URL
     * @return a configured {@link WebClient} scoped to the Buildium API
     */
    @Bean
    public WebClient buildiumWebClient(WebClient.Builder builder, BuildiumProperties properties)
    {
        return builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("x-buildium-client-id", properties.getClientId())
                .defaultHeader("x-buildium-client-secret", properties.getClientSecret())
                // Raise codec buffer to 16 MB to handle large paginated responses
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }
}
