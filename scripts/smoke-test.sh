#!/usr/bin/env bash
# smoke-test.sh — post-deployment verification script for SnakeForged.
#
# Usage:
#   ./scripts/smoke-test.sh <BASE_URL>
#
# Example:
#   ./scripts/smoke-test.sh http://localhost:8080
#
# Exits 0 when every check passes; non-zero (equal to the number of failures)
# when one or more checks fail.  Designed to be run as a CI/CD smoke step
# immediately after the application is deployed and healthy.

set -euo pipefail

# ── Argument validation ────────────────────────────────────────────────────────
BASE_URL="${1:-}"
if [[ -z "$BASE_URL" ]]; then
    echo "Usage: $0 <BASE_URL>" >&2
    echo "  Example: $0 http://localhost:8080" >&2
    exit 1
fi

# Strip trailing slash for consistent URL construction
BASE_URL="${BASE_URL%/}"

# ── Helpers ───────────────────────────────────────────────────────────────────
PASS=0
FAIL=0

check_pass() { echo "[PASS] $1"; PASS=$((PASS + 1)); }
check_fail() { echo "[FAIL] $1"; FAIL=$((FAIL + 1)); }

# Returns the HTTP status code for a GET request.
http_get_status() {
    curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$1"
}

# Returns the full response body for a GET request.
http_get_body() {
    curl -s --max-time 10 "$1"
}

# Posts JSON and returns the HTTP status code.
http_post_status() {
    local url="$1"
    local body="$2"
    curl -s -o /dev/null -w "%{http_code}" \
         --max-time 10 \
         -X POST \
         -H "Content-Type: application/json" \
         -d "$body" \
         "$url"
}

echo "================================================"
echo " SnakeForged Smoke Test"
echo " Target: $BASE_URL"
echo "================================================"
echo ""

# ── Check 1: Static homepage ──────────────────────────────────────────────────
STATUS=$(http_get_status "$BASE_URL/")
if [[ "$STATUS" == "200" ]]; then
    check_pass "GET /  →  $STATUS (static content served)"
else
    check_fail "GET /  →  $STATUS (expected 200)"
fi

# ── Check 2: Liveness probe ───────────────────────────────────────────────────
BODY=$(http_get_body "$BASE_URL/actuator/health/liveness")
STATUS=$(http_get_status "$BASE_URL/actuator/health/liveness")
if [[ "$STATUS" == "200" ]] && echo "$BODY" | grep -q '"UP"'; then
    check_pass "GET /actuator/health/liveness  →  $STATUS, status=UP"
else
    check_fail "GET /actuator/health/liveness  →  $STATUS, body=$BODY"
fi

# ── Check 3: Difficulties endpoint ───────────────────────────────────────────
STATUS=$(http_get_status "$BASE_URL/api/v1/difficulties")
BODY=$(http_get_body "$BASE_URL/api/v1/difficulties")
# Count top-level JSON array elements — expect exactly 3
ITEM_COUNT=$(echo "$BODY" | grep -o '"name"' | wc -l | tr -d ' ')
if [[ "$STATUS" == "200" ]] && [[ "$ITEM_COUNT" -ge 3 ]]; then
    check_pass "GET /api/v1/difficulties  →  $STATUS, $ITEM_COUNT items"
else
    check_fail "GET /api/v1/difficulties  →  $STATUS, $ITEM_COUNT items (expected 200 + ≥3)"
fi

# ── Check 4: POST highscore ───────────────────────────────────────────────────
POST_BODY='{"nickname":"smoketest","score":42,"difficulty":"EASY"}'
STATUS=$(http_post_status "$BASE_URL/api/v1/highscores" "$POST_BODY")
if [[ "$STATUS" == "201" ]]; then
    check_pass "POST /api/v1/highscores  →  $STATUS (score submitted)"
else
    check_fail "POST /api/v1/highscores  →  $STATUS (expected 201)"
fi

# ── Check 5: GET highscores ───────────────────────────────────────────────────
STATUS=$(http_get_status "$BASE_URL/api/v1/highscores?difficulty=EASY")
if [[ "$STATUS" == "200" ]]; then
    check_pass "GET /api/v1/highscores?difficulty=EASY  →  $STATUS"
else
    check_fail "GET /api/v1/highscores?difficulty=EASY  →  $STATUS (expected 200)"
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "================================================"
echo " Results: $PASS passed, $FAIL failed"
echo "================================================"

if [[ $FAIL -gt 0 ]]; then
    exit $FAIL
fi

exit 0
