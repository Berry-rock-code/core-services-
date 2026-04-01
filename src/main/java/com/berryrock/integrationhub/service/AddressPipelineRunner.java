package com.berryrock.integrationhub.service;
// LAYER: FEATURE:address-pipeline -- moves to address-pipeline repo

import com.berryrock.integrationhub.config.AddressPipelineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Startup hook that triggers the address pipeline and shuts down the application cleanly.
 *
 * Part of the service layer — implements {@link CommandLineRunner} so Spring Boot invokes
 * {@link #run} immediately after the application context is fully initialized. If
 * {@code address.pipeline.enabled} is {@code false}, the runner exits without doing
 * anything, which allows the same JAR to run as either a batch job or a long-lived server
 * depending on configuration.
 *
 * After the pipeline completes (or fails), the runner calls
 * {@link SpringApplication#exit(ApplicationContext, org.springframework.boot.ExitCodeGenerator)}
 * to shut the JVM down cleanly. {@code System.exit()} is intentionally avoided because it
 * kills the Surefire forked JVM during test runs before the test framework can collect results.
 */
@Component
public class AddressPipelineRunner implements CommandLineRunner
{
    private static final Logger log = LoggerFactory.getLogger(AddressPipelineRunner.class);

    private final AddressPipelineService pipelineService;
    private final AddressPipelineProperties properties;
    private final ApplicationContext context;

    /**
     * Constructs the runner with its required dependencies.
     *
     * @param pipelineService the pipeline orchestrator to invoke on startup
     * @param properties      pipeline configuration, used to check the enabled flag
     * @param context         Spring application context, used for clean shutdown
     */
    public AddressPipelineRunner(AddressPipelineService pipelineService,
                                 AddressPipelineProperties properties,
                                 ApplicationContext context)
    {
        this.pipelineService = pipelineService;
        this.properties = properties;
        this.context = context;
    }

    /**
     * Called by Spring Boot immediately after context initialization.
     *
     * Checks the {@code address.pipeline.enabled} flag; if disabled, returns without
     * doing anything. If enabled, invokes the full pipeline and then initiates a clean
     * shutdown regardless of whether the pipeline succeeded or failed.
     *
     * @param args command-line arguments passed through from the main method; unused
     * @throws Exception if an unexpected error occurs during startup (caught internally)
     */
    @Override
    public void run(String... args) throws Exception
    {
        if (!properties.isEnabled())
        {
            // Pipeline is disabled; leave the application running as a server
            return;
        }

        try
        {
            log.info("AddressPipelineRunner started. Property address.pipeline.enabled is true.");
            pipelineService.runPipeline();
        }
        catch (Exception e)
        {
            log.error("AddressPipelineRunner failed: {}", e.getMessage(), e);
        }
        finally
        {
            log.info("AddressPipelineRunner finished.");

            // Use SpringApplication.exit() rather than System.exit() to allow the
            // Surefire test runner's forked JVM to shut down cleanly after test execution
            SpringApplication.exit(context, () -> 0);
        }
    }
}
