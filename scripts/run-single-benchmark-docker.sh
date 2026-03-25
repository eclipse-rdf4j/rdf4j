#!/usr/bin/env bash
set -euo pipefail

usage() {
        cat <<USAGE
Usage: $0 [existing run-single-benchmark.sh options]
       $0 <fullyQualifiedClass.method> [existing run-single-benchmark.sh options]
       $0 <fullyQualifiedClass.method> [flexible benchmark @Param values]

Runs the benchmark helper inside a Linux Java 25 container with JFR CPU time profiling enabled.
Trailing benchmark parameter values are parsed flexibly. Examples:
  $0 org.example.Benchmark.test themeName:MEDICAL_RECORDS z_queryIndex:0
  $0 org.example.Benchmark.test themeName = MEDICAL_RECORDS, z_queryIndex = 0
  $0 org.example.Benchmark.test MEDICAL_RECORDS 0

Environment:
  RDF4J_JMH_DOCKER_IMAGE    Container image to use (default: maven:3.9.11-eclipse-temurin-25)
  RDF4J_JMH_DOCKER_PLATFORM Optional docker platform override (for example linux/amd64)
  RDF4J_JMH_DOCKER_M2_REPO  Maven local repo inside the container (default: /workspace/.m2_repo_linux_j25)
USAGE
}

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
DOCKER_IMAGE="${RDF4J_JMH_DOCKER_IMAGE:-maven:3.9.11-eclipse-temurin-25}"
DOCKER_PLATFORM="${RDF4J_JMH_DOCKER_PLATFORM:-}"
DOCKER_WORKDIR="/workspace"
INNER_HOME="/tmp/home"
CONTAINER_M2_REPO="${RDF4J_JMH_DOCKER_M2_REPO:-${DOCKER_WORKDIR}/.m2_repo_linux_j25}"
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

print_command() {
        printf '%q ' "$@"
        printf '\n'
}

docker_cmd=(docker run --rm)

if [[ -n "${DOCKER_PLATFORM}" ]]; then
        docker_cmd+=(--platform "${DOCKER_PLATFORM}")
fi

docker_cmd+=(
        -v "${REPO_ROOT}:${DOCKER_WORKDIR}"
        -w "${DOCKER_WORKDIR}"
        -e "MAVEN_OPTS=-Dmaven.repo.local=${CONTAINER_M2_REPO} -Duser.home=${INNER_HOME}"
        -e "HOME=${INNER_HOME}"
        -e "MAVEN_CONFIG=${INNER_HOME}/.m2"
        --user "$(id -u):$(id -g)"
        "${DOCKER_IMAGE}"
        bash -c "mkdir -p \"\$HOME\" \"\$HOME/.m2\" && exec scripts/run-single-benchmark.sh \"\$@\" --enable-jfr --enable-jfr-cpu-times"
        run-single-benchmark-docker.sh
)

docker_cmd+=("${passthrough_args[@]}")

if ${dry_run}; then
        print_command "${docker_cmd[@]}"
        exit 0
fi

print_command "${docker_cmd[@]}"
"${docker_cmd[@]}"
