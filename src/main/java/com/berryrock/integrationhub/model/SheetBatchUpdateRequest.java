package com.berryrock.integrationhub.model;

public class SheetBatchUpdateRequest {
    private int rowNumber;
    private String salesforceId;
    private String buildiumId;

    public SheetBatchUpdateRequest() {
    }

    public SheetBatchUpdateRequest(int rowNumber, String salesforceId, String buildiumId) {
        this.rowNumber = rowNumber;
        this.salesforceId = salesforceId;
        this.buildiumId = buildiumId;
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
}
