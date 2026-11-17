#!/bin/sh
set -e

# CI retains each JVM launch separately, including a container restarted with the same PID.
if [ -n "${RDF4J_CONTAINER_JVM_DIAGNOSTICS_ROOT:-}" ]; then
	diagnostics_directory="$(mktemp -d "$RDF4J_CONTAINER_JVM_DIAGNOSTICS_ROOT/jvm-XXXXXXXXXX")"
	export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpGzipLevel=1 -XX:HeapDumpPath=$diagnostics_directory/java_pid%p.hprof.gz -XX:ErrorFile=$diagnostics_directory/hs_err_pid%p.log"
fi

# Activate OpenTelemetry tracing when an OTLP endpoint is configured: the presence of
# the OTEL_EXPORTER_OTLP_ENDPOINT environment variable switches tracing on for this container. 
#
# Other agent/instrumentation behaviour (log export, individual instrumentation modules, ...)
# is configured the standard OpenTelemetry way, via further OTEL_* environment variables set
# on the container - the agent reads those directly, no extra handling is needed here.
if [ -n "$OTEL_EXPORTER_OTLP_ENDPOINT" ]; then
	export CATALINA_OPTS="$CATALINA_OPTS \
	 -javaagent:/opt/otel/opentelemetry-javaagent.jar \
	 -Dotel.service.name=${OTEL_SERVICE_NAME:-rdf4j} \
	 -Dorg.eclipse.rdf4j.opentelemetry.enabled=true"
fi

# RDF4J_OPTS lets deployments add extra JVM options (e.g. further -D system properties) without
# having to repeat/replace the image's own CATALINA_OPTS defaults.
export CATALINA_OPTS="$CATALINA_OPTS $RDF4J_OPTS"

exec "$@"
