import requests
import json
import sys

BASE_URL = "http://localhost:8080"

payload = {
    "dryRun": True,
    "syncGoogleSheet": True,
    "enrichBuildium": True,
    "batchSize": 200,
    "sheetId": "test-sheet-id",
    "sheetName": "test-sheet-name"
}

print(f"Triggering Address Sync Workflow on {BASE_URL}...")
print(f"Payload: {json.dumps(payload, indent=2)}")

try:
    response = requests.post(
        f"{BASE_URL}/api/v1/workflows/address-sync/run",
        json=payload,
        timeout=300
    )
    response.raise_for_status()
    summary = response.json()
    print("\n--- Workflow Summary ---")
    print(json.dumps(summary, indent=2))
except requests.exceptions.RequestException as e:
    print(f"\nError triggering workflow: {e}", file=sys.stderr)
    if e.response is not None:
        try:
            print(f"Response: {json.dumps(e.response.json(), indent=2)}", file=sys.stderr)
        except:
            print(f"Response: {e.response.text}", file=sys.stderr)
    sys.exit(1)
