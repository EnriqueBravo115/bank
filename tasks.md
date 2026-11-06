# Tareas pendientes

## account-service

### Seguridad (crítico)

- [ ] **Cualquier cliente puede mover dinero de cualquier cuenta**
  - `/api/v1/account/movement/*` es accesible vía Kong (ruta `/api/v1/account`) para cualquier `CUSTOMER_BASIC`, y nadie comprueba que la cuenta origen pertenezca al `sub` del JWT (`MovementController`).
  - Restringir los movimientos a transaction-service (rol de servicio con client credentials o bloquear la ruta en Kong) y validar la propiedad de la cuenta.

- [ ] **Montos sin validar en los movimientos**
  - Los `Client*Request` no tienen `@Valid`/`@Positive`: una transferencia con monto negativo resta dinero a la cuenta destino.

- [ ] **IDOR en consultas de cuenta**
  - `get-by-account-number`, `get-by-clabe` y `get-by-email` devuelven la cuenta de cualquier usuario a un `CUSTOMER_BASIC`.
  - Añadir `GET /me` (resuelto con el `sub` del JWT) y dejar las consultas por identificador solo a `ADMIN`.

### Funcionalidad

- [ ] **No se crea saldo al abrir la cuenta**
  - `createAccount` no inserta `account_balance`; todo movimiento falla con "balance not found" y hoy solo funciona con SQL manual.

- [ ] **Depósitos / abonos**
  - `AccountBalanceController` está comentado y `AccountBalanceServiceImpl.createAccountBalance` lanza `UnsupportedOperationException`.

- [ ] **Tarjetas**
  - `Card` solo existe como modelo y tabla: no hay service ni endpoints.
  - La compra recibe `sourceCardNumber` y `cvv` y los ignora.
  - No guardar el CVV en BD (PCI-DSS).

- [ ] **Límites de transacción incompletos**
  - No hay forma de crearlos.
  - `isWithinDailyLimit` compara el monto de una sola operación, no la suma del día.
  - `max_transactions` no se usa.

- [ ] **Campos del movimiento ignorados**
  - `transactionFee` en el retiro, `SecurityVerificationMethod` y la moneda (se puede transferir entre cuentas de monedas distintas).

### Consistencia

- [ ] **Condición de carrera en el saldo**
  - El modelo "último registro de `account_balance`" no tiene bloqueo (`SELECT ... FOR UPDATE` o `@Version`): dos débitos simultáneos leen el mismo saldo (doble gasto).

- [ ] **Tipos de saldo mal modelados**
  - Todos los movimientos se guardan como `INTRADAY`, pero `hasSufficientFundsIncludingHolds` busca `AVAILABLE`/`ON_HOLD`; `checkHolds` siempre es `false`; "el último por fecha" mezcla tipos.

- [ ] **Movimientos sin idempotencia**
  - No aceptan idempotency key; ver la tarea de inconsistencia distribuida en transaction-service.

- [ ] **Identificadores de cuenta**
  - `account_number = "ACC-" + millis` no es `UNIQUE` y puede repetirse.
  - `ClabeGenerator` debería calcular el dígito de control real.

### Calidad

- [ ] **Código muerto**
  - `AccountServiceImpl.updateEmail(accountNumber, …)` (`// TODO`, sin uso; ya lo cubre `updateEmailByUserId`).
  - `FeignConfiguration` sin ningún cliente Feign.

- [ ] **Mensaje de error incorrecto en `get-by-email`**
  - Responde `"Account not found with number: <email>"`.

- [ ] **Tests**
  - Solo 12 tests unitarios con Mockito; faltan tests de integración (controllers, consumers de Kafka, Testcontainers).

## transaction-service

### Seguridad y consistencia (crítico)

- [ ] **Inconsistencia distribuida al crear transacciones**
  - `AbstractTransactionProcessor.process` mueve el dinero en account-service y después guarda en su BD:
    - si el guardado falla, el dinero se movió sin registro;
    - si Feign hace timeout pero account-service ya hizo commit, se registra `FAILED` aunque el dinero se movió.
  - Guardar primero `PENDING`, enviar `transactionCode` como idempotency key a account-service y conciliar (o saga por Kafka).

