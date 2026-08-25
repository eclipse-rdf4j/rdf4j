#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  hsdis-docker.sh build [options]
  hsdis-docker.sh run [options] -- java <args...>

Build options:
  --runtime-image <image>  JDK image used for build and runtime
  --jdk-ref <ref>          OpenJDK git ref used for hsdis sources
  --backend <name>         hsdis backend: capstone or binutils
  --platform <platform>    Docker platform, e.g. linux/arm64
  --image <tag>            Docker image tag to use
  --cache-dir <dir>        Host cache directory for copied hsdis artifacts
  --jobs <n>               OpenJDK make parallelism
  --rebuild                Force docker rebuild and artifact refresh

Environment defaults:
  HSDIS_RUNTIME_IMAGE=eclipse-temurin:26-jdk-noble
  HSDIS_JDK_REF=jdk-26+35
  HSDIS_BACKEND=capstone
  HSDIS_CACHE_DIR=$HOME/.cache/codex/hotspot-jit-forensics/hsdis
USAGE
}

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
skill_dir="$(cd -- "${script_dir}/.." && pwd)"
dockerfile="${skill_dir}/docker/hsdis/Dockerfile"

host_platform() {
  case "$(uname -m)" in
    arm64|aarch64)
      echo "linux/arm64"
      ;;
    x86_64|amd64)
      echo "linux/amd64"
      ;;
    *)
      echo "linux/$(uname -m)"
      ;;
  esac
}

cpu_count() {
  getconf _NPROCESSORS_ONLN 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4
}

sanitize_tag_part() {
  tr -c 'A-Za-z0-9_.-' '-' | sed 's/--*/-/g;s/^-//;s/-$//'
}

command="${1:-}"
if [[ -z "${command}" || "${command}" == "-h" || "${command}" == "--help" ]]; then
  usage
  exit 0
fi

case "${command}" in
  build|run)
    shift
    ;;
  *)
    echo "Unknown command: ${command}" >&2
    usage
    exit 1
    ;;
esac

runtime_image="${HSDIS_RUNTIME_IMAGE:-eclipse-temurin:26-jdk-noble}"
jdk_ref="${HSDIS_JDK_REF:-jdk-26+35}"
backend="${HSDIS_BACKEND:-capstone}"
platform="${HSDIS_PLATFORM:-$(host_platform)}"
cache_dir="${HSDIS_CACHE_DIR:-${HOME}/.cache/codex/hotspot-jit-forensics/hsdis}"
jobs="${HSDIS_JOBS:-$(cpu_count)}"
image=""
rebuild=0
java_command=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --runtime-image)
      runtime_image="$2"
      shift 2
      ;;
    --jdk-ref)
      jdk_ref="$2"
      shift 2
      ;;
    --backend)
      backend="$2"
      shift 2
      ;;
    --platform)
      platform="$2"
      shift 2
      ;;
    --image)
      image="$2"
      shift 2
      ;;
    --cache-dir)
      cache_dir="$2"
      shift 2
      ;;
    --jobs)
      jobs="$2"
      shift 2
      ;;
    --rebuild)
      rebuild=1
      shift
      ;;
    --)
      shift
      java_command=("$@")
      break
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

cache_key="$(printf '%s-%s-%s-%s' "${platform}" "${runtime_image}" "${jdk_ref}" "${backend}" | sanitize_tag_part)"
if [[ -z "${image}" ]]; then
  image="codex-hotspot-hsdis:${cache_key}"
fi
artifact_dir="${cache_dir}/${cache_key}"

image_exists() {
  docker image inspect "$1" >/dev/null 2>&1
}

artifact_exists() {
  [[ -n "$(find "${artifact_dir}" -type f -name '*hsdis*.so' -print -quit 2>/dev/null)" ]]
}

build_image() {
  if [[ ! -f "${dockerfile}" ]]; then
    echo "Dockerfile not found: ${dockerfile}" >&2
    exit 1
  fi

  if [[ "${rebuild}" -eq 1 ]] || ! image_exists "${image}"; then
    docker build \
      --platform "${platform}" \
      --build-arg "RUNTIME_IMAGE=${runtime_image}" \
      --build-arg "JDK_GIT_REF=${jdk_ref}" \
      --build-arg "HSDIS_BACKEND=${backend}" \
      --build-arg "JOBS=${jobs}" \
      -f "${dockerfile}" \
      -t "${image}" \
      "${skill_dir}"
  fi

  if [[ "${rebuild}" -eq 1 ]] || ! artifact_exists; then
    tmp_dir="${artifact_dir}.tmp"
    rm -rf "${tmp_dir}"
    mkdir -p "${tmp_dir}"

    container="$(docker create "${image}")"
    docker cp "${container}:/opt/hsdis/." "${tmp_dir}/"
    docker rm "${container}" >/dev/null

    rm -rf "${artifact_dir}"
    mkdir -p "$(dirname "${artifact_dir}")"
    mv "${tmp_dir}" "${artifact_dir}"
  fi

  echo "Image: ${image}"
  echo "hsdis cache: ${artifact_dir}"
}

build_image

if [[ "${command}" == "run" ]]; then
  if [[ "${#java_command[@]}" -eq 0 ]]; then
    echo "Missing command after --" >&2
    usage
    exit 1
  fi
  if [[ "${java_command[0]}" != "java" ]]; then
    echo "Expected java as first command after --" >&2
    usage
    exit 1
  fi

  exec docker run --rm \
    --platform "${platform}" \
    --user "$(id -u):$(id -g)" \
    -v "${PWD}:${PWD}" \
    -w "${PWD}" \
    -v "${artifact_dir}:/opt/hsdis-cache:ro" \
    -e LD_LIBRARY_PATH="/opt/hsdis:/opt/hsdis-cache" \
    "${image}" \
    "${java_command[@]}"
fi
