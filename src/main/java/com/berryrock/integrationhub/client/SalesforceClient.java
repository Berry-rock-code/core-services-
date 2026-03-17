package com.berryrock.integrationhub.client;

/**
 * Placeholder for Salesforce Integration Client.
 * 
 * Future Implementation Details:
 * - Handle OAuth/JWT Bearer Token flow if required.
 * - Establish typed DTOs mapping to Custom Objects.
 * - Configure separate rate-limit logic appropriate for SOQL quotas.
 */
public interface SalesforceClient {
    boolean isConfigured();
    // TODO: Add fetch/upsert objects
}
