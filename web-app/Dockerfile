## web-app/Dockerfile (balanced dev)
#FROM node:20-bookworm-slim
#
## Тише и стабильнее установки npm
#ENV NPM_CONFIG_AUDIT=false \
#    NPM_CONFIG_FUND=false \
#    NPM_CONFIG_UPDATE_NOTIFIER=false
#
#WORKDIR /app
#COPY package*.json ./
#
## Используем кэш npm. Если есть lock — npm ci, иначе npm install.
## (sh, не bash — slim образ)
#RUN --mount=type=cache,target=/root/.npm \
#    /bin/sh -lc 'if [ -f package-lock.json ]; then \
#        npm ci --prefer-offline --loglevel=error; \
#      else \
#        npm install --prefer-offline --loglevel=error; \
#      fi'
#
## Исходники после установки зависимостей
#COPY . .
#
## Вайт в контейнере + минимальные логи
#ENV CHOKIDAR_USEPOLLING=true
#EXPOSE 5173
#CMD ["npm","run","dev","--","--host","0.0.0.0","--port","5173","--logLevel","warn"]


## syntax=docker/dockerfile:1.7
#FROM node:20-bookworm-slim
#
#WORKDIR /app
#COPY package*.json ./
#
## детерминированная установка
#RUN --mount=type=cache,target=/root/.npm \
#    npm ci --include=optional --no-audit --no-fund
#
#COPY . .
#
#EXPOSE 5173
#CMD ["npm","run","dev","--","--host","0.0.0.0","--port","5173","--logLevel","warn"]

# syntax=docker/dockerfile:1.7
FROM node:20-bookworm-slim

WORKDIR /app
COPY package*.json ./

# детерминированная установка по linux-совместимому lock
RUN --mount=type=cache,target=/root/.npm \
    npm ci --include=optional --no-audit --no-fund

# копируем весь проект; .dockerignore отсеет мусор
COPY . .

EXPOSE 5173
CMD ["npm","run","dev","--","--host","0.0.0.0","--port","5173","--logLevel","warn"]
