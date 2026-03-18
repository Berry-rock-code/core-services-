package com.berryrock.integrationhub.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    public void logWorkflowStarted(boolean dryRun) {
        log.info("Workflow started. Dry run: {}", dryRun);
    }

    public void logSalesforceFetchCompleted(int count) {
        log.info("Salesforce fetch completed. Fetched {} records.", count);
    }

    public void logGoogleSheetFetchCompleted(int count) {
        log.info("Google Sheet fetch completed. Fetched {} rows.", count);
    }

    public void logSalesforceToGoogleSheetMatchesComplete(int matchCount, int duplicates) {
        log.info("Salesforce -> Google Sheet matches complete. Matches: {}, Duplicates skipped: {}", matchCount, duplicates);
    }

    public void logGoogleSheetWrite(boolean skipped, int writeCount) {
        if (skipped) {
            log.info("Google Sheet write skipped due to dry-run or disabled sync flag.");
        } else {
            log.info("Google Sheet write complete. Updated {} rows.", writeCount);
        }
    }

    public void logBuildiumFetchCompleted(int count) {
        log.info("Buildium fetch completed. Fetched {} records.", count);
    }

    public void logBuildiumMatchComplete(int matchCount, int duplicates) {
        log.info("Buildium match complete. Matches: {}, Duplicates skipped: {}", matchCount, duplicates);
    }

    public void logWorkflowCompleted(long durationMs, int unmatchedCount) {
        log.info("Workflow completed in {} ms. Unmatched records: {}", durationMs, unmatchedCount);
    }

    public void logWorkflowFailed(String reason, Throwable t) {
        log.error("Workflow failed: {}", reason, t);
    }
}
