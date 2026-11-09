# Kubernetes (kind)

Deployment of the architecture on a local [kind](https://kind.sigs.k8s.io/) cluster.

## Requirements
- Docker with buildx
- kind
- kubectl

## Deployment

Host ports 8000, 9090, 8761 and 8083 must be free
(stop `docker compose` first: `docker compose down`).

```bash
# 1. Create the cluster (exposes the host ports defined in kind-config.yaml)
kind create cluster --config k8s/kind-config.yaml

# 2. Build the service images and load them into the cluster
./k8s/build-images.sh

# 3. Apply the manifests
kubectl apply -k k8s

# 4. Wait until everything is ready and test the services through Kong
./k8s/smoke-test.sh
```

## Access from the host

| Component    | URL                       |
|--------------|---------------------------|
| Kong (proxy) | http://localhost:8000     |
| Keycloak     | http://localhost:9090     |
| Eureka       | http://localhost:8761     |
| Kafka UI     | http://localhost:8083     |
| Mailpit      | http://localhost:8025     |

The microservices, the databases and Kafka are only reachable inside the cluster;
external traffic comes in through Kong.

```bash
TOKEN=$(curl -s -X POST http://localhost:9090/realms/bank/protocol/openid-connect/token \
  -d grant_type=password -d client_id=kong -d client_secret=kong-client-secret \
  -d username=admin-user -d password=admin123 | jq -r .access_token)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8000/api/v1/user/get-active
```

## Structure

```
k8s/
├── kind-config.yaml       # kind cluster + port mapping to the host
├── kustomization.yaml     # entry point (kubectl apply -k k8s)
├── build-images.sh        # builds the images and loads them with kind load
├── smoke-test.sh          # waits for the pods and tests the services through Kong
├── namespace.yaml
├── databases/             # PostgreSQL (StatefulSet + PVC) per service and for Keycloak
├── kafka/                 # broker (KRaft) and kafka-ui
├── mailpit/               # fake SMTP server for Keycloak's verification emails
├── keycloak/              # Keycloak + realm-export.json (ConfigMap)
├── kong/                  # DB-less Kong + kong.yml (ConfigMap)
└── services/              # eureka, config-server and the microservices
```

## Differences from docker-compose
- **DB-less Kong**: the routes are loaded from `kong/kong.yml` at startup.
  It replaces `kong-db`, `kong-migration` and `kong-deck`.
  To change routes: edit `kong/kong.yml` and apply again with `kubectl apply -k k8s`
  (the ConfigMap gets a new name and Kong restarts automatically).
- **Startup order**: instead of `depends_on`, each service uses `initContainers`
  that wait for its dependencies (config-server, database, Keycloak, Kafka).
- **Eureka**: services register with the pod IP (`EUREKA_INSTANCE_PREFER_IP_ADDRESS`),
  because a pod's hostname does not resolve through DNS inside the cluster.
- **`enableServiceLinks: false`** on every pod: keeps Kubernetes from injecting variables such as
  `KAFKA_UI_PORT=tcp://...`, which Kafka, Kong and Spring would read as configuration.
- `keycloak/realm-export.json` and `kong/kong.yml` are the only copy of those files:
  `docker-compose.yml` also mounts them from `k8s/`.

## Cleanup

```bash
kind delete cluster --name bank
```
