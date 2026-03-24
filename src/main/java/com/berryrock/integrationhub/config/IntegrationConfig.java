package com.berryrock.integrationhub.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        BuildiumProperties.class,
        AddressPipelineProperties.class
})
public class IntegrationConfig
{
}