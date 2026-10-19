#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: jit-run-log.sh --directives <file> --logfile <file> -- java <args...>

Example:
  jit-run-log.sh --directives c2-directives.json5 --logfile jit.xml -- \
    java -XX:+UnlockDiagnosticVMOptions -jar app.jar
USAGE
}

directives=""
logfile="jit.xml"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --directives)
      directives="$2"
      shift 2
      ;;
    --logfile)
      logfile="$2"
      shift 2
      ;;
    --)
      shift
      break
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "$directives" ]]; then
  echo "--directives is required" >&2
  usage
  exit 1
fi

if [[ $# -eq 0 ]]; then
  echo "Missing java command after --" >&2
  usage
  exit 1
fi

if [[ "$1" != "java" ]]; then
  echo "First command after -- must be 'java'" >&2
  usage
  exit 1
fi

exec "$@" \
  -XX:+CompilerDirectivesPrint \
  -XX:CompilerDirectivesFile="$directives" \
  -XX:+LogCompilation \
  -XX:LogFile="$logfile"
