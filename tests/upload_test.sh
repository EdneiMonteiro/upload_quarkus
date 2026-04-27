#!/usr/bin/env bash
# Disclaimer
# Notice: Any sample scripts, code, or commands comes with the following notification.
#
# This Sample Code is provided for the purpose of illustration only and is not intended to be used in a production
# environment. THIS SAMPLE CODE AND ANY RELATED INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER
# EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
# PARTICULAR PURPOSE.
#
# We grant You a nonexclusive, royalty-free right to use and modify the Sample Code and to reproduce and distribute
# the object code form of the Sample Code, provided that You agree: (i) to not use Our name, logo, or trademarks to
# market Your software product in which the Sample Code is embedded; (ii) to include a valid copyright notice on Your
# software product in which the Sample Code is embedded; and (iii) to indemnify, hold harmless, and defend Us and Our
# suppliers from and against any claims or lawsuits, including attorneys' fees, that arise or result from the use or
# distribution of the Sample Code.
#
# Please note: None of the conditions outlined in the disclaimer above will supersede the terms and conditions
# contained within the Customers Support Services Description.
# upload_test.sh - Upload test CSV files to the Upload Quarkus web endpoint and poll status.
# Usage: ./tests/upload_test.sh <WEB_URL>
# Example: ./tests/upload_test.sh https://ca-web-poc-upload.whiteocean-xxxx.eastus2.azurecontainerapps.io
set -euo pipefail

WEB_URL="${1:?Usage: $0 <WEB_URL>}"
DATA_DIR="tests/data"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-1800}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-15}"

echo "=== Upload Quarkus PoC Load Test ==="
echo "Target: $WEB_URL"
echo ""

# Generate test CSVs if not present
if [ ! -d "$DATA_DIR" ] || [ -z "$(ls -A "$DATA_DIR" 2>/dev/null)" ]; then
  echo "Generating 10 test CSV files (5000-10000 rows each)..."
  python3 tests/generate_csv.py --rows 0 --files 10 --output-dir "$DATA_DIR"
  echo ""
fi

declare -a UPLOAD_IDS=()

echo "--- Uploading 10 files ---"
for csv_file in "$DATA_DIR"/sample_*.csv; do
  filename=$(basename "$csv_file")
  row_count=$(( $(wc -l < "$csv_file") - 1 ))

  response=$(curl -sS -w "\n%{http_code}" \
    -X POST "$WEB_URL/uploads" \
    -F "file=@$csv_file" \
    -o /dev/null \
    -D - 2>/dev/null || true)

  # Extract redirect location to get upload_id
  location=$(echo "$response" | grep -i '^location:' | head -1 | tr -d '\r' | awk '{print $2}')

  if [ -n "$location" ]; then
    upload_id=$(echo "$location" | grep -oE '[^/]+$')
    UPLOAD_IDS+=("$upload_id")
    echo "  [OK] $filename ($row_count rows) -> upload_id=$upload_id"
  else
    # Try direct API approach via backend
    api_response=$(curl -s -X POST "$WEB_URL/uploads" -F "file=@$csv_file" 2>/dev/null)
    upload_id=$(echo "$api_response" | python3 -c "import sys,json; print(json.load(sys.stdin).get('upload_id','FAILED'))" 2>/dev/null || echo "FAILED")
    if [ "$upload_id" != "FAILED" ] && [ -n "$upload_id" ]; then
      UPLOAD_IDS+=("$upload_id")
      echo "  [OK] $filename ($row_count rows) -> upload_id=$upload_id"
    else
      echo "  [FAIL] $filename - could not extract upload_id"
    fi
  fi
done

echo ""
echo "--- Monitoring processing (polling every ${POLL_INTERVAL_SECONDS}s, timeout ${TIMEOUT_SECONDS}s) ---"

START_TIME=$(date +%s)
ALL_DONE=false

while [ "$ALL_DONE" = false ]; do
  ELAPSED=$(( $(date +%s) - START_TIME ))
  if [ "$ELAPSED" -gt "$TIMEOUT_SECONDS" ]; then
    echo "[TIMEOUT] Not all uploads completed within ${TIMEOUT_SECONDS}s"
    break
  fi

  ALL_DONE=true
  COMPLETED=0
  PROCESSING=0
  QUEUED=0
  FAILED=0

  for upload_id in "${UPLOAD_IDS[@]}"; do
    status_json=$(curl -s "$WEB_URL/uploads/$upload_id" 2>/dev/null | python3 -c "
import sys, re, json
html = sys.stdin.read()
# Try to extract status from the HTML
state_match = re.search(r'<dd>(\w+)</dd>', html)
records_match = re.search(r'Registros processados.*?<dd>(\d+)</dd>', html, re.S)
state = state_match.group(1) if state_match else 'unknown'
records = records_match.group(1) if records_match else '0'
print(json.dumps({'state': state, 'records_processed': int(records)}))
" 2>/dev/null || echo '{"state":"unknown","records_processed":0}')

    state=$(echo "$status_json" | python3 -c "import sys,json; print(json.load(sys.stdin)['state'])")
    records=$(echo "$status_json" | python3 -c "import sys,json; print(json.load(sys.stdin)['records_processed'])")

    case "$state" in
      completed) COMPLETED=$((COMPLETED + 1)) ;;
      processing) PROCESSING=$((PROCESSING + 1)); ALL_DONE=false ;;
      queued)     QUEUED=$((QUEUED + 1)); ALL_DONE=false ;;
      failed)     FAILED=$((FAILED + 1)) ;;
      *)          ALL_DONE=false ;;
    esac
  done

  TOTAL=${#UPLOAD_IDS[@]}
  printf "\r  [%ds] completed=%d  processing=%d  queued=%d  failed=%d  total=%d" \
    "$ELAPSED" "$COMPLETED" "$PROCESSING" "$QUEUED" "$FAILED" "$TOTAL"

  if [ "$ALL_DONE" = false ]; then
    sleep "$POLL_INTERVAL_SECONDS"
  fi
done

echo ""
echo ""
echo "=== Final Results ==="
for upload_id in "${UPLOAD_IDS[@]}"; do
  status_json=$(curl -s "$WEB_URL/uploads/$upload_id" 2>/dev/null | python3 -c "
import sys, re, json
html = sys.stdin.read()
state_match = re.search(r'<dd>(\w+)</dd>', html)
records_match = re.search(r'Registros processados.*?<dd>(\d+)</dd>', html, re.S)
state = state_match.group(1) if state_match else 'unknown'
records = records_match.group(1) if records_match else '0'
print(f'{state:12s}  records={records}')
" 2>/dev/null || echo "unknown")
  echo "  $upload_id  $status_json"
done

ELAPSED_TOTAL=$(( $(date +%s) - START_TIME ))
echo ""
echo "Total time: ${ELAPSED_TOTAL}s"
