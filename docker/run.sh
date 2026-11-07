#!/usr/bin/env bash
set -e
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/ci-jvm-diagnostics.sh"
./build.sh

echo "Starting the docker container for ${APP_SERVER}"
rdf4j_ci_docker_compose up --force-recreate -d

# Wait for the server to be ready. Server is ready when the log contains something like "org.apache.catalina.startup.Catalina.start Server startup in 3400 ms".
printf '%s' "Waiting for container to be ready"
while ! rdf4j_ci_docker_compose logs rdf4j | grep -q -e "Server startup in" -e "Started ServerConnector"; do
  printf '%s' "."
  # Exit with error if we have looped 30 times (e.g. 30 seconds)
  ((c++)) && ((c == 30)) && echo "" && rdf4j_ci_docker_compose logs | tee && echo "" && docker ps -a | tee && printf '\n%s\n' "Timed out while waiting!" >&2 && exit 1
  sleep 1
done

echo ""
echo ""
echo "Workbench is available at http://localhost:8080/rdf4j-workbench"
echo ""
