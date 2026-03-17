package com.berryrock.integrationhub.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SalesforceClientImpl implements SalesforceClient {
    private static final Logger log = LoggerFactory.getLogger(SalesforceClientImpl.class);

    @Value("${integration.vendor.salesforce.enabled:false}")
    private boolean enabled;

    @Override
    public boolean isConfigured() {
        log.debug("Checking Salesforce configuration status");
        return enabled;
    }
}
