#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: jit-facts.sh [--pid <pid>] [--out <file>]

Collect JVM and OS facts plus optional jcmd diagnostics for a running process.
USAGE
}

pid=""
out_file="jit-facts.txt"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --pid)
      pid="$2"
      shift 2
      ;;
    --out)
      out_file="$2"
      shift 2
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

{
  echo "=== Timestamp ==="
  date -u
  echo

  echo "=== OS/CPU ==="
  uname -a || true
  if command -v lscpu >/dev/null 2>&1; then
    lscpu
  fi
  echo

  echo "=== java -version ==="
  java -version 2>&1
  echo

  echo "=== PrintFlagsFinal (head) ==="
  java -XX:+PrintFlagsFinal -version 2>&1 | head -n 30
  echo

  if [[ -n "$pid" ]]; then
    if ! command -v jcmd >/dev/null 2>&1; then
      echo "jcmd not found; skipping VM diagnostics for pid=$pid" >&2
    else
      echo "=== jcmd VM.info ==="
      jcmd "$pid" VM.info
      echo

      echo "=== jcmd VM.flags ==="
      jcmd "$pid" VM.flags
      echo

      echo "=== jcmd VM.command_line ==="
      jcmd "$pid" VM.command_line
      echo

      echo "=== jcmd Compiler.codecache ==="
      jcmd "$pid" Compiler.codecache
      echo
    fi
  fi
} > "$out_file"

echo "Wrote $out_file"
