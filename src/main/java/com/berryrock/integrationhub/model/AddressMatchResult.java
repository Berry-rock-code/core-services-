package com.berryrock.integrationhub.model;

public class AddressMatchResult {
    private boolean matched;
    private String normalizedKey;
    private String salesforceOpportunityId;
    private Integer sheetRowNumber;
    private String buildiumPropertyId;
    private String matchSource;
    private String warning;

    public AddressMatchResult() {
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    public String getNormalizedKey() {
        return normalizedKey;
    }

    public void setNormalizedKey(String normalizedKey) {
        this.normalizedKey = normalizedKey;
    }

    public String getSalesforceOpportunityId() {
        return salesforceOpportunityId;
    }

    public void setSalesforceOpportunityId(String salesforceOpportunityId) {
        this.salesforceOpportunityId = salesforceOpportunityId;
    }

    public Integer getSheetRowNumber() {
        return sheetRowNumber;
    }

    public void setSheetRowNumber(Integer sheetRowNumber) {
        this.sheetRowNumber = sheetRowNumber;
    }

    public String getBuildiumPropertyId() {
        return buildiumPropertyId;
    }

    public void setBuildiumPropertyId(String buildiumPropertyId) {
        this.buildiumPropertyId = buildiumPropertyId;
    }

    public String getMatchSource() {
        return matchSource;
    }

    public void setMatchSource(String matchSource) {
        this.matchSource = matchSource;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}
