# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies
COPY pom.xml .
COPY common/ common/
COPY maidc-task/pom.xml maidc-task/

RUN mvn -B dependency:go-offline -pl maidc-task -am

COPY maidc-task/src maidc-task/src
RUN mvn -B -pl maidc-task -am package -DskipTests

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /build/maidc-task/target/*.jar app.jar

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
