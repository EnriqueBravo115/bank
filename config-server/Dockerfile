FROM bank-maven-cache AS build

WORKDIR /build

COPY config-server/src config-server/src

RUN mvn -pl config-server clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/config-server/target/*.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java","-jar","app.jar"]
