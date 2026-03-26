package com.berryrock.integrationhub.model;

/**
 * Describes the cell values to write back into a single Google Sheet row.
 *
 * Part of the address pipeline write-back phase — one instance is created per Loan Tape
 * row that was successfully matched to a Salesforce record. A list of these is passed to
 * {@link com.berryrock.integrationhub.client.GoogleSheetsClient#batchUpdateAddressMatches}
 * which translates each request into a Sheets API {@code ValueRange} and submits them in
 * a single batch call.
 *
 * Fields that remain {@code null} are skipped during the batch update so that pre-existing
 * column values are not overwritten.
 */
public class SheetBatchUpdateRequest
{
    /** 1-based row number in the spreadsheet to update. */
    private int rowNumber;

    /** Salesforce Opportunity ID to write into the SF ID column. */
    private String salesforceId;

    /** Buildium property ID to write into the Buildium ID column. */
    private String buildiumId;

    /** Standardized address from the matched Salesforce record. */
    private String sfStandardizedAddress;

    /** Quality label ({@code CLEAN}, {@code PARTIAL}, or {@code SUSPICIOUS}) from the SF record. */
    private String sfAddressQuality;

    /**
     * Sync status to record in the sheet.
     * Values: {@code SYNCED}, {@code SYNCED_ADDRESS_ONLY}, {@code SYNCED_PARTIAL},
     * or {@code AMBIGUOUS_BUILDIUM_MATCH}.
     */
    private String sfAddressSyncStatus;

    /** Buildium lease ID to write into the Buildium Lease ID column. */
    private String buildiumLeaseId;

    /** Buildium property ID to write into the Buildium Property ID column. */
    private String buildiumPropertyId;

    /** Constructs an empty request. */
    public SheetBatchUpdateRequest()
    {
    }

    /**
     * Returns the 1-based row number to update.
     *
     * @return row number
     */
    public int getRowNumber()
    {
        return rowNumber;
    }

    /**
     * Sets the row number to update.
     *
     * @param rowNumber 1-based row index in the spreadsheet
     */
    public void setRowNumber(int rowNumber)
    {
        this.rowNumber = rowNumber;
    }

    /**
     * Returns the Salesforce Opportunity ID to write.
     *
     * @return Opportunity ID, or {@code null} to skip this column
     */
    public String getSalesforceId()
    {
        return salesforceId;
    }

    /**
     * Sets the Salesforce Opportunity ID.
     *
     * @param salesforceId Opportunity ID from Salesforce
     */
    public void setSalesforceId(String salesforceId)
    {
        this.salesforceId = salesforceId;
    }

    /**
     * Returns the Buildium property ID to write into the Buildium ID column.
     *
     * @return Buildium property ID, or {@code null} to skip this column
     */
    public String getBuildiumId()
    {
        return buildiumId;
    }

    /**
     * Sets the Buildium property ID for the Buildium ID column.
     *
     * @param buildiumId Buildium property ID
     */
    public void setBuildiumId(String buildiumId)
    {
        this.buildiumId = buildiumId;
    }

    /**
     * Returns the Salesforce-standardized address to write.
     *
     * @return standardized address string, or {@code null} to skip this column
     */
    public String getSfStandardizedAddress()
    {
        return sfStandardizedAddress;
    }

    /**
     * Sets the Salesforce-standardized address.
     *
     * @param sfStandardizedAddress standardized address from the matched SF record
     */
    public void setSfStandardizedAddress(String sfStandardizedAddress)
    {
        this.sfStandardizedAddress = sfStandardizedAddress;
    }

    /**
     * Returns the quality label to write.
     *
     * @return quality label, or {@code null} to skip this column
     */
    public String getSfAddressQuality()
    {
        return sfAddressQuality;
    }

    /**
     * Sets the quality label.
     *
     * @param sfAddressQuality quality label assigned by the pipeline
     */
    public void setSfAddressQuality(String sfAddressQuality)
    {
        this.sfAddressQuality = sfAddressQuality;
    }

    /**
     * Returns the sync status to write.
     *
     * @return sync status string, or {@code null} to skip this column
     */
    public String getSfAddressSyncStatus()
    {
        return sfAddressSyncStatus;
    }

    /**
     * Sets the sync status.
     *
     * @param sfAddressSyncStatus sync status label from the pipeline
     */
    public void setSfAddressSyncStatus(String sfAddressSyncStatus)
    {
        this.sfAddressSyncStatus = sfAddressSyncStatus;
    }

    /**
     * Returns the Buildium lease ID to write.
     *
     * @return Buildium lease ID, or {@code null} to skip this column
     */
    public String getBuildiumLeaseId()
    {
        return buildiumLeaseId;
    }

    /**
     * Sets the Buildium lease ID.
     *
     * @param buildiumLeaseId Buildium lease ID from the joined unit record
     */
    public void setBuildiumLeaseId(String buildiumLeaseId)
    {
        this.buildiumLeaseId = buildiumLeaseId;
    }

    /**
     * Returns the Buildium property ID to write into the Buildium Property ID column.
     *
     * @return Buildium property ID, or {@code null} to skip this column
     */
    public String getBuildiumPropertyId()
    {
        return buildiumPropertyId;
    }

    /**
     * Sets the Buildium property ID for the Buildium Property ID column.
     *
     * @param buildiumPropertyId Buildium property ID
     */
    public void setBuildiumPropertyId(String buildiumPropertyId)
    {
        this.buildiumPropertyId = buildiumPropertyId;
    }
}
