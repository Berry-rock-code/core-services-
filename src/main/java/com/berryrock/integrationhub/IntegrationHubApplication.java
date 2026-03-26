package com.berryrock.integrationhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Integration Hub service.
 *
 * This Spring Boot application wires together the three external systems —
 * Salesforce, Google Sheets (Loan Tape), and Buildium — and exposes REST
 * endpoints that allow the address pipeline to be triggered on demand.
 *
 * When the address pipeline is enabled via {@code address.pipeline.enabled=true},
 * {@link com.berryrock.integrationhub.service.AddressPipelineRunner} will execute
 * the full sync on startup and shut down the process cleanly upon completion.
 */
@SpringBootApplication
public class IntegrationHubApplication
{
    /**
     * Bootstraps the Spring application context and starts the embedded server.
     *
     * @param args command-line arguments passed through to Spring Boot
     */
    public static void main(String[] args)
    {
        SpringApplication.run(IntegrationHubApplication.class, args);
    }
}
