## syntax=docker/dockerfile:1
#FROM maven:3.9-eclipse-temurin-21 AS build
#WORKDIR /build
#
#COPY pom.xml .
#COPY api-gateway/pom.xml api-gateway/
#COPY customer-service/pom.xml customer-service/
#COPY order-service/pom.xml order-service/
#COPY store-service/pom.xml store-service/
#COPY . .
#
#RUN mvn -q -DskipTests -pl order-service -am clean package
#
#FROM eclipse-temurin:21-jre
#WORKDIR /app
#COPY --from=build /build/order-service/target/*.jar /app/app.jar
#ENTRYPOINT ["java","-jar","/app/app.jar"]

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml ./pom.xml
COPY order-service/pom.xml ./order-service/pom.xml
RUN mvn -q -f ./order-service/pom.xml -DskipTests dependency:go-offline

COPY order-service ./order-service
RUN mvn -q -f ./order-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/order-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/app.jar"]
