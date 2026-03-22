package com.berryrock.integrationhub.service;

import org.springframework.stereotype.Service;

@Service
public class AddressNormalizationService {

    public String normalize(String address) {
        if (address == null) return "";

        String normalized = address.toUpperCase();

        // Remove punctuation
        normalized = normalized.replaceAll("[.,#]", "");

        // Collapse whitespace and trim
        normalized = normalized.replaceAll("\\s+", " ").trim();

        // Standardize street suffixes
        normalized = replaceWord(normalized, "STREET", "ST");
        normalized = replaceWord(normalized, "AVENUE", "AVE");
        normalized = replaceWord(normalized, "ROAD", "RD");
        normalized = replaceWord(normalized, "DRIVE", "DR");
        normalized = replaceWord(normalized, "LANE", "LN");
        normalized = replaceWord(normalized, "COURT", "CT");
        normalized = replaceWord(normalized, "PLACE", "PL");
        normalized = replaceWord(normalized, "BOULEVARD", "BLVD");
        normalized = replaceWord(normalized, "TERRACE", "TER");
        normalized = replaceWord(normalized, "PARKWAY", "PKWY");
        normalized = replaceWord(normalized, "CIRCLE", "CIR");

        // Standardize directionals
        normalized = replaceWord(normalized, "NORTHWEST", "NW");
        normalized = replaceWord(normalized, "NORTHEAST", "NE");
        normalized = replaceWord(normalized, "SOUTHWEST", "SW");
        normalized = replaceWord(normalized, "SOUTHEAST", "SE");

        // Directionals for N/S/E/W as single letters if they are full words
        normalized = replaceWord(normalized, "NORTH", "N");
        normalized = replaceWord(normalized, "SOUTH", "S");
        normalized = replaceWord(normalized, "EAST", "E");
        normalized = replaceWord(normalized, "WEST", "W");

        // Remove trailing unit-like suffixes like "- 12"
        normalized = normalized.replaceAll("\\s*-\\s*\\d+$", "");

        return normalized.trim();
    }

    public String buildNormalizedKey(String addressLine, String city, String state, String postalCode) {
        StringBuilder sb = new StringBuilder();
        if (addressLine != null) sb.append(normalize(addressLine)).append("|");
        else sb.append("|");

        if (city != null) sb.append(normalize(city)).append("|");
        else sb.append("|");

        if (state != null) sb.append(normalize(state)).append("|");
        else sb.append("|");

        if (postalCode != null) sb.append(normalize(postalCode));

        return sb.toString();
    }

    private String replaceWord(String text, String target, String replacement) {
        // use word boundaries \b to ensure we only replace full words
        return text.replaceAll("\\b" + target + "\\b", replacement);
    }
}
