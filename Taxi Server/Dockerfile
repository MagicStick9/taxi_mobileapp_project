FROM node:20-alpine

WORKDIR /app

# =========================
# Dependencies
# =========================

COPY package.json package-lock.json ./

RUN npm ci --omit=dev


# =========================
# Application
# =========================
# Только текущие файлы из build context.
# Старый admin.html из старого image
# сюда попасть не может при --no-cache.

COPY server.js admin.html ./


# =========================
# Проверка обязательных файлов
# =========================

RUN test -f /app/server.js \
    || (echo "ERROR: server.js NOT FOUND" && exit 1)

RUN test -f /app/admin.html \
    || (echo "ERROR: admin.html NOT FOUND" && exit 1)


# =========================
# Показываем, что реально
# попало в IMAGE
# =========================

RUN echo "========================================" \
    && echo "FILES INSIDE NEW APP IMAGE:" \
    && ls -lh /app/server.js /app/admin.html \
    && echo "========================================" \
    && echo "ADMIN.HTML CHECKSUM:" \
    && sha256sum /app/admin.html \
    && echo "========================================"


# =========================
# Server
# =========================

EXPOSE 3000

CMD ["node", "server.js"]

