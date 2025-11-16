#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
MAVEN_BIN="${MAVEN_BIN:-mvn}"
STOP_PORT="${RDF4J_JETTY_STOP_PORT:-8079}"
STOP_KEY="${RDF4J_JETTY_STOP_KEY:-rdf4j-dev-stop}"
HTTP_PORT="${RDF4J_HTTP_PORT:-8080}"
JETTY_LOG=""
JETTY_MAVEN_PID=""

cleanup() {
  echo "Stopping local RDF4J dev server"
  if [ -n "$JETTY_MAVEN_PID" ]; then
    if kill -0 "$JETTY_MAVEN_PID" >/dev/null 2>&1; then
      $MAVEN_BIN -pl tools/workbench jetty:stop -Prdf4j-dev-server \
        -Drdf4j.dev.stop.port="$STOP_PORT" \
        -Drdf4j.dev.stop.key="$STOP_KEY" \
        >/dev/null 2>&1 || true
      kill "$JETTY_MAVEN_PID" >/dev/null 2>&1 || true
      wait "$JETTY_MAVEN_PID" >/dev/null 2>&1 || true
    fi
  fi
  if [ -n "$JETTY_LOG" ] && [ -f "$JETTY_LOG" ]; then
    rm -f "$JETTY_LOG"
  fi
}
trap cleanup EXIT

cd "$ROOT_DIR"

if [ -z "${SKIP_BUILD:-}" ]; then
  $MAVEN_BIN -pl tools/workbench -am -DskipTests install
fi

JETTY_LOG="$(mktemp -t rdf4j-jetty.XXXX.log)"
$MAVEN_BIN -pl tools/workbench jetty:run -Prdf4j-dev-server \
  -Drdf4j.dev.stop.port="$STOP_PORT" \
  -Drdf4j.dev.stop.key="$STOP_KEY" \
  -Drdf4j.dev.http.port="$HTTP_PORT" \
  -Djetty.port="$HTTP_PORT" \
  -Djetty.http.port="$HTTP_PORT" \
  >"$JETTY_LOG" 2>&1 &
JETTY_MAVEN_PID=$!
echo "Jetty logs: $JETTY_LOG"

printf '%s' "Waiting for local server to be ready"
ready=false
for _ in $(seq 1 90); do
  if curl --silent --fail "http://localhost:${HTTP_PORT}/rdf4j-workbench/" >/dev/null; then
    echo " - ready"
    ready=true
    break
  fi
  printf '%s' '.'
  sleep 1
done

if [ "$ready" != true ]; then
  echo ""
  echo "Failed to detect a running server on port ${HTTP_PORT}" >&2
  if [ -f "$JETTY_LOG" ]; then
    echo "---- Jetty output ----" >&2
    tail -n 200 "$JETTY_LOG" >&2 || true
    echo "---------------------" >&2
  fi
  exit 1
fi

cd "$ROOT_DIR/e2e"

npm ci
npx playwright install --with-deps
export RDF4J_BASE_URL="http://localhost:${HTTP_PORT}/"
npx playwright test "$@"
