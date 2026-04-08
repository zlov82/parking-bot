FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends shadowsocks-libev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

COPY target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["/app/start.sh"]
