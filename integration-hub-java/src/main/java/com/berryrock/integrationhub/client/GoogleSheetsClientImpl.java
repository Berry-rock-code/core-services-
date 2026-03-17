package com.berryrock.integrationhub.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GoogleSheetsClientImpl implements GoogleSheetsClient {
    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsClientImpl.class);

    // TODO: Inject Google Sheets client service configuration here

    @Override
    public boolean ping() {
        log.debug("Pinging Google Sheets API (placeholder)");
        return true;
    }
}
