#!/usr/bin/env python3
# Copyright (c) 2026 Eclipse RDF4J contributors.
# SPDX-License-Identifier: BSD-3-Clause
"""Deterministically materialize the independently authored GH-5905 golden suite."""

from __future__ import annotations

import argparse
import html
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "testsuites/sail/src/main/resources/scope-safety"


@dataclass(frozen=True)
class Fixture:
    identifier: str
    category: str
    title: str
    ordered: bool
    variables: tuple[str, ...]
    rows: tuple[dict[str, str], ...]
    query: str


def bindings(name: str, values: Iterable[str]) -> tuple[dict[str, str], ...]:
    return tuple({name: value} for value in values)


def fixture(
    number: int,
    category: str,
    title: str,
    variables: tuple[str, ...],
    rows: Iterable[dict[str, str]],
    query: str,
    *,
    ordered: bool = False,
) -> Fixture:
    return Fixture(
        f"q{number:02d}",
        category,
        title,
        ordered,
        variables,
        tuple(rows),
        query.strip() + "\n",
    )


FIXTURES = (
    fixture(
        1,
        "boundness",
        "OPTIONAL introduces a possibly unbound variable",
        ("x", "y"),
        (
            {"x": "1", "y": "a"},
            {"x": "2"},
            {"x": "3", "y": "c"},
            {"x": "4"},
            {"x": "5"},
        ),
        '''SELECT * WHERE {
  VALUES ?x { "1" "2" "3" "4" "5" }
  OPTIONAL { VALUES (?x ?y) { ("1" "a") ("3" "c") } }
}''',
    ),
    fixture(
        2,
        "union",
        "UNION branch-only bindings",
        ("id", "left", "right"),
        (
            {"id": "1", "left": "a"},
            {"id": "2", "left": "b"},
            {"id": "3", "left": "c"},
            {"id": "4", "right": "d"},
            {"id": "5", "right": "e"},
        ),
        '''SELECT * WHERE {
  { VALUES (?id ?left) { ("1" "a") ("2" "b") ("3" "c") } }
  UNION
  { VALUES (?id ?right) { ("4" "d") ("5" "e") } }
}''',
    ),
    fixture(
        3,
        "values",
        "VALUES UNDEF can be supplied by a later join",
        ("x", "y", "z"),
        ({"x": "1", "z": "a"}, {"x": "2", "y": "20", "z": "b"}),
        '''SELECT * WHERE {
  VALUES (?x ?y) { ("1" UNDEF) ("2" "20") }
  VALUES (?x ?z) { ("1" "a") ("2" "b") }
}''',
    ),
    fixture(
        4,
        "values",
        "VALUES binding constrains OPTIONAL compatibility",
        ("x", "y"),
        ({"x": "1", "y": "a"}, {"x": "2"}),
        '''SELECT * WHERE {
  VALUES ?x { "1" "2" }
  OPTIONAL { VALUES (?x ?y) { ("1" "a") ("3" "c") } }
}''',
    ),
    fixture(
        5,
        "bind",
        "BIND error preserves the input mapping",
        ("x", "q"),
        (
            {"x": "0"},
            {"x": "1", "q": "q1"},
            {"x": "2", "q": "q2"},
            {"x": "3", "q": "q3"},
            {"x": "4", "q": "q4"},
        ),
        '''SELECT * WHERE {
  VALUES ?x { "0" "1" "2" "3" "4" }
  BIND(IF(?x = "0", ?missing, CONCAT("q", ?x)) AS ?q)
}''',
    ),
    fixture(
        6,
        "bind",
        "Sequential BIND targets are immediately available",
        ("x", "y", "z"),
        tuple({"x": value, "y": value + "a", "z": value + "ab"} for value in "12345"),
        '''SELECT * WHERE {
  VALUES ?x { "1" "2" "3" "4" "5" }
  BIND(CONCAT(?x, "a") AS ?y)
  BIND(CONCAT(?y, "b") AS ?z)
}''',
    ),
    fixture(
        7,
        "filter",
        "FILTER textual position within one group",
        ("x",),
        bindings("x", ("1", "2")),
        '''SELECT * WHERE {
  VALUES ?x { "1" "2" "3" }
  FILTER(?x = "1" || ?x = "2")
}''',
    ),
    fixture(
        8,
        "group-scope",
        "Nested ordinary group is not lateral",
        ("x", "y"),
        (),
        '''SELECT ?x ?y WHERE {
  VALUES ?x { "outer" }
  { SELECT ?y WHERE { VALUES ?y { "inner" } FILTER(?x = "outer") } }
}''',
    ),
    fixture(
        9,
        "group-scope",
        "Same-group FILTER sees preceding VALUES",
        ("x",),
        ({"x": "ok"},),
        '''SELECT * WHERE { VALUES ?x { "ok" } FILTER(?x = "ok") }''',
    ),
    fixture(
        10,
        "bind",
        "BIND before producer is not retroactive",
        ("x", "y", "z"),
        tuple({"x": value, "y": "v"} for value in "123456"),
        '''SELECT * WHERE {
  VALUES ?x { "1" "2" "3" "4" "5" "6" }
  BIND(CONCAT(?y, "!") AS ?z)
  VALUES ?y { "v" }
}''',
    ),
    fixture(
        11,
        "filter-union",
        "FILTER outside UNION on branch-only binding",
        ("x", "y"),
        bindings("x", ("a", "b")),
        '''SELECT * WHERE {
  { VALUES ?x { "a" "b" } } UNION { VALUES ?y { "a" "c" } }
  FILTER(BOUND(?x))
}''',
    ),
    fixture(
        12,
        "property-path",
        "Property-path intermediate variable remains hidden",
        ("s", "o"),
        ({"s": "@urn:a", "o": "@urn:c"}, {"s": "@urn:d", "o": "@urn:f"}),
        '''SELECT ?s ?o WHERE { ?s <urn:next>/<urn:next> ?o }''',
    ),
    fixture(
        13,
        "filter-join",
        "FILTER depends on a variable supplied by crossed JOIN operand",
        ("x", "y"),
        ({"x": "1", "y": "yes"},),
        '''SELECT * WHERE {
  VALUES ?x { "1" "2" }
  VALUES (?x ?y) { ("1" "yes") ("2" "no") }
  FILTER(?y = "yes")
}''',
    ),
    fixture(
        14,
        "filter-optional",
        "BOUND after OPTIONAL must not move before OPTIONAL",
        ("x", "y"),
        ({"x": "1", "y": "yes"},),
        '''SELECT * WHERE {
  VALUES ?x { "1" "2" }
  OPTIONAL { VALUES (?x ?y) { ("1" "yes") } }
  FILTER(BOUND(?y))
}''',
    ),
    fixture(
        15,
        "filter-bag",
        "OR filter is not bag-union of its disjuncts",
        ("x",),
        bindings("x", ("a", "a", "b")),
        '''SELECT * WHERE {
  VALUES ?x { "a" "a" "b" "c" }
  FILTER(?x = "a" || ?x = "b")
}''',
    ),
    fixture(
        16,
        "filter-errors",
        "Short-circuit conjunction with possibly unbound operand",
        ("x", "y"),
        ({"x": "a"}, {"x": "b", "y": "2"}),
        '''SELECT * WHERE {
  VALUES (?x ?y) { ("a" UNDEF) ("b" "2") ("c" UNDEF) }
  FILTER(?x = "a" || ?y = "2")
}''',
    ),
    fixture(
        17,
        "bind-errors",
        "COALESCE observes OPTIONAL boundness",
        ("x", "y", "z"),
        (
            {"x": "1", "y": "one", "z": "one"},
            {"x": "2", "z": "fallback"},
            {"x": "3", "y": "three", "z": "three"},
            {"x": "4", "z": "fallback"},
            {"x": "5", "z": "fallback"},
        ),
        '''SELECT * WHERE {
  VALUES ?x { "1" "2" "3" "4" "5" }
  OPTIONAL { VALUES (?x ?y) { ("1" "one") ("3" "three") } }
  BIND(COALESCE(?y, "fallback") AS ?z)
}''',
    ),
    fixture(
        18,
        "minus-filter",
        "FILTER over MINUS must not be cloned into RHS",
        ("x",),
        bindings("x", ("a", "d")),
        '''SELECT * WHERE {
  VALUES ?x { "a" "b" "c" "d" }
  MINUS { VALUES ?x { "b" } }
  FILTER(?x != "c")
}''',
    ),
    fixture(
        19,
        "minus-filter",
        "Second MINUS filter regression with fallback row",
        ("x",),
        ({"x": "a"},),
        '''SELECT * WHERE {
  VALUES ?x { "a" "b" }
  MINUS { VALUES ?x { "a" } FILTER(?missing = "x") }
  FILTER(?x = "a")
}''',
    ),
    fixture(
        20,
        "filter-union",
        "FILTER/UNION distribution preserves duplicate multiplicity",
        ("x",),
        bindings("x", ("a", "a", "a", "a")),
        '''SELECT * WHERE {
  { VALUES ?x { "a" "a" "a" "b" } } UNION { VALUES ?x { "a" "c" } }
  FILTER(?x = "a")
}''',
    ),
    fixture(
        21,
        "optional-filter",
        "Filter inside OPTIONAL condition preserves fallback",
        ("x", "y"),
        (
            {"x": "1", "y": "yes"},
            {"x": "2"},
            {"x": "3", "y": "yes"},
            {"x": "4"},
            {"x": "5"},
        ),
        '''SELECT * WHERE {
  VALUES ?x { "1" "2" "3" "4" "5" }
  OPTIONAL {
    VALUES (?x ?y) { ("1" "yes") ("2" "no") ("3" "yes") ("4" "no") }
    FILTER(?y = "yes")
  }
}''',
    ),
    fixture(
        22,
        "optional-filter",
        "Same filter outside OPTIONAL removes fallback",
        ("x", "y"),
        ({"x": "1", "y": "yes"}, {"x": "3", "y": "yes"}),
        '''SELECT * WHERE {
  VALUES ?x { "1" "2" "3" "4" "5" }
  OPTIONAL { VALUES (?x ?y) { ("1" "yes") ("2" "no") ("3" "yes") ("4" "no") } }
  FILTER(?y = "yes")
}''',
    ),
    fixture(
        23,
        "optional-bag",
        "OPTIONAL multiple witnesses and fallback multiplicity",
        ("x", "y"),
        ({"x": "1", "y": "a"}, {"x": "1", "y": "b"}, {"x": "2"}),
        '''SELECT * WHERE {
  VALUES ?x { "1" "2" }
  OPTIONAL { VALUES (?x ?y) { ("1" "a") ("1" "b") } }
}''',
    ),
    fixture(
        24,
        "optional-union",
        "OPTIONAL does not distribute over right UNION",
        ("x", "y"),
        ({"x": "1"},),
        '''SELECT * WHERE {
  VALUES ?x { "1" }
  OPTIONAL {
    { VALUES (?x ?y) { ("1" "a") } } UNION { VALUES (?x ?y) { ("1" "b") } }
    FILTER(false)
  }
}''',
    ),
    fixture(
        25,
        "optional-union",
        "OPTIONAL distributes over left UNION",
        ("x", "y"),
        ({"x": "1", "y": "a"}, {"x": "2", "y": "b"}),
        '''SELECT * WHERE {
  { VALUES ?x { "1" } } UNION { VALUES ?x { "2" } }
  OPTIONAL { VALUES (?x ?y) { ("1" "a") ("2" "b") } }
}''',
    ),
    fixture(
        26,
        "optional-correlation",
        "OPTIONAL condition sees left and right bindings",
        ("x", "limit", "y"),
        tuple({"x": value, "limit": value, "y": value} for value in "1234"),
        '''SELECT * WHERE {
  VALUES (?x ?limit) { ("1" "1") ("2" "2") ("3" "3") ("4" "4") }
  OPTIONAL {
    VALUES (?x ?y) { ("1" "1") ("2" "2") ("3" "3") ("4" "4") }
    FILTER(?y = ?limit)
  }
}''',
    ),
    fixture(
        27,
        "optional-fallback",
        "All OPTIONAL candidates fail condition: one fallback",
        ("x", "y"),
        ({"x": "1"},),
        '''SELECT * WHERE {
  VALUES ?x { "1" }
  OPTIONAL { VALUES (?x ?y) { ("1" "a") ("1" "b") } FILTER(false) }
}''',
    ),
    fixture(
        28,
        "optional-compatibility",
        "OPTIONAL compatibility conflict retains left binding",
        ("x", "y"),
        ({"x": "1", "y": "left"},),
        '''SELECT * WHERE {
  VALUES (?x ?y) { ("1" "left") }
  OPTIONAL { VALUES (?x ?y) { ("1" "right") } }
}''',
    ),
    fixture(
        29,
        "optional-bind",
        "Later JOIN and BIND after OPTIONAL fallback",
        ("x", "y", "z", "label"),
        ({"x": "1", "z": "joined", "label": "joined!"},),
        '''SELECT * WHERE {
  VALUES ?x { "1" }
  OPTIONAL { VALUES (?x ?y) { ("2" "missing") } }
  VALUES (?x ?z) { ("1" "joined") }
  BIND(CONCAT(?z, "!") AS ?label)
}''',
    ),
    fixture(
        30,
        "optional-projection",
        "Dead-looking OPTIONAL is multiplicity-live",
        ("x",),
        bindings("x", ("1", "1", "2")),
        '''SELECT ?x WHERE {
  VALUES ?x { "1" "2" }
  OPTIONAL { VALUES (?x ?witness) { ("1" "a") ("1" "b") } }
}''',
    ),
    fixture(
        31,
        "minus",
        "MINUS with disjoint domains is a no-op",
        ("x",),
        bindings("x", ("1", "2", "3", "4", "5")),
        '''SELECT * WHERE {
  VALUES ?x { "1" "2" "3" "4" "5" }
  MINUS { VALUES ?y { "unrelated" } }
}''',
    ),
    fixture(
        32,
        "minus-scope",
        "MINUS RHS variables do not escape SELECT star",
        ("x",),
        bindings("x", ("1", "2", "3")),
        '''SELECT * WHERE {
  VALUES ?x { "1" "2" "3" }
  MINUS { VALUES ?rhsOnly { "hidden" } }
}''',
    ),
    fixture(
        33,
        "minus-empty-domain",
        "MINUS empty mapping does not remove rows",
        ("x",),
        bindings("x", ("1", "2", "3", "4", "5")),
        '''SELECT * WHERE {
  VALUES ?x { "1" "2" "3" "4" "5" }
  MINUS { BIND(?missing AS ?z) }
}''',
    ),
    fixture(
        34,
        "minus-union",
        "MINUS right UNION is existential across both branches",
        ("x",),
        bindings("x", ("a", "d")),
        '''SELECT * WHERE {
  VALUES ?x { "a" "b" "c" "d" }
  MINUS { { VALUES ?x { "b" } } UNION { VALUES ?x { "c" } } }
}''',
    ),
    fixture(
        35,
        "minus-optional",
        "MINUS RHS OPTIONAL mappings remain witnesses",
        ("x",),
        bindings("x", ("a", "c", "d")),
        '''SELECT * WHERE {
  VALUES ?x { "a" "b" "c" "d" }
  MINUS { VALUES ?x { "b" } OPTIONAL { VALUES ?witness { "w" } } }
}''',
    ),
    fixture(
        36,
        "minus-correlation",
        "MINUS RHS FILTER is not correlated",
        ("x",),
        bindings("x", ("a", "b", "c", "d", "e", "f")),
        '''SELECT * WHERE {
  VALUES ?x { "a" "b" "c" "d" "e" "f" }
  MINUS { VALUES ?y { "v" } FILTER(?x = "a") }
}''',
    ),
    fixture(
        37,
        "exists-correlation",
        "NOT EXISTS is correlated unlike MINUS",
        ("x",),
        bindings("x", ("@urn:a", "@urn:b", "@urn:c", "@urn:d", "@urn:e")),
        '''SELECT * WHERE {
  VALUES ?x { <urn:a> <urn:b> <urn:c> <urn:d> <urn:e> }
  FILTER NOT EXISTS { ?x <urn:missing> ?z }
}''',
    ),
    fixture(
        38,
        "minus-values",
        "MINUS overlap depends on actual VALUES domains",
        ("x", "y"),
        ({"x": "b", "y": "one"},),
        '''SELECT * WHERE {
  VALUES (?x ?y) { ("a" UNDEF) ("b" "one") }
  MINUS { VALUES ?x { "a" } }
}''',
    ),
    fixture(
        39,
        "exists",
        "EXISTS sees current outer mapping",
        ("x",),
        ({"x": "b"},),
        '''SELECT * WHERE {
  VALUES ?x { "a" "b" "c" }
  FILTER EXISTS { VALUES ?x { "b" } }
}''',
    ),
    fixture(
        40,
        "exists-scope",
        "EXISTS-local variables do not escape SELECT star",
        ("x",),
        bindings("x", ("a", "b", "c")),
        '''SELECT * WHERE {
  VALUES ?x { "a" "b" "c" }
  FILTER EXISTS { VALUES ?localOnly { "hidden" } }
}''',
    ),
    fixture(
        41,
        "exists",
        "NOT EXISTS over missing property",
        ("x",),
        bindings("x", ("@urn:a", "@urn:b")),
        '''SELECT * WHERE {
  VALUES ?x { <urn:a> <urn:b> }
  FILTER NOT EXISTS { ?x <urn:missing> ?z }
}''',
    ),
    fixture(
        42,
        "exists-bag",
        "EXISTS correlation varies per duplicate outer row",
        ("x",),
        bindings("x", ("a", "a", "a", "b")),
        '''SELECT * WHERE {
  VALUES ?x { "a" "a" "a" "b" "c" }
  FILTER EXISTS { VALUES ?x { "a" "b" } }
}''',
    ),
    fixture(
        43,
        "subquery-scope",
        "Subselect hidden variable is independent outside",
        ("x", "hidden"),
        tuple({"x": value, "hidden": "outer"} for value in "abcd"),
        '''SELECT * WHERE {
  { SELECT ?x WHERE {
      VALUES (?x ?hidden) { ("a" "i1") ("b" "i2") ("c" "i3") ("d" "i4") }
    } }
  VALUES ?hidden { "outer" }
}''',
    ),
    fixture(
        44,
        "subquery-capture",
        "Subselect flattening must not capture hidden same-name variable",
        ("x", "hidden"),
        ({"x": "a", "hidden": "outer"}, {"x": "b", "hidden": "outer"}),
        '''SELECT * WHERE {
  VALUES ?hidden { "outer" }
  { SELECT ?x WHERE { VALUES (?x ?hidden) { ("a" "inner-a") ("b" "inner-b") } } }
}''',
    ),
    fixture(
        45,
        "subquery-distinct",
        "Subselect DISTINCT is cardinality-visible outside",
        ("x",),
        bindings("x", ("a", "b", "c", "d", "e")),
        '''SELECT * WHERE {
  { SELECT DISTINCT ?x WHERE { VALUES ?x { "a" "a" "b" "c" "d" "e" } } }
}''',
    ),
    fixture(
        46,
        "subquery-bag",
        "Subselect without DISTINCT preserves projected duplicates",
        ("x",),
        bindings("x", ("a", "a", "b", "c", "d", "e")),
        '''SELECT * WHERE {
  { SELECT ?x WHERE { VALUES ?x { "a" "a" "b" "c" "d" "e" } } }
}''',
    ),
    fixture(
        47,
        "subquery-alias",
        "Subselect alias is the exported interface",
        ("x",),
        bindings("x", ("a", "b", "c", "d", "e")),
        '''SELECT * WHERE {
  { SELECT (?inner AS ?x) WHERE { VALUES ?inner { "a" "b" "c" "d" "e" } } }
}''',
    ),
    fixture(
        48,
        "grouping",
        "Grouping creates a query-level interface",
        ("x", "count"),
        (
            {"x": "a", "count": "2"},
            {"x": "b", "count": "1"},
            {"x": "c", "count": "1"},
        ),
        '''SELECT ?x (STR(COUNT(*)) AS ?count) WHERE {
  VALUES ?x { "a" "a" "b" "c" }
} GROUP BY ?x''',
    ),
    fixture(
        49,
        "subquery-sequence",
        "Subselect ORDER BY and LIMIT remain local",
        ("x",),
        bindings("x", ("a", "b")),
        '''SELECT ?x WHERE {
  { SELECT ?x WHERE {
      VALUES (?x ?rank) { ("c" "3") ("a" "1") ("b" "2") }
    } ORDER BY ?rank LIMIT 2 }
} ORDER BY ?x''',
        ordered=True,
    ),
    fixture(
        50,
        "graph-scope",
        "GRAPH variable is exported from named-graph matching",
        ("g", "s", "mark"),
        (
            {"g": "@urn:g1", "s": "@urn:a", "mark": "m1"},
            {"g": "@urn:g2", "s": "@urn:b", "mark": "m2"},
        ),
        '''SELECT ?g ?s ?mark WHERE { GRAPH ?g { ?s <urn:mark> ?mark } }''',
    ),
)


