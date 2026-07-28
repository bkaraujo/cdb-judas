#!/usr/bin/env bash
#
# Sobe o backend Quarkus em dev mode. A SPA é servida pelo próprio backend
# (o pom de br-application copia web/ para META-INF/resources), então basta
# abrir http://localhost:8080.
#
# -pl br-application -am: o pom da raiz é só o agregador do reator; quarkus:dev
# precisa apontar para o módulo de aplicação, e -am constrói br-parent/br-commons/
# br-context-* antes.
set -euo pipefail

cd "$(dirname "$0")"

URL="http://localhost:8080"

if command -v xdg-open >/dev/null 2>&1; then
  ( sleep 8; xdg-open "$URL" >/dev/null 2>&1 || true ) &
fi

exec mvn -pl br-application -am quarkus:dev
