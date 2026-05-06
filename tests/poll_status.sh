#!/usr/bin/env bash
# Copyright (c) 2026 Ednei Monteiro. Licensed under the MIT License.
# See LICENSE and DISCLAIMER.md in the project root for details.
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
