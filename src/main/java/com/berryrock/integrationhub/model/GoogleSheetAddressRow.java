package com.berryrock.integrationhub.model;

/**
 * Represents one data row from the Loan Tape Google Sheet.
 *
 * Part of the address pipeline — rows are fetched by
 * {@link com.berryrock.integrationhub.client.GoogleSheetsClientImpl#fetchAddressRows} and
 * matched against Salesforce Opportunity records and Buildium unit records during pipeline
 * execution.
 *
 * The column names that map to each field are configured in
 * {@link com.berryrock.integrationhub.config.AddressPipelineProperties.Header}. The
 * pipeline reads these headers dynamically at runtime so the sheet layout can change
 * without a code deployment.
 *
 * After matching, the pipeline writes Salesforce and Buildium IDs plus quality and sync
 * status back into the sheet via a batch update using {@link SheetBatchUpdateRequest}.
 */
public class GoogleSheetAddressRow
{
    /** 1-based row index within the spreadsheet, used to construct A1-notation ranges for write-back. */
    private int rowNumber;

    /** Street address as it appears in the sheet's address column. May be a combined "123 Main St, City, ST 12345" string. */
    private String address;

    /** City component. May be blank if the sheet stores a combined address in the address column. */
    private String city;

    /** State code component. */
    private String state;

    /** ZIP or postal code component. */
    private String postalCode;

    /** Salesforce Opportunity ID written back by the pipeline after a successful SF match. */
    private String salesforceId;

    /** Buildium property ID written back by the pipeline after a successful Buildium match. */
    private String buildiumId;

    /**
     * Normalized composite key built during pipeline processing.
     * Not a sheet column; used internally for matching.
     */
    private String normalizedAddress;

    /** Standardized address string from the matched Salesforce record, written back to the sheet. */
    private String sfStandardizedAddress;

    /** Quality label ({@code CLEAN}, {@code PARTIAL}, or {@code SUSPICIOUS}) written back from the SF record. */
    private String sfAddressQuality;

    /** Sync status written back after matching ({@code SYNCED}, {@code SYNCED_ADDRESS_ONLY}, {@code SYNCED_PARTIAL}, {@code AMBIGUOUS_BUILDIUM_MATCH}). */
    private String sfAddressSyncStatus;

    /** Buildium lease ID written back by the pipeline. */
    private String buildiumLeaseId;

    /** Buildium property ID (duplicate of {@code buildiumId}; kept for sheet column compatibility). */
    private String buildiumPropertyId;

    /** Constructs an empty row. */
    public GoogleSheetAddressRow()
    {
    }

    /**
     * Returns the 1-based row number within the spreadsheet.
     *
     * @return row number
     */
    public int getRowNumber()
    {
        return rowNumber;
    }

    /**
     * Sets the row number.
     *
     * @param rowNumber 1-based row index in the spreadsheet
     */
    public void setRowNumber(int rowNumber)
    {
        this.rowNumber = rowNumber;
    }

    /**
     * Returns the raw address string from the sheet.
     *
     * @return address string, possibly a combined one-line address
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * Sets the raw address string.
     *
     * @param address address as it appears in the sheet
     */
    public void setAddress(String address)
    {
        this.address = address;
    }

    /**
     * Returns the city component from the sheet.
     *
     * @return city name, or {@code null} if the column is blank
     */
    public String getCity()
    {
        return city;
    }

    /**
     * Sets the city component.
     *
     * @param city city name
     */
    public void setCity(String city)
    {
        this.city = city;
    }

    /**
     * Returns the state component from the sheet.
     *
     * @return state code, or {@code null} if the column is blank
     */
    public String getState()
    {
        return state;
    }

    /**
     * Sets the state component.
     *
     * @param state two-letter state code
     */
    public void setState(String state)
    {
        this.state = state;
    }

    /**
     * Returns the postal code from the sheet.
     *
     * @return ZIP or postal code, or {@code null} if the column is blank
     */
    public String getPostalCode()
    {
        return postalCode;
    }

    /**
     * Sets the postal code.
     *
     * @param postalCode ZIP or postal code
     */
    public void setPostalCode(String postalCode)
    {
        this.postalCode = postalCode;
    }

    /**
     * Returns the Salesforce Opportunity ID stored in this row.
     *
     * @return Opportunity ID, or {@code null} if the column is blank or not yet matched
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
     * Returns the Buildium property ID stored in this row.
     *
     * @return Buildium property ID, or {@code null} if not yet matched
     */
    public String getBuildiumId()
    {
        return buildiumId;
    }

    /**
     * Sets the Buildium property ID.
     *
     * @param buildiumId Buildium property ID
     */
    public void setBuildiumId(String buildiumId)
    {
        this.buildiumId = buildiumId;
    }

    /**
     * Returns the normalized composite key used internally for matching.
     *
     * @return normalized key, or {@code null} if not yet populated
     */
    public String getNormalizedAddress()
    {
        return normalizedAddress;
    }

    /**
     * Sets the normalized composite key.
     *
     * @param normalizedAddress key produced by the pipeline's normalization step
     */
    public void setNormalizedAddress(String normalizedAddress)
    {
        this.normalizedAddress = normalizedAddress;
    }

    /**
     * Returns the standardized address string from the matched Salesforce record.
     *
     * @return SF-standardized address, or {@code null} if not yet matched
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
     * Returns the quality label assigned by the pipeline.
     *
     * @return {@code "CLEAN"}, {@code "PARTIAL"}, or {@code "SUSPICIOUS"}, or {@code null}
     */
    public String getSfAddressQuality()
    {
        return sfAddressQuality;
    }

    /**
     * Sets the quality label.
     *
     * @param sfAddressQuality quality label from the pipeline
     */
    public void setSfAddressQuality(String sfAddressQuality)
    {
        this.sfAddressQuality = sfAddressQuality;
    }

    /**
     * Returns the sync status written back by the pipeline.
     *
     * @return one of {@code SYNCED}, {@code SYNCED_ADDRESS_ONLY}, {@code SYNCED_PARTIAL},
     *         {@code AMBIGUOUS_BUILDIUM_MATCH}, or {@code null}
     */
    public String getSfAddressSyncStatus()
    {
        return sfAddressSyncStatus;
    }

    /**
     * Sets the sync status.
     *
     * @param sfAddressSyncStatus sync status label
     */
    public void setSfAddressSyncStatus(String sfAddressSyncStatus)
    {
        this.sfAddressSyncStatus = sfAddressSyncStatus;
    }

    /**
     * Returns the Buildium lease ID stored in this row.
     *
     * @return Buildium lease ID, or {@code null} if not yet matched
     */
    public String getBuildiumLeaseId()
    {
        return buildiumLeaseId;
    }

    /**
     * Sets the Buildium lease ID.
     *
     * @param buildiumLeaseId Buildium lease ID
     */
    public void setBuildiumLeaseId(String buildiumLeaseId)
    {
        this.buildiumLeaseId = buildiumLeaseId;
    }

    /**
     * Returns the Buildium property ID (same value as {@link #getBuildiumId()} in the current
     * pipeline; kept as a separate column for sheet compatibility).
     *
     * @return Buildium property ID, or {@code null} if not yet matched
     */
    public String getBuildiumPropertyId()
    {
        return buildiumPropertyId;
    }

    /**
     * Sets the Buildium property ID.
     *
     * @param buildiumPropertyId Buildium property ID
     */
    public void setBuildiumPropertyId(String buildiumPropertyId)
    {
        this.buildiumPropertyId = buildiumPropertyId;
    }
}
