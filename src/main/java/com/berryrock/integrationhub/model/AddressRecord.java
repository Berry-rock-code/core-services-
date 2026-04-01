package com.berryrock.integrationhub.model;
// LAYER: FEATURE:address-pipeline -- moves to address-pipeline repo

/**
 * Base representation of an address record shared across integration systems.
 *
 * Holds the raw address string as received from the source system alongside the
 * normalized form produced by the address pipeline. The {@code id} field carries
 * whatever opaque identifier the originating system uses.
 *
 * Subclasses ({@link SalesforceAddressRecord}, {@link BuildiumAddressRecord},
 * {@link GoogleSheetAddressRow}) extend this concept with system-specific fields.
 */
public class AddressRecord
{
    /** Opaque identifier from the originating system. */
    private String id;

    /** Address string exactly as received from the source, before any normalization. */
    private String rawAddress;

    /**
     * Normalized form of the address, populated by the pipeline after
     * {@link com.berryrock.integrationhub.util.AddressNormalizer} runs.
     */
    private String normalizedAddress;

    /** Constructs an empty record. */
    public AddressRecord()
    {
    }

    /**
     * Constructs a fully populated record.
     *
     * @param id                system-specific identifier
     * @param rawAddress        address string as received from the source
     * @param normalizedAddress normalized address produced by the pipeline
     */
    public AddressRecord(String id, String rawAddress, String normalizedAddress)
    {
        this.id = id;
        this.rawAddress = rawAddress;
        this.normalizedAddress = normalizedAddress;
    }

    /**
     * Returns the system-specific identifier for this record.
     *
     * @return opaque ID string, or {@code null} if not set
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the system-specific identifier.
     *
     * @param id opaque ID string
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Returns the address string exactly as received from the source system.
     *
     * @return raw address, or {@code null} if not set
     */
    public String getRawAddress()
    {
        return rawAddress;
    }

    /**
     * Sets the raw address string.
     *
     * @param rawAddress address as received from the source
     */
    public void setRawAddress(String rawAddress)
    {
        this.rawAddress = rawAddress;
    }

    /**
     * Returns the normalized address string produced by the pipeline.
     *
     * @return normalized address, or {@code null} if normalization has not yet run
     */
    public String getNormalizedAddress()
    {
        return normalizedAddress;
    }

    /**
     * Sets the normalized address string.
     *
     * @param normalizedAddress normalized form produced by the pipeline
     */
    public void setNormalizedAddress(String normalizedAddress)
    {
        this.normalizedAddress = normalizedAddress;
    }
}
