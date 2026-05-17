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
  --theme-store-directory <path>
                            Store ThemeQueryBenchmark LMDB data outside target
  --theme-store-reuse-confirmed
                            Confirm external theme store disk/data compatibility
  --theme-store-compatibility-key <key>
                            Compatibility key for invalidating the external theme store
  --jfr-output <path>       Override the JFR output path
  --dry-run                 Print the effective docker helper command
  --help                    Show this help

Notes:
  - This wrapper always uses scripts/run-single-benchmark-docker.sh.
  - JFR CPU-time profiling is already enabled by the repo docker helper.
  - This wrapper injects stackdepth plus DebugNonSafepoints fidelity flags.
  - After a successful run, this wrapper converts the JFR CPU-time profile to an SVG flame graph.
  - Raw JMH args are intentionally blocked. Use --param or --jvm-arg instead.
USAGE
}

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../../../.." && pwd)"
HELPER="${REPO_ROOT}/scripts/run-single-benchmark-docker.sh"
FLAMEGRAPH_CONVERTER="${REPO_ROOT}/scripts/jfr-collapsed-to-svg.py"
DOCKER_WORKDIR="/workspace"

benchmark_id=""
module=""
benchmark_class=""
benchmark_method=""
passthrough_args=()
user_jvm_args=()
dry_run=false

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

contains_jvm_property() {
	local property_name="$1"
	shift
	local item

	for item in "$@"; do
		if [[ "${item}" == "-D${property_name}="* ]]; then
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
	--param|--jfr-output|--jvm-arg|--theme-store-directory|--theme-store-compatibility-key)
		require_value "$@"
		passthrough_args+=("$1" "$2")
		if [[ "$1" == "--jvm-arg" ]]; then
			user_jvm_args+=("$2")
		fi
		shift 2
		;;
	--theme-store-reuse-confirmed)
		passthrough_args+=("$1")
		shift
		;;
	--dry-run)
		dry_run=true
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
	"-Drdf4j.lmdb.themeQueryBenchmark.waitForSketchesTimeoutSeconds=300"
)

for arg in "${default_jvm_args[@]}"; do
	add_arg=true
	if [[ "${arg}" == -D*=* ]]; then
		property_name="${arg#-D}"
		property_name="${property_name%%=*}"
		if contains_jvm_property "${property_name}" "${user_jvm_args[@]}"; then
			add_arg=false
		fi
	elif contains_exact "${arg}" "${user_jvm_args[@]}"; then
		add_arg=false
	fi
	if ${add_arg}; then
		passthrough_args+=(--jvm-arg "${arg}")
	fi
done

cmd=("${HELPER}")

if [[ -n "${benchmark_id}" ]]; then
	cmd+=("${benchmark_id}")
fi

cmd+=("${passthrough_args[@]}")

if ${dry_run}; then
	exec "${cmd[@]}"
fi

find_jfrconv() {
	if command -v jfrconv >/dev/null 2>&1; then
		command -v jfrconv
		return 0
	fi
	if [[ -x /opt/homebrew/bin/jfrconv ]]; then
		printf '%s\n' /opt/homebrew/bin/jfrconv
		return 0
	fi
	if [[ -x /opt/homebrew/Cellar/async-profiler/4.4/bin/jfrconv ]]; then
		printf '%s\n' /opt/homebrew/Cellar/async-profiler/4.4/bin/jfrconv
		return 0
	fi
	return 1
}

host_path_for_jfr() {
	local jfr_path="$1"

	if [[ "${jfr_path}" == "${DOCKER_WORKDIR}/"* ]]; then
		printf '%s/%s\n' "${REPO_ROOT}" "${jfr_path#${DOCKER_WORKDIR}/}"
	else
		printf '%s\n' "${jfr_path}"
	fi
}

extract_jfr_path() {
	local log_path="$1"

	sed -n 's/^.*Recording will be written to \(.*\)\.$/\1/p' "${log_path}" | tail -1
}

write_flamegraph() {
	local jfr_path="$1"
	local jfrconv_bin collapsed_path svg_path width profile_label

	if [[ ! -f "${jfr_path}" ]]; then
		echo "Warning: JFR file not found for flame graph conversion: ${jfr_path}" >&2
		return 1
	fi
	if [[ ! -x "${FLAMEGRAPH_CONVERTER}" ]]; then
		echo "Warning: Flame graph converter not found or not executable: ${FLAMEGRAPH_CONVERTER}" >&2
		return 1
	fi
	if ! jfrconv_bin="$(find_jfrconv)"; then
		echo "Warning: jfrconv not found; install async-profiler to create JFR flame graphs." >&2
		return 1
	fi

	collapsed_path="${jfr_path%.jfr}.cpu-time.collapsed"
	svg_path="${jfr_path%.jfr}.cpu-time.svg"
	width="${RDF4J_JFR_FLAMEGRAPH_WIDTH:-8000}"
	profile_label="CPU-time"

	if ! "${jfrconv_bin}" --cpu-time --output collapsed "${jfr_path}" "${collapsed_path}" \
		|| ! python3 "${FLAMEGRAPH_CONVERTER}" "${collapsed_path}" "${svg_path}" --width "${width}" \
			--title "JFR CPU-time flame graph: $(basename "${jfr_path}")"; then
		echo "Warning: no CPU-time samples found; falling back to regular JFR execution samples." >&2
		collapsed_path="${jfr_path%.jfr}.cpu.collapsed"
		svg_path="${jfr_path%.jfr}.cpu.svg"
		profile_label="CPU execution-sample"
		"${jfrconv_bin}" --cpu --output collapsed "${jfr_path}" "${collapsed_path}"
		python3 "${FLAMEGRAPH_CONVERTER}" "${collapsed_path}" "${svg_path}" --width "${width}" \
			--title "JFR CPU flame graph: $(basename "${jfr_path}")"
	fi

	echo
	echo "JFR ${profile_label} collapsed stacks: ${collapsed_path}"
	echo "JFR ${profile_label} SVG flame graph: ${svg_path}"
	echo "Reminder: inspect the SVG flame graph before leaving this benchmark loop."
}

run_log="$(mktemp "${TMPDIR:-/tmp}/rdf4j-docker-jfr-loop.XXXXXX")"
set +e
"${cmd[@]}" 2>&1 | tee "${run_log}"
cmd_status=${PIPESTATUS[0]}
set -e

jfr_path="$(extract_jfr_path "${run_log}")"
if [[ -n "${jfr_path}" ]]; then
	host_jfr_path="$(host_path_for_jfr "${jfr_path}")"
	if [[ ${cmd_status} -eq 0 ]]; then
		if ! write_flamegraph "${host_jfr_path}"; then
			echo "Warning: automatic SVG flame graph creation failed." >&2
			echo "Reminder: inspect the JFR manually before leaving this benchmark loop: ${host_jfr_path}" >&2
		fi
	else
		echo "JFR recording path reported before failure: ${host_jfr_path}" >&2
		echo "Reminder: inspect the JFR if it was written before the failed benchmark exited." >&2
	fi
else
	echo "Warning: could not find a JFR recording path in benchmark output." >&2
fi

exit "${cmd_status}"
