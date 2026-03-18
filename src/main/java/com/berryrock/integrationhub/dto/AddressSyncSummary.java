package com.berryrock.integrationhub.dto;

import java.util.ArrayList;
import java.util.List;

public class AddressSyncSummary {
    private String status;
    private int salesforceRecordsFetched;
    private int googleSheetRowsFetched;
    private int buildiumRecordsFetched;
    private int googleSheetMatches;
    private int buildiumMatches;
    private int unmatchedCount;
    private List<String> warnings = new ArrayList<>();

    public AddressSyncSummary() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getSalesforceRecordsFetched() {
        return salesforceRecordsFetched;
    }

    public void setSalesforceRecordsFetched(int salesforceRecordsFetched) {
        this.salesforceRecordsFetched = salesforceRecordsFetched;
    }

    public int getGoogleSheetRowsFetched() {
        return googleSheetRowsFetched;
    }

    public void setGoogleSheetRowsFetched(int googleSheetRowsFetched) {
        this.googleSheetRowsFetched = googleSheetRowsFetched;
    }

    public int getBuildiumRecordsFetched() {
        return buildiumRecordsFetched;
    }

    public void setBuildiumRecordsFetched(int buildiumRecordsFetched) {
        this.buildiumRecordsFetched = buildiumRecordsFetched;
    }

    public int getGoogleSheetMatches() {
        return googleSheetMatches;
    }

    public void setGoogleSheetMatches(int googleSheetMatches) {
        this.googleSheetMatches = googleSheetMatches;
    }

    public int getBuildiumMatches() {
        return buildiumMatches;
    }

    public void setBuildiumMatches(int buildiumMatches) {
        this.buildiumMatches = buildiumMatches;
    }

    public int getUnmatchedCount() {
        return unmatchedCount;
    }

    public void setUnmatchedCount(int unmatchedCount) {
        this.unmatchedCount = unmatchedCount;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }
}
