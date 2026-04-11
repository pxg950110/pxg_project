# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies
COPY pom.xml .
COPY common/ common/
COPY maidc-msg/pom.xml maidc-msg/

RUN mvn -B dependency:go-offline -pl maidc-msg -am

COPY maidc-msg/src maidc-msg/src
RUN mvn -B -pl maidc-msg -am package -DskipTests

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /build/maidc-msg/target/*.jar app.jar

EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
