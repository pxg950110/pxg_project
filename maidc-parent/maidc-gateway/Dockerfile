# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies
COPY pom.xml .
COPY common/ common/
COPY maidc-gateway/pom.xml maidc-gateway/

RUN mvn -B dependency:go-offline -pl maidc-gateway -am

COPY maidc-gateway/src maidc-gateway/src
RUN mvn -B -pl maidc-gateway -am package -DskipTests

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /build/maidc-gateway/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
