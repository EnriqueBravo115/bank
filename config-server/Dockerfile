FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app

COPY . .

WORKDIR /app/config-server

RUN mvn dependency:go-offline
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=build /app/config-server/target/*.jar config-server.jar
ENTRYPOINT ["java", "-jar", "config-server.jar"]
