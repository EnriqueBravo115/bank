# Tareas pendientes

## Build / Docker

- [ ] **Instalar `docker-buildx`**
  - Los Dockerfiles usan `RUN --mount=type=cache,target=/root/.m2`, que requiere BuildKit.
  - Sin buildx, Docker usa el builder legacy y falla con:
    `the --mount option requires BuildKit`.
  - Solución (Arch): `sudo pacman -S docker-buildx`.

- [ ] **Verificar que las 5 imágenes se construyen**
  - Correr `docker compose build` después de instalar buildx.
  - Hasta ahora solo se verificó el build con Maven local, no las imágenes Docker.

- [x] **Agregar `.dockerignore`**
  - El contexto enviado al daemon bajó de ~451 MB a ~640 KB: solo `pom.xml` y `src` de cada servicio.

- [ ] **(Opcional) Cambiar el `context` de cada servicio en `docker-compose.yml` a su propia carpeta**
  - Ahora que cada servicio es independiente, puede ser `context: ./account-service`.
  - En ese caso, el Dockerfile tendría que copiar `pom.xml` y `src`, sin el prefijo del servicio.

- [ ] **Definir estrategia de caché de Maven para CI/CD**
  - El `RUN --mount=type=cache,target=/root/.m2` solo acelera builds donde el builder de BuildKit persiste: local o runners self-hosted.
  - En runners efímeros (GitHub Actions hosted, GitLab shared runners) la caché empieza vacía en cada job.
  - `--cache-to type=gha` y `--cache-to type=registry` cachean capas, no cache mounts.
  - Opciones al armar el pipeline:
    1. Compilar el `.jar` en el CI con la caché del propio CI (p. ej. `actions/setup-java` con `cache: maven`) y que el Dockerfile solo copie el jar. Permite además correr los tests con Testcontainers en ese paso.
    2. Usar Jib (`jib-maven-plugin`) para construir la imagen desde Maven sin Dockerfile.
    3. Mantener el cache mount y persistirlo con `buildkit-cache-dance` (GitHub Actions).
  - Mantener el Dockerfile actual para desarrollo local con `docker compose`.

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
