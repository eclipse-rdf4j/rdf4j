#!/usr/bin/env bash
set -euo pipefail

dump_threads() {
  echo "== Cancellation received: capturing JVM thread dumps =="
  local pids
  pids=$(pgrep -f '[j]ava' || true)
  if [[ -z "${pids}" ]]; then
    echo "No Java processes found."
    return 0
  fi

  if command -v jcmd >/dev/null 2>&1; then
    for pid in ${pids}; do
      echo "-- jcmd Thread.print for PID ${pid} --"
      jcmd "${pid}" Thread.print || true
    done
    return 0
  fi

  if command -v jstack >/dev/null 2>&1; then
    for pid in ${pids}; do
      echo "-- jstack for PID ${pid} --"
      jstack "${pid}" || true
    done
    return 0
  fi

  for pid in ${pids}; do
    echo "-- kill -QUIT ${pid} (no jcmd/jstack available) --"
    kill -QUIT "${pid}" || true
  done
}

on_term() {
  dump_threads
  if [[ -n "${child_pid:-}" ]]; then
    kill -TERM "${child_pid}" 2>/dev/null || true
  fi
}

if [[ $# -eq 0 ]]; then
  echo "Usage: $0 <command> [args...]" >&2
  exit 2
fi

trap on_term INT TERM

"$@" &
child_pid=$!

wait "${child_pid}"
status=$?

trap - INT TERM
exit "${status}"
