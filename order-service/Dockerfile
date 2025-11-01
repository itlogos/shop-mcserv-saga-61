# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY customer-service/pom.xml customer-service/
COPY order-service/pom.xml order-service/
COPY store-service/pom.xml store-service/
COPY . .

RUN mvn -q -DskipTests -pl order-service -am clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/order-service/target/*.jar /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
