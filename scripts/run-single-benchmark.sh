#!/usr/bin/env bash
set -euo pipefail

usage() {
        cat <<USAGE
Usage: $0 --module <modulePath> --class <fullyQualifiedClass> --method <methodName> [options]
       $0 --theme-plan-run --theme-query <THEME:INDEX> [options]

Options:
  --theme-plan-run                  Shortcut for ThemeQueryPlanRunBenchmark.runQuery
  --theme-query <THEME:INDEX>       Add themeName/z_queryIndex params, e.g. SOCIAL_MEDIA:5
  --clean                           Force a clean rebuild before packaging
  --no-build, --skip-build          Skip Maven packaging and run an existing benchmark jar
  --dry-run                         Print the Maven and JMH commands without executing them
  --warmup-iterations <number>      Number of warmup iterations (default: 1)
  --measurement-iterations <number> Number of measurement iterations (default: 3)
  --forks <number>                  Number of forks (default: 1)
  --param <name=value>              Append a benchmark parameter override (can be repeated)
  --jvm-arg <value>                 Append a JVM argument (can be repeated)
  --jmh-arg <value>                 Append a raw JMH argument (can be repeated)
  --enable-jfr                      Enable JFR profiling with fixed iteration and timing settings
  --enable-jfr-cpu-times            Include Java 25 CPU time JFR options (requires --enable-jfr)
  --jfr-output <path>               Override the destination file for the JFR recording
  --enable-jmh-jfr                  Enable JMH's Java Flight Recorder profiler
  --jmh-jfr-output-dir <path>       Override the destination directory for JMH JFR recordings
  --                                Treat the remaining arguments as raw JMH arguments

Environment:
  RDF4J_BENCHMARK_PLAN_GUARD=false  Disable early query-plan risk detection
USAGE
}

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

module=""
benchmark_class=""
benchmark_method=""
theme_plan_run=false
theme_query=""
clean_build=false
no_build=false
dry_run=false
warmup_iterations=1
measurement_iterations=3
forks=1
benchmark_params=()
jmh_extra_args=()
jvm_args=()
profiling_jvm_args=()
measurement_time=""
enable_jfr=false
enable_jfr_cpu_times=false
jfr_output=""
enable_jmh_jfr=false
jmh_jfr_output_dir=""
warmup_overridden=false
measurement_overridden=false
forks_overridden=false
jfr_notice=""
jmh_jfr_notice=""
mvn_threads="${RDF4J_BENCHMARK_MVN_THREADS:-2C}"

