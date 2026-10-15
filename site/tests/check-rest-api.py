#!/usr/bin/env python3
"""Check the REST API page source and the copy included in the Hugo build."""

from html.parser import HTMLParser
from pathlib import Path
import sys


REDOC_URL = "https://cdn.redoc.ly/redoc/v2.5.3/bundles/redoc.standalone.js"
SPEC_HREF = "./rdf4j-openapi.yaml"
FALLBACK_TEXT = "download the OpenAPI specification"
PAGE_TITLE = "RDF4J REST API - OpenAPI spec"
LEGACY_JAVADOC_URL = "http://docs.rdf4j.org/rest-api"
CURRENT_JAVADOC_URL = "https://rdf4j.org/documentation/reference/rest-api/"


class RestApiPage(HTMLParser):
	def __init__(self):
		super().__init__(convert_charrefs=True)
		self.title_parts = []
		self.in_title = False
		self.redoc_depth = 0
		self.spec_urls = []
		self.scripts = []
		self.links = []
		self.active_link = None

	def handle_starttag(self, tag, attrs):
		attributes = dict(attrs)
		if tag == "title":
			self.in_title = True
		elif tag == "redoc":
			self.redoc_depth += 1
			self.spec_urls.append(attributes.get("spec-url"))
		elif tag == "script":
			self.scripts.append(attributes)
		elif tag == "a":
			self.active_link = {
				"href": attributes.get("href"),
				"inside_redoc": self.redoc_depth > 0,
				"text": [],
			}

	def handle_endtag(self, tag):
		if tag == "title":
			self.in_title = False
		elif tag == "redoc":
			self.redoc_depth = max(0, self.redoc_depth - 1)
		elif tag == "a" and self.active_link is not None:
			self.active_link["text"] = "".join(self.active_link["text"]).strip()
			self.links.append(self.active_link)
			self.active_link = None

	def handle_data(self, data):
		if self.in_title:
			self.title_parts.append(data)
		if self.active_link is not None:
			self.active_link["text"].append(data)


def require(condition, message):
	if not condition:
		raise SystemExit(f"FAIL: {message}")


def validate_page(index_path):
	page = RestApiPage()
	page.feed(index_path.read_text(encoding="utf-8"))
	page.close()

	title = "".join(page.title_parts).strip()
	require(title == PAGE_TITLE, f"{index_path}: expected title {PAGE_TITLE!r}, found {title!r}")
	require(page.spec_urls == [SPEC_HREF], f"{index_path}: Redoc must reference {SPEC_HREF!r}")
	require(
		len(page.scripts) == 1 and page.scripts[0].get("src") == REDOC_URL,
		f"{index_path}: expected one pinned Redoc bundle at {REDOC_URL!r}",
	)
	require(
		page.scripts[0].get("type", "").casefold() != "module",
		f"{index_path}: Redoc 2.5.3 must load as a classic script",
	)
	fallbacks = [
		link
		for link in page.links
		if link["href"] == SPEC_HREF and link["text"] == FALLBACK_TEXT and not link["inside_redoc"]
	]
	require(
		len(fallbacks) == 1,
		f"{index_path}: expected an always-visible OpenAPI download link outside Redoc",
	)

	spec_path = index_path.parent / SPEC_HREF
	require(spec_path.is_file() and spec_path.stat().st_size > 0, f"missing OpenAPI specification {spec_path}")
	return spec_path


def main():
	require(len(sys.argv) == 4, "usage: check-rest-api.py SOURCE_INDEX BUILT_INDEX RDF4J_PROTOCOL_SESSION")
	source_index, built_index, protocol_session = map(Path, sys.argv[1:])
	source_spec = validate_page(source_index)
	built_spec = validate_page(built_index)
	require(source_spec.read_bytes() == built_spec.read_bytes(), "built OpenAPI specification differs from source")

	java_source = protocol_session.read_text(encoding="utf-8")
	require(
		LEGACY_JAVADOC_URL not in java_source,
		f"{protocol_session}: legacy REST API Javadoc URL remains",
	)
	require(
		java_source.count(CURRENT_JAVADOC_URL) == 2,
		f"{protocol_session}: expected both REST API Javadoc links to use {CURRENT_JAVADOC_URL!r}",
	)
	print("PASS: source and built REST API pages use pinned Redoc and an external OpenAPI fallback")
	print("PASS: built OpenAPI file matches the published source and both Javadoc links use the current URL")


if __name__ == "__main__":
	main()
