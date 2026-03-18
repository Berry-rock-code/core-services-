package com.berryrock.integrationhub.dto;

public class AddressSyncRequest {
    private boolean dryRun;
    private boolean syncGoogleSheet;
    private boolean enrichBuildium;
    private Integer batchSize;
    private String sheetId;
    private String sheetName;

    public AddressSyncRequest() {
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isSyncGoogleSheet() {
        return syncGoogleSheet;
    }

    public void setSyncGoogleSheet(boolean syncGoogleSheet) {
        this.syncGoogleSheet = syncGoogleSheet;
    }

    public boolean isEnrichBuildium() {
        return enrichBuildium;
    }

    public void setEnrichBuildium(boolean enrichBuildium) {
        this.enrichBuildium = enrichBuildium;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
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
}
