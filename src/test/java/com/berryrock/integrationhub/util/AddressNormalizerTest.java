package com.berryrock.integrationhub.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AddressNormalizerTest
{
    private final AddressNormalizer normalizer = new AddressNormalizer();

    // Rule 1 — null input returns empty string
    @Test
    void testNormalize_NullInputReturnsEmptyString()
    {
        assertEquals("", normalizer.normalize(null));
    }

    // Rule 2 — punctuation removal (periods, commas, hash signs, apostrophes)
    @Test
    void testNormalize_PunctuationRemoval()
    {
        assertEquals("123 ST JOHNS RD", normalizer.normalize("123 St. John's Road"));
        assertEquals("333 SPRUCE BLVD APT 4", normalizer.normalize("333 Spruce Blvd. Apt #4"));
        assertEquals("APT 4B 123 MAIN ST", normalizer.normalize("Apt 4B, 123 Main Street."));
    }

    // Rule 3 — whitespace collapse
    @Test
    void testNormalize_WhitespaceCollapse()
    {
        assertEquals("123 MAIN ST", normalizer.normalize("  123   Main   Street  "));
    }

    // Rule 4 — ZIP+4 strip
    @Test
    void testNormalize_ZipPlusFourStrip()
    {
        assertEquals("TULSA OK 74101", normalizer.normalize("Tulsa OK 74101-5678"));
    }

    // Rule 5 — street suffix abbreviation
    @Test
    void testNormalize_StreetSuffixes()
    {
        assertEquals("123 MAIN ST", normalizer.normalize("123 Main Street"));
        assertEquals("456 ELM AVE", normalizer.normalize("456 Elm Avenue"));
        assertEquals("789 OAK RD", normalizer.normalize("789 Oak Road"));
        assertEquals("101 PINE DR", normalizer.normalize("101 Pine Drive"));
        assertEquals("303 CEDAR LN", normalizer.normalize("303 Cedar Lane"));
        assertEquals("404 BIRCH CT", normalizer.normalize("404 Birch Court"));
        assertEquals("505 WALNUT PL", normalizer.normalize("505 Walnut Place"));
        assertEquals("202 MAPLE BLVD", normalizer.normalize("202 Maple Boulevard"));
        assertEquals("600 ELM TER", normalizer.normalize("600 Elm Terrace"));
        assertEquals("700 OAK PKWY", normalizer.normalize("700 Oak Parkway"));
        assertEquals("800 PINE CIR", normalizer.normalize("800 Pine Circle"));
    }

    // Rule 5 — suffix abbreviation does not affect words containing the suffix (e.g. STREETCAR)
    @Test
    void testNormalize_StreetSuffixWordBoundary()
    {
        assertEquals("1 STREETCAR WAY", normalizer.normalize("1 Streetcar Way"));
    }

    // Rule 6 — city alias resolution
    @Test
    void testNormalize_CityAliases()
    {
        assertEquals("OKC", normalizer.normalize("Oklahoma City"));
        assertEquals("STL", normalizer.normalize("St Louis"));
        assertEquals("STL", normalizer.normalize("Saint Louis"));
        assertEquals("STL", normalizer.normalize("St. Louis"));
        assertEquals("KC", normalizer.normalize("Kansas City"));
    }

    // Rule 7 — directional abbreviation, compound before cardinal
    @Test
    void testNormalize_DirectionalsCompoundBeforeCardinal()
    {
        assertEquals("123 NW ELM ST", normalizer.normalize("123 Northwest Elm Street"));
        assertEquals("456 NE ELM AVE", normalizer.normalize("456 Northeast Elm Avenue"));
        assertEquals("789 SW OAK RD", normalizer.normalize("789 Southwest Oak Road"));
        assertEquals("101 SE PINE DR", normalizer.normalize("101 Southeast Pine Drive"));
        assertEquals("202 N MAPLE BLVD", normalizer.normalize("202 North Maple Boulevard"));
        assertEquals("303 S CEDAR LN", normalizer.normalize("303 South Cedar Lane"));
        assertEquals("404 E BIRCH CT", normalizer.normalize("404 East Birch Court"));
        assertEquals("505 W WALNUT PL", normalizer.normalize("505 West Walnut Place"));
    }

    // Rule 8 — trailing unit suffix strip
    @Test
    void testNormalize_TrailingUnitSuffixStrip()
    {
        assertEquals("222 CEDAR LN", normalizer.normalize("222 Cedar Lane - 12"));
        assertEquals("456 OAK AVE", normalizer.normalize("456 Oak Avenue - 5"));
    }

    // buildNormalizedKey — full pipe-delimited output with city alias and ZIP+4 stripping
    @Test
    void testBuildNormalizedKey_FullPipeDelimited()
    {
        String key = normalizer.buildNormalizedKey("123 Main Street", "Saint Louis", "MO", "63101-4567");
        assertEquals("123 MAIN ST|STL|MO|63101", key);
    }

    @Test
    void testBuildNormalizedKey_StandardAddress()
    {
        String key = normalizer.buildNormalizedKey("123 Main Street", "New York", "NY", "10001");
        assertEquals("123 MAIN ST|NEW YORK|NY|10001", key);
    }

    @Test
    void testBuildNormalizedKey_UnitSuffixStrippedInAddressLine()
    {
        String key = normalizer.buildNormalizedKey("456 Oak Avenue - 5", "Los Angeles", "CA", "90001");
        assertEquals("456 OAK AVE|LOS ANGELES|CA|90001", key);
    }

    // buildNormalizedKey — null components contribute empty segments, keeping delimiter structure consistent
    @Test
    void testBuildNormalizedKey_NullComponentsContributeEmptySegments()
    {
        String key = normalizer.buildNormalizedKey("789 Oak Road", null, null, null);
        assertEquals("789 OAK RD|||", key);
    }
}
