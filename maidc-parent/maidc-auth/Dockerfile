# 使用本地 Maven 预构建产物（提交前在 maidc-parent 执行：
#   mvn -B -pl maidc-auth -am package -DskipTests
# 或统一构建全部服务），镜像仅打包 jar，不在容器内联网构建。
# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY maidc-auth/target/maidc-auth-*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
