#!/usr/bin/env bash
set -euo pipefail

usage() {
	cat <<'USAGE'
Usage:
  ./theme-query-benchmark-results.sh clean [options] [file ...]
  ./theme-query-benchmark-results.sh capture [options] [file]

Modes:
  clean    Remove known benchmark noise from stdin or files.
  capture  Read full JMH output, clean it, move the summary table to the top,
           and write a markdown result file.

Options:
  --in-place           Rewrite the provided files instead of printing to stdout.
  --output <path>      Output path. For capture, defaults to a new markdown file
                       in the theme-query benchmark results folder.
  --results-dir <dir>  Override the default capture directory.
  --help, -h           Show this help text.

Examples:
  pbpaste | ./theme-query-benchmark-results.sh capture
  ./theme-query-benchmark-results.sh capture raw-jmh.txt
  ./theme-query-benchmark-results.sh clean --in-place results-2026-04-24-2.md
USAGE
}

die() {
	echo "Error: $*" >&2
	exit 1
}

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_RESULTS_DIR="${SCRIPT_DIR}"

mode="${1:-}"
if [[ -z "${mode}" ]]; then
	usage >&2
	exit 1
fi
shift || true

case "${mode}" in
clean | capture)
	;;
--help | -h)
	usage
	exit 0
	;;
*)
	die "unknown mode '${mode}'"
	;;
esac

in_place=false
output_path=""
results_dir="${DEFAULT_RESULTS_DIR}"
inputs=()

while [[ $# -gt 0 ]]; do
	case "$1" in
	--in-place)
		in_place=true
		shift
		;;
	--output)
		output_path="$2"
		shift 2
		;;
	--results-dir)
		results_dir="$2"
		shift 2
		;;
	--help | -h)
		usage
		exit 0
		;;
	--)
		shift
		while [[ $# -gt 0 ]]; do
			inputs+=("$1")
			shift
		done
		;;
	-*)
		die "unknown option '$1'"
		;;
	*)
		inputs+=("$1")
		shift
		;;
	esac
done

clean_stream() {
	awk '
	function is_noise(line) {
		return line ~ /^[0-9][0-9]:[0-9][0-9]:[0-9][0-9],[0-9][0-9][0-9] \|-[A-Z]+ in ch\.qos\.logback\./ \
			|| line ~ /^\/Users\/.*\/bin\/java .*org\.openjdk\.jmh\.Main/ \
			|| line ~ /^Process finished with exit code 0$/ \
			|| line ~ /^WARNING: A terminally deprecated method in / \
			|| line ~ /^WARNING: A restricted method in / \
			|| line ~ /^WARNING: .* has been called by / \
			|| line ~ /^WARNING: Please consider reporting this to the maintainers of class / \
			|| line ~ /^WARNING: Use --enable-native-access=/ \
			|| line ~ /^WARNING: Use --enable-final-field-mutation=/ \
			|| line ~ /^WARNING: Restricted methods will be blocked in a future release/ \
			|| line ~ /^WARNING: Mutating final fields will be blocked in a future release/ \
			|| line ~ /^WARNING: Final field .* has been mutated reflectively by class / \
			|| line ~ /^WARNING: .* will be removed in a future release/
	}
	{
		if ($0 ~ /^REMEMBER: The numbers below are just data\./) {
			skip_remember = 1
			next
		}
		if (skip_remember) {
			if ($0 ~ /^Do not assume the numbers tell you what you want them to tell\.$/) {
				skip_remember = 0
			}
			next
		}
		if ($0 ~ /^NOTE: Current JVM experimentally supports Compiler Blackholes, and they are in use\./) {
			skip_blackhole_note = 1
			next
		}
		if (skip_blackhole_note) {
			if ($0 ~ /^modes can be very significant\. Please make sure you use the consistent Blackhole mode for comparisons\.$/) {
				skip_blackhole_note = 0
			}
			next
		}
		if ($0 ~ /\/target\/lmdb-theme-query-benchmark\/complete/) {
			sub(/\/[^[:space:]]*\/target\/lmdb-theme-query-benchmark\/complete/, "")
			sub(/[[:space:]]+$/, "")
			if ($0 ~ /^[[:space:]]*$/) {
				next
			}
		}
		if (is_noise($0)) {
			next
		}
		if ($0 ~ /^[[:space:]]*$/) {
			if (!blank_printed) {
				print ""
			}
			blank_printed = 1
			next
		}
		print
		blank_printed = 0
	}
	' "$1"
}

extract_summary_table() {
	awk '
	/^Benchmark[[:space:]]+\(themeName\)[[:space:]]+\(z_queryIndex\)[[:space:]]+Mode[[:space:]]+Cnt[[:space:]]+Score/ {
		in_table = 1
	}
	in_table {
		if ($0 ~ /^[[:space:]]*$/) {
			exit
		}
		print
	}
	' "$1"
}

