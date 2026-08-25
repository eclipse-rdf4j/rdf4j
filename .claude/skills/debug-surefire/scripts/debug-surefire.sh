#!/usr/bin/env bash
set -euo pipefail

# debug-surefire.sh
#
# Run Maven Surefire tests in "wait for debugger" mode (JDWP).
#
# Optional inputs:
#   --module <maven-module-id>      Reactor project selector passed to "mvn -pl".
#                                  Convenience: if you pass just an artifactId (no ':' or '/'),
#                                  the script prefixes it with ':' (e.g., "foo" -> ":foo").
#   --test-class <TestClass>        Runs a single test class (Surefire -Dtest=...).
#   --test <TestClass#method>       Runs a single test method/pattern (Surefire -Dtest=Class#method).
#                                  IMPORTANT: quote values containing '#', e.g. --test 'MyTest#myMethod'
#   --skip-install                  Skip the pre-test quick install step.
#   --no-offline | --online         Run Maven without "-o" (useful if offline resolution fails).
#
# Extras:
#   Everything after "--" is passed through to Maven unchanged.
#
# Environment:
#   SUREFIRE_DEBUG_PORT   If set (e.g. 8000), uses that port instead of 55005.
#                         Binds to localhost for safety.

usage() {
  cat <<'USAGE'
Usage:
  debug-surefire.sh [--module <id>] [--test-class <class>] [--test <class#method>] [--skip-install] [--no-offline|--online] [-- <extra mvn args>]

Examples:
  # Debug a single test class (current module / reactor defaults)
  ./debug-surefire.sh --test-class MyTest

  # Debug a single test method (quote the '#')
  ./debug-surefire.sh --test 'MyTest#shouldDoThing'

  # Debug a test in one module of a multi-module build (artifactId shorthand)
  ./debug-surefire.sh --module my-module --test-class MyTest

  # Pass through extra Maven args
  ./debug-surefire.sh --test-class MyTest -- -DtrimStackTrace=false -DfailIfNoTests=false

  # Use a different debug port
  SUREFIRE_DEBUG_PORT=8000 ./debug-surefire.sh --test-class MyTest
USAGE
}

MODULE=""
TEST_CLASS=""
TEST_TARGET=""
SKIP_INSTALL="0"
OFFLINE="1"
PASSTHRU=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    -m|--module)
      MODULE="${2:-}"; shift 2 ;;
    -c|--class|--test-class)
      TEST_CLASS="${2:-}"; shift 2 ;;
    -t|--test|--test-target)
      TEST_TARGET="${2:-}"; shift 2 ;;
    --skip-install)
      SKIP_INSTALL="1"; shift ;;
    --no-offline|--online)
      OFFLINE="0"; shift ;;
    -h|--help)
      usage; exit 0 ;;
    --)
      shift
      PASSTHRU+=("$@")
      break
      ;;
    *)
      echo "Unknown argument: $1" >&2
      echo >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ -n "$TEST_CLASS" && -n "$TEST_TARGET" ]]; then
  echo "ERROR: Use either --test-class OR --test (Class#method), not both." >&2
  exit 2
fi

ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$ROOT"

# Prefer Maven Wrapper if present.
MVN="mvn"
if [[ -x "./mvnw" ]]; then
  MVN="./mvnw"
fi

PORT="${SUREFIRE_DEBUG_PORT:-55005}"
REPO_LOCAL="-Dmaven.repo.local=$ROOT/.m2_repo"

OFFLINE_ARGS=()
if [[ "$OFFLINE" == "1" ]]; then
  OFFLINE_ARGS+=("-o")
fi

# If it looks like a bare artifactId, convert to :artifactId for -pl convenience.
PL=""
if [[ -n "$MODULE" ]]; then
  PL="$MODULE"
  if [[ "$PL" != *":"* && "$PL" != *"/"* ]]; then
    PL=":$PL"
  fi
fi

if [[ "$SKIP_INSTALL" != "1" ]]; then
  INSTALL_CMD=("$MVN" "-T" "1C" "${OFFLINE_ARGS[@]}" "$REPO_LOCAL" "-Pquick")
  if [[ -n "$PL" ]]; then
    INSTALL_CMD+=("-pl" "$PL" "-am")
  fi
  INSTALL_CMD+=("clean" "install")
  echo "=== Pre-test install (fast, no tests) ===" >&2
  printf '  %q' "${INSTALL_CMD[@]}"; echo >&2
  "${INSTALL_CMD[@]}"
  echo >&2
fi

# Build Maven test command.
CMD=("$MVN" "${OFFLINE_ARGS[@]}" "$REPO_LOCAL")

if [[ -n "$PL" ]]; then
  CMD+=("-pl" "$PL")
fi

# Enable Surefire's debug mode.
CMD+=("-Dmaven.surefire.debug=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:${PORT}")


# Narrow the test scope if requested.
if [[ -n "$TEST_TARGET" ]]; then
  CMD+=("-Dtest=$TEST_TARGET")
elif [[ -n "$TEST_CLASS" ]]; then
  CMD+=("-Dtest=$TEST_CLASS")
fi

# Pass-through args (e.g., -DfailIfNoTests=false)
if [[ ${#PASSTHRU[@]} -gt 0 ]]; then
  CMD+=("${PASSTHRU[@]}")
fi

CMD+=("test")

{
  echo "=== Maven Surefire Debug Runner ==="
  echo "Root: $ROOT"
  echo "Maven: $MVN"
  if [[ -n "$PL" ]]; then
    echo "Module selector (-pl): $PL"
  fi
  if [[ -n "$TEST_TARGET" ]]; then
    echo "Test selector: -Dtest=$TEST_TARGET"
  elif [[ -n "$TEST_CLASS" ]]; then
    echo "Test selector: -Dtest=$TEST_CLASS"
  else
    echo "Test selector: (all tests selected by Maven/Surefire)"
  fi
  if [[ "$OFFLINE" == "1" ]]; then
    echo "Offline: yes (-o)"
  else
    echo "Offline: no"
  fi
  echo "Local repo: $ROOT/.m2_repo"
  echo "JDWP port: $PORT (attach to localhost)"
  echo
  echo "Attach a debugger, e.g.:"
  echo "  jdb -attach $PORT"
  echo
  echo "Running:"
  printf '  %q' "${CMD[@]}"; echo
  echo "=================================="
  echo
} >&2

exec "${CMD[@]}"

