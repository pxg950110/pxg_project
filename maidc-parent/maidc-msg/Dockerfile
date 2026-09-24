# 使用本地 Maven 预构建产物（提交前在 maidc-parent 执行：
#   mvn -B -pl maidc-msg -am package -DskipTests
# 或统一构建全部服务），镜像仅打包 jar，不在容器内联网构建。
# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY maidc-msg/target/maidc-msg-*.jar app.jar

EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
