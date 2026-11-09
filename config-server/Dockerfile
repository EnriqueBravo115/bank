FROM maven:3.9.5-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml .
COPY src src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre
# Security updates the base image does not have yet (e.g. apt itself)
RUN apt-get update \
    && apt-get upgrade -y --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java","-jar","app.jar"]
