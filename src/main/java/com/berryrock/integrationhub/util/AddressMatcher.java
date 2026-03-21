package com.berryrock.integrationhub.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class AddressMatcher {

    /**
     * Groups a list of records by their normalized address.
     *
     * @param records              The list of records to group.
     * @param addressExtractor     A function to extract the normalized address from a record.
     * @param <T>                  The type of the record.
     * @return A map where the key is the normalized address and the value is a list of records with that address.
     */
    public <T> Map<String, List<T>> groupRecordsByNormalizedAddress(List<T> records, Function<T, String> addressExtractor) {
        Map<String, List<T>> map = new HashMap<>();
        if (records == null) {
            return map;
        }

        for (T record : records) {
            String normalizedAddress = addressExtractor.apply(record);
            if (normalizedAddress != null && !normalizedAddress.isEmpty()) {
                map.computeIfAbsent(normalizedAddress, k -> new ArrayList<>()).add(record);
            }
        }
        return map;
    }
}
