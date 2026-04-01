package com.berryrock.integrationhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration properties for the address pipeline.
 *
 * Part of the config package — binds the {@code address.pipeline.*} prefix from
 * {@code application.yml} to strongly typed fields. Registered via
 * {@link IntegrationConfig}.
 *
 * The nested {@link Header} class maps column header names configured in
 * {@code address.pipeline.header.*} to the physical column labels used in the Loan Tape
 * Google Sheet. This allows the sheet layout to evolve without code changes — only the
 * YAML needs to be updated.
 */
@ConfigurationProperties(prefix = "address.pipeline")
public class AddressPipelineProperties
{
    /**
     * Creates a new instance; field values are populated by Spring Boot from the
     * {@code address.pipeline.*} configuration prefix.
     */
    public AddressPipelineProperties()
    {
    }

    /**
     * Master on/off switch for the pipeline.
     * When {@code false}, the pipeline runner exits immediately without making any
     * external calls.
     */
    private boolean enabled;

    /**
     * When {@code true}, all matching work runs normally but no updates are written
     * back to Google Sheets. The top 10 proposed updates are logged instead.
     */
    private boolean dryRun;

    /**
     * Google Sheets spreadsheet ID for the Loan Tape.
     * Resolved from {@code GOOGLE_SHEETS_SPREADSHEET_ID}.
     */
    private String sheetId;

    /**
     * Name of the sheet tab within the spreadsheet.
     * Resolved from {@code GOOGLE_SHEETS_SHEET_NAME}.
     */
    private String sheetName;

    /**
     * 1-based row number of the header row in the sheet.
     * Rows before this index are ignored during fetch. Defaults to {@code 2}.
     */
    private int headerRow = 2;

    /** Column header label mappings for the Loan Tape sheet. */
    private Header header = new Header();

    /**
     * Returns whether the pipeline is enabled.
     *
     * @return {@code true} if the pipeline will run on startup
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Sets the pipeline enabled flag.
     *
     * @param enabled {@code true} to allow the pipeline to run
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Returns whether dry-run mode is active.
     *
     * @return {@code true} if write-back to Google Sheets is suppressed
     */
    public boolean isDryRun()
    {
        return dryRun;
    }

    /**
     * Sets the dry-run flag.
     *
     * @param dryRun {@code true} to suppress Google Sheets write-back
     */
    public void setDryRun(boolean dryRun)
    {
        this.dryRun = dryRun;
    }

    /**
     * Returns the Google Sheets spreadsheet ID.
     *
     * @return spreadsheet ID
     */
    public String getSheetId()
    {
        return sheetId;
    }

    /**
     * Sets the spreadsheet ID.
     *
     * @param sheetId Google Sheets spreadsheet ID
     */
    public void setSheetId(String sheetId)
    {
        this.sheetId = sheetId;
    }

    /**
     * Returns the sheet tab name.
     *
     * @return sheet tab name
     */
    public String getSheetName()
    {
        return sheetName;
    }

    /**
     * Sets the sheet tab name.
     *
     * @param sheetName name of the sheet tab within the spreadsheet
     */
    public void setSheetName(String sheetName)
    {
        this.sheetName = sheetName;
    }

    /**
     * Returns the 1-based index of the header row.
     *
     * @return header row number (default 2)
     */
    public int getHeaderRow()
    {
        return headerRow;
    }

    /**
     * Sets the header row index.
     *
     * @param headerRow 1-based row index of the header row
     */
    public void setHeaderRow(int headerRow)
    {
        this.headerRow = headerRow;
    }

    /**
     * Returns the column header label mappings.
     *
     * @return {@link Header} instance with column name configuration
     */
    public Header getHeader()
    {
        return header;
    }

    /**
     * Sets the column header label mappings.
     *
     * @param header column name configuration object
     */
    public void setHeader(Header header)
    {
        this.header = header;
    }

    /**
     * Maps logical field names to the physical column header labels used in the sheet.
     *
     * Each field in this class holds the exact text that appears in the header row of the
     * Loan Tape for the corresponding column. Defaults match the Berry Rock sheet layout
     * but can be overridden in {@code application.yml} or via environment variables without
     * redeploying the service.
     */
    public static class Header
    {
        /**
         * Creates a new instance with all column label fields set to their default values.
         */
        public Header()
        {
        }

        /** Header label for the street address column. Default: {@code "Address"}. */
        private String address = "Address";

        /** Header label for the city column. Default: {@code "City"}. */
        private String city = "City";

        /** Header label for the state column. Default: {@code "State"}. */
        private String state = "State";

        /** Header label for the ZIP/postal code column. Default: {@code "Zip"}. */
        private String postalCode = "Zip";

        /** Header label for the Salesforce ID column. Default: {@code "Salesforce ID"}. */
        private String salesforceId = "Salesforce ID";

        /** Header label for the Buildium ID column. Default: {@code "Buildium ID"}. */
        private String buildiumId = "Buildium ID";

