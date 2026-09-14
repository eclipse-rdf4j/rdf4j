#!/usr/bin/env bash

set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
site_dir=$(cd -- "${script_dir}/.." && pwd)
output_dir="${HUGO_TEST_OUTPUT_DIR:-${site_dir}/test-output}"
base_url="${HUGO_TEST_BASE_URL:-https://rdf4j.org/}"

hugo --source "${site_dir}" \
	--destination "${output_dir}" \
	--minify \
	--baseURL "${base_url}"

test -s "${output_dir}/index.html"
test -s "${output_dir}/documentation/index.html"
test -s "${output_dir}/javadoc/index.html"
test -s "${output_dir}/categories/news/index.html"

grep -Fq '<meta name="author" content="Eclipse RDF4J developers"/>' "${output_dir}/index.html"
grep -Fq '<meta name="author" content="Eclipse RDF4J developers"/>' "${output_dir}/categories/news/index.html"
