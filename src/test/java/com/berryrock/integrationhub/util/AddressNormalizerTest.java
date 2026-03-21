package com.berryrock.integrationhub.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AddressNormalizerTest {

    private final AddressNormalizer normalizer = new AddressNormalizer();

    @Test
    void testNormalize_StreetSuffixes() {
        assertEquals("123 MAIN ST", normalizer.normalize("123 Main Street"));
        assertEquals("456 ELM AVE", normalizer.normalize("456 Elm Avenue"));
        assertEquals("789 OAK RD", normalizer.normalize("789 Oak Road"));
        assertEquals("101 PINE DR", normalizer.normalize("101 Pine Drive"));
        assertEquals("202 MAPLE BLVD", normalizer.normalize("202 Maple Boulevard"));
        assertEquals("303 CEDAR LN", normalizer.normalize("303 Cedar Lane"));
        assertEquals("404 BIRCH CT", normalizer.normalize("404 Birch Court"));
        assertEquals("505 WALNUT PL", normalizer.normalize("505 Walnut Place"));
    }

    @Test
    void testNormalize_Directionals() {
        assertEquals("123 N MAIN ST", normalizer.normalize("123 North Main Street"));
        assertEquals("456 S ELM AVE", normalizer.normalize("456 South Elm Avenue"));
        assertEquals("789 E OAK RD", normalizer.normalize("789 East Oak Road"));
        assertEquals("101 W PINE DR", normalizer.normalize("101 West Pine Drive"));
        assertEquals("202 NE MAPLE BLVD", normalizer.normalize("202 Northeast Maple Boulevard"));
        assertEquals("303 NW CEDAR LN", normalizer.normalize("303 Northwest Cedar Lane"));
        assertEquals("404 SE BIRCH CT", normalizer.normalize("404 Southeast Birch Court"));
        assertEquals("505 SW WALNUT PL", normalizer.normalize("505 Southwest Walnut Place"));
    }

    @Test
    void testNormalize_PunctuationAndWhitespace() {
        assertEquals("123 ST JOHNS RD", normalizer.normalize("123 St. John's Road"));
        assertEquals("123 MAIN ST", normalizer.normalize("  123   Main   Street  "));
        assertEquals("APT 4B 123 MAIN ST", normalizer.normalize("Apt 4B, 123 Main Street."));
    }

    @Test
    void testNormalizeCity_Variants() {
        assertEquals("STL", normalizer.normalizeCity("Saint Louis"));
        assertEquals("STL", normalizer.normalizeCity("St Louis"));
        assertEquals("STL", normalizer.normalizeCity("St. Louis"));
        assertEquals("CHICAGO", normalizer.normalizeCity("Chicago"));
        assertEquals("NEW YORK", normalizer.normalizeCity("New York"));
    }

    @Test
    void testBuildComparableKey() {
        assertEquals("123 MAIN ST|STL|MO|63101", normalizer.buildComparableKey("123 MAIN ST", "STL", "MO", "63101"));
        assertEquals("456 ELM AVE|CHICAGO|IL|60601", normalizer.buildComparableKey("456 ELM AVE", "CHICAGO", "IL", "60601-1234")); // Should truncate ZIP
        assertEquals("789 OAK RD", normalizer.buildComparableKey("789 OAK RD", null, null, null));
    }
}
