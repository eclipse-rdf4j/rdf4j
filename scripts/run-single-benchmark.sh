#!/usr/bin/env bash
set -euo pipefail

usage() {
        cat <<USAGE
Usage: $0 --module <modulePath> --class <fullyQualifiedClass> --method <methodName> [options]

Options:
  --dry-run                         Print the Maven and JMH commands without executing them
  --warmup-iterations <number>      Number of warmup iterations (default: 1)
  --measurement-iterations <number> Number of measurement iterations (default: 3)
  --forks <number>                  Number of forks (default: 1)
  --jvm-arg <value>                 Append a JVM argument (can be repeated)
  --jmh-arg <value>                 Append a raw JMH argument (can be repeated)
  --enable-jfr                      Enable JFR profiling with fixed iteration and timing settings
  --enable-jfr-cpu-times            Include Java 25 CPU time JFR options (requires --enable-jfr)
  --jfr-output <path>               Override the destination file for the JFR recording
  --                                Treat the remaining arguments as raw JMH arguments
USAGE
}

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

module=""
benchmark_class=""
benchmark_method=""
dry_run=false
warmup_iterations=1
measurement_iterations=3
forks=1
jmh_extra_args=()
jvm_args=()
measurement_time=""
enable_jfr=false
enable_jfr_cpu_times=false
jfr_output=""
warmup_overridden=false
measurement_overridden=false
forks_overridden=false

while [[ $# -gt 0 ]]; do
        case "$1" in
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

if [[ -z "${module}" || -z "${benchmark_class}" || -z "${benchmark_method}" ]]; then
        echo "Error: --module, --class, and --method are required." >&2
        usage >&2
        exit 1
fi

module_dir="${REPO_ROOT}/${module}"
if [[ ! -d "${module_dir}" ]]; then
        echo "Error: Module directory '${module}' does not exist." >&2
        exit 1
fi

if ${enable_jfr_cpu_times} && ! ${enable_jfr}; then
        echo "Error: --enable-jfr-cpu-times requires --enable-jfr." >&2
        exit 1
fi

if ${enable_jfr}; then
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

        jvm_args+=("-XX:StartFlightRecording=settings=profile,dumponexit=true,filename=${jfr_output},duration=120s")

        if ${enable_jfr_cpu_times}; then
                jvm_args+=("-XX:FlightRecorderOptions=enableThreadCpuTime=true,enableProcessCpuTime=true")
        fi
fi

mvn_cmd=(mvn "-pl" "${module}" "-am" "-P" "benchmarks" "-DskipTests" package)

benchmark_pattern="${benchmark_class}.${benchmark_method}"
jmh_args=(-wi "${warmup_iterations}" -i "${measurement_iterations}" -f "${forks}")
if [[ -n "${measurement_time}" ]]; then
        jmh_args+=(-r "${measurement_time}")
fi
for arg in "${jvm_args[@]}"; do
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

print_command() {
        printf '%q ' "$@"
        printf '\n'
}

if ${dry_run}; then
        jar_path="$(find_benchmark_jar "${module_dir}" false)"
        print_command "${mvn_cmd[@]}"
        java_cmd=(java -jar "${jar_path}" "${jmh_args[@]}" "${benchmark_pattern}")
        print_command "${java_cmd[@]}"
        exit 0
fi

(
        cd "${REPO_ROOT}"
        "${mvn_cmd[@]}"
)

jar_path="$(find_benchmark_jar "${module_dir}" true)"
java_cmd=(java -jar "${jar_path}" "${jmh_args[@]}" "${benchmark_pattern}")

if ${enable_jfr}; then
        mkdir -p "$(dirname "${jfr_output}")"
fi

printf 'Running benchmark with jar %s\n' "${jar_path}"
"${java_cmd[@]}"
