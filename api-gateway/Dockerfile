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
#RUN mvn -q -DskipTests -pl api-gateway -am clean package
#
#FROM eclipse-temurin:21-jre
#WORKDIR /app
#COPY --from=build /build/api-gateway/target/*.jar /app/app.jar
#ENTRYPOINT ["java","-jar","/app/app.jar"]

# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# копируем только то, что нужно для билда модуля
COPY pom.xml ./pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml
RUN mvn -q -f ./api-gateway/pom.xml -DskipTests dependency:go-offline

COPY api-gateway ./api-gateway
RUN mvn -q -f ./api-gateway/pom.xml -DskipTests package

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
