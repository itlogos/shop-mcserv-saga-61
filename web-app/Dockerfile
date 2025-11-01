#FROM node:20-bookworm-slim
#
## Нативные тулзы, если вдруг нужны для сборки бинарников (оставь — не мешает)
#RUN apt-get update && apt-get install -y --no-install-recommends \
#    python3 make g++ git ca-certificates curl dnsutils \
# && rm -rf /var/lib/apt/lists/*
#
#WORKDIR /app
#COPY package*.json ./
#
## Сетевые настройки npm + проверка реестра
#RUN npm config set fetch-timeout 600000 \
# && npm config set fetch-retries 5 \
# && npm config set fund false \
# && npm config set audit false \
# && npm -v && node -v && npm ping
#
## 1) Ставим зависимости без постскриптов и без optional — чтобы не залипало
#RUN --mount=type=cache,target=/root/.npm \
#    npm install --no-audit --no-fund --prefer-offline --loglevel=error \
#    --fetch-retries=5 --fetch-retry-maxtimeout=120000 --fetch-retry-mintimeout=20000
#
## 2) (ОПЦИОНАЛЬНО) Если нужен esbuild/playwright и т.п. — добираем точечно
## RUN npm rebuild esbuild
## RUN npx playwright install --with-deps
#
#COPY . .
#
#ENV CHOKIDAR_USEPOLLING=true
#EXPOSE 5173
#CMD ["npm","run","dev","--","--host","0.0.0.0","--port","5173"]


## web-app/Dockerfile (фрагмент)
#FROM node:20-alpine
#
#WORKDIR /app
#COPY package*.json ./
#
## КЛЮЧЕВОЕ: чистим кэш, удаляем lock (workaround бага), ставим с optional
#RUN rm -rf node_modules package-lock.json \
# && npm cache clean --force \
# && npm i --include=optional --no-audit --no-fund
#
## Копируем исходники после установки зависимостей
#COPY . .
#
#EXPOSE 5173
CMD ["npm","run","dev","--","--host","0.0.0.0"]