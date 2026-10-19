#!/usr/bin/env bash

set -euo pipefail

show_usage() {
	cat <<'EOF'
Usage: build-javadoc-archive.sh [--output-dir <path>] [--help]

Builds the aggregated RDF4J Javadocs and creates a compressed archive for
the current project version. By default, the archive is written to
site/static/javadoc/<version>.tar.xz and a latest.tar.xz companion copy is
maintained.

Environment overrides:
  JAVADOC_BUILD_CMD      Custom shell command that prepares target/reports/apidocs.
                         When set, the Maven build steps are skipped.
  JAVADOC_SKIP_INSTALL   If set to "true", skips the initial mvn -Pquick install.
  JAVADOC_SOURCE_DIR     Use an existing apidocs directory instead of searching defaults.
  JAVADOC_ARCHIVE_VERSION
                         Override the version used for the archive file name.
EOF
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

OUTPUT_DIR="${REPO_ROOT}/site/static/javadoc"
COMPRESSOR="${JAVADOC_COMPRESSOR:-xz}"

while [[ $# -gt 0 ]]; do
	case "$1" in
		--output-dir)
			if [[ $# -lt 2 ]]; then
				echo "Missing value for --output-dir" >&2
				exit 1
			fi
			OUTPUT_DIR="$2"
			shift 2
			;;
		-h|--help)
			show_usage
			exit 0
			;;
		*)
			echo "Unknown option: $1" >&2
			show_usage >&2
			exit 1
			;;
	esac
done

mkdir -p "${OUTPUT_DIR}"
OUTPUT_DIR="$(cd "${OUTPUT_DIR}" && pwd)"

cd "${REPO_ROOT}"

run_maven_with_fallback() {
	local -a args=("$@")
	local -a offline_cmd=("mvn" "-o" "${args[@]}")
	if "${offline_cmd[@]}"; then
		return 0
	fi

	echo "Offline Maven command failed, retrying online: mvn ${args[*]}" >&2
	local -a online_cmd=("mvn" "${args[@]}")
	"${online_cmd[@]}"
}

locate_javadoc_dir() {
	if [[ -n "${JAVADOC_SOURCE_DIR:-}" ]]; then
		if [[ -d "${JAVADOC_SOURCE_DIR}" ]]; then
			printf '%s\n' "${JAVADOC_SOURCE_DIR}"
			return 0
		fi
		echo "JAVADOC_SOURCE_DIR '${JAVADOC_SOURCE_DIR}' does not exist" >&2
		return 1
	fi

	local candidate
	for candidate in "${REPO_ROOT}/target/reports/apidocs" "${REPO_ROOT}/target/site/apidocs"; do
		if [[ -d "${candidate}" ]]; then
			printf '%s\n' "${candidate}"
			return 0
		fi
	done

	return 1
}

determine_version() {
	local version
	version="$(awk -F'[<>]' '/<version>/{print $3; exit}' pom.xml)"
	if [[ -z "${version:-}" ]]; then
		echo "Unable to determine project version from pom.xml" >&2
		exit 1
	fi
	printf '%s\n' "${version}"
}

prepare_javadocs() {
	if [[ -n "${JAVADOC_SOURCE_DIR:-}" && -d "${JAVADOC_SOURCE_DIR}" ]]; then
		echo "Using pre-built Javadocs from ${JAVADOC_SOURCE_DIR}; skipping Maven build." >&2
		return
	fi

	if [[ -n "${JAVADOC_BUILD_CMD:-}" ]]; then
		echo "Using custom javadoc build command from JAVADOC_BUILD_CMD" >&2
		bash -c "${JAVADOC_BUILD_CMD}"
		return
	fi

	if [[ "${JAVADOC_SKIP_INSTALL:-}" != "true" ]]; then
		run_maven_with_fallback "-Pquick" "install"
	else
		echo "Skipping Maven install step because JAVADOC_SKIP_INSTALL=true" >&2
	fi

	run_maven_with_fallback "-Passembly" "-DskipTests" "-Djapicmp.skip" "javadoc:aggregate-no-fork"
}

PROJECT_VERSION="${JAVADOC_ARCHIVE_VERSION:-$(determine_version)}"
echo "Building Javadocs for RDF4J ${PROJECT_VERSION}"

prepare_javadocs

JAVADOC_DIR="$(locate_javadoc_dir 2>/dev/null || true)"
if [[ -z "${JAVADOC_DIR}" || ! -d "${JAVADOC_DIR}" ]]; then
	echo "Unable to locate generated Javadocs. Checked target/reports/apidocs and target/site/apidocs." >&2
	exit 1
fi

if [[ -z "$(find "${JAVADOC_DIR}" -mindepth 1 -print -quit)" ]]; then
	echo "Javadoc directory '${JAVADOC_DIR}' is empty" >&2
	exit 1
fi

TMP_ARCHIVE="$(mktemp "${OUTPUT_DIR}/.${PROJECT_VERSION}.XXXXXX.tar.${COMPRESSOR}")"
trap 'rm -f "${TMP_ARCHIVE}"' EXIT

echo "Compressing javadocs"

case "${COMPRESSOR}" in
	xz)
		tar --no-xattrs --exclude '*/.*' -c -C "${JAVADOC_DIR}" . | xz -9 -T0 > "${TMP_ARCHIVE}"
		EXTENSION="tar.xz"
		;;
	zst|zstd)
		tar --no-xattrs --exclude '*/.*' -c -C "${JAVADOC_DIR}" . | zstd -q --ultra -22 -T0 -o "${TMP_ARCHIVE}"
		EXTENSION="tar.zst"
		;;
	gz|gzip)
		tar --no-xattrs --exclude '*/.*' -czf "${TMP_ARCHIVE}" -C "${JAVADOC_DIR}" .
		EXTENSION="tgz"
		;;
	*)
		echo "Unsupported compressor '${COMPRESSOR}'. Supported values: xz, zst, gz." >&2
		exit 1
		;;
esac

FINAL_ARCHIVE="${OUTPUT_DIR}/${PROJECT_VERSION}.${EXTENSION}"
mv -f "${TMP_ARCHIVE}" "${FINAL_ARCHIVE}"
trap - EXIT

cp -f "${FINAL_ARCHIVE}" "${OUTPUT_DIR}/latest.${EXTENSION}"

echo "Created archive: ${FINAL_ARCHIVE}"
echo "Updated latest.${EXTENSION}"
