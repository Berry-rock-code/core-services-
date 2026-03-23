package com.berryrock.integrationhub.model;

public class SheetBatchUpdateRequest {
    private int rowNumber;
    private String salesforceId;
    private String buildiumId;
    private String sfStandardizedAddress;
    private String sfAddressQuality;
    private String sfAddressSyncStatus;
    private String buildiumLeaseId;
    private String buildiumPropertyId;

    public SheetBatchUpdateRequest() {
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
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
