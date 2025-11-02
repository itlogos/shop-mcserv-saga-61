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
#RUN mvn -q -DskipTests -pl store-service -am clean package
##RUN --mount=type=cache,target=/root/.m2 mvn -DskipTests -e -X -pl store-service -am clean package
#FROM eclipse-temurin:21-jre
#WORKDIR /app
#COPY --from=build /build/store-service/target/*.jar /app/app.jar
#ENTRYPOINT ["java","-jar","/app/app.jar"]

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml ./pom.xml
COPY store-service/pom.xml ./store-service/pom.xml
RUN mvn -q -f ./store-service/pom.xml -DskipTests dependency:go-offline

COPY store-service ./store-service
RUN mvn -q -f ./store-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/store-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