DATASET = '''@prefix ex: <urn:> .

ex:a ex:next ex:b ; ex:value "1" ; ex:kind "A" .
ex:b ex:next ex:c ; ex:value "2" ; ex:kind "B" .
ex:c ex:value "3" ; ex:kind "A" .
ex:d ex:next ex:e .
ex:e ex:next ex:f .

<urn:g1> { ex:a ex:mark "m1" . }
<urn:g2> { ex:b ex:mark "m2" . }
'''


def xml_value(value: str) -> str:
    if value.startswith("@"):
        return f"<uri>{html.escape(value[1:])}</uri>"
    return f"<literal>{html.escape(value)}</literal>"


def render_result(item: Fixture) -> str:
    lines = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        '<sparql xmlns="http://www.w3.org/2005/sparql-results#">',
        "  <head>",
    ]
    lines.extend(f'    <variable name="{html.escape(name)}"/>' for name in item.variables)
    lines.extend(("  </head>", "  <results>"))
    for row in item.rows:
        lines.append("    <result>")
        for name in item.variables:
            if name in row:
                lines.append(f'      <binding name="{html.escape(name)}">{xml_value(row[name])}</binding>')
        lines.append("    </result>")
    lines.extend(("  </results>", "</sparql>", ""))
    return "\n".join(lines)


