package com.berryrock.integrationhub.model;

public class AddressRecord
{
    private String id;
    private String rawAddress;
    private String normalizedAddress;

    public AddressRecord()
    {
    }

    public AddressRecord(String id, String rawAddress, String normalizedAddress)
    {
        this.id = id;
        this.rawAddress = rawAddress;
        this.normalizedAddress = normalizedAddress;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getRawAddress()
    {
        return rawAddress;
    }

    public void setRawAddress(String rawAddress)
    {
        this.rawAddress = rawAddress;
    }

    public String getNormalizedAddress()
    {
        return normalizedAddress;
    }

    public void setNormalizedAddress(String normalizedAddress)
    {
        this.normalizedAddress = normalizedAddress;
    }
}