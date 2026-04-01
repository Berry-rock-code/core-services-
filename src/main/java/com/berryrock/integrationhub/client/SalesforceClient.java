package com.berryrock.integrationhub.client;
// LAYER: PLATFORM -- stays in integration-hub

import com.berryrock.integrationhub.model.SalesforceAddressRecord;

import java.util.List;

/**
 * Contract for all interactions with the Salesforce REST API.
 *
 * Part of the client package — defines the operations needed by the address pipeline to
 * retrieve Opportunity records whose address fields will be matched against the Loan Tape
 * and Buildium data.
 *
 * The implementation uses the Salesforce JWT bearer token flow: a short-lived JWT is
 * signed with a private RSA key and exchanged for an access token at the Salesforce
 * OAuth2 token endpoint. All subsequent SOQL queries are issued against the instance URL
 * returned in the token response.
 *
 * Implementations must honor the {@code integration.vendor.salesforce.enabled} flag:
 * when {@code false}, {@link #fetchAddressesForGoogleSheetBuildiumSync()} must return
 * an empty list without authenticating or querying Salesforce.
 */
public interface SalesforceClient
{
    /**
     * Returns whether the Salesforce integration is configured and enabled.
     *
     * @return {@code true} if {@code integration.vendor.salesforce.enabled} is {@code true}
     */
    boolean isConfigured();

    /**
     * Fetches all Salesforce Opportunity records that have a non-null street address,
     * intended for use in the Google Sheet / Buildium address sync pipeline.
     *
     * Constructs a SOQL query dynamically from the field names configured in
     * {@code integration.vendor.salesforce.fields.*}. If a
     * {@code integration.vendor.salesforce.query.stage-filter} is set, a
     * {@code StageName = '<filter>'} predicate is added to the WHERE clause.
     *
     * Handles Salesforce query result pagination automatically via the
     * {@code nextRecordsUrl} field in each response page.
     *
     * @return list of typed address records; empty list if the integration is disabled or
     *         an error occurs during authentication or query execution
     */
    List<SalesforceAddressRecord> fetchAddressesForGoogleSheetBuildiumSync();
}
