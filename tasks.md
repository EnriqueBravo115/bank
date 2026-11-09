# Tareas pendientes

## account-service

### Seguridad (crítico)

- [x] **Cualquier cliente puede mover dinero de cualquier cuenta**
  - `/api/v1/account/movement/*` era accesible vía Kong para cualquier `CUSTOMER_BASIC`.
  - Ahora exige el rol `SERVICE`: solo lo tiene el cliente `transaction-service` de Keycloak (client credentials). La propiedad de la cuenta la valida transaction-service antes de mover el dinero.

- [x] **Montos sin validar en los movimientos**
  - Los `Client*Request` llevan `@NotBlank`/`@NotNull @Positive` (`@PositiveOrZero` la comisión) y el controller `@Valid`: un monto negativo o cero es `400`.

- [x] **IDOR en consultas de cuenta**
  - `GET /me` devuelve la cuenta del `sub` del JWT; `get-by-account-number` y `get-by-clabe` quedan para `ADMIN` y `SERVICE` (transaction-service resuelve con ellas cuentas y dueño), `get-by-email` solo `ADMIN`.

### Funcionalidad

- [x] **No se crea saldo al abrir la cuenta**
  - `createAccount` guarda un `account_balance` en 0 (`OPENING`) en la misma transacción que la cuenta.
  - `V6` crea ese saldo inicial a las cuentas que no tenían ninguno.

- [ ] **Depósitos / abonos**
  - `AccountBalanceController` está comentado y `AccountBalanceServiceImpl.createAccountBalance` lanza `UnsupportedOperationException`.

- [x] **Tarjetas**
  - `/api/v1/account/card`: `POST` emite una tarjeta de débito para la cuenta del `sub` (a nombre del claim `name`; `409` si ya tiene una `ACTIVE`/`BLOCKED`, `422` si la cuenta no está `OPEN`). Es la única respuesta con el número completo y el CVV. `GET /me` las lista enmascaradas; `POST /{id}/block|unblock|cancel` para el dueño o `ADMIN` (de otro usuario `404`, transición inválida `409`, `CANCELLED` es final).
  - Número de 16 dígitos con BIN `412345` y dígito Luhn (`CardNumberGenerator`); vence al final del mes, a 5 años.
  - El CVV no se guarda (`V6` borra la columna): `CardSecurityCode` lo deriva con HMAC-SHA256 del número y la expiración con `card.cvv-secret` (`CARD_CVV_SECRET`), como la clave CVK de un emisor, y lo compara en tiempo constante. Cambiar el secreto invalida el CVV de todas las tarjetas.
  - La compra exige `sourceCardNumber` (`400`) y la declina (`422`, `Card declined: …`) si la tarjeta no es de la cuenta, no está `ACTIVE`, venció o el CVV no coincide. transaction-service solo manda el CVV en el primer intento: un reintento que nunca llegó a account-service se declina.
  - `GET /get-by-card-number/{cardNumber}` (`ADMIN`, `SERVICE`) devuelve la cuenta de la tarjeta.

- [ ] **Tarjetas: pendientes**
  - El número completo de la tarjeta (PAN) se guarda en claro; PCI-DSS pide cifrarlo o tokenizarlo.
  - `card.cvv-secret` tiene un valor por defecto solo para desarrollo en config-server: fuera de local hay que definir `CARD_CVV_SECRET` (en k8s, en el `secretGenerator`).

- [ ] **Límites de transacción incompletos**
  - No hay forma de crearlos.
  - `isWithinDailyLimit` compara el monto de una sola operación, no la suma del día.
  - `max_transactions` no se usa.

- [ ] **Campos del movimiento ignorados**
  - `transactionFee` en el retiro, `SecurityVerificationMethod` y la moneda (se puede transferir entre cuentas de monedas distintas).

### Consistencia

