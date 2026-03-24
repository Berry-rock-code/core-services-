package com.berryrock.integrationhub.util;

import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;

@Component
public class AddressNormalizer {

    public String normalize(String address) {
        if (StringUtils.isBlank(address)) {
            return null;
        }

        String normalized = address.toUpperCase()
                .replace(".", "")
                .replace(",", "")
                .replace("'", "");

        // Replace common suffixes
        normalized = normalized.replaceAll("\\bSTREET\\b", "ST")
                .replaceAll("\\bAVENUE\\b", "AVE")
                .replaceAll("\\bPLACE\\b", "PL")
                .replaceAll("\\bROAD\\b", "RD")
                .replaceAll("\\bDRIVE\\b", "DR")
                .replaceAll("\\bLANE\\b", "LN")
                .replaceAll("\\bBOULEVARD\\b", "BLVD")
                .replaceAll("\\bCOURT\\b", "CT")
                .replaceAll("\\bTERRACE\\b", "TER");

        // Directionals
        normalized = normalized.replaceAll("\\bNORTH\\b", "N")
                .replaceAll("\\bSOUTH\\b", "S")
                .replaceAll("\\bEAST\\b", "E")
                .replaceAll("\\bWEST\\b", "W")
                .replaceAll("\\bNORTHEAST\\b", "NE")
                .replaceAll("\\bNORTHWEST\\b", "NW")
                .replaceAll("\\bSOUTHEAST\\b", "SE")
                .replaceAll("\\bSOUTHWEST\\b", "SW");


        normalized = normalized.replaceAll("\\bSAINT LOUIS\\b", "STL")
                .replaceAll("\\bST LOUIS\\b", "STL");

        normalized = normalized.replaceAll("\\bSAINT\\b", "ST");

        // Clean up whitespace
        normalized = normalized.trim().replaceAll("\\s+", " ");

        return normalized;
    }

    public String normalizeCity(String city) {
        if (StringUtils.isBlank(city)) {
            return null;
        }

        String normalized = city.toUpperCase()
                .replace(".", "")
                .replace(",", "");

        normalized = normalized.replaceAll("\\bSAINT LOUIS\\b", "STL")
                           .replaceAll("\\bST LOUIS\\b", "STL");

        normalized = normalized.replaceAll("\\bOKLAHOMA CITY\\b", "OKC")
                           .replaceAll("\\bKANSAS CITY\\b", "KC");

        normalized = normalized.replaceAll("\\bSAINT\\b", "ST");

        normalized = normalized.trim().replaceAll("\\s+", " ");

        return normalized;
    }

    public String buildComparableKey(String address, String city, String state, String zip) {
        StringBuilder sb = new StringBuilder();
        if (address != null) {
            sb.append(address);
        }

        if (city != null) {
            sb.append("|").append(city);
        }

        if (state != null) {
            sb.append("|").append(state);
        }

        if (zip != null) {
            if (zip.length() > 5 && zip.contains("-")) {
                zip = zip.substring(0, 5);
            }
            sb.append("|").append(zip);
        }

        return sb.toString();
    }
}
