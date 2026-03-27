package com.berryrock.integrationhub.util;

import org.springframework.stereotype.Component;

/**
 * Normalizes raw address strings and builds composite comparison keys.
 *
 * The canonical normalization implementation shared by both the startup-time
 * batch pipeline ({@link com.berryrock.integrationhub.service.AddressPipelineService})
 * and the API-triggered sync path
 * ({@link com.berryrock.integrationhub.service.AddressSyncService}).
 *
 * Transformations applied (in order):
 * <ol>
 *   <li>Uppercase the entire string</li>
 *   <li>Remove periods, commas, hash signs, and apostrophes</li>
 *   <li>Collapse and trim whitespace</li>
 *   <li>Strip ZIP+4 suffix down to 5-digit ZIP</li>
 *   <li>Abbreviate street suffixes (STREET -> ST, AVENUE -> AVE, etc.)</li>
 *   <li>Resolve city aliases (OKLAHOMA CITY -> OKC, ST LOUIS / SAINT LOUIS -> STL, KANSAS CITY -> KC)</li>
 *   <li>Abbreviate directional words — compound before cardinal to avoid NW being parsed as N + W</li>
 *   <li>Strip trailing apartment/unit suffixes (e.g., "- 12" at end of string)</li>
 * </ol>
 */
@Component
public class AddressNormalizer
{
    /**
     * Normalizes a single address component string.
     *
     * Applies the full transformation chain described in the class Javadoc. Passing
     * {@code null} returns an empty string rather than throwing.
     *
     * @param address raw address string; may be {@code null}
     * @return normalized uppercase string with punctuation removed and abbreviations applied;
     *         empty string if input is {@code null}
     */
    public String normalize(String address)
    {
        if (address == null)
        {
            return "";
        }

        String normalized = address.toUpperCase();

        // Remove common punctuation characters that vary across data sources
        normalized = normalized.replaceAll("[.,#']", "");

        // Collapse multiple whitespace characters to a single space and trim edges
        normalized = normalized.replaceAll("\\s+", " ").trim();

        // Strip ZIP+4 extended format to the base 5-digit ZIP for consistent matching
        normalized = normalized.replaceAll("\\b(\\d{5})-\\d{4}\\b", "$1");

        // Standardize street suffix words to USPS abbreviations
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

        // Resolve known city aliases to their canonical abbreviations.
        // Multi-word aliases must be applied before single-word directionals
        // to avoid OKLAHOMA being collapsed to a directional "N" or similar.
        normalized = replaceWord(normalized, "OKLAHOMA CITY", "OKC");
        normalized = replaceWord(normalized, "ST LOUIS", "STL");
        normalized = replaceWord(normalized, "SAINT LOUIS", "STL");
        normalized = replaceWord(normalized, "KANSAS CITY", "KC");

        // Abbreviate compound directional words before single-letter directionals
        // to avoid NW being parsed as N + W
        normalized = replaceWord(normalized, "NORTHWEST", "NW");
        normalized = replaceWord(normalized, "NORTHEAST", "NE");
        normalized = replaceWord(normalized, "SOUTHWEST", "SW");
        normalized = replaceWord(normalized, "SOUTHEAST", "SE");

        // Abbreviate single-word cardinal directions
        normalized = replaceWord(normalized, "NORTH", "N");
        normalized = replaceWord(normalized, "SOUTH", "S");
        normalized = replaceWord(normalized, "EAST", "E");
        normalized = replaceWord(normalized, "WEST", "W");

        // Remove trailing apartment/unit designators like "- 12" that appear in some sources
        normalized = normalized.replaceAll("\\s*-\\s*\\d+$", "");

        return normalized.trim();
    }

    /**
     * Builds a pipe-delimited composite key from the four address components.
     *
     * Each component is individually normalized via {@link #normalize} before being
     * concatenated with {@code |} as the delimiter. A {@code null} component contributes
     * an empty segment (two consecutive pipes) rather than being skipped, so keys remain
     * structurally consistent regardless of which fields are populated.
     *
     * Example result: {@code "123 MAIN ST|TULSA|OK|74101"}
     *
     * @param addressLine street address component; may be {@code null}
     * @param city        city component; may be {@code null}
     * @param state       state component; may be {@code null}
     * @param postalCode  ZIP or postal code component; may be {@code null}
     * @return pipe-delimited normalized key
     */
    public String buildNormalizedKey(String addressLine, String city, String state, String postalCode)
    {
        StringBuilder sb = new StringBuilder();

        // Each segment normalized independently; null -> empty segment to keep delimiter structure consistent
        sb.append(addressLine != null ? normalize(addressLine) : "").append("|");
        sb.append(city != null ? normalize(city) : "").append("|");
        sb.append(state != null ? normalize(state) : "").append("|");
        sb.append(postalCode != null ? normalize(postalCode) : "");

        return sb.toString();
    }

    /**
     * Replaces whole-word occurrences of {@code target} with {@code replacement} in {@code text}.
     *
     * Uses word-boundary anchors ({@code \b}) so that, for example, replacing STREET
     * does not affect STREETCAR.
     *
     * @param text        the string to search within
     * @param target      the word to find (should be uppercase)
     * @param replacement the abbreviation to substitute
     * @return the modified string
     */
    private String replaceWord(String text, String target, String replacement)
    {
        return text.replaceAll("\\b" + target + "\\b", replacement);
    }
}