- [x] **Condición de carrera en el saldo**
  - Cada movimiento (y su reversa) bloquea sus cuentas con `SELECT ... FOR UPDATE` (`AccountRepository.lockByAccountNumber`) antes de leer el saldo, en orden de número de cuenta para que dos transferencias opuestas no hagan deadlock.
  - El saldo actual es la última fila por `id`, no por `creation_date` (dos filas podían compartir fecha); índice `(account_id, id DESC)`.
  - `MovementControllerTest`: 10 débitos simultáneos de 100 sobre 500 → exactamente 5 se aplican (sin el bloqueo se aplicaban los 10), y transferencias opuestas simultáneas sin deadlock.

- [ ] **Tipos de saldo mal modelados**
  - Todos los movimientos se guardan como `INTRADAY`, pero `hasSufficientFundsIncludingHolds` busca `AVAILABLE`/`ON_HOLD`; `checkHolds` siempre es `false`; "el último por fecha" mezcla tipos.

- [x] **Movimientos sin idempotencia**
  - Cada movimiento exige el header `Idempotency-Key` y se registra en la tabla `movement` (`V5`, clave `UNIQUE`): repetir la clave devuelve el resultado guardado sin mover el dinero otra vez; dos peticiones simultáneas con la misma clave → `409`. Un rechazo (`422`) no se guarda, así que un reintento se evalúa de nuevo.
  - `POST /movement/{key}/reverse`: devuelve el dinero de un movimiento `COMPLETED` (`REVERSED`) o, si la clave nunca llegó, la bloquea (`CANCELLED`) para que una petición tardía no se aplique.

- [ ] **Identificadores de cuenta**
  - `account_number = "ACC-" + millis` no es `UNIQUE` y puede repetirse.
  - `ClabeGenerator` debería calcular el dígito de control real.

### Calidad

- [ ] **Código muerto**
  - `AccountServiceImpl.updateEmail(accountNumber, …)` (`// TODO`, sin uso; ya lo cubre `updateEmailByUserId`).
  - `FeignConfiguration` sin ningún cliente Feign.

- [x] **Mensaje de error incorrecto en `get-by-email`**
  - Ahora `"Account not found with email: <email>"` (y `clabe`/`userId` en sus consultas).

- [ ] **Tests**
  - Añadidos `MovementControllerTest` y `AccountControllerTest` (PostgreSQL real con Testcontainers: seguridad, validación, idempotencia, reversa). Faltan los consumers de Kafka.

## transaction-service

### Seguridad y consistencia (crítico)

- [x] **Inconsistencia distribuida al crear transacciones**
  - `TransactionSettlement` guarda primero la transacción como `PENDING` (commit) y luego llama a account-service con el `transactionCode` como `Idempotency-Key`.
  - Solo las respuestas que prueban que el dinero no se movió son finales (`422` → `DECLINED`; `404`/`400`/`401`/`403` → `FAILED`). Timeout, 5xx, `409` o circuito abierto la dejan `PENDING`.
  - `PendingTransactionScheduler` (cada `transaction.reconciliation.interval`, 5 s) reenvía las `PENDING` vencidas con la misma clave y backoff (5 s, 10 s… hasta 15 min), con `FOR UPDATE SKIP LOCKED` para varias réplicas. Migración `V4` (`attempts`, `next_attempt_at`, índice parcial).
  - Feign con `connectTimeout` 2 s y `readTimeout` 5 s (antes el default de 60 s): pasado ese tiempo la transacción queda `PENDING` y el backoff empieza a contar al terminar el intento.

- [x] **IDOR en consultas y creación**
  - Cada transacción guarda `owner_id` (el `userId` de la cuenta origen, según account-service). Todas las consultas, analítica y soporte filtran por el `sub` del JWT salvo para `ADMIN`; `/{transactionCode}` de otro usuario es `404`.
  - Al crear, la cuenta origen debe ser del usuario (`403`); la destino debe existir (`404`). Si account-service no responde a la consulta, `503` y no se registra nada.

