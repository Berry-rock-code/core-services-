package com.berryrock.integrationhub.model;

public class BuildiumAddressRecord {
    private String buildiumPropertyId;
    private String buildiumUnitId;
    private String rawAddress;
    private String city;
    private String state;
    private String postalCode;
    private String normalizedAddress;
    private String buildiumLeaseId;

    public BuildiumAddressRecord() {
    }

    public String getBuildiumPropertyId() {
        return buildiumPropertyId;
    }

    public void setBuildiumPropertyId(String buildiumPropertyId) {
        this.buildiumPropertyId = buildiumPropertyId;
    }

    public String getBuildiumUnitId() {
        return buildiumUnitId;
    }

    public void setBuildiumUnitId(String buildiumUnitId) {
        this.buildiumUnitId = buildiumUnitId;
    }

    public String getRawAddress() {
        return rawAddress;
    }

    public void setRawAddress(String rawAddress) {
        this.rawAddress = rawAddress;
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

    public String getNormalizedAddress() {
        return normalizedAddress;
    }

    public void setNormalizedAddress(String normalizedAddress) {
        this.normalizedAddress = normalizedAddress;
    }

    public String getBuildiumLeaseId()
    {
        return buildiumLeaseId;
    }

    public void setBuildiumLeaseId(String buildiumLeaseId)
    {
        this.buildiumLeaseId = buildiumLeaseId;
    }
}
