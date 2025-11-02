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
#RUN mvn -q -DskipTests -pl customer-service -am clean package
#
#FROM eclipse-temurin:21-jre
#WORKDIR /app
#COPY --from=build /build/customer-service/target/*.jar /app/app.jar
#ENTRYPOINT ["java","-jar","/app/app.jar"]

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml ./pom.xml
COPY customer-service/pom.xml ./customer-service/pom.xml
RUN mvn -q -f ./customer-service/pom.xml -DskipTests dependency:go-offline

COPY customer-service ./customer-service
RUN mvn -q -f ./customer-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/customer-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
