package com.berryrock.integrationhub.service;
// LAYER: FEATURE:address-pipeline -- moves to address-pipeline repo

import com.berryrock.integrationhub.model.SalesforceAddressRecord;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AddressQualityServiceTest {

    private final AddressQualityService service = new AddressQualityService();

    @Test
    void testCleanAddress() {
        SalesforceAddressRecord record = new SalesforceAddressRecord();
        record.setAddressLine("123 Main St");
        record.setCity("Seattle");
        record.setState("WA");
        record.setPostalCode("98101");

        assertEquals(AddressQualityService.Quality.CLEAN, service.classify(record));
    }

    @Test
    void testPartialAddress() {
        SalesforceAddressRecord record = new SalesforceAddressRecord();
        record.setAddressLine("123 Main St");
        // missing city
        record.setState("WA");
        record.setPostalCode("98101");

        assertEquals(AddressQualityService.Quality.PARTIAL, service.classify(record));
    }

    @Test
    void testSuspiciousEmbeddedZip() {
        SalesforceAddressRecord record = new SalesforceAddressRecord();
        record.setAddressLine("123 Main St 98101");
        record.setCity("Seattle");
        record.setState("WA");
        record.setPostalCode("98101");

        assertEquals(AddressQualityService.Quality.SUSPICIOUS, service.classify(record));
    }

    @Test
    void testSuspiciousEmbeddedCity() {
        SalesforceAddressRecord record = new SalesforceAddressRecord();
        record.setAddressLine("123 Main St Seattle WA");
        record.setCity("Seattle");
        record.setState("WA");
        record.setPostalCode("98101");

        assertEquals(AddressQualityService.Quality.SUSPICIOUS, service.classify(record));
    }

    @Test
    void testSuspiciousEmbeddedState() {
        SalesforceAddressRecord record = new SalesforceAddressRecord();
        record.setAddressLine("123 Main St WA");
        record.setCity("Seattle");
        record.setState("WA");
        record.setPostalCode("98101");

        assertEquals(AddressQualityService.Quality.SUSPICIOUS, service.classify(record));
    }

    @Test
    void testSuspiciousEmbeddedOKC() {
        SalesforceAddressRecord record = new SalesforceAddressRecord();
        record.setAddressLine("123 Main St OKC");
        record.setCity("Oklahoma City");
        record.setState("OK");
        record.setPostalCode("73101");

        assertEquals(AddressQualityService.Quality.SUSPICIOUS, service.classify(record));
    }
}