synthesize_summary_table() {
	awk '
	BEGIN {
		header = sprintf("%-37s %-15s %15s %5s %4s %10s %7s %s",
			"Benchmark", "(themeName)", "(z_queryIndex)", "Mode", "Cnt", "Score", "Error", "Units")
	}
	function trim(value) {
		sub(/^[[:space:]]+/, "", value)
		sub(/[[:space:]]+$/, "", value)
		return value
	}
	/^# Parameters: \(themeName = [A-Z_]+, z_queryIndex = [0-9]+\)$/ {
		line = $0
		sub(/^# Parameters: \(themeName = /, "", line)
		sub(/\)$/, "", line)
		split(line, parts, /, z_queryIndex = /)
		theme = trim(parts[1])
		query = trim(parts[2])
		next
	}
	/^Result "org\.eclipse\.rdf4j\.sail\.lmdb\.benchmark\.ThemeQueryBenchmark\.executeQuery":$/ {
		expect_score = 1
		next
	}
	expect_score && $0 ~ /^[[:space:]]*[0-9.]+[[:space:]]+ms\/op$/ {
		line = trim($0)
		split(line, parts, /[[:space:]]+/)
		rows[++count] = sprintf("%-37s %15s %15s %5s %4s %10s %7s %s",
			"ThemeQueryBenchmark.executeQuery", theme, query, "avgt", "", parts[1], "", "ms/op")
		expect_score = 0
	}
	END {
		if (count == 0) {
			exit
		}
		print header
		for (i = 1; i <= count; i++) {
			print rows[i]
		}
	}
	' "$1"
}

remove_summary_table() {
	awk '
	BEGIN {
		header_seen = 0
	}
	!header_seen && /^Benchmark[[:space:]]+\(themeName\)[[:space:]]+\(z_queryIndex\)[[:space:]]+Mode[[:space:]]+Cnt[[:space:]]+Score/ {
		header_seen = 1
		skipping = 1
		next
	}
	skipping {
		if ($0 ~ /^[[:space:]]*$/) {
			skipping = 0
			next
		}
		next
	}
	{
		print
	}
	' "$1"
}

render_capture_markdown() {
	local summary_file="$1"
	local body_file="$2"
	local target_file="$3"

	{
		printf '```\n'
		cat "${summary_file}"
		printf '\n```\n'

		if [[ -s "${body_file}" ]]; then
			printf '\n```\n'
			cat "${body_file}"
			printf '\n```\n'
		fi
	} >"${target_file}"
}

next_default_capture_path() {
	local target_dir="$1"
	local day
	local candidate
	local suffix=1

	day="$(date +%F)"
	candidate="${target_dir}/results-${day}.md"
	while [[ -e "${candidate}" ]]; do
		suffix=$((suffix + 1))
		candidate="${target_dir}/results-${day}-${suffix}.md"
	done
	printf '%s\n' "${candidate}"
}

rewrite_in_place() {
	local source="$1"
	local temp_file
	temp_file="$(mktemp "${TMPDIR:-/tmp}/theme-query-clean.XXXXXX")"
	clean_stream "${source}" >"${temp_file}"
	mv "${temp_file}" "${source}"
}

case "${mode}" in
clean)
	if ${in_place}; then
		[[ ${#inputs[@]} -gt 0 ]] || die "clean --in-place needs at least one file"
		[[ -z "${output_path}" ]] || die "clean --in-place cannot be combined with --output"
		for input in "${inputs[@]}"; do
			[[ -f "${input}" ]] || die "input file '${input}' does not exist"
			rewrite_in_place "${input}"
			echo "Cleaned ${input}"
		done
		exit 0
	fi

	if [[ ${#inputs[@]} -eq 0 ]]; then
		stdin_file="$(mktemp "${TMPDIR:-/tmp}/theme-query-clean-stdin.XXXXXX")"
		cat >"${stdin_file}"
		if [[ -n "${output_path}" ]]; then
			clean_stream "${stdin_file}" >"${output_path}"
		else
			clean_stream "${stdin_file}"
		fi
		rm -f "${stdin_file}"
		exit 0
	fi

	[[ ${#inputs[@]} -eq 1 ]] || die "clean without --in-place accepts exactly one input file"
	[[ -f "${inputs[0]}" ]] || die "input file '${inputs[0]}' does not exist"
	if [[ -n "${output_path}" ]]; then
		clean_stream "${inputs[0]}" >"${output_path}"
	else
		clean_stream "${inputs[0]}"
	fi
	;;
capture)
	${in_place} && die "capture does not support --in-place"
	[[ ${#inputs[@]} -le 1 ]] || die "capture accepts at most one input file"

	working_input="$(mktemp "${TMPDIR:-/tmp}/theme-query-capture-input.XXXXXX")"
	cleaned_file="$(mktemp "${TMPDIR:-/tmp}/theme-query-capture-clean.XXXXXX")"
	summary_file="$(mktemp "${TMPDIR:-/tmp}/theme-query-capture-summary.XXXXXX")"
	body_file="$(mktemp "${TMPDIR:-/tmp}/theme-query-capture-body.XXXXXX")"

	if [[ ${#inputs[@]} -eq 1 ]]; then
		[[ -f "${inputs[0]}" ]] || die "input file '${inputs[0]}' does not exist"
		cat "${inputs[0]}" >"${working_input}"
	else
		cat >"${working_input}"
	fi

	clean_stream "${working_input}" >"${cleaned_file}"
	extract_summary_table "${cleaned_file}" >"${summary_file}" || true
	if [[ ! -s "${summary_file}" ]]; then
		synthesize_summary_table "${cleaned_file}" >"${summary_file}" || true
	fi
	[[ -s "${summary_file}" ]] || die "could not find or synthesize a theme query benchmark summary table"
	remove_summary_table "${cleaned_file}" >"${body_file}"

	if [[ -z "${output_path}" ]]; then
		mkdir -p "${results_dir}"
		output_path="$(next_default_capture_path "${results_dir}")"
	else
		mkdir -p "$(dirname "${output_path}")"
	fi

	render_capture_markdown "${summary_file}" "${body_file}" "${output_path}"
	echo "Wrote ${output_path}"

	rm -f "${working_input}" "${cleaned_file}" "${summary_file}" "${body_file}"
	;;
esac
