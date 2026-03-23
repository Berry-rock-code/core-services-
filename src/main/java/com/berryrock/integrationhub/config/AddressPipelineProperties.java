package com.berryrock.integrationhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "address.pipeline")
public class AddressPipelineProperties {
    private boolean enabled;
    private boolean dryRun;
    private String sheetId;
    private String sheetName;
    private int headerRow = 2;
    private Header header = new Header();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public String getSheetId() {
        return sheetId;
    }

    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public int getHeaderRow() {
        return headerRow;
    }

    public void setHeaderRow(int headerRow) {
        this.headerRow = headerRow;
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public static class Header {
        private String address = "Address";
        private String city = "City";
        private String state = "State";
        private String postalCode = "Zip";
        private String salesforceId = "Salesforce ID";
        private String buildiumId = "Buildium ID";
        private String sfStandardizedAddress = "SF Standardized Address";
        private String sfAddressQuality = "SF Address Quality";
        private String sfAddressSyncStatus = "SF Address Sync Status";
        private String buildiumLeaseId = "Buildium lease ID";
        private String buildiumPropertyId = "Buildium Property ID";

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }

        public String getSalesforceId() {
            return salesforceId;
        }

        public void setSalesforceId(String salesforceId) {
            this.salesforceId = salesforceId;
        }

        public String getBuildiumId() {
            return buildiumId;
        }

        public void setBuildiumId(String buildiumId) {
            this.buildiumId = buildiumId;
        }

        public String getSfStandardizedAddress() {
            return sfStandardizedAddress;
        }

        public void setSfStandardizedAddress(String sfStandardizedAddress) {
            this.sfStandardizedAddress = sfStandardizedAddress;
        }

        public String getSfAddressQuality() {
            return sfAddressQuality;
        }

        public void setSfAddressQuality(String sfAddressQuality) {
            this.sfAddressQuality = sfAddressQuality;
        }

        public String getSfAddressSyncStatus() {
            return sfAddressSyncStatus;
        }

        public void setSfAddressSyncStatus(String sfAddressSyncStatus) {
            this.sfAddressSyncStatus = sfAddressSyncStatus;
        }

        public String getBuildiumLeaseId() {
            return buildiumLeaseId;
        }

        public void setBuildiumLeaseId(String buildiumLeaseId) {
            this.buildiumLeaseId = buildiumLeaseId;
        }

        public String getBuildiumPropertyId() {
            return buildiumPropertyId;
        }

        public void setBuildiumPropertyId(String buildiumPropertyId) {
            this.buildiumPropertyId = buildiumPropertyId;
        }
    }
}
