#!/usr/bin/env bash
set -euo pipefail

usage() {
        cat <<USAGE
Usage: $0 [existing run-single-benchmark.sh options]
       $0 <fullyQualifiedClass.method> [existing run-single-benchmark.sh options]
       $0 <fullyQualifiedClass.method> [flexible benchmark @Param values]

Runs the benchmark helper inside a Linux Java 26 container with JFR CPU time profiling enabled.
Trailing benchmark parameter values are parsed flexibly. Examples:
  $0 org.example.Benchmark.test themeName:MEDICAL_RECORDS z_queryIndex:0
  $0 org.example.Benchmark.test themeName = MEDICAL_RECORDS, z_queryIndex = 0
  $0 org.example.Benchmark.test MEDICAL_RECORDS 0

Environment:
  RDF4J_JMH_DOCKER_IMAGE    Container image to use (default: maven:3.9.14-sapmachine-26)
  RDF4J_JMH_DOCKER_PLATFORM Optional docker platform override (for example linux/amd64)
  RDF4J_JMH_DOCKER_M2_REPO  Maven local repo inside the container (default: /workspace/.m2_repo_linux_j26)
  RDF4J_JMH_DOCKER_CONTAINER_NAME Optional reusable container name override
  RDF4J_THEME_BENCHMARK_STORE_DIRECTORY Host path for reusable ThemeQueryBenchmark LMDB data
  RDF4J_THEME_BENCHMARK_REUSE_CONFIRMED Set true after confirming disk/data compatibility
  RDF4J_THEME_BENCHMARK_STORE_COMPATIBILITY_KEY Optional external store invalidation key
USAGE
}

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
FLAMEGRAPH_CONVERTER="${REPO_ROOT}/scripts/jfr-collapsed-to-svg.py"
DOCKER_IMAGE="${RDF4J_JMH_DOCKER_IMAGE:-maven:3.9.14-sapmachine-26}"
DOCKER_PLATFORM="${RDF4J_JMH_DOCKER_PLATFORM:-}"
DOCKER_WORKDIR="/workspace"
INNER_HOME="/tmp/home"
CONTAINER_M2_REPO="${RDF4J_JMH_DOCKER_M2_REPO:-${DOCKER_WORKDIR}/.m2_repo_linux_j26}"
DOCKER_CONTAINER_NAME="${RDF4J_JMH_DOCKER_CONTAINER_NAME:-}"
dry_run=false
passthrough_args=()
benchmark_id=""
seen_double_dash=false
has_explicit_module=false
has_explicit_class=false
has_explicit_method=false
explicit_class=""
benchmark_class_name=""
benchmark_method_name=""
benchmark_source_path=""
flexible_param_tokens=()
benchmark_param_names=()
benchmark_param_types=()
benchmark_param_allowed_values=()
benchmark_param_assigned_values=()
normalized_flexible_param_tokens=()
theme_store_directory_host="${RDF4J_THEME_BENCHMARK_STORE_DIRECTORY:-}"
theme_store_directory_container=""
theme_store_reuse_confirmed="${RDF4J_THEME_BENCHMARK_REUSE_CONFIRMED:-false}"
theme_store_compatibility_key="${RDF4J_THEME_BENCHMARK_STORE_COMPATIBILITY_KEY:-}"
docker_extra_mounts=()

