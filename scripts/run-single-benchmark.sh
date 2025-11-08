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
                shift 2
                ;;
        --measurement-iterations)
                measurement_iterations="$2"
                shift 2
                ;;
        --forks)
                forks="$2"
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

mvn_cmd=(mvn "-pl" "${module}" "-P" "benchmarks" "-DskipTests" package)

benchmark_pattern="${benchmark_class}.${benchmark_method}"
jmh_args=(-wi "${warmup_iterations}" -i "${measurement_iterations}" -f "${forks}")
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

printf 'Running benchmark with jar %s\n' "${jar_path}"
"${java_cmd[@]}"
