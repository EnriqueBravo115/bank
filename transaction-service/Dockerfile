FROM maven:3.9.5-eclipse-temurin-17 AS build

WORKDIR /build

COPY transaction-service/pom.xml .
COPY transaction-service/src src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]
