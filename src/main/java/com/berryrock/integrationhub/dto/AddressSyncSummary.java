package com.berryrock.integrationhub.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary of the results returned after a sync run completes.
 *
 * Returned as the response body of {@code POST /api/v1/workflows/address-sync/run}.
 * Contains record counts for each fetch and match phase, plus a list of human-readable
 * warnings describing data quality issues or partial matches encountered during the run.
 */
public class AddressSyncSummary
{
    /**
     * Creates a new instance with all counters at zero and an empty warnings list.
     */
    public AddressSyncSummary()
    {
    }

    /**
     * Overall status of the sync operation.
     * Set to {@code "SUCCESS"} when the pipeline completes without a fatal error.
     */
    private String status;

    /** Number of Opportunity records fetched from Salesforce before quality filtering. */
    private int salesforceRecordsFetched;

    /** Number of rows read from the Loan Tape Google Sheet (or local CSV). */
    private int googleSheetRowsFetched;

    /** Number of active-lease address records fetched from Buildium. */
    private int buildiumRecordsFetched;

    /** Number of Salesforce records that were successfully matched to a sheet row. */
    private int googleSheetMatches;

    /** Number of sheet rows that were also matched to a Buildium record. */
    private int buildiumMatches;

    /** Number of Salesforce records for which no matching sheet row was found. */
    private int unmatchedCount;

    /**
     * Human-readable warnings generated during the run.
     * Examples: partial address quality skips, ambiguous Buildium matches, duplicate keys.
     */
    private List<String> warnings = new ArrayList<>();

    /**
     * Returns the overall status string.
     *
     * @return {@code "SUCCESS"} or an error label
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * Sets the overall status.
     *
     * @param status status label
     */
    public void setStatus(String status)
    {
        this.status = status;
    }

    /**
     * Returns the number of Salesforce records fetched.
     *
     * @return count of Opportunity records before quality filtering
     */
    public int getSalesforceRecordsFetched()
    {
        return salesforceRecordsFetched;
    }

    /**
     * Sets the Salesforce fetch count.
     *
     * @param salesforceRecordsFetched number of Opportunity records fetched
     */
    public void setSalesforceRecordsFetched(int salesforceRecordsFetched)
    {
        this.salesforceRecordsFetched = salesforceRecordsFetched;
    }

    /**
     * Returns the number of Loan Tape rows read from Google Sheets (or CSV).
     *
     * @return row count
     */
    public int getGoogleSheetRowsFetched()
    {
        return googleSheetRowsFetched;
    }

    /**
     * Sets the Google Sheet row count.
     *
     * @param googleSheetRowsFetched number of rows read from the sheet
     */
    public void setGoogleSheetRowsFetched(int googleSheetRowsFetched)
    {
        this.googleSheetRowsFetched = googleSheetRowsFetched;
    }

    /**
     * Returns the number of Buildium active-lease records fetched.
     *
     * @return count of Buildium records; 0 if Buildium enrichment was disabled
     */
    public int getBuildiumRecordsFetched()
    {
        return buildiumRecordsFetched;
    }

    /**
     * Sets the Buildium fetch count.
     *
     * @param buildiumRecordsFetched number of active-lease records fetched from Buildium
     */
    public void setBuildiumRecordsFetched(int buildiumRecordsFetched)
    {
        this.buildiumRecordsFetched = buildiumRecordsFetched;
    }

    /**
     * Returns the number of Salesforce records matched to sheet rows.
     *
     * @return SF-to-sheet match count
     */
    public int getGoogleSheetMatches()
    {
        return googleSheetMatches;
    }

    /**
     * Sets the SF-to-sheet match count.
     *
     * @param googleSheetMatches number of successful SF-to-sheet matches
     */
    public void setGoogleSheetMatches(int googleSheetMatches)
    {
        this.googleSheetMatches = googleSheetMatches;
    }

    /**
     * Returns the number of sheet rows also matched to a Buildium record.
     *
     * @return sheet-to-Buildium match count
     */
    public int getBuildiumMatches()
    {
        return buildiumMatches;
    }

    /**
     * Sets the sheet-to-Buildium match count.
     *
     * @param buildiumMatches number of sheet rows successfully matched to Buildium
     */
    public void setBuildiumMatches(int buildiumMatches)
    {
        this.buildiumMatches = buildiumMatches;
    }

    /**
     * Returns the number of Salesforce records for which no sheet row was found.
     *
     * @return unmatched SF record count
     */
    public int getUnmatchedCount()
    {
        return unmatchedCount;
    }

    /**
     * Sets the unmatched count.
     *
     * @param unmatchedCount number of SF records with no matching sheet row
     */
    public void setUnmatchedCount(int unmatchedCount)
    {
        this.unmatchedCount = unmatchedCount;
    }

    /**
     * Returns the list of warnings generated during the run.
     *
     * @return mutable list of warning strings; never {@code null}
     */
    public List<String> getWarnings()
    {
        return warnings;
    }

    /**
     * Replaces the warnings list.
     *
     * @param warnings list of warning messages
     */
    public void setWarnings(List<String> warnings)
    {
        this.warnings = warnings;
    }
}
