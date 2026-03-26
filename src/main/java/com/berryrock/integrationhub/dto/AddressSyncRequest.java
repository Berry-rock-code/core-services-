package com.berryrock.integrationhub.dto;

/**
 * Request body for the address sync REST endpoint.
 *
 * Sent as a JSON body to {@code POST /api/v1/workflows/address-sync/run}. The caller
 * can use this to override runtime behavior — enabling or disabling dry-run mode,
 * choosing which external systems to involve, and optionally pointing the pipeline at
 * a local CSV file instead of the live Google Sheet.
 */
public class AddressSyncRequest
{
    /**
     * When {@code true}, the pipeline performs all matching work but does not write
     * any results back to Google Sheets. Useful for validating match rates without
     * modifying live data.
     */
    private boolean dryRun;

    /**
     * When {@code true}, matched results are written back to the Google Sheet.
     * Has no effect if {@code dryRun} is also {@code true}.
     */
    private boolean syncGoogleSheet;

    /**
     * When {@code true}, Buildium records are fetched and used in the matching phase.
     * Set to {@code false} to skip the Buildium leg of the pipeline.
     */
    private boolean enrichBuildium;

    /**
     * Optional upper bound on how many sheet rows are processed per run.
     * {@code null} means process all rows.
     */
    private Integer batchSize;

    /**
     * Google Sheets spreadsheet ID to use for this run. Overrides the value in
     * {@code address.pipeline.sheet-id} when provided.
     */
    private String sheetId;

    /**
     * Name of the sheet tab within the spreadsheet. Overrides the value in
     * {@code address.pipeline.sheet-name} when provided.
     */
    private String sheetName;

    /**
     * Local file system path to a CSV file used instead of the live Google Sheet.
     * The CSV must have rows in the format {@code rowNumber,address}. Intended for
     * offline testing without live credentials.
     */
    private String csvPath;

    /**
     * Returns whether dry-run mode is active.
     *
     * @return {@code true} if no writes will be committed
     */
    public boolean isDryRun()
    {
        return dryRun;
    }

    /**
     * Sets the dry-run flag.
     *
     * @param dryRun {@code true} to suppress all write-back operations
     */
    public void setDryRun(boolean dryRun)
    {
        this.dryRun = dryRun;
    }

    /**
     * Returns whether matched results should be written back to Google Sheets.
     *
     * @return {@code true} if sheet write-back is enabled
     */
    public boolean isSyncGoogleSheet()
    {
        return syncGoogleSheet;
    }

    /**
     * Sets the Google Sheet sync flag.
     *
     * @param syncGoogleSheet {@code true} to enable write-back to the sheet
     */
    public void setSyncGoogleSheet(boolean syncGoogleSheet)
    {
        this.syncGoogleSheet = syncGoogleSheet;
    }

    /**
     * Returns whether Buildium records should be fetched and used during matching.
     *
     * @return {@code true} if Buildium enrichment is enabled
     */
    public boolean isEnrichBuildium()
    {
        return enrichBuildium;
    }

    /**
     * Sets the Buildium enrichment flag.
     *
     * @param enrichBuildium {@code true} to include Buildium in the matching phase
     */
    public void setEnrichBuildium(boolean enrichBuildium)
    {
        this.enrichBuildium = enrichBuildium;
    }

    /**
     * Returns the optional batch size limit.
     *
     * @return maximum rows to process, or {@code null} for no limit
     */
    public Integer getBatchSize()
    {
        return batchSize;
    }

    /**
     * Sets the batch size limit.
     *
     * @param batchSize maximum number of sheet rows to process; {@code null} for no limit
     */
    public void setBatchSize(Integer batchSize)
    {
        this.batchSize = batchSize;
    }

    /**
     * Returns the spreadsheet ID override.
     *
     * @return spreadsheet ID, or {@code null} to use the configured default
     */
    public String getSheetId()
    {
        return sheetId;
    }

    /**
     * Sets the spreadsheet ID override.
     *
     * @param sheetId Google Sheets spreadsheet ID
     */
    public void setSheetId(String sheetId)
    {
        this.sheetId = sheetId;
    }

    /**
     * Returns the sheet tab name override.
     *
     * @return tab name, or {@code null} to use the configured default
     */
    public String getSheetName()
    {
        return sheetName;
    }

    /**
     * Sets the sheet tab name override.
     *
     * @param sheetName name of the sheet tab within the spreadsheet
     */
    public void setSheetName(String sheetName)
    {
        this.sheetName = sheetName;
    }

    /**
     * Returns the local CSV path used in place of the live Google Sheet.
     *
     * @return absolute file path, or {@code null} to use the live sheet
     */
    public String getCsvPath()
    {
        return csvPath;
    }

    /**
     * Sets the local CSV path.
     *
     * @param csvPath absolute path to a {@code rowNumber,address} CSV file
     */
    public void setCsvPath(String csvPath)
    {
        this.csvPath = csvPath;
    }
}
