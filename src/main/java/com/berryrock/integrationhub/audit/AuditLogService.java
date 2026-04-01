package com.berryrock.integrationhub.audit;
// LAYER: PLATFORM -- stays in integration-hub

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Structured logging service for address pipeline workflow events.
 *
 * Part of the audit package — emits one log line per major pipeline phase so that
 * operators monitoring the application logs can follow a pipeline run end-to-end.
 * Each method corresponds to a distinct step in the workflow (fetch, match, write-back).
 *
 * Unlike {@link AuditService}, this class focuses on operational observability rather
 * than compliance; it logs at INFO level and does not persist records to a database.
 */
@Service
public class AuditLogService
{
    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    /**
     * Logs the start of a pipeline workflow run.
     *
     * @param dryRun {@code true} if the run will not commit any writes to external systems
     */
    public void logWorkflowStarted(boolean dryRun)
    {
        log.info("Workflow started. Dry run: {}", dryRun);
    }

    /**
     * Logs completion of the Salesforce Opportunity fetch phase.
     *
     * @param count number of Opportunity records retrieved (before quality filtering)
     */
    public void logSalesforceFetchCompleted(int count)
    {
        log.info("Salesforce fetch completed. Fetched {} records.", count);
    }

    /**
     * Logs completion of the Google Sheet (Loan Tape) fetch phase.
     *
     * @param count number of data rows read from the sheet
     */
    public void logGoogleSheetFetchCompleted(int count)
    {
        log.info("Google Sheet fetch completed. Fetched {} rows.", count);
    }

    /**
     * Logs completion of the Salesforce-to-sheet address matching phase.
     *
     * @param matchCount  number of Salesforce records successfully matched to sheet rows
     * @param duplicates  number of potential matches skipped because the normalized key
     *                    appeared more than once in either data set
     */
    public void logSalesforceToGoogleSheetMatchesComplete(int matchCount, int duplicates)
    {
        log.info("Salesforce -> Google Sheet matches complete. Matches: {}, Duplicates skipped: {}", matchCount, duplicates);
    }

    /**
     * Logs the result of the Google Sheet write-back step.
     *
     * @param skipped    {@code true} if writes were suppressed by dry-run mode or a disabled sync flag
     * @param writeCount number of rows actually updated in the sheet (0 when skipped)
     */
    public void logGoogleSheetWrite(boolean skipped, int writeCount)
    {
        if (skipped)
        {
            log.info("Google Sheet write skipped due to dry-run or disabled sync flag.");
        }
        else
        {
            log.info("Google Sheet write complete. Updated {} rows.", writeCount);
        }
    }

    /**
     * Logs completion of the Buildium active-lease address fetch phase.
     *
     * @param count number of Buildium address records retrieved
     */
    public void logBuildiumFetchCompleted(int count)
    {
        log.info("Buildium fetch completed. Fetched {} records.", count);
    }

    /**
     * Logs completion of the Buildium address matching phase.
     *
     * @param matchCount number of sheet rows successfully matched to Buildium records
     * @param duplicates number of ambiguous matches skipped due to multiple Buildium
     *                   records sharing the same normalized key
     */
    public void logBuildiumMatchComplete(int matchCount, int duplicates)
    {
        log.info("Buildium match complete. Matches: {}, Duplicates skipped: {}", matchCount, duplicates);
    }

    /**
     * Logs successful completion of the entire pipeline workflow.
     *
     * @param durationMs    total elapsed time in milliseconds
     * @param unmatchedCount number of Salesforce records for which no sheet row was found
     */
    public void logWorkflowCompleted(long durationMs, int unmatchedCount)
    {
        log.info("Workflow completed in {} ms. Unmatched records: {}", durationMs, unmatchedCount);
    }

    /**
     * Logs a fatal workflow failure.
     *
     * @param reason short human-readable description of what went wrong
     * @param t      the exception that caused the failure
     */
    public void logWorkflowFailed(String reason, Throwable t)
    {
        log.error("Workflow failed: {}", reason, t);
    }
}
