#!/usr/bin/env bash
set -euo pipefail

SRC_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DB="$SRC_DIR/database.mv.db"
SNAPSHOT_DIR="/tmp/cdb-judas-db-$(date +%Y%m%d-%H%M%S)"
SNAPSHOT_DB="$SNAPSHOT_DIR/database"

if [ ! -f "$SRC_DB" ]; then
  echo "Banco não encontrado: $SRC_DB" >&2
  exit 1
fi

mkdir -p "$SNAPSHOT_DIR"
cp "$SRC_DB" "$SNAPSHOT_DB.mv.db"

echo "Snapshot: $SNAPSHOT_DB.mv.db"
echo "Alterações no snapshot NÃO afetam o banco original."

java -cp ~/.m2/repository/com/h2database/h2/2.4.240/h2-2.4.240.jar org.h2.tools.Console -url "jdbc:h2:file:$SNAPSHOT_DB" -user sa -password ""