trim_whitespace() {
        local value="$1"
        value="${value#"${value%%[![:space:]]*}"}"
        value="${value%"${value##*[![:space:]]}"}"
        printf '%s' "${value}"
}

strip_token_delimiters() {
        local value="$1"
        while [[ "${value}" == ,* ]]; do
                value="${value#,}"
        done
        while [[ "${value}" == *, ]]; do
                value="${value%,}"
        done
        printf '%s' "${value}"
}

repo_absolute_path() {
        local path="$1"

        if [[ "${path}" == /* ]]; then
                printf '%s\n' "${path}"
        else
                printf '%s/%s\n' "${REPO_ROOT}" "${path}"
        fi
}

host_path_inside_repo() {
        local path="$1"

        [[ "${path}" == "${REPO_ROOT}" || "${path}" == "${REPO_ROOT}/"* ]]
}

container_path_for_theme_store() {
        local host_path="$1"

        if host_path_inside_repo "${host_path}"; then
                if [[ "${host_path}" == "${REPO_ROOT}" ]]; then
                        printf '%s\n' "${DOCKER_WORKDIR}"
                else
                        printf '%s/%s\n' "${DOCKER_WORKDIR}" "${host_path#${REPO_ROOT}/}"
                fi
        else
                printf '%s\n' "/benchmark-cache/theme-query-store"
        fi
}

resolve_benchmark_class_source() {
        local benchmark_class="$1"
        local class_path="${benchmark_class//./\/}.java"
        local -a matches

        mapfile -t matches < <(find "${REPO_ROOT}" -type f -path "*/src/*/java/${class_path}" | sort)

        if [[ ${#matches[@]} -eq 0 ]]; then
                echo "Error: Could not resolve benchmark class '${benchmark_class}' to a module." >&2
                exit 1
        fi

        if [[ ${#matches[@]} -gt 1 ]]; then
                echo "Error: Benchmark class '${benchmark_class}' resolves to multiple modules." >&2
                printf '  %s\n' "${matches[@]}" >&2
                exit 1
        fi

        printf '%s\n' "${matches[0]}"
}

resolve_benchmark_id() {
        local benchmark_ref="$1"
        local benchmark_class benchmark_method class_source_path module_path

        benchmark_class="${benchmark_ref%.*}"
        benchmark_method="${benchmark_ref##*.}"
        if [[ -z "${benchmark_class}" || -z "${benchmark_method}" || "${benchmark_class}" == "${benchmark_ref}" ]]; then
                echo "Error: benchmark shorthand must look like fully.qualified.Class.method" >&2
                exit 1
        fi

        class_source_path="$(resolve_benchmark_class_source "${benchmark_class}")"
        module_path="${class_source_path#${REPO_ROOT}/}"
        module_path="${module_path%%/src/*}"

        benchmark_class_name="${benchmark_class}"
        benchmark_method_name="${benchmark_method}"
        benchmark_source_path="${class_source_path}"
        passthrough_args+=(--module "${module_path}" --class "${benchmark_class}" --method "${benchmark_method}")
}

extract_quoted_values_from_text() {
        local text="$1"
        local after_quote value joined=""

        while [[ "${text}" == *\"*\"* ]]; do
                after_quote="${text#*\"}"
                value="${after_quote%%\"*}"
                if [[ -n "${joined}" ]]; then
                        joined+=$'\037'
                fi
                joined+="${value}"
                text="${after_quote#*\"}"
        done

        printf '%s' "${joined}"
}

load_benchmark_param_metadata() {
        local source_path="$1"
        local annotation=""
        local line normalized type name allowed_values
        local in_param=false
        local expect_field=false
        local modifier_regex='^(public|protected|private|static|final|volatile|transient)[[:space:]]+'
        local field_regex='^([^[:space:];=]+)[[:space:]]+([A-Za-z0-9_]+)[[:space:]]*[;=]'

        benchmark_param_names=()
        benchmark_param_types=()
        benchmark_param_allowed_values=()
        benchmark_param_assigned_values=()

        while IFS= read -r line || [[ -n "${line}" ]]; do
                if ${in_param}; then
                        annotation+=$'\n'"${line}"
                        if [[ "${line}" == *")"* ]]; then
                                in_param=false
                                expect_field=true
                        fi
                        continue
                fi

                if [[ "${line}" =~ ^[[:space:]]*@Param[[:space:]]*\( ]]; then
                        annotation="${line}"
                        if [[ "${line}" == *")"* ]]; then
                                expect_field=true
                        else
                                in_param=true
                        fi
                        continue
                fi

                if ${expect_field}; then
                        if [[ "${line}" =~ ^[[:space:]]*$ || "${line}" =~ ^[[:space:]]*// || "${line}" =~ ^[[:space:]]*@ ]]; then
                                continue
                        fi

                        normalized="$(trim_whitespace "${line}")"
                        while [[ "${normalized}" =~ ${modifier_regex} ]]; do
                                normalized="${normalized#${BASH_REMATCH[0]}}"
                        done

                        if [[ "${normalized}" =~ ${field_regex} ]]; then
                                type="${BASH_REMATCH[1]}"
                                name="${BASH_REMATCH[2]}"
                                allowed_values="$(extract_quoted_values_from_text "${annotation}")"
                                benchmark_param_names+=("${name}")
                                benchmark_param_types+=("${type}")
                                benchmark_param_allowed_values+=("${allowed_values}")
                                benchmark_param_assigned_values+=("")
                        fi

                        annotation=""
                        expect_field=false
                fi
        done < "${source_path}"
}

param_index_for_name() {
        local param_name="$1"
        local index

        for index in "${!benchmark_param_names[@]}"; do
                if [[ "${benchmark_param_names[$index]}" == "${param_name}" ]]; then
                        printf '%s\n' "${index}"
                        return 0
                fi
        done

        return 1
}

value_matches_allowed_at_index() {
        local index="$1"
        local value="$2"
        local allowed_values_string="${benchmark_param_allowed_values[$index]}"
        local allowed_value
        local old_ifs="${IFS}"
        local -a allowed_values=()

        if [[ -z "${allowed_values_string}" ]]; then
                return 1
        fi

        IFS=$'\037'
        read -r -a allowed_values <<< "${allowed_values_string}"
        IFS="${old_ifs}"

        for allowed_value in "${allowed_values[@]}"; do
                if [[ "${allowed_value}" == "${value}" ]]; then
                        return 0
                fi
        done

        return 1
}

value_matches_specialized_type_at_index() {
        local index="$1"
        local value="$2"

        case "${benchmark_param_types[$index]}" in
        byte|short|int|long|Byte|Short|Integer|Long)
                [[ "${value}" =~ ^-?[0-9]+$ ]]
                ;;
        float|double|Float|Double)
                [[ "${value}" =~ ^-?[0-9]+([.][0-9]+)?$ ]]
                ;;
        boolean|Boolean)
                [[ "${value}" == "true" || "${value}" == "false" ]]
                ;;
        char|Character)
                [[ ${#value} -eq 1 ]]
                ;;
        *)
                return 1
                ;;
        esac
}

count_unassigned_params() {
        local index count=0

        for index in "${!benchmark_param_names[@]}"; do
                if [[ -z "${benchmark_param_assigned_values[$index]}" ]]; then
                        count=$((count + 1))
                fi
        done

        printf '%s\n' "${count}"
}

first_unassigned_param_index() {
        local index

        for index in "${!benchmark_param_names[@]}"; do
                if [[ -z "${benchmark_param_assigned_values[$index]}" ]]; then
                        printf '%s\n' "${index}"
                        return 0
                fi
        done

        return 1
}

append_normalized_param_token() {
        local token="$1"
        local stripped key value param_index

        stripped="$(strip_token_delimiters "${token}")"
        if [[ -z "${stripped}" ]]; then
                return 0
        fi

        if [[ "${stripped}" == "=" || "${stripped}" == ":" ]]; then
                normalized_flexible_param_tokens+=("${stripped}")
                return 0
        fi

        if [[ "${stripped}" != -* && "${stripped}" == *=* ]]; then
                key="${stripped%%=*}"
                value="${stripped#*=}"
                if param_index="$(param_index_for_name "${key}" 2>/dev/null)" && [[ -n "${value}" ]]; then
                        normalized_flexible_param_tokens+=("${key}" "=" "${value}")
                        return 0
                fi
        fi

        if [[ "${stripped}" != -* && "${stripped}" == *:* ]]; then
                key="${stripped%%:*}"
                value="${stripped#*:}"
                if param_index="$(param_index_for_name "${key}" 2>/dev/null)" && [[ -n "${value}" ]]; then
                        normalized_flexible_param_tokens+=("${key}" ":" "${value}")
                        return 0
                fi
        fi

        normalized_flexible_param_tokens+=("${stripped}")
}

normalize_flexible_param_tokens() {
        local token

        normalized_flexible_param_tokens=()
        for token in "${flexible_param_tokens[@]}"; do
                append_normalized_param_token "${token}"
        done
}

infer_param_index_for_value() {
        local value="$1"
        local remaining_values="$2"
        local index
        local -a exact_matches=()
        local -a specialized_matches=()
        local unassigned_count

        for index in "${!benchmark_param_names[@]}"; do
                if [[ -n "${benchmark_param_assigned_values[$index]}" ]]; then
                        continue
                fi
                if value_matches_allowed_at_index "${index}" "${value}"; then
                        exact_matches+=("${index}")
                fi
        done

        if [[ ${#exact_matches[@]} -eq 1 ]]; then
                printf '%s\n' "${exact_matches[0]}"
                return 0
        fi

        for index in "${!benchmark_param_names[@]}"; do
                if [[ -n "${benchmark_param_assigned_values[$index]}" ]]; then
                        continue
                fi
                if value_matches_specialized_type_at_index "${index}" "${value}"; then
                        specialized_matches+=("${index}")
                fi
        done

        if [[ ${#specialized_matches[@]} -eq 1 ]]; then
                printf '%s\n' "${specialized_matches[0]}"
                return 0
        fi

        unassigned_count="$(count_unassigned_params)"
        if [[ "${unassigned_count}" == "1" || "${unassigned_count}" == "${remaining_values}" ]]; then
                first_unassigned_param_index
                return 0
        fi

        return 1
}

append_flexible_benchmark_params() {
        local token_count param_index next_index value index
        local -a positional_values=()

        if [[ -z "${benchmark_class_name}" ]]; then
                echo "Error: Flexible benchmark parameter parsing requires benchmark shorthand or --class." >&2
                exit 1
        fi

        if [[ -z "${benchmark_source_path}" ]]; then
                benchmark_source_path="$(resolve_benchmark_class_source "${benchmark_class_name}")"
        fi

        load_benchmark_param_metadata "${benchmark_source_path}"
        if [[ ${#benchmark_param_names[@]} -eq 0 ]]; then
                echo "Error: Benchmark class '${benchmark_class_name}' does not define any @Param fields." >&2
                exit 1
        fi

        normalize_flexible_param_tokens
        token_count=${#normalized_flexible_param_tokens[@]}
        index=0
        while (( index < token_count )); do
                value="${normalized_flexible_param_tokens[$index]}"
                if [[ "${value}" == "=" || "${value}" == ":" ]]; then
                        echo "Error: Unexpected separator '${value}' in benchmark parameters." >&2
                        exit 1
                fi

                if param_index="$(param_index_for_name "${value}" 2>/dev/null)"; then
                        next_index=$((index + 1))
                        if (( next_index < token_count )) && [[ "${normalized_flexible_param_tokens[$next_index]}" =~ ^(=|:)$ ]]; then
                                next_index=$((next_index + 1))
                        fi
                        if (( next_index >= token_count )); then
                                echo "Error: Missing value for benchmark parameter '${value}'." >&2
                                exit 1
                        fi
                        benchmark_param_assigned_values[$param_index]="${normalized_flexible_param_tokens[$next_index]}"
                        index=$((next_index + 1))
                        continue
                fi

                positional_values+=("${value}")
                index=$((index + 1))
        done

        for index in "${!positional_values[@]}"; do
                value="${positional_values[$index]}"
                if ! param_index="$(infer_param_index_for_value "${value}" "$(( ${#positional_values[@]} - index ))" 2>/dev/null)"; then
                        echo "Error: Could not infer which @Param field should receive '${value}' for benchmark class '${benchmark_class_name}'." >&2
                        exit 1
                fi
                benchmark_param_assigned_values[$param_index]="${value}"
        done

        for index in "${!benchmark_param_names[@]}"; do
                if [[ -n "${benchmark_param_assigned_values[$index]}" ]]; then
                        passthrough_args+=(--param "${benchmark_param_names[$index]}=${benchmark_param_assigned_values[$index]}")
                fi
        done
}

while [[ $# -gt 0 ]]; do
        case "$1" in
        --help|-h)
                usage
                exit 0
                ;;
        --dry-run)
                dry_run=true
                passthrough_args+=("$1")
                shift
                ;;
        --module|-m)
                has_explicit_module=true
                passthrough_args+=("$1" "$2")
                shift 2
                ;;
        --class|-c)
                has_explicit_class=true
                explicit_class="$2"
                benchmark_class_name="$2"
                passthrough_args+=("$1" "$2")
                shift 2
                ;;
        --method|-b|--benchmark)
                has_explicit_method=true
                benchmark_method_name="$2"
                passthrough_args+=("$1" "$2")
                shift 2
                ;;
        --warmup-iterations|--measurement-iterations|--forks|--jvm-arg|--jmh-arg|--jfr-output|--param)
                passthrough_args+=("$1" "$2")
                shift 2
                ;;
        --theme-store-directory)
                theme_store_directory_host="$2"
                shift 2
                ;;
        --theme-store-reuse-confirmed)
                theme_store_reuse_confirmed=true
                shift
                ;;
        --theme-store-compatibility-key)
                theme_store_compatibility_key="$2"
                shift 2
                ;;
        --enable-jfr|--enable-jfr-cpu-times)
                passthrough_args+=("$1")
                shift
                ;;
        --)
                seen_double_dash=true
                passthrough_args+=("$1")
                shift
                ;;
        *)
                if ! ${seen_double_dash} && [[ "${1}" != -* ]] && [[ -z "${benchmark_id}" ]] && ! ${has_explicit_module} && ! ${has_explicit_class} && ! ${has_explicit_method}; then
                        benchmark_id="$1"
                elif ! ${seen_double_dash} && [[ "${1}" != -* ]]; then
                        flexible_param_tokens+=("$1")
                else
                        passthrough_args+=("$1")
                fi
                shift
                ;;
        esac
done

if [[ -n "${benchmark_id}" ]]; then
        resolve_benchmark_id "${benchmark_id}"
elif [[ -n "${explicit_class}" ]]; then
        benchmark_source_path="$(resolve_benchmark_class_source "${explicit_class}")"
fi

if [[ ${#flexible_param_tokens[@]} -gt 0 ]]; then
        append_flexible_benchmark_params
fi

if [[ -n "${theme_store_directory_host}" ]]; then
        theme_store_directory_host="$(repo_absolute_path "${theme_store_directory_host}")"
        theme_store_directory_container="$(container_path_for_theme_store "${theme_store_directory_host}")"
        passthrough_args+=(--theme-store-directory "${theme_store_directory_container}")
        if ! host_path_inside_repo "${theme_store_directory_host}"; then
                docker_extra_mounts+=("-v" "${theme_store_directory_host}:${theme_store_directory_container}")
        fi
fi
if [[ "${theme_store_reuse_confirmed}" == "true" ]]; then
        passthrough_args+=(--theme-store-reuse-confirmed)
fi
if [[ -n "${theme_store_compatibility_key}" ]]; then
        passthrough_args+=(--theme-store-compatibility-key "${theme_store_compatibility_key}")
fi

print_command() {
        printf '%q ' "$@"
        printf '\n'
}

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
        if [[ -x /opt/homebrew/Cellar/async-profiler/4.3/bin/jfrconv ]]; then
                printf '%s\n' /opt/homebrew/Cellar/async-profiler/4.3/bin/jfrconv
                return 0
        fi
        return 1
}

host_path_for_container_path() {
        local path="$1"

        if [[ "${path}" == "${DOCKER_WORKDIR}/"* ]]; then
                printf '%s/%s\n' "${REPO_ROOT}" "${path#${DOCKER_WORKDIR}/}"
        else
                printf '%s\n' "${path}"
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

sanitize_container_name_component() {
        local value="$1"

        value="${value//[^A-Za-z0-9_.-]/-}"
        value="${value##[-.]}"
        value="${value%%[-.]}"
        if [[ -z "${value}" ]]; then
                value="repo"
        fi

        printf '%s\n' "${value}"
}

compute_container_name() {
        local repo_component hash_input hash_value

        if [[ -n "${DOCKER_CONTAINER_NAME}" ]]; then
                printf '%s\n' "${DOCKER_CONTAINER_NAME}"
                return 0
        fi

        repo_component="$(sanitize_container_name_component "$(basename "${REPO_ROOT}")")"
        hash_input="${REPO_ROOT}|${DOCKER_IMAGE}|${DOCKER_PLATFORM}|$(id -u)|${CONTAINER_M2_REPO}|${theme_store_directory_host}|${theme_store_directory_container}"
        if command -v shasum >/dev/null 2>&1; then
                hash_value="$(printf '%s' "${hash_input}" | shasum -a 256 | awk '{print substr($1, 1, 12)}')"
        elif command -v sha256sum >/dev/null 2>&1; then
                hash_value="$(printf '%s' "${hash_input}" | sha256sum | awk '{print substr($1, 1, 12)}')"
        else
                echo "Error: Need shasum or sha256sum to derive a reusable benchmark container name." >&2
                exit 1
        fi

        printf 'rdf4j-jmh-%s-%s\n' "${repo_component}" "${hash_value}"
}

container_exists() {
        local container_name="$1"
        docker container inspect "${container_name}" >/dev/null 2>&1
}

container_running() {
        local container_name="$1"
        [[ "$(docker container inspect -f '{{.State.Running}}' "${container_name}" 2>/dev/null)" == "true" ]]
}

docker_create_cmd=(docker create)
docker_start_cmd=()
docker_exec_cmd=()
container_name="$(compute_container_name)"

if [[ -n "${DOCKER_PLATFORM}" ]]; then
        docker_create_cmd+=(--platform "${DOCKER_PLATFORM}")
fi

docker_create_cmd+=(
        --name "${container_name}"
        -v "${REPO_ROOT}:${DOCKER_WORKDIR}"
        "${docker_extra_mounts[@]}"
        -w "${DOCKER_WORKDIR}"
        -e "MAVEN_OPTS=-Dmaven.repo.local=${CONTAINER_M2_REPO} -Duser.home=${INNER_HOME}"
        -e "HOME=${INNER_HOME}"
        -e "MAVEN_CONFIG=${INNER_HOME}/.m2"
        --user "$(id -u):$(id -g)"
        "${DOCKER_IMAGE}"
        sleep infinity
)

docker_start_cmd=(docker start "${container_name}")
docker_exec_cmd=(
        docker exec
        -w "${DOCKER_WORKDIR}"
        "${container_name}"
        bash -c "mkdir -p \"\$HOME\" \"\$HOME/.m2\" && exec scripts/run-single-benchmark.sh \"\$@\" --enable-jfr --enable-jfr-cpu-times"
        run-single-benchmark-docker.sh
)

docker_exec_cmd+=("${passthrough_args[@]}")

if ${dry_run}; then
        echo "# Create reusable benchmark container if missing:"
        print_command "${docker_create_cmd[@]}"
        echo "# Start reusable benchmark container if stopped:"
        print_command "${docker_start_cmd[@]}"
        echo "# Run the benchmark inside the reusable container:"
        print_command "${docker_exec_cmd[@]}"
        exit 0
fi

if [[ -n "${theme_store_directory_host}" ]] && ! host_path_inside_repo "${theme_store_directory_host}"; then
        mkdir -p "${theme_store_directory_host}"
fi

if ! container_exists "${container_name}"; then
        print_command "${docker_create_cmd[@]}"
        "${docker_create_cmd[@]}" >/dev/null
fi

if ! container_running "${container_name}"; then
        print_command "${docker_start_cmd[@]}"
        "${docker_start_cmd[@]}" >/dev/null
fi

print_command "${docker_exec_cmd[@]}"
run_log="$(mktemp "${TMPDIR:-/tmp}/rdf4j-docker-benchmark.XXXXXX")"
set +e
"${docker_exec_cmd[@]}" 2>&1 | tee "${run_log}"
cmd_status=${PIPESTATUS[0]}
set -e

jfr_path="$(extract_jfr_path "${run_log}")"
if [[ -n "${jfr_path}" ]]; then
        host_jfr_path="$(host_path_for_container_path "${jfr_path}")"
        if [[ ${cmd_status} -eq 0 ]]; then
                if ! write_flamegraph "${host_jfr_path}"; then
                        echo "Warning: automatic SVG flame graph creation failed." >&2
                        echo "Reminder: inspect the JFR manually before leaving this benchmark loop: ${host_jfr_path}" >&2
                fi
        else
                echo "JFR recording path reported before failure: ${host_jfr_path}" >&2
                echo "Reminder: inspect the JFR if it was written before the failed benchmark exited." >&2
        fi
fi

exit "${cmd_status}"