        /** Header label for the SF standardized address column. Default: {@code "SF Standardized Address"}. */
        private String sfStandardizedAddress = "SF Standardized Address";

        /** Header label for the SF address quality column. Default: {@code "SF Address Quality"}. */
        private String sfAddressQuality = "SF Address Quality";

        /** Header label for the SF address sync status column. Default: {@code "SF Address Sync Status"}. */
        private String sfAddressSyncStatus = "SF Address Sync Status";

        /** Header label for the Buildium lease ID column. Default: {@code "Buildium lease ID"}. */
        private String buildiumLeaseId = "Buildium lease ID";

        /** Header label for the Buildium property ID column. Default: {@code "Buildium Property ID"}. */
        private String buildiumPropertyId = "Buildium Property ID";

        /**
         * Returns the header label for the address column.
         *
         * @return column header text
         */
        public String getAddress()
        {
            return address;
        }

        /**
         * Sets the header label for the address column.
         *
         * @param address column header text
         */
        public void setAddress(String address)
        {
            this.address = address;
        }

        /**
         * Returns the header label for the city column.
         *
         * @return column header text
         */
        public String getCity()
        {
            return city;
        }

        /**
         * Sets the header label for the city column.
         *
         * @param city column header text
         */
        public void setCity(String city)
        {
            this.city = city;
        }

        /**
         * Returns the header label for the state column.
         *
         * @return column header text
         */
        public String getState()
        {
            return state;
        }

        /**
         * Sets the header label for the state column.
         *
         * @param state column header text
         */
        public void setState(String state)
        {
            this.state = state;
        }

        /**
         * Returns the header label for the ZIP/postal code column.
         *
         * @return column header text
         */
        public String getPostalCode()
        {
            return postalCode;
        }

        /**
         * Sets the header label for the ZIP/postal code column.
         *
         * @param postalCode column header text
         */
        public void setPostalCode(String postalCode)
        {
            this.postalCode = postalCode;
        }

        /**
         * Returns the header label for the Salesforce ID column.
         *
         * @return column header text
         */
        public String getSalesforceId()
        {
            return salesforceId;
        }

        /**
         * Sets the header label for the Salesforce ID column.
         *
         * @param salesforceId column header text
         */
        public void setSalesforceId(String salesforceId)
        {
            this.salesforceId = salesforceId;
        }

        /**
         * Returns the header label for the Buildium ID column.
         *
         * @return column header text
         */
        public String getBuildiumId()
        {
            return buildiumId;
        }

        /**
         * Sets the header label for the Buildium ID column.
         *
         * @param buildiumId column header text
         */
        public void setBuildiumId(String buildiumId)
        {
            this.buildiumId = buildiumId;
        }

        /**
         * Returns the header label for the SF standardized address column.
         *
         * @return column header text
         */
        public String getSfStandardizedAddress()
        {
            return sfStandardizedAddress;
        }

        /**
         * Sets the header label for the SF standardized address column.
         *
         * @param sfStandardizedAddress column header text
         */
        public void setSfStandardizedAddress(String sfStandardizedAddress)
        {
            this.sfStandardizedAddress = sfStandardizedAddress;
        }

        /**
         * Returns the header label for the SF address quality column.
         *
         * @return column header text
         */
        public String getSfAddressQuality()
        {
            return sfAddressQuality;
        }

        /**
         * Sets the header label for the SF address quality column.
         *
         * @param sfAddressQuality column header text
         */
        public void setSfAddressQuality(String sfAddressQuality)
        {
            this.sfAddressQuality = sfAddressQuality;
        }

        /**
         * Returns the header label for the SF address sync status column.
         *
         * @return column header text
         */
        public String getSfAddressSyncStatus()
        {
            return sfAddressSyncStatus;
        }

        /**
         * Sets the header label for the SF address sync status column.
         *
         * @param sfAddressSyncStatus column header text
         */
        public void setSfAddressSyncStatus(String sfAddressSyncStatus)
        {
            this.sfAddressSyncStatus = sfAddressSyncStatus;
        }

        /**
         * Returns the header label for the Buildium lease ID column.
         *
         * @return column header text
         */
        public String getBuildiumLeaseId()
        {
            return buildiumLeaseId;
        }

        /**
         * Sets the header label for the Buildium lease ID column.
         *
         * @param buildiumLeaseId column header text
         */
        public void setBuildiumLeaseId(String buildiumLeaseId)
        {
            this.buildiumLeaseId = buildiumLeaseId;
        }

        /**
         * Returns the header label for the Buildium property ID column.
         *
         * @return column header text
         */
        public String getBuildiumPropertyId()
        {
            return buildiumPropertyId;
        }

        /**
         * Sets the header label for the Buildium property ID column.
         *
         * @param buildiumPropertyId column header text
         */
        public void setBuildiumPropertyId(String buildiumPropertyId)
        {
            this.buildiumPropertyId = buildiumPropertyId;
        }
    }
}
