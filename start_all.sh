#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-konajdk-17.0.19-1.tl4}"
export PATH="$JAVA_HOME/bin:/data/home/acluckywang/tools/apache-maven-3.9.9/bin:${PATH:-}"

# Load .env if present
if [[ -f "$ROOT/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/.env"
  set +a
fi

mkdir -p "$ROOT/.run"

start_algo() {
  if curl -fsS "http://127.0.0.1:22500/health" >/dev/null 2>&1; then
    echo "[ok] algorithm mock already running"
    return
  fi
  setsid python3 "$ROOT/algorithm/mock_server.py" >"$ROOT/.run/algorithm.log" 2>&1 < /dev/null &
  echo $! >"$ROOT/.run/algorithm.pid"
  sleep 1
  curl -fsS "http://127.0.0.1:22500/health" >/dev/null
  echo "[ok] algorithm mock started (pid $(cat "$ROOT/.run/algorithm.pid"))"
}

start_backend() {
  if curl -fsS "http://127.0.0.1:${AICO_SERVER_PORT:-8081}/parents" >/dev/null 2>&1; then
    echo "[ok] backend already running"
    return
  fi
  JAR="$ROOT/backend/target/smart_intervention-0.0.1-SNAPSHOT.jar"
  if [[ ! -f "$JAR" ]]; then
    (cd "$ROOT/backend" && mvn -DskipTests package)
  fi
  setsid java -jar "$JAR" >"$ROOT/.run/backend.log" 2>&1 < /dev/null &
  echo $! >"$ROOT/.run/backend.pid"
  echo "[..] waiting for backend..."
  for i in $(seq 1 60); do
    if curl -fsS "http://127.0.0.1:${AICO_SERVER_PORT:-8081}/parents" >/dev/null 2>&1; then
      echo "[ok] backend started (pid $(cat "$ROOT/.run/backend.pid"))"
      return
    fi
    sleep 1
  done
  echo "[err] backend failed to start; see $ROOT/.run/backend.log" >&2
  tail -40 "$ROOT/.run/backend.log" >&2 || true
  exit 1
}

start_frontend() {
  if curl -fsS "http://127.0.0.1:5173" >/dev/null 2>&1; then
    echo "[ok] frontend already running"
    return
  fi
  (
    cd "$ROOT/frontend"
    setsid npm run dev -- --host 0.0.0.0 --port 5173 >"$ROOT/.run/frontend.log" 2>&1 < /dev/null &
    echo $! >"$ROOT/.run/frontend.pid"
  )
  echo "[..] waiting for frontend..."
  for i in $(seq 1 40); do
    if curl -fsS "http://127.0.0.1:5173" >/dev/null 2>&1; then
      echo "[ok] frontend started (pid $(cat "$ROOT/.run/frontend.pid"))"
      return
    fi
    sleep 1
  done
  echo "[err] frontend failed to start; see $ROOT/.run/frontend.log" >&2
  tail -40 "$ROOT/.run/frontend.log" >&2 || true
  exit 1
}

start_algo
start_backend
start_frontend

echo
echo "AICO is up:"
echo "  Frontend : http://127.0.0.1:5173"
echo "  Backend  : http://127.0.0.1:${AICO_SERVER_PORT:-8081}"
echo "  Algorithm: http://127.0.0.1:22500/health"
echo "  Login    : username = parent | expert | user"
echo
echo "Logs: $ROOT/.run/"
