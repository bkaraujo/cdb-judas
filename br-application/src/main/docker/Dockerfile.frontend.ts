# syntax=docker/dockerfile:1

# ============================================================================
# Frontend TS + Vite servido por nginx — variante de Dockerfile.frontend para o
# profile Maven `frontend-ts` (ver br-application/pom.xml). Ao contrário do
# frontend/ vanilla (sem etapa de build), este precisa compilar antes de servir:
# duas stages — build (node) produz frontend/dist, run (nginx) só copia o
# resultado, sem levar node_modules/toolchain pra imagem final.
#
# IMPORTANTE: o contexto de build é a RAIZ do repositório (.), logo todos os
# caminhos de COPY são relativos à raiz, não a este diretório.
# ============================================================================
FROM node:22-alpine AS build

WORKDIR /src
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM nginx:alpine

# Versão atual do sistema (pom <version>). Sobrescrevível pelo build do compose.
ARG APP_VERSION=0.1.0
LABEL org.opencontainers.image.title="cdb-judas-frontend" \
      org.opencontainers.image.version="${APP_VERSION}"

# Bundle de produção (index.html + assets/), mesma saída que o profile Maven
# `frontend-ts` empacota via <resource> do pom (frontend/dist → META-INF/resources).
COPY --from=build /src/dist/ /usr/share/nginx/html/

# Config do server: serve o estático e faz proxy reverso das rotas do backend.
COPY br-application/src/main/docker/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