- [ ] **IDOR en consultas y creación**
  - Historial, consultas y analítica de cliente aceptan cualquier `sourceIdentifier`.
  - La creación no verifica que el origen pertenezca al usuario (TODO en `TransactionQueryController`, `/{transactionCode}`).

- [ ] **`IdentifierType` no se respeta**
  - Acepta `CLABE`, `ACCOUNT` y `CARD`, pero account-service solo busca por `accountNumber`.

### Funcionalidad

- [ ] **Endpoint de retiro**
  - El servicio y `WithdrawalTransactionProcessor` existen, pero `TransactionCreationController` no lo expone.

- [ ] **Cancelación y reversa**
  - `cancelTransaction` está vacío y `reverseTransfer` es un TODO.

### Bugs

- [ ] **Consultas devuelven `transactionCode` y `description` en `null`** (`// FIX`)
  - `/all`, `/by-sources`, `/max`, `/date-range`, `/amount-range`, `/identifier-type`.

- [ ] **`/analytics/max-by-type` responde 500** (`// FIX: Internal Server Error`).

- [ ] **`process` envuelve todo en `RuntimeException`**
  - Cualquier error termina en un 500 genérico.

### Calidad

- [x] **Código muerto**
  - Eliminados: `TransactionHelper`, `KafkaTopicConstants`, `ErrorMessage`, `AccountServiceException`, `ApiRequestException`, `AccountStatus`, `TransferResponse`, los 8 `FilterStatus*Request`, `TransferTransactionRepository`, `cancelTransaction` vacío y `@Service` en las interfaces.
  - Dependencias quitadas: `spring-kafka`, `spring-kafka-test` y `spring-boot-starter-oauth2-client`.
  - `TestConstants` movido a `src/test`.

- [ ] **Datos de prueba en migraciones de producción**
  - `V2__populate_transactions.sql` siembra datos de ejemplo; moverlo a `src/test` o a una migración solo de desarrollo. Los tests ya no dependen de él (cada clase borra y crea sus datos).
  - Ojo: borrar una migración ya aplicada hace fallar la validación de Flyway en las BD existentes; mejor una `V3` que borre esas filas o `spring.flyway.locations` distintas por perfil.

- [x] **Tests al patrón de user-service**
  - Tests de integración por controller con PostgreSQL real; cada clase crea sus datos (`TransactionFixtures`) en vez de usar la semilla `V2`.
  - Eliminados los tests unitarios de services con Mockito (`TransactionQueryServiceTest` levantaba un contenedor que no usaba) y `TransactionServiceTestHelper`.
  - Nuevos: `TransactionCreationControllerTest` (`@MockitoBean AccountClient`: lo que se envía a account-service y lo que se guarda con COMPLETED/DECLINED, validación 400, 401/403) y `AccountClientFallbackTest` (422 → DECLINED con el mensaje, 404/401/5xx → FAILED); analítica `type-summary`, `average-days` y `partition-by-amount`.
  - Pendiente: `/max-by-type` (500) y las aserciones de los bugs conocidos, al corregirlos. Que el `@CircuitBreaker` realmente invoque el fallback ante un error de Feign requeriría WireMock.

## user-service

- [x] **Fallo parcial en Keycloak**
  - En el KYC, si `assignRole` funciona y falla `updateRegistrationStatus`, la BD hace rollback pero el rol queda asignado en Keycloak. Mismo riesgo en `updateEmail` y en el cambio de rol.
  - Corregido con `RollbackCompensation`: antes de llamar a Keycloak se registra cómo deshacerlo y se ejecuta si la transacción hace rollback (por un fallo de Keycloak o del commit). Cubre KYC (quita `CUSTOMER_BASIC` y vuelve `registration_status` a `PROFILE`), cambio de rol (restaura el nivel anterior), cambio de email (restaura email, `username` y su verificación) y bloqueo/desbloqueo (restaura `enabled`; las sesiones cerradas no vuelven).
  - Si la compensación también falla, solo queda un `log.error`: no hay reintento automático.

