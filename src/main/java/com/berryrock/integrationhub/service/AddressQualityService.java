package com.berryrock.integrationhub.service;
// LAYER: FEATURE:address-pipeline -- moves to address-pipeline repo

import com.berryrock.integrationhub.model.SalesforceAddressRecord;
import org.springframework.stereotype.Service;

/**
 * Classifies the data quality of a Salesforce address record before it enters the matching phase.
 *
 * Part of the address pipeline — every Salesforce Opportunity record is run through
 * {@link #classify} before being added to any lookup map. The classification drives which
 * matching passes the record participates in:
 * <ul>
 *   <li>{@link Quality#CLEAN} — all four components present and internally consistent;
 *       eligible for both the full-key match and the address-only fallback.</li>
 *   <li>{@link Quality#PARTIAL} — at least one of city, state, or postal code is missing;
 *       eligible for the address-only fallback pass only.</li>
 *   <li>{@link Quality#SUSPICIOUS} — the street address line contains data that belongs in
 *       other fields (city name, state code, or ZIP embedded in the street), suggesting
 *       a data-entry problem in Salesforce. Excluded from all matching.</li>
 * </ul>
 */
@Service
public class AddressQualityService
{
    /**
     * Three-tier quality classification for Salesforce address records.
     */
    public enum Quality
    {
        /**
         * All four address components are present and the street line does not appear to
         * contain data from other fields. Records in this tier are eligible for all matching passes.
         */
        CLEAN,

        /**
         * The street address is present but at least one of city, state, or postal code is
         * missing. Records in this tier can participate in the address-only fallback pass.
         */
        PARTIAL,

        /**
         * The street address line appears to contain city, state, or ZIP data, indicating
         * a data entry problem. Records in this tier are excluded from all matching.
         */
        SUSPICIOUS
    }

    /**
     * Classifies the quality of the address carried by the given Salesforce record.
     *
     * Applies the PARTIAL check first (missing component fields), then the SUSPICIOUS check
     * (cross-field contamination). A record that passes both checks is classified CLEAN.
     *
     * @param record the Salesforce Opportunity record to evaluate
     * @return the quality tier: {@link Quality#CLEAN}, {@link Quality#PARTIAL},
     *         or {@link Quality#SUSPICIOUS}
     */
    public Quality classify(SalesforceAddressRecord record)
    {
        // A missing street line or any missing geographic component downgrades to PARTIAL
        if (isBlank(record.getAddressLine())
                || isBlank(record.getCity())
                || isBlank(record.getState())
                || isBlank(record.getPostalCode()))
        {
            return Quality.PARTIAL;
        }

        // Detect cross-field contamination that indicates a data entry problem
        if (isSuspicious(record.getAddressLine(), record.getCity(), record.getState(), record.getPostalCode()))
        {
            return Quality.SUSPICIOUS;
        }

        return Quality.CLEAN;
    }

    /**
     * Detects signs of cross-field contamination in a street address line.
     *
     * Checks whether the postal code, city, or state appears literally inside the street
     * line. Also catches a common Salesforce data entry pattern where the state abbreviation
     * "OK" or the city abbreviation "OKC" is accidentally entered as part of the street.
     *
     * @param streetLine  the street address line to inspect (must not be null)
     * @param city        city component, used as a literal substring to search for
     * @param state       state code, matched as a whole word
     * @param postalCode  postal code, used as a literal substring to search for
     * @return {@code true} if the street line appears to contain data from other fields
     */
    private boolean isSuspicious(String streetLine, String city, String state, String postalCode)
    {
        if (streetLine == null)
        {
            return false;
        }

        String upperStreet = streetLine.toUpperCase();

        // ZIP code should never appear literally inside the street line
        if (postalCode != null && upperStreet.contains(postalCode.toUpperCase()))
        {
            return true;
        }

        // City name should not be embedded in the street line
        if (city != null && upperStreet.contains(city.toUpperCase()))
        {
            return true;
        }

        // State abbreviation should not appear as a standalone word in the street line
        if (state != null && upperStreet.matches(".*\\b" + state.toUpperCase() + "\\b.*"))
        {
            return true;
        }

        // Catch "OK" (Oklahoma state) or "OKC" (Oklahoma City) accidentally placed in the street field
        if (upperStreet.matches(".*\\bOK\\b.*") || upperStreet.matches(".*\\bOKC\\b.*"))
        {
            return true;
        }

        return false;
    }

    /**
     * Returns {@code true} if the given string is {@code null} or contains only whitespace.
     *
     * @param val the string to test
     * @return {@code true} if blank
     */
    private boolean isBlank(String val)
    {
        return val == null || val.trim().isEmpty();
    }
}
