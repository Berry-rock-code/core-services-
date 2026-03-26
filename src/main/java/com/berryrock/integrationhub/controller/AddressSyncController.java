package com.berryrock.integrationhub.controller;

import com.berryrock.integrationhub.dto.AddressSyncRequest;
import com.berryrock.integrationhub.dto.AddressSyncSummary;
import com.berryrock.integrationhub.service.AddressSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for triggering the address sync pipeline on demand.
 *
 * Part of the controller layer — exposes a single endpoint that allows an operator or
 * upstream scheduler to run the Salesforce-to-Loan-Tape-to-Buildium sync without
 * restarting the service.
 *
 * Unlike the startup-time
 * {@link com.berryrock.integrationhub.service.AddressPipelineRunner}, this endpoint
 * uses {@link AddressSyncService} which supports additional per-request options such as
 * dry-run mode, local CSV input, and selective Buildium enrichment.
 *
 * Base path: {@code /api/v1/workflows/address-sync}
 */
@RestController
@RequestMapping("/api/v1/workflows/address-sync")
public class AddressSyncController
{
    private final AddressSyncService addressSyncService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param addressSyncService service that executes the sync pipeline
     */
    public AddressSyncController(AddressSyncService addressSyncService)
    {
        this.addressSyncService = addressSyncService;
    }

    /**
     * Triggers a full address sync pipeline run.
     *
     * Accepts a JSON body that controls runtime behavior (dry-run, sheet target,
     * Buildium enrichment). Returns a summary of fetch counts, match counts, and
     * any warnings generated during the run.
     *
     * @param request options for this sync run
     * @return {@code 200 OK} with an {@link AddressSyncSummary} as the response body
     */
    @PostMapping("/run")
    public ResponseEntity<AddressSyncSummary> run(@RequestBody AddressSyncRequest request)
    {
        return ResponseEntity.ok(addressSyncService.runSync(request));
    }
}
