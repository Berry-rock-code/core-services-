package com.berryrock.integrationhub.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AddressNormalizationServiceTest {

    private final AddressNormalizationService service = new AddressNormalizationService();

    @Test
    void testNormalize() {
        assertEquals("123 MAIN ST", service.normalize("123 Main Street"));
        assertEquals("456 OAK AVE", service.normalize("  456 Oak Avenue  "));
        assertEquals("789 PINE RD", service.normalize("789 Pine Road,"));
        assertEquals("101 MAPLE DR", service.normalize("101 Maple Drive."));

        // Directionals
        assertEquals("111 NW ELM ST", service.normalize("111 Northwest Elm Street"));
        assertEquals("111 N ELM ST", service.normalize("111 North Elm Street"));

        // Unit stripping
        assertEquals("222 CEDAR LN", service.normalize("222 Cedar Lane - 12"));

        // Remove punctuation
        assertEquals("333 SPRUCE BLVD APT 4", service.normalize("333 Spruce Blvd. Apt #4"));
    }

    @Test
    void testBuildNormalizedKey() {
        String key = service.buildNormalizedKey("123 Main Street", "New York", "NY", "10001");
        assertEquals("123 MAIN ST|NEW YORK|NY|10001", key);

        String key2 = service.buildNormalizedKey("456 OAK AVE - 5", "Los Angeles", "CA", "90001");
        assertEquals("456 OAK AVE|LOS ANGELES|CA|90001", key2);
    }
}