- [x] **`IdentifierType` no se respeta**
  - `AccountResolver` busca por número de cuenta, CLABE o tarjeta según el tipo y envía a account-service siempre el `accountNumber`.
  - `CARD` se resuelve con `GET /api/v1/account/get-by-card-number/{cardNumber}` (antes `400`): sirve como origen de transferencias y en consultas y analítica. Los mensajes de error muestran solo los últimos 4 dígitos (`**** 1111`).

### Funcionalidad

- [x] **Las consultas filtran por el identificador con que se creó la transacción**
  - Consultas, analítica y soporte filtran por `account_number`, no por `sourceIdentifier`: una cuenta ve sus transacciones hechas con el número de cuenta o con la CLABE.
  - Parámetro opcional `sourceIdentifierType` (`ACCOUNT` por defecto, sin llamar a account-service). Con `CLABE`, `AccountResolver.accountNumberOf` la resuelve a su cuenta (`404` si no existe, `503` si account-service no responde); `CARD` es `400`.
  - `/query/by-sources?sourceIdentifiers=` pasa a `/query/by-accounts?accountNumbers=`.
  - `V5` rellena `account_number` de las filas anteriores a `V4` creadas con número de cuenta (las creadas con CLABE quedan en `NULL`) y cambia los índices a `account_number`.

- [x] **Endpoint de retiro**
  - `POST /api/v1/transaction/create/withdrawal`, con validación (`@Positive`, `@NotBlank`, `@NotNull`) igual que compra y pago de servicio.

- [x] **Cancelación y reversa**
  - `POST /api/v1/transaction/{code}/cancel` (dueño o `ADMIN`): solo `PENDING`; account-service la deshace si llegó a aplicarse o bloquea la clave → `CANCELLED` / `REVERSED`.
  - `POST /api/v1/transaction/{code}/reverse` (`ADMIN`): solo `COMPLETED` → `REVERSED`. Si la cuenta destino ya no tiene el dinero, `409`; si account-service no responde, `503` y no cambia nada.

### Bugs

- [x] **Consultas devuelven `transactionCode` y `description` en `null`**
  - `TransactionCommonProjection` no tenía esos getters; ModelMapper no tenía de dónde copiarlos.

- [x] **`/analytics/max-by-type` responde 500**
  - `BasicMapper.convertToResponseList` hacía `lists.contains(null)`, que lanza `NullPointerException` en las listas de `List.of(...)`.

- [x] **Compras, pagos de servicio y retiros no guardaban `sourceIdentifier`**
  - No aparecían en ninguna consulta. Ahora se guarda la cuenta de origen con `identifierType = ACCOUNT`.

- [x] **Compras y retiros no enviaban la cuenta a account-service**
  - `PurchaseRequest` y `WithdrawalRequest` piden `sourceAccountNumber`, y `ClientPurchaseRequest`/`ClientWithdrawalRequest` ya coinciden con los de account-service. La tarjeta se sigue guardando solo en `purchase_transaction`, hasta que account-service resuelva tarjeta → cuenta.

- [x] **Filtros ignorados**
  - `/date-range` no filtraba por `status`; `/max` siempre usaba `COMPLETED`; `/status-year` filtraba en memoria (ahora usa `findAllBySourceIdentifierAndYear`).

- [x] **`/{transactionCode}` y `/max` respondían `200` con `null`**
  - `GlobalExceptionHandler` con `TransactionNotFoundException` → `404` y `{status, message, timestamp}`, como user-service.

- [x] **`process` envuelve todo en `RuntimeException`**
  - Ya no: los rechazos de account-service llegan como DECLINED/FAILED por el fallback y cualquier otro error se propaga y hace rollback (no se registra la transacción).

- [x] **Se guardaba el CVV** en `purchase_transaction.cvv` (PCI-DSS)
  - `V3` borra la columna; el CVV solo se reenvía a account-service. Feign pasa de log `FULL` (que escribía los bodies, tarjeta y CVV incluidos) a `BASIC`.