def render_manifest() -> str:
    lines = [
        "@prefix mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#> .",
        "@prefix qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#> .",
        "@prefix ss: <https://rdf4j.org/tests/scope-safety#> .",
        "",
        '<> a mf:Manifest ;',
        '   mf:name "RDF4J scope-safety optimizer regression suite" ;',
        "   mf:entries (",
    ]
    lines.extend(f"      <#{item.identifier}>" for item in FIXTURES)
    lines.extend(("   ) .", ""))
    for item in FIXTURES:
        lines.extend(
            (
                f"<#{item.identifier}> a mf:QueryEvaluationTest ;",
                f'   mf:name "{item.title}" ;',
                f"   mf:action [ qt:query <queries/{item.identifier}.rq> ; "
                "ss:dataset <data/scope-data.trig> ] ;",
                f"   mf:result <results/{item.identifier}.srx> ;",
                f"   ss:ordered {str(item.ordered).lower()} ;",
                f'   ss:category "{item.category}" .',
                "",
            )
        )
    return "\n".join(lines)


def render_index() -> str:
    lines = ["id,category,rows,ordered,title,query,result"]
    for item in FIXTURES:
        title = item.title.replace('"', '""')
        lines.append(
            f'{item.identifier},{item.category},{len(item.rows)},{str(item.ordered).lower()},'
            f'"{title}",queries/{item.identifier}.rq,results/{item.identifier}.srx'
        )
    return "\n".join(lines) + "\n"


