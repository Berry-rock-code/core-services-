package com.berryrock.integrationhub.model;

/**
 * Captures the outcome of attempting to match a single address across systems.
 *
 * Part of the address pipeline — produced after the matching phase and used to track
 * which records were successfully correlated between Salesforce, the Loan Tape
 * (Google Sheets), and Buildium.
 *
 * The {@code matchSource} field records which system initiated the match and which pass
 * (full key or address-only fallback) was used.
 */
public class AddressMatchResult
{
    /** {@code true} if a match was found across systems; {@code false} otherwise. */
    private boolean matched;

    /**
     * The normalized composite key used to perform the match.
     * Format: {@code ADDRESS|CITY|STATE|ZIP} (pipe-delimited).
     */
    private String normalizedKey;

    /** Salesforce Opportunity ID of the matched SF record, or {@code null} if unmatched. */
    private String salesforceOpportunityId;

    /** 1-based row number in the Google Sheet that was matched, or {@code null} if unmatched. */
    private Integer sheetRowNumber;

    /** Buildium property ID of the matched Buildium unit, or {@code null} if unmatched or ambiguous. */
    private String buildiumPropertyId;

    /**
     * Human-readable label describing which systems and which matching pass produced this result.
     * Examples: {@code "SF->Sheet (full key)"}, {@code "SF->Sheet (address only)"}.
     */
    private String matchSource;

    /**
     * Warning message if the match was ambiguous (e.g., multiple Buildium records for the same
     * normalized key) or if data quality issues were detected.
     */
    private String warning;

    /** Constructs an empty result. */
    public AddressMatchResult()
    {
    }

    /**
     * Returns whether a match was found.
     *
     * @return {@code true} if matched
     */
    public boolean isMatched()
    {
        return matched;
    }

    /**
     * Sets the matched flag.
     *
     * @param matched {@code true} if a match was found
     */
    public void setMatched(boolean matched)
    {
        this.matched = matched;
    }

    /**
     * Returns the normalized composite key used during matching.
     *
     * @return pipe-delimited key, or {@code null} if not set
     */
    public String getNormalizedKey()
    {
        return normalizedKey;
    }

    /**
     * Sets the normalized key.
     *
     * @param normalizedKey pipe-delimited key from the normalization step
     */
    public void setNormalizedKey(String normalizedKey)
    {
        this.normalizedKey = normalizedKey;
    }

    /**
     * Returns the Salesforce Opportunity ID of the matched record.
     *
     * @return Opportunity ID, or {@code null} if no SF match was found
     */
    public String getSalesforceOpportunityId()
    {
        return salesforceOpportunityId;
    }

    /**
     * Sets the Salesforce Opportunity ID.
     *
     * @param salesforceOpportunityId Opportunity ID from Salesforce
     */
    public void setSalesforceOpportunityId(String salesforceOpportunityId)
    {
        this.salesforceOpportunityId = salesforceOpportunityId;
    }

    /**
     * Returns the 1-based row number in the Google Sheet that was matched.
     *
     * @return sheet row number, or {@code null} if no sheet match was found
     */
    public Integer getSheetRowNumber()
    {
        return sheetRowNumber;
    }

    /**
     * Sets the sheet row number.
     *
     * @param sheetRowNumber 1-based row index in the spreadsheet
     */
    public void setSheetRowNumber(Integer sheetRowNumber)
    {
        this.sheetRowNumber = sheetRowNumber;
    }

    /**
     * Returns the Buildium property ID of the matched unit.
     *
     * @return Buildium property ID, or {@code null} if no Buildium match was found
     */
    public String getBuildiumPropertyId()
    {
        return buildiumPropertyId;
    }

    /**
     * Sets the Buildium property ID.
     *
     * @param buildiumPropertyId Buildium property ID of the matched unit
     */
    public void setBuildiumPropertyId(String buildiumPropertyId)
    {
        this.buildiumPropertyId = buildiumPropertyId;
    }

    /**
     * Returns a description of which systems and which matching pass produced this result.
     *
     * @return match source label, or {@code null} if not set
     */
    public String getMatchSource()
    {
        return matchSource;
    }

    /**
     * Sets the match source description.
     *
     * @param matchSource human-readable label for the matching pass
     */
    public void setMatchSource(String matchSource)
    {
        this.matchSource = matchSource;
    }

    /**
     * Returns any warning associated with this result (e.g., ambiguous Buildium match).
     *
     * @return warning message, or {@code null} if no warning
     */
    public String getWarning()
    {
        return warning;
    }

    /**
     * Sets the warning message.
     *
     * @param warning description of any data quality issue or ambiguity
     */
    public void setWarning(String warning)
    {
        this.warning = warning;
    }
}
