FROM openjdk:17-slim

WORKDIR /app

# 复制 JAR 包
COPY target/mianmiantong.jar app.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD java -cp app.jar org.springframework.boot.loader.JarLauncher --server.port=80 || exit 1

# 云托管要求监听 80 端口
EXPOSE 80

CMD ["java", "-Dserver.port=80", "-jar", "app.jar"]
