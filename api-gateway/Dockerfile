FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app

COPY . .

RUN mvn clean package -pl api-gateway -am -DskipTests

FROM eclipse-temurin:17-jdk
COPY --from=build /app/api-gateway/target/api-gateway-*.jar /app/api-gateway.jar
ENTRYPOINT ["java", "-jar", "/app/api-gateway.jar"]
