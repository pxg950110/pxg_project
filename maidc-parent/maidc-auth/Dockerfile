# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies
COPY pom.xml .
COPY common/ common/
COPY maidc-auth/pom.xml maidc-auth/

RUN mvn -B dependency:go-offline -pl maidc-auth -am

COPY maidc-auth/src maidc-auth/src
RUN mvn -B -pl maidc-auth -am package -DskipTests

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /build/maidc-auth/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
