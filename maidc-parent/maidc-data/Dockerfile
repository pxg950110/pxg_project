# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies
COPY pom.xml .
COPY common/ common/
COPY maidc-data/pom.xml maidc-data/

RUN mvn -B dependency:go-offline -pl maidc-data -am

COPY maidc-data/src maidc-data/src
RUN mvn -B -pl maidc-data -am package -DskipTests

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /build/maidc-data/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
