package com.berryrock.integrationhub.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Utility for normalizing address strings and building composite comparison keys.
 *
 * Part of the util package — used by
 * {@link com.berryrock.integrationhub.service.AddressSyncService} as the normalization
 * engine for the API-triggered sync path.
 *
 * This class applies a deterministic set of transformations (uppercasing, punctuation
 * removal, suffix abbreviation, directional expansion, city aliases, ZIP+4 stripping) so
 * that addresses that are textually different but semantically identical will produce the
 * same normalized string and therefore the same lookup key.
 *
 * Note: {@link com.berryrock.integrationhub.service.AddressNormalizationService} is a
 * parallel implementation used by the startup-time pipeline path. The two share the same
 * rule set but are independent Spring beans.
 */
@Component
public class AddressNormalizer
{
    /**
     * Normalizes a single address component (typically a street address line).
     *
     * Transformations applied in order:
     * <ol>
     *   <li>Uppercase the entire string</li>
     *   <li>Remove periods, commas, and apostrophes</li>
     *   <li>Expand street suffix words to standard USPS abbreviations
     *       (e.g., STREET -> ST, AVENUE -> AVE)</li>
     *   <li>Expand directional words (NORTH -> N, NORTHEAST -> NE, etc.)</li>
     *   <li>Apply city alias rules that collapse Saint Louis variants to STL</li>
     *   <li>Abbreviate remaining standalone SAINT to ST</li>
     *   <li>Collapse multiple whitespace characters to a single space</li>
     * </ol>
     *
     * @param address the raw address string to normalize; may be {@code null}
     * @return normalized uppercase string, or {@code null} if input is blank
     */
    public String normalize(String address)
    {
        if (StringUtils.isBlank(address))
        {
            return null;
        }

        String normalized = address.toUpperCase()
                .replace(".", "")
                .replace(",", "")
                .replace("'", "");

        // Standardize street suffixes to USPS abbreviations
        normalized = normalized.replaceAll("\\bSTREET\\b", "ST")
                .replaceAll("\\bAVENUE\\b", "AVE")
                .replaceAll("\\bPLACE\\b", "PL")
                .replaceAll("\\bROAD\\b", "RD")
                .replaceAll("\\bDRIVE\\b", "DR")
                .replaceAll("\\bLANE\\b", "LN")
                .replaceAll("\\bBOULEVARD\\b", "BLVD")
                .replaceAll("\\bCOURT\\b", "CT")
                .replaceAll("\\bTERRACE\\b", "TER");

        // Standardize directional words to single- or two-letter abbreviations
        normalized = normalized.replaceAll("\\bNORTH\\b", "N")
                .replaceAll("\\bSOUTH\\b", "S")
                .replaceAll("\\bEAST\\b", "E")
                .replaceAll("\\bWEST\\b", "W")
                .replaceAll("\\bNORTHEAST\\b", "NE")
                .replaceAll("\\bNORTHWEST\\b", "NW")
                .replaceAll("\\bSOUTHEAST\\b", "SE")
                .replaceAll("\\bSOUTHWEST\\b", "SW");

        // Collapse Saint Louis variants to STL before the general SAINT -> ST rule
        // to avoid producing "ST LOUIS" as an intermediate value
        normalized = normalized.replaceAll("\\bSAINT LOUIS\\b", "STL")
                .replaceAll("\\bST LOUIS\\b", "STL");

        // Abbreviate remaining standalone SAINT occurrences
        normalized = normalized.replaceAll("\\bSAINT\\b", "ST");

        // Collapse internal whitespace and trim
        normalized = normalized.trim().replaceAll("\\s+", " ");

        return normalized;
    }

    /**
     * Normalizes a city name using city-specific alias rules and general punctuation cleanup.
     *
     * In addition to the punctuation and whitespace rules applied by {@link #normalize},
     * this method resolves known city name variants to canonical abbreviations:
     * <ul>
     *   <li>Saint Louis / St Louis -> STL</li>
     *   <li>Oklahoma City -> OKC</li>
     *   <li>Kansas City -> KC</li>
     *   <li>Saint (prefix) -> ST</li>
     * </ul>
     *
     * @param city the raw city name to normalize; may be {@code null}
     * @return normalized city string, or {@code null} if input is blank
     */
    public String normalizeCity(String city)
    {
        if (StringUtils.isBlank(city))
        {
            return null;
        }

        String normalized = city.toUpperCase()
                .replace(".", "")
                .replace(",", "");

        // Apply Saint Louis variants first (order matters: must precede the SAINT -> ST rule)
        normalized = normalized.replaceAll("\\bSAINT LOUIS\\b", "STL")
                .replaceAll("\\bST LOUIS\\b", "STL");

        // Alias Oklahoma City and Kansas City to their common abbreviations
        normalized = normalized.replaceAll("\\bOKLAHOMA CITY\\b", "OKC")
                .replaceAll("\\bKANSAS CITY\\b", "KC");

        // Abbreviate remaining SAINT occurrences
        normalized = normalized.replaceAll("\\bSAINT\\b", "ST");

        normalized = normalized.trim().replaceAll("\\s+", " ");

        return normalized;
    }

    /**
     * Builds a pipe-delimited composite key from pre-normalized address components.
     *
     * The resulting key is used as the lookup key in the address matching maps. Components
     * that are {@code null} are omitted from the key. The ZIP code is truncated to 5 digits
     * if a ZIP+4 format ({@code 12345-6789}) is detected.
     *
     * @param address normalized street address line (from {@link #normalize})
     * @param city    normalized city name (from {@link #normalizeCity})
     * @param state   normalized state code (caller should uppercase before passing)
     * @param zip     postal code; ZIP+4 format is automatically truncated to 5 digits
     * @return pipe-delimited composite key, e.g., {@code "123 MAIN ST|TULSA|OK|74101"}
     */
    public String buildComparableKey(String address, String city, String state, String zip)
    {
        StringBuilder sb = new StringBuilder();

        if (address != null)
        {
            sb.append(address);
        }

        if (city != null)
        {
            sb.append("|").append(city);
        }

        if (state != null)
        {
            sb.append("|").append(state);
        }

        if (zip != null)
        {
            // Truncate ZIP+4 to the base 5-digit ZIP for consistent key comparison
            if (zip.length() > 5 && zip.contains("-"))
            {
                zip = zip.substring(0, 5);
            }
            sb.append("|").append(zip);
        }

        return sb.toString();
    }
}
