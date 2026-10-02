#!/usr/bin/env bash
set -euo pipefail

usage() {
	cat <<'USAGE'
Usage:
  run-docker-jfr-loop.sh <fullyQualifiedClass.method> [options]
  run-docker-jfr-loop.sh --module <module> --class <fullyQualifiedClass> --method <method> [options]

Options:
  --param <name=value>      Pass a benchmark @Param override
  --jvm-arg <value>         Pass an extra JVM arg to the benchmark JVM
  --jfr-output <path>       Override the JFR output path
  --dry-run                 Print the effective docker helper command
  --help                    Show this help

Notes:
  - This wrapper always uses scripts/run-single-benchmark-docker.sh.
  - JFR CPU-time profiling is already enabled by the repo docker helper.
  - This wrapper injects stackdepth plus DebugNonSafepoints fidelity flags.
  - Raw JMH args are intentionally blocked. Use --param or --jvm-arg instead.
USAGE
}

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../../../.." && pwd)"
HELPER="${REPO_ROOT}/scripts/run-single-benchmark-docker.sh"

benchmark_id=""
module=""
benchmark_class=""
benchmark_method=""
passthrough_args=()
user_jvm_args=()

contains_exact() {
	local needle="$1"
	shift
	local item

	for item in "$@"; do
		if [[ "${item}" == "${needle}" ]]; then
			return 0
		fi
	done

	return 1
}

require_value() {
	local option="$1"

	if [[ $# -lt 2 ]]; then
		echo "Error: ${option} requires a value." >&2
		exit 1
	fi
}

while [[ $# -gt 0 ]]; do
	case "$1" in
	--help|-h)
		usage
		exit 0
		;;
	--module|-m)
		require_value "$@"
		module="$2"
		passthrough_args+=("$1" "$2")
		shift 2
		;;
	--class|-c)
		require_value "$@"
		benchmark_class="$2"
		passthrough_args+=("$1" "$2")
		shift 2
		;;
	--method|-b|--benchmark)
		require_value "$@"
		benchmark_method="$2"
		passthrough_args+=("$1" "$2")
		shift 2
		;;
	--param|--jfr-output|--jvm-arg)
		require_value "$@"
		passthrough_args+=("$1" "$2")
		if [[ "$1" == "--jvm-arg" ]]; then
			user_jvm_args+=("$2")
		fi
		shift 2
		;;
	--dry-run)
		passthrough_args+=("$1")
		shift
		;;
	--enable-jfr|--enable-jfr-cpu-times)
		echo "Error: ${1} is already handled by ${HELPER}." >&2
		exit 1
		;;
	--warmup-iterations|--measurement-iterations|--forks|--jmh-arg)
		echo "Error: ${1} is blocked here to keep the JFR loop reproducible." >&2
		exit 1
		;;
	--)
		echo "Error: raw JMH arguments are blocked here. Use --param or --jvm-arg." >&2
		exit 1
		;;
	-*)
		echo "Error: Unsupported option ${1}." >&2
		exit 1
		;;
	*)
		if [[ -n "${benchmark_id}" || -n "${module}" || -n "${benchmark_class}" || -n "${benchmark_method}" ]]; then
			echo "Error: only one shorthand selector is allowed. Use --param for benchmark parameters." >&2
			exit 1
		fi
		benchmark_id="$1"
		shift
		;;
	esac
done

if [[ ! -x "${HELPER}" ]]; then
	echo "Error: helper not found or not executable: ${HELPER}" >&2
	exit 1
fi

if [[ -n "${benchmark_id}" && ( -n "${module}" || -n "${benchmark_class}" || -n "${benchmark_method}" ) ]]; then
	echo "Error: choose shorthand selector or --module/--class/--method, not both." >&2
	exit 1
fi

if [[ -z "${benchmark_id}" && ( -z "${module}" || -z "${benchmark_class}" || -z "${benchmark_method}" ) ]]; then
	echo "Error: provide either <fullyQualifiedClass.method> or --module/--class/--method." >&2
	exit 1
fi

default_jvm_args=(
	"-XX:FlightRecorderOptions=stackdepth=1024"
	"-XX:+UnlockDiagnosticVMOptions"
	"-XX:+DebugNonSafepoints"
)

for arg in "${default_jvm_args[@]}"; do
	if ! contains_exact "${arg}" "${user_jvm_args[@]}"; then
		passthrough_args+=(--jvm-arg "${arg}")
	fi
done

cmd=("${HELPER}")

if [[ -n "${benchmark_id}" ]]; then
	cmd+=("${benchmark_id}")
fi

cmd+=("${passthrough_args[@]}")

exec "${cmd[@]}"
