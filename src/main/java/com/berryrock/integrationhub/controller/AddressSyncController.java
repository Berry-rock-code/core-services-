package com.berryrock.integrationhub.controller;

import com.berryrock.integrationhub.dto.AddressSyncRequest;
import com.berryrock.integrationhub.dto.AddressSyncSummary;
import com.berryrock.integrationhub.service.AddressSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workflows/address-sync")
public class AddressSyncController {

    private final AddressSyncService addressSyncService;

    public AddressSyncController(AddressSyncService addressSyncService) {
        this.addressSyncService = addressSyncService;
    }

    @PostMapping("/run")
    public ResponseEntity<AddressSyncSummary> runAddressSync(@RequestBody AddressSyncRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }

        AddressSyncSummary summary = addressSyncService.run(request);

        // We return the summary regardless of dry-run or full-run mode
        // Only return non-200 if it was an unexpected failure
        if ("FAILED".equals(summary.getStatus())) {
            return ResponseEntity.internalServerError().body(summary);
        }

        return ResponseEntity.ok(summary);
    }
}
