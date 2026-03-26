package com.berryrock.integrationhub.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Generic utility for grouping address records into lookup maps keyed by normalized address.
 *
 * Part of the util package — used by
 * {@link com.berryrock.integrationhub.service.AddressSyncService} to build the in-memory
 * indexes over Salesforce, Google Sheet, and Buildium records before the two-pass matching
 * loop runs. Because the method is generic, the same grouping logic handles all three
 * record types without duplication.
 *
 * Records whose normalized address is {@code null} or blank are silently excluded from the
 * map. Duplicate keys (multiple records sharing the same normalized address) are preserved
 * as multi-element lists; callers inspect list size to detect ambiguous matches.
 */
@Component
public class AddressMatcher
{
    /**
     * Groups a list of records into a map keyed by each record's normalized address string.
     *
     * The key is extracted by applying {@code addressExtractor} to each record. Records that
     * produce a {@code null} or empty key are omitted from the result. Multiple records with
     * the same key appear as a list under that key, allowing the caller to detect duplicates.
     *
     * @param records           the records to group; {@code null} is treated as an empty list
     * @param addressExtractor  a function that extracts the normalized address string from a record
     * @param <T>               the record type
     * @return a map from normalized address string to the list of records that share that key;
     *         never {@code null}, but may be empty
     */
    public <T> Map<String, List<T>> groupRecordsByNormalizedAddress(
            List<T> records,
            Function<T, String> addressExtractor)
    {
        Map<String, List<T>> map = new HashMap<>();

        if (records == null)
        {
            return map;
        }

        for (T record : records)
        {
            String normalizedAddress = addressExtractor.apply(record);

            // Skip records whose normalized key is null or blank -- they cannot be matched
            if (normalizedAddress != null && !normalizedAddress.isEmpty())
            {
                map.computeIfAbsent(normalizedAddress, k -> new ArrayList<>()).add(record);
            }
        }

        return map;
    }
}
