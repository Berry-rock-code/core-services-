package com.berryrock.integrationhub.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Root Spring configuration class for the integration hub.
 *
 * Part of the config package — activates {@code @ConfigurationProperties} binding for
 * all vendor and pipeline property classes. Without this annotation, Spring Boot would
 * not register the properties classes as managed beans and injection would fail at
 * startup.
 *
 * Additional cross-cutting {@code @Bean} definitions that do not belong to a specific
 * integration concern can be added here.
 */
@Configuration
@EnableConfigurationProperties({
        BuildiumProperties.class,
        AddressPipelineProperties.class
})
public class IntegrationConfig
{
    /**
     * Creates a new instance; Spring activates {@code @ConfigurationProperties} binding
     * for all registered property classes.
     */
    public IntegrationConfig()
    {
    }
}
