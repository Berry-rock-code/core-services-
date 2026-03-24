package com.berryrock.integrationhub.service;

import com.berryrock.integrationhub.config.AddressPipelineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class AddressPipelineRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AddressPipelineRunner.class);

    private final AddressPipelineService pipelineService;
    private final AddressPipelineProperties properties;
    private final ApplicationContext context;

    public AddressPipelineRunner(AddressPipelineService pipelineService, AddressPipelineProperties properties, ApplicationContext context) {
        this.pipelineService = pipelineService;
        this.properties = properties;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            log.info("AddressPipelineRunner started. Property address.pipeline.enabled is true.");
            pipelineService.runPipeline();
        } catch (Exception e) {
            log.error("AddressPipelineRunner failed: {}", e.getMessage(), e);
        } finally {
            log.info("AddressPipelineRunner finished.");
            int exitCode = SpringApplication.exit(context, () -> 0);
            System.exit(exitCode);
        }
    }
}
