#!/usr/bin/env bash
# Smoke test of the deployment: waits for every pod and calls each service through Kong.
set -euo pipefail

NAMESPACE="${NAMESPACE:-bank}"
KONG_URL="${KONG_URL:-http://localhost:8000}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:9090}"

echo "==> Waiting for pods in namespace '$NAMESPACE'"
kubectl -n "$NAMESPACE" wait --for=condition=Ready pods --all --timeout=600s

echo "==> Requesting token from Keycloak"
TOKEN=$(curl -sf -X POST "$KEYCLOAK_URL/realms/bank/protocol/openid-connect/token" \
  -d grant_type=password \
  -d client_id=kong \
  -d client_secret=kong-client-secret \
  -d username=admin-user \
  -d password=admin123 |
  python3 -c 'import sys, json; print(json.load(sys.stdin)["access_token"])')

failed=0

# check <description> <expected status> <curl args...>
check() {
  local description=$1 expected=$2
  shift 2
  local status
  status=$(curl -s -o /dev/null -w '%{http_code}' --retry 5 --retry-delay 3 --retry-all-errors "$@" || true)
  if [ "$status" = "$expected" ]; then
    echo "  OK   $description -> $status"
  else
    echo "  FAIL $description -> $status (expected $expected)"
    failed=1
  fi
}

echo "==> Health checks through Kong"
for svc in user transaction account; do
  check "$svc health" 200 "$KONG_URL/$svc/actuator/health"
done

echo "==> Authenticated requests through Kong"
check "user get-active" 200 \
  -H "Authorization: Bearer $TOKEN" "$KONG_URL/api/v1/user/get-active"
check "account get-by-email (no account yet)" 404 \
  -H "Authorization: Bearer $TOKEN" "$KONG_URL/api/v1/account/get-by-email/ana.martinez92@gmail.com"
check "transaction by-sources" 200 -X GET \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '["646180110400000001"]' "$KONG_URL/api/v1/transaction/query/by-sources"
check "user get-active without token" 401 "$KONG_URL/api/v1/user/get-active"

if [ "$failed" -ne 0 ]; then
  echo "==> Smoke test FAILED"
  kubectl -n "$NAMESPACE" get pods
  exit 1
fi
echo "==> Smoke test passed"