- [x] **Mantenimiento del outbox**
  - Revisar/reintentar eventos `FAILED` (endpoint de admin o job) y purgar los `SENT` antiguos.
  - Endpoints `ADMIN`: `GET /api/v1/user/outbox/failed` (paginado) y `POST /api/v1/user/outbox/{id}/retry` (vuelve a `PENDING` con los intentos en 0; `409` si no está `FAILED`, `404` si no existe).
  - Limpieza diaria (`outbox.cleanup.cron`, 03:00 por defecto) de los `SENT` con más de `outbox.cleanup.sent-retention` (`P7D`).
  - Backoff exponencial entre intentos (5 s, 10 s, 20 s… hasta 15 min): un evento pasa a `FAILED` tras unos 40 min fallando, no tras ~20 s de Kafka caído.
  - Orden por usuario: mientras un evento está `FAILED` o esperando su backoff, los siguientes del mismo usuario esperan. Antes se publicaban y al reintentar el viejo se aplicaba fuera de orden (p. ej. un bloqueo después del desbloqueo).

- [ ] **Orden de eventos del outbox con varias réplicas**
  - `FOR UPDATE SKIP LOCKED` evita enviar dos veces la misma fila, pero si dos réplicas publican a la vez pueden enviar dos eventos del mismo usuario en distinto orden. Opciones: un advisory lock para que publique una sola réplica, o lock por `aggregate_id`.

- [x] **CURP/RFC no se cruzan**
  - No se comprueba que coincidan entre sí ni con `birthday`.
  - `KycConsistency` en el paso KYC (`400` con el motivo): la fecha `YYMMDD` de CURP y RFC debe ser la de `birthday`, el carácter 17 de la CURP debe cuadrar con el siglo (dígito hasta 1999, letra desde 2000) y las 4 letras deben coincidir (acepta Ñ→X en la CURP y la palabra inconveniente, que la CURP cambia en la 2.ª letra y el RFC en la 4.ª).
  - Pendiente, si se quiere más: dígito verificador de la CURP, sexo de la CURP contra `gender` y las 4 letras contra nombre y apellidos (reglas de nombres compuestos como MARÍA/JOSÉ y partículas DE/LA).

- [ ] **(Opcional) Cierre / baja de usuario**
  - Con su evento para cerrar la cuenta en account-service.

## Build / Docker

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

## Maven

- [ ] **Compilación local con JDK 25 falla por Lombok**
  - Desde JDK 23, `javac` no ejecuta annotation processors a menos que se habiliten explícitamente.
  - Afecta a `account-service`, `user-service` y `transaction-service`: errores `cannot find symbol` en getters, setters y `builder()`.
  - En Docker no pasa porque la imagen usa JDK 17.
  - Opciones:
    - Compilar con `mvn package -Dmaven.compiler.proc=full`.
    - Agregar `<maven.compiler.proc>full</maven.compiler.proc>` en `<properties>` de cada pom.
    - Usar JDK 17/21 localmente.

- [ ] **Versiones duplicadas entre servicios**
  - Spring Boot `3.5.8`, Spring Cloud `2025.0.0`, `modelmapper 3.2.0` y `testcontainers 1.21.4` están repetidas en cada pom.
  - Al actualizar hay que cambiarlas en todos, o mover testcontainers a su BOM (`testcontainers-bom`).

## Kubernetes (`k8s/`)

- [ ] **Secretos en texto plano en `k8s/kustomization.yaml`**
  - Las credenciales de PostgreSQL y del admin de Keycloak están como `literals` en el `secretGenerator`.
  - Para algo más allá de desarrollo local: Sealed Secrets, External Secrets o SOPS.

- [ ] **Datos de Kafka efímeros**
  - El broker no tiene volumen (igual que en docker-compose); los tópicos se pierden si el pod se reinicia.

- [ ] **Probar flujos entre servicios en Kubernetes**
  - Verificado: health, endpoints de los 3 servicios vía Kong con token de Keycloak, registro en Eureka y kafka-ui.
  - Pendiente: flujo de registro (user-service → Kafka → account-service) y Feign (transaction-service → account-service).

## Seguridad

- [ ] **Contraseña de aplicación de Gmail en el historial de git**
  - Ya no está en el código, pero sigue en el historial: **hay que revocarla** en la cuenta de Google.
