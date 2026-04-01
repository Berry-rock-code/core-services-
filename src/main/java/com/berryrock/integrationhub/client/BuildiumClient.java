package com.berryrock.integrationhub.client;
// LAYER: PLATFORM -- stays in integration-hub

import com.berryrock.integrationhub.model.BuildiumAddressRecord;

import java.util.List;
import java.util.Map;

/**
 * Contract for all interactions with the Buildium property management API.
 *
 * Part of the client package — defines the operations needed by the address pipeline to
 * retrieve rental unit and active-lease data from Buildium. The primary method for
 * pipeline use is {@link #fetchActiveLeaseAddresses()}, which performs the units-plus-leases
 * join internally and returns typed records.
 *
 * The lower-level {@link #getRentalsPage} and {@link #getAllRentals} methods are available
 * for direct rental property listing use cases or for health-check purposes.
 *
 * Implementations must honor the {@code integration.vendor.buildium.enabled} flag: when
 * the flag is {@code false}, all methods must return empty results without making any
 * outbound HTTP calls.
 */
public interface BuildiumClient
{
    /**
     * Performs a lightweight connectivity check against the Buildium API.
     *
     * Attempts to fetch a single page of rentals with a limit of 1. A non-null response
     * is considered a successful ping. Returns {@code false} immediately if the integration
     * is disabled.
     *
     * @return {@code true} if the Buildium API responded successfully; {@code false} otherwise
     */
    boolean ping();

    /**
     * Fetches a single page of rental properties from the Buildium {@code /v1/rentals} endpoint.
     *
     * @param limit  maximum number of records to return in this page
     * @param offset zero-based record offset for pagination
     * @return list of raw property maps as returned by the API; empty list on error or no data
     */
    List<Map<String, Object>> getRentalsPage(int limit, int offset);

    /**
     * Fetches all rental properties by repeatedly calling {@link #getRentalsPage} until
     * the API returns a partial page.
     *
     * @return complete list of raw property maps; empty list if the integration is disabled
     *         or an error occurs
     */
    List<Map<String, Object>> getAllRentals();

    /**
     * Fetches all active-lease address records by joining the units and leases endpoints.
     *
     * Performs three internal API calls:
     * <ol>
     *   <li>Fetches all rental units (which carry the address data)</li>
     *   <li>Fetches all active leases (which carry the unit-to-lease mapping)</li>
     *   <li>Joins the two result sets on unit ID to produce typed {@link BuildiumAddressRecord} objects</li>
     * </ol>
     * Units without a street address are skipped. Units with no associated active lease
     * still produce a record but with {@code buildiumLeaseId} set to {@code null}.
     *
     * @return list of address records for all units with a street address; empty list if
     *         the integration is disabled or an error occurs
     */
    List<BuildiumAddressRecord> fetchActiveLeaseAddresses();
}
