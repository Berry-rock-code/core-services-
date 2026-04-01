package com.berryrock.integrationhub.model;
// LAYER: FEATURE:address-pipeline -- moves to address-pipeline repo

/**
 * Represents a single Salesforce Opportunity record as fetched by the address pipeline.
 *
 * Part of the address pipeline — produced by
 * {@link com.berryrock.integrationhub.client.SalesforceClientImpl} via a SOQL query against
 * the Opportunity object. Only the address-related fields and the opportunity ID are carried
 * here; no other opportunity metadata is stored.
 *
 * The {@code quality} field is populated by
 * {@link com.berryrock.integrationhub.service.AddressQualityService} and drives which
 * lookup maps the record is placed into during pipeline execution.
 */
public class SalesforceAddressRecord
{
    /** Salesforce Opportunity ID (18-character SFID). */
    private String opportunityId;

    /**
     * Composite raw address string assembled from all available address components.
     * Built by the client during record parsing; used as a human-readable reference.
     */
    private String rawAddress;

    /** Street address line from the Salesforce compound address field. */
    private String addressLine;

    /** City component from the Salesforce compound address field. */
    private String city;

    /** State code component from the Salesforce compound address field. */
    private String state;

    /** Postal code component from the Salesforce compound address field. */
    private String postalCode;

    /** Country code component from the Salesforce compound address field. */
    private String countryCode;

    /**
     * Normalized composite key built by the pipeline for address matching.
     * Populated by {@link com.berryrock.integrationhub.util.AddressNormalizer}.
     */
    private String normalizedAddress;

    /** Always {@code "Salesforce"} — identifies the originating system for audit purposes. */
    private String sourceSystem;

    /**
     * Quality classification assigned by
     * {@link com.berryrock.integrationhub.service.AddressQualityService}.
     * One of {@code CLEAN}, {@code PARTIAL}, or {@code SUSPICIOUS}.
     */
    private String quality;

    /** Constructs an empty record. */
    public SalesforceAddressRecord()
    {
    }

    /**
     * Returns the Salesforce Opportunity ID.
     *
     * @return 18-character Opportunity ID, or {@code null} if not set
     */
    public String getOpportunityId()
    {
        return opportunityId;
    }

    /**
     * Sets the Salesforce Opportunity ID.
     *
     * @param opportunityId 18-character Opportunity ID
     */
    public void setOpportunityId(String opportunityId)
    {
        this.opportunityId = opportunityId;
    }

    /**
     * Returns the composite raw address string.
     *
     * @return raw address assembled from all address components, or {@code null} if not set
     */
    public String getRawAddress()
    {
        return rawAddress;
    }

    /**
     * Sets the composite raw address string.
     *
     * @param rawAddress raw address assembled by the client during parsing
     */
    public void setRawAddress(String rawAddress)
    {
        this.rawAddress = rawAddress;
    }

    /**
     * Returns the street address line from Salesforce.
     *
     * @return street address line, or {@code null} if not set
     */
    public String getAddressLine()
    {
        return addressLine;
    }

    /**
     * Sets the street address line.
     *
     * @param addressLine street address line from the Salesforce compound address field
     */
    public void setAddressLine(String addressLine)
    {
        this.addressLine = addressLine;
    }

    /**
     * Returns the city component of the address.
     *
     * @return city name, or {@code null} if not set
     */
    public String getCity()
    {
        return city;
    }

    /**
     * Sets the city component.
     *
     * @param city city name
     */
    public void setCity(String city)
    {
        this.city = city;
    }

    /**
     * Returns the state code component of the address.
     *
     * @return two-letter state code, or {@code null} if not set
     */
    public String getState()
    {
        return state;
    }

    /**
     * Sets the state code component.
     *
     * @param state two-letter state code
     */
    public void setState(String state)
    {
        this.state = state;
    }

    /**
     * Returns the postal code component of the address.
     *
     * @return ZIP or postal code, or {@code null} if not set
     */
    public String getPostalCode()
    {
        return postalCode;
    }

    /**
     * Sets the postal code component.
     *
     * @param postalCode ZIP or postal code
     */
    public void setPostalCode(String postalCode)
    {
        this.postalCode = postalCode;
    }

    /**
     * Returns the country code component of the address.
     *
     * @return ISO country code, or {@code null} if not set
     */
    public String getCountryCode()
    {
        return countryCode;
    }

    /**
     * Sets the country code component.
     *
     * @param countryCode ISO country code
     */
    public void setCountryCode(String countryCode)
    {
        this.countryCode = countryCode;
    }

    /**
     * Returns the normalized composite key used for address matching.
     *
     * @return normalized key in the form {@code ADDRESS|CITY|STATE|ZIP}, or {@code null} if not yet populated
     */
    public String getNormalizedAddress()
    {
        return normalizedAddress;
    }

    /**
     * Sets the normalized composite key.
     *
     * @param normalizedAddress normalized key produced by the pipeline
     */
    public void setNormalizedAddress(String normalizedAddress)
    {
        this.normalizedAddress = normalizedAddress;
    }

    /**
     * Returns the name of the source system that produced this record.
     *
     * @return always {@code "Salesforce"}
     */
    public String getSourceSystem()
    {
        return sourceSystem;
    }

    /**
     * Sets the source system name.
     *
     * @param sourceSystem name of the originating system
     */
    public void setSourceSystem(String sourceSystem)
    {
        this.sourceSystem = sourceSystem;
    }

    /**
     * Returns the quality classification for this record's address.
     *
     * @return {@code "CLEAN"}, {@code "PARTIAL"}, or {@code "SUSPICIOUS"}; {@code null} before classification runs
     */
    public String getQuality()
    {
        return quality;
    }

    /**
     * Sets the quality classification.
     *
     * @param quality quality label assigned by {@link com.berryrock.integrationhub.service.AddressQualityService}
     */
    public void setQuality(String quality)
    {
        this.quality = quality;
    }
}
