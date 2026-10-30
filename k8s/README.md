# Kubernetes (kind)

Despliegue de la arquitectura en un clúster local de [kind](https://kind.sigs.k8s.io/).

## Requisitos
- Docker con buildx
- kind
- kubectl

## Despliegue

Los puertos 8000, 9090, 8761 y 8083 del host deben estar libres
(detén `docker compose` antes: `docker compose down`).

```bash
# 1. Crear el clúster (expone los puertos del host definidos en kind-config.yaml)
kind create cluster --config k8s/kind-config.yaml

# 2. Construir las imágenes de los servicios y cargarlas en el clúster
./k8s/build-images.sh

# 3. Aplicar los manifiestos
kubectl apply -k k8s

# 4. Esperar a que todo esté listo y probar los servicios a través de Kong
./k8s/smoke-test.sh
```

## Acceso desde el host

| Componente   | URL                       |
|--------------|---------------------------|
| Kong (proxy) | http://localhost:8000     |
| Keycloak     | http://localhost:9090     |
| Eureka       | http://localhost:8761     |
| Kafka UI     | http://localhost:8083     |

Los microservicios, las bases de datos y Kafka solo son accesibles dentro del clúster;
el tráfico externo entra por Kong.

```bash
TOKEN=$(curl -s -X POST http://localhost:9090/realms/bank/protocol/openid-connect/token \
  -d grant_type=password -d client_id=kong -d client_secret=kong-client-secret \
  -d username=admin-user -d password=admin123 | jq -r .access_token)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8000/api/v1/user/get-active
```

## Estructura

```
k8s/
├── kind-config.yaml       # clúster kind + mapeo de puertos al host
├── kustomization.yaml     # punto de entrada (kubectl apply -k k8s)
├── build-images.sh        # build + kind load de las imágenes
├── smoke-test.sh          # espera los pods y prueba los servicios vía Kong
├── namespace.yaml
├── databases/             # PostgreSQL (StatefulSet + PVC) por servicio y Keycloak
├── kafka/                 # broker (KRaft) y kafka-ui
├── keycloak/              # Keycloak + realm-export.json (ConfigMap)
├── kong/                  # Kong DB-less + kong.yml (ConfigMap)
└── services/              # eureka, config-server y microservicios
```

## Diferencias con docker-compose
- **Kong en modo DB-less**: las rutas se cargan desde `kong/kong.yml` al arrancar.
  Sustituye a `kong-db`, `kong-migration` y `kong-deck`.
  Para cambiar rutas: editar `kong/kong.yml` y volver a aplicar con `kubectl apply -k k8s`
  (el ConfigMap cambia de nombre y Kong se reinicia automáticamente).
- **Orden de arranque**: en lugar de `depends_on`, cada servicio usa `initContainers`
  que esperan a sus dependencias (config-server, base de datos, Keycloak, Kafka).
- **Eureka**: los servicios se registran con la IP del pod (`EUREKA_INSTANCE_PREFER_IP_ADDRESS`),
  porque el hostname de un pod no se resuelve por DNS dentro del clúster.
- **`enableServiceLinks: false`** en todos los pods: evita que Kubernetes inyecte variables como
  `KAFKA_UI_PORT=tcp://...`, que Kafka, Kong y Spring interpretarían como configuración.
- `keycloak/realm-export.json` y `kong/kong.yml` son copias de los archivos de la raíz
  (kustomize no permite referenciar archivos fuera de su directorio). Si cambias uno, actualiza el otro.

## Limpieza

```bash
kind delete cluster --name bank
```
