package com.berryrock.integrationhub.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AddressNormalizerTest {

    @Test
    void testNormalize_StreetSuffixes() {
        assertEquals("123 MAIN ST", AddressNormalizer.normalize("123 Main Street"));
        assertEquals("456 ELM AVE", AddressNormalizer.normalize("456 Elm Avenue"));
        assertEquals("789 OAK RD", AddressNormalizer.normalize("789 Oak Road"));
        assertEquals("101 PINE DR", AddressNormalizer.normalize("101 Pine Drive"));
        assertEquals("202 MAPLE BLVD", AddressNormalizer.normalize("202 Maple Boulevard"));
        assertEquals("303 CEDAR LN", AddressNormalizer.normalize("303 Cedar Lane"));
        assertEquals("404 BIRCH CT", AddressNormalizer.normalize("404 Birch Court"));
        assertEquals("505 WALNUT PL", AddressNormalizer.normalize("505 Walnut Place"));
    }

    @Test
    void testNormalize_Directionals() {
        assertEquals("123 N MAIN ST", AddressNormalizer.normalize("123 North Main Street"));
        assertEquals("456 S ELM AVE", AddressNormalizer.normalize("456 South Elm Avenue"));
        assertEquals("789 E OAK RD", AddressNormalizer.normalize("789 East Oak Road"));
        assertEquals("101 W PINE DR", AddressNormalizer.normalize("101 West Pine Drive"));
        assertEquals("202 NE MAPLE BLVD", AddressNormalizer.normalize("202 Northeast Maple Boulevard"));
        assertEquals("303 NW CEDAR LN", AddressNormalizer.normalize("303 Northwest Cedar Lane"));
        assertEquals("404 SE BIRCH CT", AddressNormalizer.normalize("404 Southeast Birch Court"));
        assertEquals("505 SW WALNUT PL", AddressNormalizer.normalize("505 Southwest Walnut Place"));
    }

    @Test
    void testNormalize_PunctuationAndWhitespace() {
        assertEquals("123 ST JOHNS RD", AddressNormalizer.normalize("123 St. John's Road"));
        assertEquals("123 MAIN ST", AddressNormalizer.normalize("  123   Main   Street  "));
        assertEquals("APT 4B 123 MAIN ST", AddressNormalizer.normalize("Apt 4B, 123 Main Street."));
    }

    @Test
    void testNormalizeCity_Variants() {
        assertEquals("STL", AddressNormalizer.normalizeCity("Saint Louis"));
        assertEquals("STL", AddressNormalizer.normalizeCity("St Louis"));
        assertEquals("STL", AddressNormalizer.normalizeCity("St. Louis"));
        assertEquals("CHICAGO", AddressNormalizer.normalizeCity("Chicago"));
        assertEquals("NEW YORK", AddressNormalizer.normalizeCity("New York"));
    }

    @Test
    void testBuildComparableKey() {
        assertEquals("123 MAIN ST|STL|MO|63101", AddressNormalizer.buildComparableKey("123 MAIN ST", "STL", "MO", "63101"));
        assertEquals("456 ELM AVE|CHICAGO|IL|60601", AddressNormalizer.buildComparableKey("456 ELM AVE", "CHICAGO", "IL", "60601-1234")); // Should truncate ZIP
        assertEquals("789 OAK RD", AddressNormalizer.buildComparableKey("789 OAK RD", null, null, null));
    }
}
