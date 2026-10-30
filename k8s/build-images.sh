#!/usr/bin/env bash
# Builds the service images and loads them into the kind cluster.
set -euo pipefail

CLUSTER="${CLUSTER:-bank}"
TAG="${TAG:-1.0.0}"
SERVICES=(eureka-server config-server user-service transaction-service account-service)

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

for svc in "${SERVICES[@]}"; do
  echo "==> Building bank/$svc:$TAG"
  docker build -t "bank/$svc:$TAG" -f "$ROOT/$svc/Dockerfile" "$ROOT"
done

for svc in "${SERVICES[@]}"; do
  echo "==> Loading bank/$svc:$TAG into kind cluster '$CLUSTER'"
  kind load docker-image "bank/$svc:$TAG" --name "$CLUSTER"
done
