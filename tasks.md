# Tareas pendientes

## Build / Docker

- [x] **Instalar `docker-buildx`**
  - Los Dockerfiles usan `RUN --mount=type=cache,target=/root/.m2`, que requiere BuildKit.
  - Sin buildx, Docker usa el builder legacy y falla con:
    `the --mount option requires BuildKit`.
  - Solución (Arch): `sudo pacman -S docker-buildx`.

- [x] **Verificar que las 5 imágenes se construyen**
  - `docker compose up --build` levanta toda la arquitectura; todos los contenedores quedan `healthy`.

- [x] **Puertos de `account-service` y `transaction-service` desalineados en `docker-compose.yml`**
  - Config Server les asigna `8082` y `8081`, pero compose mapeaba `808x:8080` y el healthcheck consultaba `8080`.
  - Resultado: ambos quedaban `unhealthy` y no eran accesibles desde el host (Kong sí funcionaba porque usa los puertos reales).
  - Corregido: mapeo `8081:8081` / `8082:8082`, healthcheck al puerto real y `EXPOSE` en sus Dockerfiles.

- [x] **Agregar `.dockerignore`**
  - El contexto enviado al daemon bajó de ~451 MB a ~640 KB: solo `pom.xml` y `src` de cada servicio.

- [ ] **(Opcional) Cambiar el `context` de cada servicio en `docker-compose.yml` a su propia carpeta**
  - Ahora que cada servicio es independiente, puede ser `context: ./account-service`.
  - En ese caso, el Dockerfile tendría que copiar `pom.xml` y `src`, sin el prefijo del servicio.

- [ ] **Pipeline de CI/CD (GitHub Actions)**
  - Se armó un workflow y se retiró por ahora. Diseño propuesto:
    - `test`: `mvn verify` por servicio con JDK 17 y `actions/setup-java` (`cache: maven`).
    - `image`: build con caché de capas de BuildKit (`type=gha`, `mode=max`), escaneo con Trivy y push a GHCR en `main`.
    - `e2e`: clúster kind + `kubectl apply -k k8s` + `k8s/smoke-test.sh`.
  - El cache mount de `/root/.m2` en los Dockerfiles no persiste en runners efímeros: si cambia el `src` de un servicio, su imagen vuelve a descargar las dependencias.
  - Al armarlo: Trivy primero en modo solo-reporte y luego bloqueante; construir solo los servicios que cambiaron (`dorny/paths-filter`).

- [x] **Tests de `transaction-service` fallaban**
  - `TransactionControllerTest` probaba endpoints que ya no existen: se reemplazó por `TransactionQueryControllerTest`, `TransactionAnalyticsControllerTest` y `TransactionSupportControllerTest` (PostgreSQL con Testcontainers, datos de Flyway `V2`, `SecurityConfig` real y JWT simulado).
  - `TransactionQueryServiceTest.getTransactionsInDateRange` usaba el repositorio mock como si fuera real; se reescribió con mocks.
  - Se eliminaron los scripts obsoletos `db/sql-test/` (referenciaban tablas inexistentes).

- [ ] **Endpoints sin cubrir en los tests de `transaction-service`**
  - `TransactionCreationController` (`/create/transfer`, `/purchase`, `/service-payment`): llama a account-service por Feign; requiere mockear `AccountClient` o WireMock.
  - `/analytics/type-summary`, `/average-days`, `/partition-by-amount` y `/max-by-type` (este último marcado `// FIX: Internal Server Error`).

- [ ] **Perfil `test` de `transaction-service` obsoleto**
  - `src/main/resources/application-test.yml` apunta a `user_db`, a un realm `bank-realm` inexistente e incluye la contraseña de Gmail; con ese perfil `SecurityConfig` (y el bean `ModelMapper`) no se carga. Ningún test lo usa ya.

- [ ] **`TransactionQueryServiceTest` levanta un contenedor PostgreSQL que no usa**
  - Es un test unitario con Mockito; el `@Container` y el `@DynamicPropertySource` sobran y alargan el build.

## Maven

