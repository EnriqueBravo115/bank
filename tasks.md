# Pending tasks

Work left for later. Completed tasks were removed (see the git history of this file).

## Security

- [ ] **Gmail app password in the git history**
  - It is no longer in the code, but it is still in the history: **it must be revoked** in the Google account.

## user-service

- [ ] **Outbox event order with several replicas**
  - `FOR UPDATE SKIP LOCKED` avoids sending the same row twice, but if two replicas publish at the same time they can send two events of the same user in a different order. Options: an advisory lock so a single replica publishes, or a lock per `aggregate_id`.

- [ ] **(Optional) User closure / deregistration**
  - With its event to close the account in account-service.

## Maven / dependencies

- [ ] **Local build with JDK 25 fails because of Lombok**
  - Since JDK 23, `javac` does not run annotation processors unless they are explicitly enabled.
  - It affects `account-service`, `user-service` and `transaction-service`: `cannot find symbol` errors on getters, setters and `builder()`.
  - It does not happen in Docker because the image uses JDK 17.
  - Options:
    - Build with `mvn package -Dmaven.compiler.proc=full`.
    - Add `<maven.compiler.proc>full</maven.compiler.proc>` to the `<properties>` of each pom.
    - Use JDK 17/21 locally.

- [ ] **Versions duplicated across services**
  - Spring Boot `3.5.16`, Spring Cloud `2025.0.3`, the Trivy version overrides, `modelmapper 3.2.0` and `testcontainers 1.21.4` are repeated in every pom.
  - Upgrading means changing them in all of them: a parent pom, or moving testcontainers to its BOM (`testcontainers-bom`).

- [ ] **Migrate to Spring Boot 4**
  - Two `spring-webmvc` 6.2.19 CVEs (XsltView, SSE with view fragments) are only fixed in Spring Framework 7; they are accepted in `.trivyignore` because no service uses those features.
  - When migrating: remove the version overrides added for Trivy from each pom and the entries of `.trivyignore`, then scan again (command in `.trivyignore`).

## Kubernetes (`k8s/`)

- [ ] **Plain-text secrets in `k8s/kustomization.yaml`**
  - The PostgreSQL and Keycloak admin credentials are `literals` in the `secretGenerator`.
  - For anything beyond local development: Sealed Secrets, External Secrets or SOPS.

- [ ] **Ephemeral data in docker-compose**
  - In k8s the broker and the databases have volumes. In `docker-compose.yml` no service has one: a recreated
    container (`docker compose down`, `up --force-recreate`) starts empty, the Kafka broker included.

- [ ] **Single replica of each service**
  - Deleting or restarting a pod gives a few seconds of `502` through Kong (measured: about 3-11 s).
  - For zero downtime: `replicas: 2`, a `preStop` sleep so the endpoint is removed before the shutdown, and
    `server.shutdown: graceful`. With several replicas of user-service, see the outbox order task above.
  - After account-service's pod is replaced, transaction-service keeps calling the old IP for about 35 s
    (Eureka registry fetch every 30 s plus the LoadBalancer cache): new transactions get `503` meanwhile.
    Options: lower `eureka.client.registry-fetch-interval-seconds` and `spring.cloud.loadbalancer.cache.ttl`,
    or retry on another instance once there are two replicas.
