#!/usr/bin/env bash
set -euo pipefail

# Always resolve relative paths from the distribution root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
LIB_DIR="${DIST_DIR}/lib"

JAVA_CMD="${JAVA_CMD:-java}"
JVM_MIN_HEAP="${RDF4J_JVM_MIN_HEAP:-512m}"
JVM_MAX_HEAP="${RDF4J_JVM_MAX_HEAP:-2g}"
DATA_DIR="${RDF4J_DATA_DIR:-${DIST_DIR}/data}"
LOG_DIR="${RDF4J_LOG_DIR:-${DIST_DIR}/logs}"
LOGGING_CONFIG="${RDF4J_LOGGING_CONFIG:-${DIST_DIR}/config/logback-spring.xml}"
SPRING_CONFIG="${RDF4J_SPRING_CONFIG:-${DIST_DIR}/config/application.properties}"

mkdir -p "${DATA_DIR}" "${LOG_DIR}"

shopt -s nullglob
JARS=("${LIB_DIR}"/rdf4j-server-boot-*.jar)
shopt -u nullglob
if [[ ${#JARS[@]} -eq 0 ]]; then
  echo "Unable to find rdf4j-server-boot jar inside ${LIB_DIR}" >&2
  exit 1
fi
SERVER_JAR="${JARS[0]}"

JVM_ARGS=(
  "-Xms${JVM_MIN_HEAP}"
  "-Xmx${JVM_MAX_HEAP}"
  "-XX:+UseG1GC"
  "-Dorg.eclipse.rdf4j.appdata.basedir=${DATA_DIR}"
  "-Dorg.eclipse.rdf4j.server.base=${DIST_DIR}"
  "-Dorg.eclipse.rdf4j.server.logdir=${LOG_DIR}"
  "-Dlogging.config=${LOGGING_CONFIG}"
)

if [[ -n "${RDF4J_JAVA_OPTS:-}" ]]; then
  # shellcheck disable=SC2206
  EXTRA_OPTS=(${RDF4J_JAVA_OPTS})
  JVM_ARGS+=("${EXTRA_OPTS[@]}")
fi

if [[ -n "${JAVA_OPTS:-}" ]]; then
  # shellcheck disable=SC2206
  GLOBAL_OPTS=(${JAVA_OPTS})
  JVM_ARGS+=("${GLOBAL_OPTS[@]}")
fi

APP_ARGS=("--spring.config.additional-location=${SPRING_CONFIG}")

echo "Starting RDF4J Server with command: ${JAVA_CMD} ${JVM_ARGS[*]} -jar ${SERVER_JAR} ${APP_ARGS[*]} $*"
echo "By default the workbench is available at http://localhost:8080/rdf4j-workbench/"

exec "${JAVA_CMD}" "${JVM_ARGS[@]}" -jar "${SERVER_JAR}" "${APP_ARGS[@]}" "$@"
