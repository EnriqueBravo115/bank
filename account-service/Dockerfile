FROM bank-maven-cache AS build

WORKDIR /build

COPY account-service/src account-service/src

RUN mvn -pl account-service clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/account-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
