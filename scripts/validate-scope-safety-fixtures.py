#!/usr/bin/env python3
# Copyright (c) 2026 Eclipse RDF4J contributors.
# SPDX-License-Identifier: BSD-3-Clause
"""Cross-check the checked-in scope-safety fixtures with an independent SPARQL engine."""

from __future__ import annotations

import argparse
from collections import Counter
import importlib.util
import json
from pathlib import Path
import subprocess
import sys


ROOT = Path(__file__).resolve().parents[1]
FIXTURE_ROOT = ROOT / "testsuites/sail/src/main/resources/scope-safety"


def load_fixture_catalog():
    source = ROOT / "scripts/generate-scope-safety-fixtures.py"
    spec = importlib.util.spec_from_file_location("scope_safety_fixture_catalog", source)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {source}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module.FIXTURES


def encoded(value) -> str:
    from rdflib import BNode, URIRef

    if isinstance(value, URIRef):
        return "@" + str(value)
    if isinstance(value, BNode):
        return "_:" + str(value)
    return str(value)


def row_key(row: dict[str, str]) -> tuple[tuple[str, str], ...]:
    return tuple(sorted(row.items()))


def validate_rdflib() -> list[str]:
    from rdflib import Dataset

    dataset = Dataset(default_union=False)
    dataset.parse(FIXTURE_ROOT / "data/scope-data.trig", format="trig")
    failures: list[str] = []
    for fixture in load_fixture_catalog():
        try:
            result = dataset.query(fixture.query)
            actual_variables = {str(variable) for variable in result.vars}
            expected_variables = set(fixture.variables)
            if actual_variables != expected_variables:
                failures.append(
                    f"{fixture.identifier}: variables {sorted(actual_variables)} != {sorted(expected_variables)}"
                )
                continue
            actual_rows = []
            for solution in result:
                actual_rows.append(
                    {
                        str(variable): encoded(value)
                        for variable, value in solution.asdict().items()
                        if value is not None
                    }
                )
            expected_rows = list(fixture.rows)
            if fixture.ordered:
                matches = [row_key(row) for row in actual_rows] == [row_key(row) for row in expected_rows]
            else:
                matches = Counter(map(row_key, actual_rows)) == Counter(map(row_key, expected_rows))
            if not matches:
                failures.append(
                    f"{fixture.identifier}: rows {Counter(map(row_key, actual_rows))} "
                    f"!= {Counter(map(row_key, expected_rows))}"
                )
        except Exception as failure:  # independent-engine diagnostics belong in the report
            failures.append(f"{fixture.identifier}: {type(failure).__name__}: {failure}")
    return failures


def validate_jena(command: str) -> list[str]:
    failures: list[str] = []
    data = FIXTURE_ROOT / "data/scope-data.trig"
    for fixture in load_fixture_catalog():
        query = FIXTURE_ROOT / f"queries/{fixture.identifier}.rq"
        completed = subprocess.run(
            [command, f"--data={data}", f"--query={query}", "--results=JSON"],
            check=False,
            capture_output=True,
            text=True,
        )
        if completed.returncode != 0:
            failures.append(f"{fixture.identifier}: Jena exited {completed.returncode}: {completed.stderr.strip()}")
            continue
        try:
            result = json.loads(completed.stdout)
        except json.JSONDecodeError as failure:
            failures.append(f"{fixture.identifier}: invalid Jena JSON: {failure}")
            continue
        actual_variables = set(result["head"].get("vars", ()))
        expected_variables = set(fixture.variables)
        if actual_variables != expected_variables:
            failures.append(
                f"{fixture.identifier}: variables {sorted(actual_variables)} != {sorted(expected_variables)}"
            )
            continue
        actual_rows = []
        for solution in result["results"]["bindings"]:
            actual_rows.append(
                {
                    name: "@" + value["value"] if value["type"] == "uri" else value["value"]
                    for name, value in solution.items()
                }
            )
        expected_rows = list(fixture.rows)
        if fixture.ordered:
            matches = [row_key(row) for row in actual_rows] == [row_key(row) for row in expected_rows]
        else:
            matches = Counter(map(row_key, actual_rows)) == Counter(map(row_key, expected_rows))
        if not matches:
            failures.append(
                f"{fixture.identifier}: rows {Counter(map(row_key, actual_rows))} "
                f"!= {Counter(map(row_key, expected_rows))}"
            )
    return failures


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--engine", choices=("rdflib", "jena"), default="rdflib")
    parser.add_argument("--jena-command", default="arq")
    args = parser.parse_args()

    failures = validate_rdflib() if args.engine == "rdflib" else validate_jena(args.jena_command)
    if failures:
        print(f"{args.engine}: {len(failures)} fixture mismatch(es)")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print(f"{args.engine}: all 50 scope-safety fixtures match exact expected results")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
