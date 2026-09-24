# 使用本地 Maven 预构建产物（提交前在 maidc-parent 执行：
#   mvn -B clean install -DskipTests
# 并将 maidc-app/target/maidc-app-*.jar 复制为 docker/build/maidc-app.jar）。
# 注意：勿直接引用 target/ 下产物——本地 IDE 的增量编译可能与之竞态写入坏类，
# build/ 是构建后校验过的稳定副本（构建上下文内）。
# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY build/maidc-app.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-Xmx1024m", "-jar", "app.jar"]
