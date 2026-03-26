FROM bank-maven-cache AS build

WORKDIR /build

COPY transaction-service/src transaction-service/src

RUN mvn -pl transaction-service clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/transaction-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
