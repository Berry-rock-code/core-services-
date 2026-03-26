package com.berryrock.integrationhub.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Centralized compliance-oriented audit event logging.
 *
 * Part of the audit package — writes structured log lines to a dedicated
 * {@code AUDIT_LOGGER} category so that audit events can be routed separately
 * from application logs in aggregation tools (e.g., Datadog, Elastic, Splunk).
 *
 * The structured format ({@code [AUDIT] EVENT | PRINCIPAL | DETAILS}) is intentionally
 * kept parseable so that future compliance requirements (SOC 2, GLBA) can easily extract
 * these events from the log stream or route them to a persistent store such as a database
 * table or a Kafka topic without changing the call sites.
 *
 * The {@code TODO} comment inside {@link #logEvent} marks the integration point for
 * persistent storage; the current implementation only logs to the console.
 */
@Service
public class AuditService
{
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT_LOGGER");

    /**
     * Records a structured audit event.
     *
     * Writes a log line to the {@code AUDIT_LOGGER} category in the format:
     * {@code [AUDIT] EVENT: <eventType> | PRINCIPAL: <userOrSystemId> | DETAILS: <details>}
     *
     * @param eventType      short token describing what happened (e.g., {@code "PING_CHECK"},
     *                       {@code "PIPELINE_STARTED"})
     * @param userOrSystemId identifier of the actor that triggered the event; use
     *                       {@code "SYSTEM"} for automated pipeline events
     * @param details        free-text description of the event payload or relevant context
     */
    public void logEvent(String eventType, String userOrSystemId, String details)
    {
        // Structured format for easy parsing by log aggregators
        auditLog.info("[AUDIT] EVENT: {} | PRINCIPAL: {} | DETAILS: {}", eventType, userOrSystemId, details);

        // TODO: persist to AuditLog database entity or publish to Kafka topic for compliance storage
    }
}
