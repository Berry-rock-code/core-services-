package com.berryrock.integrationhub.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BuildiumClientImpl implements BuildiumClient {

    private static final Logger log = LoggerFactory.getLogger(BuildiumClientImpl.class);

    // TODO: Inject configuration properties and RestTemplate/WebClient here

    @Override
    public boolean ping() {
        log.debug("Pinging Buildium API (placeholder)");
        return true;
    }
}
