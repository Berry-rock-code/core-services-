package com.berryrock.integrationhub.util;

import org.apache.commons.lang3.StringUtils;

public class AddressNormalizer {

    public static String normalize(String rawAddress) {
        if (StringUtils.isBlank(rawAddress)) {
            return "";
        }

        String normalized = rawAddress.toUpperCase();

        // Remove punctuation that hurts matching
        normalized = normalized.replaceAll("[.,']", "");

        // Collapse repeated spaces and trim
        normalized = normalized.replaceAll("\\s+", " ").trim();

        // Standardize common street suffixes (adding word boundaries to avoid replacing parts of words)
        normalized = normalized.replaceAll("\\bSTREET\\b", "ST");
        normalized = normalized.replaceAll("\\bAVENUE\\b", "AVE");
        normalized = normalized.replaceAll("\\bROAD\\b", "RD");
        normalized = normalized.replaceAll("\\bDRIVE\\b", "DR");
        normalized = normalized.replaceAll("\\bBOULEVARD\\b", "BLVD");
        normalized = normalized.replaceAll("\\bLANE\\b", "LN");
        normalized = normalized.replaceAll("\\bCOURT\\b", "CT");
        normalized = normalized.replaceAll("\\bPLACE\\b", "PL");

        // Standardize directional markers
        normalized = normalized.replaceAll("\\bNORTH\\b", "N");
        normalized = normalized.replaceAll("\\bSOUTH\\b", "S");
        normalized = normalized.replaceAll("\\bEAST\\b", "E");
        normalized = normalized.replaceAll("\\bWEST\\b", "W");
        normalized = normalized.replaceAll("\\bNORTHEAST\\b", "NE");
        normalized = normalized.replaceAll("\\bNORTHWEST\\b", "NW");
        normalized = normalized.replaceAll("\\bSOUTHEAST\\b", "SE");
        normalized = normalized.replaceAll("\\bSOUTHWEST\\b", "SW");

        return normalized.trim();
    }

    public static String normalizeCity(String city) {
        if (StringUtils.isBlank(city)) {
            return "";
        }

        String normalized = city.toUpperCase();

        // Remove punctuation
        normalized = normalized.replaceAll("[.,']", "");

        normalized = normalized.replaceAll("\\s+", " ").trim();

        // Standardize city variants
        normalized = normalized.replaceAll("\\bSAINT LOUIS\\b", "STL");
        normalized = normalized.replaceAll("\\bST LOUIS\\b", "STL");

        return normalized.trim();
    }

    public static String buildComparableKey(String normalizedAddressLine, String normalizedCity, String state, String postalCode) {
        StringBuilder keyBuilder = new StringBuilder();

        if (StringUtils.isNotBlank(normalizedAddressLine)) {
            keyBuilder.append(normalizedAddressLine);
        }

        if (StringUtils.isNotBlank(normalizedCity)) {
            if (!keyBuilder.isEmpty()) keyBuilder.append("|");
            keyBuilder.append(normalizedCity);
        }

        if (StringUtils.isNotBlank(state)) {
            if (!keyBuilder.isEmpty()) keyBuilder.append("|");
            keyBuilder.append(state.toUpperCase().trim());
        }

        if (StringUtils.isNotBlank(postalCode)) {
            if (!keyBuilder.isEmpty()) keyBuilder.append("|");
            // Only use the first 5 digits of postal code
            String cleanZip = postalCode.replaceAll("[^0-9]", "");
            if (cleanZip.length() > 5) {
                cleanZip = cleanZip.substring(0, 5);
            }
            keyBuilder.append(cleanZip);
        }

        return keyBuilder.toString();
    }
}
