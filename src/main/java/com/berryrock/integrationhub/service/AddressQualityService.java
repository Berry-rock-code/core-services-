package com.berryrock.integrationhub.service;

import com.berryrock.integrationhub.model.SalesforceAddressRecord;
import org.springframework.stereotype.Service;

@Service
public class AddressQualityService {

    public enum Quality {
        CLEAN,
        PARTIAL,
        SUSPICIOUS
    }

    public Quality classify(SalesforceAddressRecord record) {
        if (isBlank(record.getAddressLine()) ||
            isBlank(record.getCity()) ||
            isBlank(record.getState()) ||
            isBlank(record.getPostalCode())) {
            return Quality.PARTIAL;
        }

        if (isSuspicious(record.getAddressLine(), record.getCity(), record.getState(), record.getPostalCode())) {
            return Quality.SUSPICIOUS;
        }

        return Quality.CLEAN;
    }

    private boolean isSuspicious(String streetLine, String city, String state, String postalCode) {
        if (streetLine == null) return false;
        String upperStreet = streetLine.toUpperCase();

        if (postalCode != null && upperStreet.contains(postalCode.toUpperCase())) {
            return true;
        }

        if (city != null && upperStreet.contains(city.toUpperCase())) {
            return true;
        }

        if (state != null && upperStreet.matches(".*\\b" + state.toUpperCase() + "\\b.*")) {
            return true;
        }

        // embedded OK or OKC
        if (upperStreet.matches(".*\\bOK\\b.*") || upperStreet.matches(".*\\bOKC\\b.*")) {
            return true;
        }

        return false;
    }

    private boolean isBlank(String val) {
        return val == null || val.trim().isEmpty();
    }
}