- [x] **Configuración de Resilience4j**
  - `AccountServiceIntegrationTest` levanta un account-service falso (HTTP real, sin dependencias nuevas) y comprueba que el `@CircuitBreaker` de `AccountClient` sí ejecuta el fallback: 422 → DECLINED con el mensaje, 404 y 5xx → FAILED, y el JSON que se envía.
  - Quitados `feign.circuitbreaker.enabled` (nombre antiguo y no necesario: el fallback es del aspecto de Resilience4j) y el bloque `resilience4j.retry`, que nadie usaba.

### Calidad

- [x] **Código muerto**
  - Eliminados: `TransactionHelper`, `KafkaTopicConstants`, `ErrorMessage`, `AccountServiceException`, `ApiRequestException`, `AccountStatus`, `TransferResponse`, los 8 `FilterStatus*Request`, `TransferTransactionRepository`, `cancelTransaction` vacío y `@Service` en las interfaces.
  - Dependencias quitadas: `spring-kafka`, `spring-kafka-test` y `spring-boot-starter-oauth2-client`.
  - `TestConstants` movido a `src/test`.

- [x] **Datos de prueba en migraciones de producción**
  - `V3__remove_seed_data_and_cvv.sql` borra las filas de `V2` y reajusta la secuencia de `transactions.id`: `V2` insertaba ids explícitos sin moverla, así que las primeras inserciones reales chocaban con ellos.
  - También añade `UNIQUE (transaction_code)` y el índice `(source_identifier, transaction_status, transaction_date)` que usan todas las consultas.

- [x] **Respuestas al estilo de user-service**
  - Records como proyección directa de JPQL (`TransactionResponse`, `TransactionSummaryResponse`, `TransactionResultResponse.from`); eliminados las 3 proyecciones, sus 3 DTOs, `BasicMapper`, ModelMapper y `HeaderResponse`.
  - `/query/all` devuelve `PagedModel` con `page`/`size` validados (`Paging`, máximo 100) como user-service. `/analytics/max-by-type` devuelve una transacción por tipo, no una lista.
  - `/query/by-sources` pasa de `GET` con body a `?sourceIdentifiers=a,b`.
  - `PurchaseRequest` y `ServiceRequest` ya no llevan `transactionType` (se ignoraba).

- [x] **Tests al patrón de user-service**
  - Tests de integración por controller con PostgreSQL real; cada clase crea sus datos (`TransactionFixtures`) en vez de usar la semilla `V2`.
  - Eliminados los tests unitarios de services con Mockito (`TransactionQueryServiceTest` levantaba un contenedor que no usaba) y `TransactionServiceTestHelper`.
  - Nuevos: `TransactionCreationControllerTest` (`@MockitoBean AccountClient`: lo que se envía a account-service y lo que se guarda con COMPLETED/DECLINED, validación 400, 401/403) y `AccountClientFallbackTest` (422 → DECLINED con el mensaje, 404/401/5xx → FAILED); analítica `type-summary`, `average-days` y `partition-by-amount`.

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

- [ ] **Kong cachea la IP de los servicios**
  - Si se recrea o reinicia un solo servicio (`docker compose up -d <servicio>`, `docker stop/start`), cambia su IP y Kong responde `502` hasta reiniciarlo (`docker compose restart kong`). Opción: `KONG_DNS_VALID_TTL` bajo en el servicio `kong`.

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
  - transaction-service necesita llegar a Keycloak (`KEYCLOAK_HOST`) para pedir su token de servicio.
  - Verificado: health, endpoints de los 3 servicios vía Kong con token de Keycloak, registro en Eureka y kafka-ui.
  - Pendiente: flujo de registro (user-service → Kafka → account-service) y Feign (transaction-service → account-service).

## Seguridad

- [ ] **Contraseña de aplicación de Gmail en el historial de git**
  - Ya no está en el código, pero sigue en el historial: **hay que revocarla** en la cuenta de Google.
