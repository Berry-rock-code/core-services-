package com.berryrock.integrationhub.model;

public class GoogleSheetAddressRow {
    private int rowNumber;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String salesforceId;
    private String buildiumId;
    private String normalizedAddress;
    private String sfStandardizedAddress;
    private String sfAddressQuality;
    private String sfAddressSyncStatus;
    private String buildiumLeaseId;
    private String buildiumPropertyId;

    public GoogleSheetAddressRow() {
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

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

    public String getNormalizedAddress() {
        return normalizedAddress;
    }

    public void setNormalizedAddress(String normalizedAddress) {
        this.normalizedAddress = normalizedAddress;
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
