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

# Validate metadata values semantically because minification changes HTML quoting.
python3 - "${output_dir}/index.html" "${output_dir}/categories/news/index.html" <<'PY'
import sys
from html.parser import HTMLParser
from pathlib import Path


EXPECTED_AUTHOR = "Eclipse RDF4J developers"


class MetaParser(HTMLParser):
	def __init__(self):
		super().__init__(convert_charrefs=True)
		self.author_contents = []

	def handle_starttag(self, tag, attrs):
		if tag.casefold() != "meta":
			return

		attributes = {name.casefold(): value for name, value in attrs if name}
		name = attributes.get("name")
		if isinstance(name, str) and name.casefold() == "author":
			self.author_contents.append(attributes.get("content"))


failures = []
for filename in sys.argv[1:]:
	parser = MetaParser()
	parser.feed(Path(filename).read_text(encoding="utf-8"))
	parser.close()
	if EXPECTED_AUTHOR not in parser.author_contents:
		failures.append(
			f"{filename}: expected author meta content {EXPECTED_AUTHOR!r}; "
			f"found {parser.author_contents!r}"
		)

if failures:
	print("\n".join(failures), file=sys.stderr)
	raise SystemExit(1)
PY
