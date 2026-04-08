#!/bin/sh

if [ -n "$SS_SERVER_HOST" ]; then
    echo "Starting ss-local -> $SS_SERVER_HOST:${SS_SERVER_PORT:-8388}"
    ss-local \
        -s "$SS_SERVER_HOST" \
        -p "${SS_SERVER_PORT:-8388}" \
        -l 1080 \
        -k "$SS_PASSWORD" \
        -m chacha20-ietf-poly1305 &
    sleep 1
fi

exec java -jar /app/app.jar
