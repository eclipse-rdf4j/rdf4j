#!/bin/bash

set -o pipefail

determine_target_dir() {
	local archive_name="$1"

	case "${archive_name}" in
		*.tar.xz) printf '%s\n' "${archive_name%.tar.xz}" ;;
		*.tar.zst) printf '%s\n' "${archive_name%.tar.zst}" ;;
		*.tar.gz) printf '%s\n' "${archive_name%.tar.gz}" ;;
		*.tgz) printf '%s\n' "${archive_name%.tgz}" ;;
		*) printf '%s\n' "${archive_name%.*}" ;;
	esac
}

extract_archive() {
	local archive_path="$1"
	local target_dir="$2"
	local archive_name

	archive_name="$(basename -- "${archive_path}")"

	case "${archive_name}" in
		*.tar.xz)
			tar -xJf "${archive_path}" -C "${target_dir}"
			;;
		*.tar.zst)
			if command -v unzstd >/dev/null 2>&1; then
				tar --use-compress-program=unzstd -xf "${archive_path}" -C "${target_dir}"
			else
				if ! command -v zstd >/dev/null 2>&1; then
					echo "Required 'zstd' binary not found." >&2
					return 1
				fi
				if ! zstd -d --stdout "${archive_path}" | tar -xf - -C "${target_dir}"; then
					echo "Failed to extract '${archive_name}' with zstd." >&2
					return 1
				fi
			fi
			;;
		*.tar.gz|*.tgz)
			tar -xzf "${archive_path}" -C "${target_dir}"
			;;
		*)
			echo "Unsupported archive '${archive_name}'." >&2
			return 1
			;;
	esac
}

for path in ./*.tar.xz ./*.tar.zst ./*.tar.gz ./*.tgz; do
	[ -e "${path}" ] || continue

	archive="$(basename -- "${path}")"
	dirname="$(determine_target_dir "${archive}")"

	should_extract=false
	if [ ! -d "${dirname}" ]; then
		mkdir -p "${dirname}"
		should_extract=true
	elif [ "${dirname}" -ot "${archive}" ]; then
		should_extract=true
	fi

	if "${should_extract}"; then
		if ! extract_archive "${path}" "${dirname}"; then
			echo "Failed to extract '${archive}'." >&2
			exit 1
		fi
	fi

	touch "${dirname}"
done
