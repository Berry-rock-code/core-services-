package com.berryrock.integrationhub.client;

/**
 * Placeholder for Buildium Integration Client.
 * 
 * Future Implementation Details:
 * - Inject RestTemplate or WebClient configured with timeouts.
 * - Manage custom authentication headers (x-buildium-client-id, x-buildium-client-secret).
 * - Handle exponential backoff retries for 429 and 5xx.
 * - Map transient API errors to specific exception classes.
 */
public interface BuildiumClient {
    boolean ping();
    
    // TODO: Add methods like List<Lease> getActiveLeases()
}
