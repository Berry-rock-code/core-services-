package com.berryrock.integrationhub.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SalesforceClientImpl implements SalesforceClient {
    private static final Logger log = LoggerFactory.getLogger(SalesforceClientImpl.class);

    @Value("${integration.vendor.salesforce.enabled:false}")
    private boolean enabled;

    @Override
    public boolean isConfigured() {
        log.debug("Checking Salesforce configuration status");
        return enabled;
    }

    @Override
    public java.util.List<com.berryrock.integrationhub.model.SalesforceAddressRecord> fetchAddressesForGoogleSheetBuildiumSync() {
        log.info("Mock fetching Salesforce records for sync");
        java.util.List<com.berryrock.integrationhub.model.SalesforceAddressRecord> mockRecords = new java.util.ArrayList<>();

        com.berryrock.integrationhub.model.SalesforceAddressRecord r1 = new com.berryrock.integrationhub.model.SalesforceAddressRecord();
        r1.setOpportunityId("0061Q00000aaaaaAAA");
        r1.setRawAddress("123 Main Street");
        r1.setAddressLine("123 Main Street");
        r1.setCity("Saint Louis");
        r1.setState("MO");
        r1.setPostalCode("63101");
        r1.setSourceSystem("Salesforce");
        mockRecords.add(r1);

        com.berryrock.integrationhub.model.SalesforceAddressRecord r2 = new com.berryrock.integrationhub.model.SalesforceAddressRecord();
        r2.setOpportunityId("0061Q00000bbbbbBBB");
        r2.setRawAddress("456 Elm Avenue");
        r2.setAddressLine("456 Elm Avenue");
        r2.setCity("St. Louis");
        r2.setState("MO");
        r2.setPostalCode("63102");
        r2.setSourceSystem("Salesforce");
        mockRecords.add(r2);

        com.berryrock.integrationhub.model.SalesforceAddressRecord r3 = new com.berryrock.integrationhub.model.SalesforceAddressRecord();
        r3.setOpportunityId("0061Q00000cccccCCC");
        r3.setRawAddress("789 Oak Lane");
        r3.setAddressLine("789 Oak Lane");
        r3.setCity("Clayton");
        r3.setState("MO");
        r3.setPostalCode("63105");
        r3.setSourceSystem("Salesforce");
        mockRecords.add(r3);

        // Add a duplicate to test collision warning
        com.berryrock.integrationhub.model.SalesforceAddressRecord r4 = new com.berryrock.integrationhub.model.SalesforceAddressRecord();
        r4.setOpportunityId("0061Q00000dddddDDD");
        r4.setRawAddress("123 Main Street");
        r4.setAddressLine("123 Main Street");
        r4.setCity("Saint Louis");
        r4.setState("MO");
        r4.setPostalCode("63101");
        r4.setSourceSystem("Salesforce");
        mockRecords.add(r4);

        return mockRecords;
    }
}
