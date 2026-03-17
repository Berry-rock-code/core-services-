package com.berryrock.integrationhub.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * A centralized Audit Logging foundation.
 * <p>
 * Future compliance features (SOC2, GLBA) can plug in here to push
 * security, data-access, and lifecycle events to persistent storage (e.g., DataDog, Elastic, DB).
 * <p>
 * Ensures business logic doesn't pollute the controllers with print statements.
 */
@Service
public class AuditService {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT_LOGGER");

    public void logEvent(String eventType, String userOrSystemId, String details) {
        // Structuring the log output to be easily parsable by log aggregators
        auditLog.info("[AUDIT] EVENT: {} | PRINCIPAL: {} | DETAILS: {}", eventType, userOrSystemId, details);
        
        // TODO: In the future, this can save to an `AuditLog` database entity or publish to a Kafka topic.
    }
}
