package com.berryrock.integrationhub.service;

import com.berryrock.integrationhub.dto.AddressSyncRequest;
import com.berryrock.integrationhub.dto.AddressSyncSummary;
import com.berryrock.integrationhub.model.AddressRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AddressSyncService
{
    public AddressSyncSummary runSync(AddressSyncRequest request)
    {
        List<AddressRecord> salesforceRecords = buildMockSalesforceRecords();
        List<AddressRecord> googleSheetRows = buildMockGoogleSheetRows();
        List<AddressRecord> buildiumRecords = request.isEnrichBuildium()
                ? buildMockBuildiumRecords()
                : new ArrayList<>();

        int googleSheetMatches = countMatches(salesforceRecords, googleSheetRows);
        int buildiumMatches = countMatches(googleSheetRows, buildiumRecords);

        AddressSyncSummary summary = new AddressSyncSummary();
        summary.setStatus("SUCCESS");
        summary.setSalesforceRecordsFetched(salesforceRecords.size());
        summary.setGoogleSheetRowsFetched(googleSheetRows.size());
        summary.setBuildiumRecordsFetched(buildiumRecords.size());
        summary.setGoogleSheetMatches(googleSheetMatches);
        summary.setBuildiumMatches(buildiumMatches);
        summary.setUnmatchedCount(Math.max(0, salesforceRecords.size() - googleSheetMatches));

        if (request.isDryRun())
        {
            summary.getWarnings().add("Dry run enabled. No external systems were modified.");
        }

        return summary;
    }

    private int countMatches(List<AddressRecord> left, List<AddressRecord> right)
    {
        int matches = 0;

        for (AddressRecord l : left)
        {
            for (AddressRecord r : right)
            {
                if (l.getNormalizedAddress() != null
                        && l.getNormalizedAddress().equals(r.getNormalizedAddress()))
                {
                    matches++;
                    break;
                }
            }
        }

        return matches;
    }

    private List<AddressRecord> buildMockSalesforceRecords()
    {
        List<AddressRecord> records = new ArrayList<>();
        records.add(new AddressRecord("SF-001", "123 Main St Oklahoma City OK 73102", normalize("123 Main St Oklahoma City OK 73102")));
        records.add(new AddressRecord("SF-002", "500 Oak Ave Tulsa OK 74103", normalize("500 Oak Ave Tulsa OK 74103")));
        records.add(new AddressRecord("SF-003", "999 Missing Rd Edmond OK 73034", normalize("999 Missing Rd Edmond OK 73034")));
        return records;
    }

    private List<AddressRecord> buildMockGoogleSheetRows()
    {
        List<AddressRecord> records = new ArrayList<>();
        records.add(new AddressRecord("ROW-10", "123 Main Street Oklahoma City OK 73102", normalize("123 Main Street Oklahoma City OK 73102")));
        records.add(new AddressRecord("ROW-11", "500 Oak Avenue Tulsa OK 74103", normalize("500 Oak Avenue Tulsa OK 74103")));
        records.add(new AddressRecord("ROW-12", "777 Other Pl Norman OK 73069", normalize("777 Other Pl Norman OK 73069")));
        return records;
    }

    private List<AddressRecord> buildMockBuildiumRecords()
    {
        List<AddressRecord> records = new ArrayList<>();
        records.add(new AddressRecord("B-100", "123 Main Street Oklahoma City OK 73102", normalize("123 Main Street Oklahoma City OK 73102")));
        records.add(new AddressRecord("B-101", "777 Other Place Norman OK 73069", normalize("777 Other Place Norman OK 73069")));
        return records;
    }

    private String normalize(String address)
    {
        if (address == null)
        {
            return null;
        }

        return address.toUpperCase()
                .replace(".", "")
                .replace(",", "")
                .replace("STREET", "ST")
                .replace("AVENUE", "AVE")
                .replace("PLACE", "PL")
                .trim()
                .replaceAll("\\s+", " ");
    }
}