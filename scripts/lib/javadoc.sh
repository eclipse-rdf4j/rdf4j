#!/bin/bash

resolve_javadoc_dir() {
  local base_dir=${1:-.}
  local candidates=("target/reports/apidocs" "target/site/apidocs")
  local candidate

  for candidate in "${candidates[@]}"; do
    if [[ -d "${base_dir}/${candidate}" ]]; then
      printf '%s\n' "${base_dir}/${candidate}"
      return 0
    fi
  done

  return 1
}