def render_catalog() -> str:
    lines = [
        "# Scope-safety golden query catalog",
        "",
        "The suite contains exactly **50** checked-in SPARQL query files. Result comparison is an exact multiset "
        "comparison, except q49 which is sequence-sensitive.",
        "",
        "| ID | Category | Rows | Ordered | Title |",
        "|---|---|---:|:---:|---|",
    ]
    for item in FIXTURES:
        lines.append(
            f"| {item.identifier} | {item.category} | {len(item.rows)} | "
            f"{'yes' if item.ordered else 'no'} | {item.title} |"
        )
    return "\n".join(lines) + "\n"


def rendered_files() -> dict[Path, str]:
    files = {
        OUTPUT / "data/scope-data.trig": DATASET,
        OUTPUT / "manifest.ttl": render_manifest(),
        OUTPUT / "suite.csv": render_index(),
        OUTPUT / "CATALOG.md": render_catalog(),
    }
    for item in FIXTURES:
        files[OUTPUT / f"queries/{item.identifier}.rq"] = item.query
        files[OUTPUT / f"results/{item.identifier}.srx"] = render_result(item)
    return files


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="fail if committed output differs")
    args = parser.parse_args()

    failures: list[str] = []
    for path, content in rendered_files().items():
        if args.check:
            if not path.exists() or path.read_text(encoding="utf-8") != content:
                failures.append(str(path.relative_to(ROOT)))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8", newline="\n")
    if failures:
        print("Scope-safety fixtures are stale:")
        for failure in failures:
            print(f"- {failure}")
        return 1
    if not args.check:
        print(f"Wrote {len(rendered_files())} deterministic scope-safety fixture files.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
