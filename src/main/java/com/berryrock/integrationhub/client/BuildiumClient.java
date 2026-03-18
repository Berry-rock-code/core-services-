package com.berryrock.integrationhub.client;

import java.util.List;
import java.util.Map;

public interface BuildiumClient
{
    boolean ping();

    List<Map<String, Object>> getRentalsPage(int limit, int offset);

    List<Map<String, Object>> getAllRentals();

    List<com.berryrock.integrationhub.model.BuildiumAddressRecord> fetchActiveLeaseAddresses();
}