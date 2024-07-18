#!/usr/bin/env bash
set -e

echo "Stopping the docker container for ${APP_SERVER}"
docker compose down --rmi all -v

