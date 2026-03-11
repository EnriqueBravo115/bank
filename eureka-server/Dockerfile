FROM bank-maven-cache AS build

WORKDIR /build

COPY eureka-server/src eureka-server/src

RUN mvn -pl eureka-server clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/eureka-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java","-jar","app.jar"]
