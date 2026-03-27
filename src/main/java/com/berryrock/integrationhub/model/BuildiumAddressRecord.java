package com.berryrock.integrationhub.model;

/**
 * Represents a single address record fetched from Buildium.
 *
 * Part of the address pipeline — produced by
 * {@link com.berryrock.integrationhub.client.BuildiumClientImpl#fetchActiveLeaseAddresses()}
 * by joining the {@code /v1/rentals/units} and {@code /v1/leases} endpoints. Each record
 * corresponds to one rental unit that has a street address and, where available, an active
 * lease linked to that unit.
 *
 * The pipeline uses the normalized form of the raw address to look up matching rows in the
 * Loan Tape (Google Sheets) so that Buildium property and lease IDs can be written back.
 */
public class BuildiumAddressRecord
{
    /** Buildium's internal property ID for the parent property of this unit. */
    private String buildiumPropertyId;

    /** Buildium's internal unit ID. Used as the lease ID column in the sheet write-back. */
    private String buildiumUnitId;

    /** Street address line as returned by the Buildium API (pre-normalization). */
    private String rawAddress;

    /** City component of the unit's address. */
    private String city;

    /** Two-letter state code component. */
    private String state;

    /** ZIP or postal code component. */
    private String postalCode;

    /**
     * Composite normalized key built by the pipeline for address matching.
     * Populated by {@link com.berryrock.integrationhub.util.AddressNormalizer}.
     */
    private String normalizedAddress;

    /**
     * Buildium lease ID of the active lease associated with this unit, if one exists.
     * Derived by joining the leases endpoint on unit ID during fetch.
     */
    private String buildiumLeaseId;

    /** Constructs an empty record. */
    public BuildiumAddressRecord()
    {
    }

    /**
     * Returns the Buildium property ID for the parent property.
     *
     * @return Buildium property ID, or {@code null} if not set
     */
    public String getBuildiumPropertyId()
    {
        return buildiumPropertyId;
    }

    /**
     * Sets the Buildium property ID.
     *
     * @param buildiumPropertyId Buildium property ID
     */
    public void setBuildiumPropertyId(String buildiumPropertyId)
    {
        this.buildiumPropertyId = buildiumPropertyId;
    }

    /**
     * Returns the Buildium unit ID.
     *
     * @return Buildium unit ID, or {@code null} if not set
     */
    public String getBuildiumUnitId()
    {
        return buildiumUnitId;
    }

    /**
     * Sets the Buildium unit ID.
     *
     * @param buildiumUnitId Buildium unit ID
     */
    public void setBuildiumUnitId(String buildiumUnitId)
    {
        this.buildiumUnitId = buildiumUnitId;
    }

    /**
     * Returns the raw street address as received from Buildium.
     *
     * @return raw address string, or {@code null} if not set
     */
    public String getRawAddress()
    {
        return rawAddress;
    }

    /**
     * Sets the raw address string.
     *
     * @param rawAddress street address as returned by the Buildium API
     */
    public void setRawAddress(String rawAddress)
    {
        this.rawAddress = rawAddress;
    }

    /**
     * Returns the city component of the unit's address.
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
     * Returns the state component of the unit's address.
     *
     * @return two-letter state code, or {@code null} if not set
     */
    public String getState()
    {
        return state;
    }

    /**
     * Sets the state component.
     *
     * @param state two-letter state code
     */
    public void setState(String state)
    {
        this.state = state;
    }

    /**
     * Returns the postal code component of the unit's address.
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
     * Returns the Buildium lease ID of the active lease linked to this unit.
     *
     * @return lease ID, or {@code null} if no active lease is associated
     */
    public String getBuildiumLeaseId()
    {
        return buildiumLeaseId;
    }

    /**
     * Sets the Buildium lease ID.
     *
     * @param buildiumLeaseId lease ID from the Buildium active-leases endpoint
     */
    public void setBuildiumLeaseId(String buildiumLeaseId)
    {
        this.buildiumLeaseId = buildiumLeaseId;
    }
}
