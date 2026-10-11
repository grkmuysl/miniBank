#!/usr/bin/env bash
# Steady transfer load through the gateway, for chaos tests.
# Usage: ./scripts/load.sh <number-of-transfers>
# Requires: USER1, PASS1, IBAN1, USER2, PASS2, IBAN2 environment variables.
set -u

COUNT=${1:-30}
GW=http://localhost:8000/api/v1/transfers
KC=http://localhost:8180/realms/minibank/protocol/openid-connect/token
CLIENT=minibank-test

# Small, medium, above new-beneficiary limit, above amount limit
AMOUNTS=(5 25 75 150 300 999 2500 15000 60000)

token() {
  curl -s -X POST "$KC" -d grant_type=password -d client_id="$CLIENT" \
       -d username="$1" -d password="$2" |
    sed -E 's/.*"access_token":"([^"]+)".*/\1/'
}

for ((i = 1; i <= COUNT; i++)); do
  # Refresh tokens every 10 transfers (access tokens are short-lived)
  if (( (i - 1) % 10 == 0 )); then
    T1=$(token "$USER1" "$PASS1"); T2=$(token "$USER2" "$PASS2")
  fi

  if (( i % 2 )); then T=$T1; S=$IBAN1; D=$IBAN2; else T=$T2; S=$IBAN2; D=$IBAN1; fi
  A=${AMOUNTS[$((RANDOM % ${#AMOUNTS[@]}))]}

  CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GW" \
    -H "Authorization: Bearer $T" -H "Idempotency-Key: load-$i-$(date +%s%N)" \
    -H "Content-Type: application/json" \
    -d "{\"sourceIban\":\"$S\",\"targetIban\":\"$D\",\"amount\":$A.00,\"currency\":\"TRY\"}")
  echo "$(date +%T) #$i $A TRY -> $CODE"

  # Each user sends one transfer every ~13s: under the gateway's 5/min per-user limit.
  sleep 6.5
done