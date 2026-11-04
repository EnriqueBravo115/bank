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

- [x] **`kong.yml` y `realm-export.json` duplicados**
  - Se eliminaron `kong/` y `keycloak/` de la raíz; `docker-compose.yml` monta `k8s/kong/kong.yml` y `k8s/keycloak/realm-export.json`.
  - Nota: los tests de `user-service` usan su propio realm (`user-service/src/main/resources/keycloak/realm-export.json`), distinto al de despliegue.

- [ ] **Datos de Kafka efímeros**
  - El broker no tiene volumen (igual que en docker-compose); los tópicos se pierden si el pod se reinicia.

- [ ] **Probar flujos entre servicios en Kubernetes**
  - Verificado: health, endpoints de los 3 servicios vía Kong con token de Keycloak, registro en Eureka y kafka-ui.
  - Pendiente: flujo de registro (user-service → Kafka → account-service) y Feign (transaction-service → account-service).

## Seguridad

- [ ] **Contraseña de aplicación de Gmail en el repositorio**
  - Ya se quitó de `user-service` (no usaba el correo) y de `config-server/.../user-service.yml`.
  - Sigue en `transaction-service/src/main/resources/application-test.yml` y en el historial de git: **hay que revocarla** en la cuenta de Google.

## user-service

- [x] **Recorte de endpoints de consulta (de ~37 a 15)**
  - Se eliminaron los filtros sin caso de uso (por género, país, estado civil, ocupación, fuente/rango de ingreso, tipo de documento), los `get-all-detailed`/`get-active` duplicados por tabla, los `exists/{userId}`, `get-full-name`, `get-by-role` y el endpoint de depuración `/test-jwt`.
  - IDOR corregido: un `CUSTOMER_BASIC` podía leer el perfil, el KYC (RFC/CURP) y los ingresos de cualquier usuario por id. Ahora el cliente usa `GET .../me` (resuelto con el `sub` del JWT) y las consultas por id/email/RFC/CURP son solo `ADMIN`.
  - Eliminado código muerto: `EmailService`/`EmailConfig`, `UserNotFoundEmailException`, `PersonalInfoResponse`, `FinancialProfileResponse` y las dependencias `mail`, `openfeign`, `resilience4j` y `oauth2-client`.
  - Registro: si falla el guardado en BD después de crear el usuario en Keycloak, se borra el usuario de Keycloak (antes quedaba huérfano y bloqueaba el email). Las violaciones de unicidad responden `409`.

- [x] **Pasos del registro protegidos con JWT**
  - Antes `PUT /register/{userId}/profile|kyc|financial` era `permitAll` con ids secuenciales: cualquiera podía completar el onboarding de otro usuario.
  - Ahora solo `POST /register` es público. Tras registrarse, el cliente inicia sesión y los pasos usan `PUT /register/me/profile|kyc|financial`, resueltos con el `sub` del JWT. `RegisterStatusFilter` no aplica a `/register/**`, pero sigue bloqueando el resto de endpoints mientras el registro esté incompleto.

- [x] **Rol asignado en el KYC y cambio de nivel de cliente**
  - `users.role` es `NULL` hasta el KYC; ahí se asigna `CUSTOMER_BASIC` en `user_db` y en Keycloak a la vez.
  - `PUT /api/v1/user/{userId}/role` (`ADMIN`) cambia entre `CUSTOMER_BASIC`, `CUSTOMER_PREMIUM` y `CUSTOMER_BUSINESS`. En Keycloak `CUSTOMER_BASIC` se conserva siempre (todos los endpoints lo exigen) y el nivel se agrega encima. `ADMIN` no se puede otorgar por la API (400) y los usuarios sin KYC responden 409.

- [x] **Validar duplicados antes de crear el usuario en Keycloak**
  - `POST /register` comprueba email (sin distinguir mayúsculas) y teléfono en `user_db` antes de llamar a Keycloak; el paso KYC comprueba CURP y RFC. Todos responden `409` con el campo duplicado.
  - Un email que solo existe en Keycloak (p. ej. un admin creado en la consola) también responde `409` en vez de `500`.
  - Si dos registros simultáneos pasan las comprobaciones, la restricción `UNIQUE` de la BD responde `409` y se borra el usuario de Keycloak.

- [x] **Evento `CreateAccountEvent` con patrón outbox**
  - El KYC guarda el evento en `outbox_event` en la misma transacción; `OutboxScheduler` lo publica cada 2 s (`FOR UPDATE SKIP LOCKED`), espera el ack de Kafka y lo marca `SENT`. Si Kafka falla queda `PENDING` y se reintenta; tras 10 intentos pasa a `FAILED`.
  - account-service ignora solicitudes repetidas (`existsByUserId`) y `account.user_id` es `UNIQUE`.

- [x] **Funcionalidad que faltaba**
  - Bloquear/desbloquear (`PUT /api/v1/user/{userId}/active`, `ADMIN`): actualiza `active`, deshabilita el usuario en Keycloak (y cierra sus sesiones) y avisa por outbox a account-service, que congela (`FROZEN`) o reabre la cuenta.
  - Cambio de contacto (`PUT /api/v1/user/me/email` y `/me/phone`, cliente): valida duplicados; el email cambia también el `username` de Keycloak, vuelve a pedir verificación y se propaga a account-service por outbox. El usuario ya no puede editar `username`/`email` desde la consola de cuenta de Keycloak.
  - Paginación (`page`, `size` de 1 a 100) en `get-active`, `profile/search` y `profile/get-by-status`; responden `{content, page}`.
  - Validaciones: CURP y RFC (persona física) con regex, `birthday` como `LocalDate` (`dd/MM/yyyy`, fechas imposibles rechazadas) y mayoría de edad (`@Adult`).
  - Verificación de email: Keycloak la envía al registrarse (Mailpit en local: http://localhost:8025), se puede reenviar con `POST /register/me/verify-email` y el KYC la exige.

- [ ] **Pendientes que salieron de lo anterior**
  - Un usuario bloqueado conserva su access token hasta que expira (5 min); si hace falta corte inmediato, validar `active` en `RegisterStatusFilter` o reducir la vida del token.
  - Antes del KYC el usuario no puede corregir un email mal escrito (los endpoints de contacto exigen el registro completo).
  - No se comprueba que CURP/RFC coincidan entre sí ni con `birthday`.
  - Revisar/reintentar eventos `FAILED` del outbox y limpiar los `SENT` antiguos.

- [ ] **Configuración de tests dentro de `src/main`**
  - `TestKeycloakConfig`, `TestPostgresConfig` y `application-test-*.yml` se empaquetan en la imagen; moverlos a `src/test`.

## Otros

- [ ] **`eureka.instance.hostname` de user-service apunta a `eureka-server`**
  - En `config-server/src/main/resources/config/user-service.yml`: `instance.hostname: ${EUREKA_HOST:localhost}`.
  - user-service se registra en Eureka con el hostname de Eureka; si otro servicio lo llamara por Feign, la llamada iría a Eureka.
  - En Kubernetes se mitiga con `EUREKA_INSTANCE_PREFER_IP_ADDRESS=true`; en compose debería usar `prefer-ip-address: true` como los demás servicios.

- [ ] **Mensaje de error incorrecto en `account-service` `get-by-email`**
  - Responde `"Account not found with number: <email>"`.
