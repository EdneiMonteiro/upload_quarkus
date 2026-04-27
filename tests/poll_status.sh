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
set -euo pipefail

WEB_URL="${1:-${WEB_URL:-}}"
IDS_FILE="${IDS_FILE:-tests/upload_ids.txt}"
OUT_FILE="${OUT_FILE:-tests/final_results.txt}"
MAX_WAIT="${MAX_WAIT_SECONDS:-3600}"
POLL_INTERVAL="${POLL_INTERVAL_SECONDS:-30}"

if [[ -z "$WEB_URL" ]]; then
  echo "Uso: $0 <WEB_URL>"
  echo "Ou defina a variavel WEB_URL no ambiente."
  exit 1
fi

START=$(date +%s)

get_state() {
  curl -s "${WEB_URL}/uploads/$1" 2>/dev/null | python3 -c "
import sys, re
html = sys.stdin.read()
s = re.search(r'<dd>(\w+)</dd>', html)
r = re.search(r'Registros processados.*?<dd>(\d+)</dd>', html, re.S)
print(f'{s.group(1) if s else \"unknown\"}|{r.group(1) if r else \"0\"}')
" 2>/dev/null || echo "unknown|0"
}

while true; do
  elapsed=$(( $(date +%s) - START ))
  all_done=true
  completed=0
  total=0

  while IFS='|' read -r num uid rows; do
    total=$((total+1))
    st=$(get_state "$uid")
    state=$(echo "$st" | cut -d'|' -f1)
    if [[ "$state" == "completed" || "$state" == "failed" ]]; then
      completed=$((completed+1))
    else
      all_done=false
    fi
  done < "$IDS_FILE"

  echo "[${elapsed}s] ${completed}/${total} done"

  if [[ "$all_done" == true ]]; then
    echo "All done! Writing results..."
    break
  fi
  if [[ $elapsed -gt $MAX_WAIT ]]; then
    echo "TIMEOUT after ${MAX_WAIT}s"
    break
  fi
  sleep "$POLL_INTERVAL"
done

echo "num|upload_id|rows_sent|state|records_processed" > "$OUT_FILE"
while IFS='|' read -r num uid rows; do
  st=$(get_state "$uid")
  echo "${num}|${uid}|${rows}|${st}" >> "$OUT_FILE"
  echo "  ${num}: ${st}"
done < "$IDS_FILE"
echo ""
echo "Results saved to $OUT_FILE"
