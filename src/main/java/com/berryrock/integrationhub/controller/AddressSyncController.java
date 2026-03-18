package com.berryrock.integrationhub.controller;

import com.berryrock.integrationhub.dto.AddressSyncRequest;
import com.berryrock.integrationhub.dto.AddressSyncSummary;
import com.berryrock.integrationhub.service.AddressSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workflows/address-sync")
public class AddressSyncController
{
    private final AddressSyncService addressSyncService;

    public AddressSyncController(AddressSyncService addressSyncService)
    {
        this.addressSyncService = addressSyncService;
    }

    @PostMapping("/run")
    public ResponseEntity<AddressSyncSummary> run(@RequestBody AddressSyncRequest request)
    {
        return ResponseEntity.ok(addressSyncService.runSync(request));
    }
}