while [[ $# -gt 0 ]]; do
        case "$1" in
        --theme-plan-run)
                theme_plan_run=true
                shift
                ;;
        --theme-query)
                theme_query="$2"
                shift 2
                ;;
        --clean)
                clean_build=true
                shift
                ;;
        --no-build|--skip-build)
                no_build=true
                shift
                ;;
        --module|-m)
                module="$2"
                shift 2
                ;;
        --class|-c)
                benchmark_class="$2"
                shift 2
                ;;
        --method|-b|--benchmark)
                benchmark_method="$2"
                shift 2
                ;;
        --warmup-iterations)
                warmup_iterations="$2"
                warmup_overridden=true
                shift 2
                ;;
        --measurement-iterations)
                measurement_iterations="$2"
                measurement_overridden=true
                shift 2
                ;;
        --forks)
                forks="$2"
                forks_overridden=true
                shift 2
                ;;
        --param)
                benchmark_params+=("$2")
                shift 2
                ;;
        --jvm-arg)
                jvm_args+=("$2")
                shift 2
                ;;
        --jmh-arg)
                jmh_extra_args+=("$2")
                shift 2
                ;;
        --enable-jfr)
                enable_jfr=true
                shift
                ;;
        --enable-jfr-cpu-times)
                enable_jfr_cpu_times=true
                shift
                ;;
        --jfr-output)
                jfr_output="$2"
                shift 2
                ;;
        --enable-jmh-jfr)
                enable_jmh_jfr=true
                shift
                ;;
        --jmh-jfr-output-dir)
                jmh_jfr_output_dir="$2"
                shift 2
                ;;
        --dry-run)
                dry_run=true
                shift
                ;;
        --help|-h)
                usage
                exit 0
                ;;
        --)
                shift
                while [[ $# -gt 0 ]]; do
                        jmh_extra_args+=("$1")
                        shift
                done
                ;;
        *)
                echo "Unknown option: $1" >&2
                usage >&2
                exit 1
                ;;
        esac
done

if ${theme_plan_run}; then
        if [[ -z "${module}" ]]; then
                module="core/sail/lmdb"
        fi
        if [[ -z "${benchmark_class}" ]]; then
                benchmark_class="org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryPlanRunBenchmark"
        fi
        if [[ -z "${benchmark_method}" ]]; then
                benchmark_method="runQuery"
        fi
        if ! ${warmup_overridden}; then
                warmup_iterations=2
        fi
        if ! ${measurement_overridden}; then
                measurement_iterations=2
        fi
fi

if [[ -n "${theme_query}" ]]; then
        if [[ "${theme_query}" != *:* ]]; then
                echo "Error: --theme-query expects THEME:INDEX." >&2
                exit 1
        fi
        theme_query_theme="${theme_query%%:*}"
        theme_query_index="${theme_query##*:}"
        if [[ -z "${theme_query_theme}" || -z "${theme_query_index}" ]]; then
                echo "Error: --theme-query expects THEME:INDEX." >&2
                exit 1
        fi
        if ! [[ "${theme_query_index}" =~ ^[0-9]+$ ]]; then
                echo "Error: --theme-query index must be numeric." >&2
                exit 1
        fi
        benchmark_params+=("themeName=${theme_query_theme}")
        benchmark_params+=("z_queryIndex=${theme_query_index}")
fi

if [[ -z "${module}" || -z "${benchmark_class}" || -z "${benchmark_method}" ]]; then
        echo "Error: --module, --class, and --method are required." >&2
        usage >&2
        exit 1
fi

if ${clean_build} && ${no_build}; then
        echo "Error: --clean and --no-build cannot be combined." >&2
        exit 1
fi

for param in "${benchmark_params[@]}"; do
        if [[ "${param}" != *=* ]]; then
                echo "Error: --param expects name=value." >&2
                exit 1
        fi
done

module_dir="${REPO_ROOT}/${module}"
if [[ ! -d "${module_dir}" ]]; then
        echo "Error: Module directory '${module}' does not exist." >&2
        exit 1
fi

if ${enable_jfr} && ${enable_jmh_jfr}; then
        echo "Error: --enable-jfr and --enable-jmh-jfr cannot be combined." >&2
        exit 1
fi

if ${enable_jfr_cpu_times} && ! ${enable_jfr}; then
        echo "Error: --enable-jfr-cpu-times requires --enable-jfr." >&2
        exit 1
fi

if ${enable_jfr}; then
        start_flight_recording_options=()

        if (( ${#jmh_extra_args[@]} > 0 )); then
                echo "Error: --enable-jfr cannot be combined with additional JMH arguments." >&2
                exit 1
        fi

        if ${warmup_overridden} && [[ "${warmup_iterations}" != "0" ]]; then
                echo "Error: --enable-jfr requires 0 warmup iterations." >&2
                exit 1
        fi

        if ${measurement_overridden} && [[ "${measurement_iterations}" != "10" ]]; then
                echo "Error: --enable-jfr requires 10 measurement iterations." >&2
                exit 1
        fi

        if ${forks_overridden} && [[ "${forks}" != "1" ]]; then
                echo "Error: --enable-jfr requires a single fork." >&2
                exit 1
        fi

        warmup_iterations=0
        measurement_iterations=10
        measurement_time="10s"
        forks=1

        if [[ -z "${jfr_output}" ]]; then
                local_class="${benchmark_class##*.}"
                sanitized_class="${local_class//[^A-Za-z0-9_]/_}"
                sanitized_method="${benchmark_method//[^A-Za-z0-9_]/_}"
                jfr_output="${module_dir}/target/${sanitized_class}.${sanitized_method}.jfr"
        elif [[ "${jfr_output}" != /* ]]; then
                jfr_output="${REPO_ROOT}/${jfr_output}"
        fi

        start_flight_recording_options+=("settings=profile")
        start_flight_recording_options+=("dumponexit=true")
        start_flight_recording_options+=("filename=${jfr_output}")
        start_flight_recording_options+=("duration=1200s")

        if ${enable_jfr_cpu_times}; then
                start_flight_recording_options+=("jdk.CPUTimeSample#enabled=true")
                start_flight_recording_options+=("method-profiling=max")
                start_flight_recording_options+=("report-on-exit=cpu-time-hot-methods")
        fi

        start_flight_recording_option="$(printf '%s,' "${start_flight_recording_options[@]}")"
        start_flight_recording_option="${start_flight_recording_option%,}"
        jvm_args+=("-XX:StartFlightRecording:${start_flight_recording_option}")
        profiling_jvm_args+=("-Drdf4j.benchmark.profiling=true")

        jfr_notice="JFR profiling enabled: enforcing warmup=0, measurement=10 iterations of 10s, forks=1. Recording will be written to ${jfr_output}."
fi

if ${enable_jmh_jfr}; then
        if (( ${#jmh_extra_args[@]} > 0 )); then
                echo "Error: --enable-jmh-jfr cannot be combined with additional JMH arguments." >&2
                exit 1
        fi

        if ${warmup_overridden} && [[ "${warmup_iterations}" != "0" ]]; then
                echo "Error: --enable-jmh-jfr requires 0 warmup iterations." >&2
                exit 1
        fi

        if ${measurement_overridden} && [[ "${measurement_iterations}" != "10" ]]; then
                echo "Error: --enable-jmh-jfr requires 10 measurement iterations." >&2
                exit 1
        fi

        if ${forks_overridden} && [[ "${forks}" != "1" ]]; then
                echo "Error: --enable-jmh-jfr requires a single fork." >&2
                exit 1
        fi

        warmup_iterations=0
        measurement_iterations=10
        measurement_time="10s"
        forks=1

        local_class="${benchmark_class##*.}"
        sanitized_class="${local_class//[^A-Za-z0-9_]/_}"
        sanitized_method="${benchmark_method//[^A-Za-z0-9_]/_}"
        if [[ -z "${jmh_jfr_output_dir}" ]]; then
                jmh_jfr_output_dir="${module_dir}/target/jmh-jfr/${sanitized_class}.${sanitized_method}"
        elif [[ "${jmh_jfr_output_dir}" != /* ]]; then
                jmh_jfr_output_dir="${REPO_ROOT}/${jmh_jfr_output_dir}"
        fi

        jmh_extra_args+=("-prof" "jfr:dir=${jmh_jfr_output_dir},configName=profile,debugNonSafePoints=true")
        profiling_jvm_args+=("-Drdf4j.benchmark.profiling=true")
        jmh_jfr_notice="JMH JFR profiling enabled: enforcing warmup=0, measurement=10 iterations of 10s, forks=1. Recordings will be written under ${jmh_jfr_output_dir}."
fi

mvn_common_args=(
        "-Dmaven.repo.local=.m2_repo"
        "-pl" "${module}"
        "-am"
        "-P" "benchmarks,quick"
)
mvn_goals=(package)
if ${clean_build}; then
        mvn_goals=(clean package)
fi

mvn_cmd_parallel=(mvn)
if [[ -n "${mvn_threads}" ]]; then
        mvn_cmd_parallel+=("-T" "${mvn_threads}")
fi
mvn_cmd_parallel+=("${mvn_common_args[@]}" "${mvn_goals[@]}")

mvn_cmd_single_threaded=(mvn "${mvn_common_args[@]}" "${mvn_goals[@]}")

benchmark_pattern="${benchmark_class}.${benchmark_method}"
jmh_args=(-wi "${warmup_iterations}" -i "${measurement_iterations}" -f "${forks}")
if [[ -n "${measurement_time}" ]]; then
        jmh_args+=(-r "${measurement_time}")
fi
for param in "${benchmark_params[@]}"; do
        jmh_args+=(-p "${param}")
done
for arg in "${jvm_args[@]}"; do
        jmh_args+=("-jvmArgsAppend" "${arg}")
done
for arg in "${profiling_jvm_args[@]}"; do
        jmh_args+=("-jvmArgsAppend" "${arg}")
done
for arg in "${jmh_extra_args[@]}"; do
        jmh_args+=("${arg}")
done

find_benchmark_jar() {
        local module_path="$1"
        local require_existing="$2"
        local target_dir="${module_path}/target"
        mapfile -t candidates < <(find "${target_dir}" -maxdepth 2 -type f \( -name '*jmh*.jar' -o -name '*benchmark*.jar' \) 2>/dev/null | sort)
        if [[ ${#candidates[@]} -gt 0 ]]; then
                for jar in "${candidates[@]}"; do
                        if [[ "$(basename "${jar}")" != original-* ]]; then
                                printf '%s\n' "${jar}"
                                return 0
                        fi
                done
                printf '%s\n' "${candidates[0]}"
                return 0
        fi

        if [[ "${require_existing}" == "true" ]]; then
                echo "Error: Unable to locate a benchmark jar in '${target_dir}'." >&2
                exit 1
        fi

        printf '%s\n' "${module_path}/target/jmh.jar"
}

java_major_version() {
        java -version 2>&1 | awk -F '[\".]' '/version/ { print $2; exit }'
}

require_linux_java25_for_cpu_time_jfr() {
        local os_name
        local java_major

        os_name="$(uname -s)"
        if [[ "${os_name}" != "Linux" ]]; then
                echo "Error: --enable-jfr-cpu-times requires Linux at runtime. Use --dry-run on other platforms." >&2
                exit 1
        fi

        java_major="$(java_major_version)"
        if ! [[ "${java_major}" =~ ^[0-9]+$ ]] || (( java_major < 25 )); then
                echo "Error: --enable-jfr-cpu-times requires Java 25 or newer at runtime." >&2
                exit 1
        fi
}

check_jmh_fork_socket_support() {
        if [[ "${forks}" == "0" ]]; then
                return 0
        fi

        if [[ "${RDF4J_BENCHMARK_SKIP_FORK_SOCKET_PREFLIGHT:-false}" == "true" ]]; then
                return 0
        fi

        local output
        local status
        set +e
        if [[ -n "${RDF4J_BENCHMARK_FORK_SOCKET_PREFLIGHT_CMD:-}" ]]; then
                output="$(bash -c "${RDF4J_BENCHMARK_FORK_SOCKET_PREFLIGHT_CMD}" 2>&1)"
                status=$?
        else
                local tmp_dir
                local source_file
                tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/rdf4j-jmh-preflight.XXXXXX")"
                source_file="${tmp_dir}/JmhForkSocketPreflight.java"
                cat > "${source_file}" <<'JAVA'
import java.net.ServerSocket;

public class JmhForkSocketPreflight {
	public static void main(String[] args) throws Exception {
		try (ServerSocket serverSocket = new ServerSocket(0)) {
			serverSocket.setReuseAddress(true);
		}
	}
}
JAVA
                output="$(java "${source_file}" 2>&1)"
                status=$?
                rm -rf "${tmp_dir}"
        fi
        set -e

        if [[ ${status} -ne 0 ]]; then
                echo "Error: JMH fork socket preflight failed." >&2
                echo "JMH opens a local ServerSocket before forked benchmark runs. This environment will likely fail later with BinaryLinkServer/SocketException after packaging." >&2
                echo "Escalate or run outside the restricted sandbox before rerunning this benchmark, or pass --forks 0 for an in-process diagnostic run." >&2
                if [[ -n "${output}" ]]; then
                        echo "Preflight output:" >&2
                        echo "${output}" >&2
                fi
                exit 1
        fi
}

print_command() {
        printf '%q ' "$@"
        printf '\n'
}

plan_guard_enabled() {
        case "${RDF4J_BENCHMARK_PLAN_GUARD:-true}" in
        false|FALSE|0|no|NO)
                return 1
                ;;
        *)
                return 0
                ;;
        esac
}

resolve_plan_guard_python() {
        if [[ -n "${RDF4J_BENCHMARK_PLAN_GUARD_PYTHON:-}" ]]; then
                command -v "${RDF4J_BENCHMARK_PLAN_GUARD_PYTHON}"
                return
        fi

        command -v python3 || command -v python
}

run_benchmark_command() {
        if ! plan_guard_enabled; then
                "${java_cmd[@]}"
                return
        fi

        local guard_script="${REPO_ROOT}/scripts/query-plan-risk-guard.py"
        local benchmark_log="${module_dir}/target/benchmark-output.log"
        local guard_python
        mkdir -p "$(dirname "${benchmark_log}")"

        if ! guard_python="$(resolve_plan_guard_python)"; then
                echo "Warning: query-plan risk guard disabled because no Python interpreter was found." >&2
                set +e
                "${java_cmd[@]}" 2>&1 | tee "${benchmark_log}"
                local pipeline_status=("${PIPESTATUS[@]}")
                local java_status=${pipeline_status[0]:-1}
                set -e
                return "${java_status}"
        fi

        set +e
        "${java_cmd[@]}" 2>&1 | "${guard_python}" "${guard_script}" --compact --log "${benchmark_log}"
        local pipeline_status=("${PIPESTATUS[@]}")
        local java_status=${pipeline_status[0]:-1}
        local guard_status=${pipeline_status[1]:-1}
        set -e

        if [[ ${guard_status} -ne 0 ]]; then
                return "${guard_status}"
        fi
        return "${java_status}"
}

run_maven_packaging_command() {
        local append_mode="$1"
        shift
        local log_path="${REPO_ROOT}/maven-build.log"
        local tee_args=()
        if [[ "${append_mode}" == "append" ]]; then
                tee_args=(-a)
        fi

        set +e
        "$@" 2>&1 | tee "${tee_args[@]}" "${log_path}" | awk '
                /\[WARNING\]/ { next }
                /\[ERROR\]/ { print; next }
                /Reactor Summary/ { summary=1 }
                summary { print }
        '
        local status=${PIPESTATUS[0]}
        set -e
        return "${status}"
}

if ${dry_run}; then
        if ${enable_jfr}; then
                echo "${jfr_notice}"
        fi
        if ${enable_jmh_jfr}; then
                echo "${jmh_jfr_notice}"
        fi
        jar_path="$(find_benchmark_jar "${module_dir}" false)"
        if ${no_build}; then
                echo "# Maven packaging skipped by --no-build."
        else
                print_command "${mvn_cmd_parallel[@]}"
                echo "# On failure, reruns single-threaded:"
                print_command "${mvn_cmd_single_threaded[@]}"
        fi
        java_cmd=(java -jar "${jar_path}" "${jmh_args[@]}" "${benchmark_pattern}")
        print_command "${java_cmd[@]}"
        exit 0
fi

check_jmh_fork_socket_support

if ${no_build}; then
        echo "Maven packaging skipped by --no-build."
else
        (
                cd "${REPO_ROOT}"
                if ! run_maven_packaging_command truncate "${mvn_cmd_parallel[@]}"; then
                        echo "Parallel install failed. Retrying single-threaded install..." >&2
                        run_maven_packaging_command append "${mvn_cmd_single_threaded[@]}"
                fi
        )
fi

if ${enable_jfr_cpu_times}; then
        require_linux_java25_for_cpu_time_jfr
fi

if ${enable_jfr_cpu_times}; then
        require_linux_java25_for_cpu_time_jfr
fi

jar_path="$(find_benchmark_jar "${module_dir}" true)"
java_cmd=(java -jar "${jar_path}" "${jmh_args[@]}" "${benchmark_pattern}")

if ${enable_jfr}; then
        echo "${jfr_notice}"
        mkdir -p "$(dirname "${jfr_output}")"
fi

if ${enable_jmh_jfr}; then
        echo "${jmh_jfr_notice}"
        mkdir -p "${jmh_jfr_output_dir}"
fi

printf 'Running benchmark with jar %s\n' "${jar_path}"
run_benchmark_command