- [ ] **Compilación local con JDK 25 falla por Lombok**
  - Desde JDK 23, `javac` no ejecuta annotation processors a menos que se habiliten explícitamente.
  - Afecta a `account-service`, `user-service` y `transaction-service`: errores `cannot find symbol` en getters, setters y `builder()`.
  - En Docker no pasa porque la imagen usa JDK 17.
  - Opciones:
    - Compilar con `mvn package -Dmaven.compiler.proc=full`.
    - Agregar `<maven.compiler.proc>full</maven.compiler.proc>` en `<properties>` de cada pom.
    - Usar JDK 17/21 localmente.

- [x] **`mvnw` y `.mvn/` quedaron en la raíz sin `pom.xml`**
  - Eliminados: el build se hace con Docker (imagen `maven:3.9.5-eclipse-temurin-17`) o con Maven instalado localmente.

- [ ] **Versiones duplicadas entre servicios**
  - Spring Boot `3.5.8`, Spring Cloud `2025.0.0`, `modelmapper 3.2.0` y `testcontainers 1.21.4` ahora están repetidas en cada pom.
  - Al actualizar hay que cambiarlas en todos, o mover testcontainers a su BOM (`testcontainers-bom`).

## Dependencias innecesarias

- [x] **Quitar `spring-boot-starter-data-jpa` de `eureka-server` y `config-server`**
  - Solo la tenían porque el pom raíz la imponía a todos los módulos.
  - Por eso ambas clases main usan `@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)`.
  - Al quitar la dependencia, eliminar también ese `exclude`.

- [x] **Revisar `lombok` y `spring-boot-starter-web` en `eureka-server` y `config-server`**
  - También venían heredadas del pom raíz.
  - Ninguno de los dos servicios usa Lombok.
  - `spring-cloud-starter-netflix-eureka-server` y `spring-cloud-config-server` ya traen la capa web.

## Kubernetes (`k8s/`)

- [ ] **Secretos en texto plano en `k8s/kustomization.yaml`**
  - Las credenciales de PostgreSQL y del admin de Keycloak están como `literals` en el `secretGenerator`.
  - Para algo más allá de desarrollo local: Sealed Secrets, External Secrets o SOPS.

- [ ] **`kong.yml` y `realm-export.json` duplicados**
  - `k8s/kong/kong.yml` y `k8s/keycloak/realm-export.json` son copias de `kong/kong.yml` y `keycloak/realm-export.json` (kustomize no permite referenciar archivos fuera de su directorio).
  - Opciones: dejar una sola copia (p. ej. que `docker-compose.yml` monte los de `k8s/`) o sincronizarlos en CI.

- [ ] **Datos de Kafka efímeros**
  - El broker no tiene volumen (igual que en docker-compose); los tópicos se pierden si el pod se reinicia.

- [ ] **Probar flujos entre servicios en Kubernetes**
  - Verificado: health, endpoints de los 3 servicios vía Kong con token de Keycloak, registro en Eureka y kafka-ui.
  - Pendiente: flujo de registro (user-service → Kafka → account-service) y Feign (transaction-service → account-service).

## Seguridad

- [ ] **Contraseña de aplicación de Gmail en el repositorio**
  - `spring.mail.password` aparece en texto plano en `config-server/src/main/resources/config/user-service.yml` y en los `application-test*.yml` de `user-service` y `transaction-service`.
  - Revocarla en la cuenta de Google y moverla a una variable de entorno / Secret.

## Otros

- [ ] **`eureka.instance.hostname` de user-service apunta a `eureka-server`**
  - En `config-server/src/main/resources/config/user-service.yml`: `instance.hostname: ${EUREKA_HOST:localhost}`.
  - user-service se registra en Eureka con el hostname de Eureka; si otro servicio lo llamara por Feign, la llamada iría a Eureka.
  - En Kubernetes se mitiga con `EUREKA_INSTANCE_PREFER_IP_ADDRESS=true`; en compose debería usar `prefer-ip-address: true` como los demás servicios.

- [ ] **Mensaje de error incorrecto en `account-service` `get-by-email`**
  - Responde `"Account not found with number: <email>"`.
