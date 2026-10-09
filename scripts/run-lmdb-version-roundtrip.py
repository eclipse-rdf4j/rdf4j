#!/usr/bin/env python3
"""Deterministic, independently-oracled LMDB binary-version round trips.

This is an investigation harness: it never modifies production sources or store metadata.
Only the explicitly selected output directory contains generated repositories and evidence.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import gzip
import importlib.util
import json
import os
from pathlib import Path
import random
import signal
import shlex
import shutil
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
SUPPORT = ROOT / "scripts" / "lmdb-compatibility"
SOURCES = {
    "release": "46a2d41ab16fead609884ee655e26e6baa1ce325",
    "optimize": "39650fd2a0324d06133578c1cee620e0e456a585",
    "current": "7c0fcac6094774aa96d45e29ad9e79080ddf3a14",
}
XSD = "http://www.w3.org/2001/XMLSchema#"
RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
NS = "urn:compat:"
WRITER_STAGES = ("release-create", "optimize-first", "current-downgrade", "optimize-reupgrade")
WRITER_SEED = 17011


def b64(text: str) -> str:
    return base64.b64encode(text.encode("utf-8")).decode("ascii")


def iri(text: str) -> str:
    return "I~" + b64(text)


def blank(text: str) -> str:
    return "B~" + b64(text)


def literal(label: str, datatype: str = XSD + "string", language: str = "", direction: str = "") -> str:
    # Preserve input spelling; the Java comparator applies RDF's case-insensitive
    # language-tag identity while recording the actual decoded language tags.
    return "L~" + "~".join((b64(label), b64(datatype), b64(language), direction))


def triple(subject: str, predicate: str, obj: str) -> str:
    return "T~" + "~".join(map(b64, (subject, predicate, obj)))


def statement(subject: str, predicate: str, obj: str, context: str = "N") -> str:
    return "\t".join((subject, predicate, obj, context))


def corpus() -> list[dict[str, str]]:
    cases: list[dict[str, str]] = []

    def add(category: str, label: str, datatype: str = "string", language: str = "", direction: str = "",
            query_numeric: bool = False, query_calendar: bool = False):
        full_datatype = datatype if ":" in datatype else XSD + datatype
        if language:
            full_datatype = RDF + ("dirLangString" if direction else "langString")
        cases.append({"id": f"{category}-{sum(c['category'] == category for c in cases):03d}",
                      "category": category, "label": label, "datatype": full_datatype,
                      "language": language, "direction": direction,
                      "query_numeric": query_numeric, "query_calendar": query_calendar,
                      "term": literal(label, full_datatype, language, direction)})

    for label in ("true", "false", "1", "0", "TRUE", " true ", "invalid"):
        add("boolean", label, "boolean")
    bounds = {
        "byte": (-128, 127), "short": (-32768, 32767), "int": (-2147483648, 2147483647),
        "long": (-9223372036854775808, 9223372036854775807),
        "unsignedByte": (0, 255), "unsignedShort": (0, 65535),
        "unsignedInt": (0, 4294967295), "unsignedLong": (0, 18446744073709551615),
        "integer": (-10**80, 10**80), "nonNegativeInteger": (0, 10**60),
        "positiveInteger": (1, 10**60), "nonPositiveInteger": (-10**60, 0),
        "negativeInteger": (-10**60, -1),
    }
    for datatype, (low, high) in bounds.items():
        def valid_integer(value: int) -> bool:
            if datatype == "integer":
                return True
            if datatype == "nonNegativeInteger":
                return value >= 0
            if datatype == "positiveInteger":
                return value > 0
            if datatype == "nonPositiveInteger":
                return value <= 0
            if datatype == "negativeInteger":
                return value < 0
            return low <= value <= high
        for label in dict.fromkeys(map(str, (low - 1, low, low + 1, -1, 0, 1, high - 1, high, high + 1))):
            add("integer-" + datatype, label, datatype, query_numeric=valid_integer(int(label)))
        for label in ("+0042", "-000", "000", " 42 ", "not-an-integer"):
            add("integer-" + datatype, label, datatype,
                query_numeric=label != "not-an-integer" and valid_integer(int(label)))
    for datatype in ("float", "double"):
        for label in ("0", "0.0", "-0", "-0.0", "1.0", "-1.25", "1E10", "1e-45", "NaN", "INF",
                      "-INF", "+INF", "1.7976931348623157E308", "invalid"):
            add(datatype, label, datatype, query_numeric=label not in ("+INF", "invalid"))
    for label in ("0", "0.0", "-0.00", "1", "1.0", "1.00", "-123.4500", "+0001.00",
                  "123456789012345678901234567890.12345678901234567890", "invalid"):
        add("decimal", label, "decimal", query_numeric=label != "invalid")
    for label in ("", "a", "µ", "😀", "日本語 e\u0301 é", "\n\t\r", "dictionary" * 300):
        add("string", label)
    for language in ("en", "EN", "eN-us", "zh-Hant-TW", "en-" + "abcdefgh-" * 9 + "ij"):
        add("language", "language µ 😀", language=language)
    for direction in ("LTR", "RTL"):
        add("direction", "direction µ 😀", language="EN-us", direction=direction)
    add("custom", "custom dictionary literal", NS + "datatype:" + "x" * 100)
    add("custom", "", NS + "empty-datatype")
    for datatype in ("dateTime", "dateTimeStamp"):
        for year in ("2026", "-0042", "12026"):
            for fraction in ("", ".0", ".00", ".000", ".1", ".01", ".001", ".12", ".120", ".123456789"):
                for zone in ("Z", "", "+00:01", "+00:15", "-14:00", "+14:00"):
                    add("calendar-" + datatype, year + "-10-03T12:00:01" + fraction + zone, datatype,
                        query_calendar=year == "2026" and (datatype == "dateTime" or bool(zone)))
        for label in ("2026-10-03T24:00:00Z", "2026-02-30T12:00:00Z", "invalid calendar lexical"):
            add("calendar-" + datatype, label, datatype)
    for label in ("2026-10-03", "2026-10-03Z", "2026-10-03+00:01", "2026-10-03-14:00",
                  "-0042-10-03Z", "12026-10-03Z", "2026-02-30", "invalid date"):
        add("calendar-date", label, "date")
    core_types = ("anyURI", "base64Binary", "hexBinary", "duration", "dayTimeDuration", "yearMonthDuration",
                  "time", "gYear", "gYearMonth", "gMonth", "gMonthDay", "gDay", "normalizedString", "token",
                  "language", "Name", "NCName", "NMTOKEN", "NMTOKENS", "ID", "IDREF", "IDREFS", "ENTITY",
                  "ENTITIES", "QName", "NOTATION")
    for datatype in core_types:
        add("core-fallback", "dictionary:" + datatype, datatype)
    add("core-fallback", "{}", RDF + "JSON")
    add("core-fallback", "<element/>", RDF + "XMLLiteral")
    add("core-fallback", "<b>text</b>", RDF + "HTML")
    add("core-fallback", "POINT (1 2)", "http://www.opengis.net/ont/geosparql#wktLiteral")
    return cases


def writer_corpus_cases(stage: str) -> list[dict]:
    """Phase-unique lexical terms using the authoritative corpus's categories.

    These are original expected inputs, not a model of any runtime's codec.
    Canonical boolean spellings are finite, so the fixed boolean owners are
    rewritten separately; fresh booleans use whitespace and invalid fallbacks.
    """
    phase = WRITER_STAGES.index(stage)
    fixed = corpus()
    cases = []

    def add(category, label, *, source_label=None, language=None, direction=None,
            query_numeric=False, query_calendar=False):
        source = next(case for case in fixed if case["category"] == category
                      and (source_label is None or case["label"] == source_label)
                      and (direction is None or case["direction"] == direction))
        case = {**source, "id": f"writer-{phase}-{category}-{sum(c['category'] == category for c in cases):03d}",
                "source_case": source["id"], "phase": stage, "label": label,
                "query_numeric": query_numeric, "query_calendar": query_calendar}
        if language is not None:
            case["language"] = language
        case["term"] = literal(label, case["datatype"], case["language"], case["direction"])
        cases.append(case)

    number = 60 + phase  # Valid for byte/unsignedByte and all wider positive subtypes.
    integer_categories = dict.fromkeys(case["category"] for case in fixed if case["category"].startswith("integer-"))
    for category in integer_categories:
        value = -number if category in ("integer-negativeInteger", "integer-nonPositiveInteger") else number
        add(category, str(value), query_numeric=True)
        add(category, ("-" if value < 0 else "+") + "000" + str(abs(value)), query_numeric=True)
        add(category, f"invalid-integer-w{phase}")
    # Large values and lexical scales exercise a distinct dictionary opportunity.
    add("integer-integer", str(10**90 + phase), query_numeric=True)
    for datatype in ("float", "double"):
        for label in (f"{number}.25", f"{number}e+0", "-0." + "0" * (phase + 2)):
            add(datatype, label, query_numeric=True)
        add(datatype, f"invalid-{datatype}-w{phase}")
    for label in (f"{number}.0", f"{number}.00", "-0." + "0" * (phase + 3),
                  str(10**80 + phase) + ".12345678901234567890"):
        add("decimal", label, query_numeric=True)
    for lexical in ("true", "false", "1", "0"):
        add("boolean", " " * (phase + 2) + lexical + " ", source_label=lexical)
    add("boolean", f"invalid-boolean-w{phase}")
    add("string", f"w{phase}", source_label="a")
    add("string", f"w{phase}µ", source_label="µ")
    add("string", f"writer-{phase}:" + "µ😀" * 64, source_label="dictionary" * 300)
    add("language", f"w{phase}", language="en")
    add("language", f"u{phase}", language="EN-us")
    add("language", f"writer-{phase}:" + "µ😀" * 64, language="eN-us")
    for direction in ("LTR", "RTL"):
        add("direction", f"w{phase}", language="en", direction=direction)
        add("direction", f"writer-{phase}:" + "µ😀" * 64, language="EN-us", direction=direction)
    for datatype in ("dateTime", "dateTimeStamp"):
        category = "calendar-" + datatype
        for fraction in ("", ".0", ".00", ".000", ".1", ".001", ".123456789"):
            for zone in ("Z", "+00:01", ""):
                source_label = "2026-10-03T12:00:01" + fraction + zone
                add(category, f"2026-10-03T12:{phase + 1:02d}:01" + fraction + zone,
                    source_label=source_label, query_calendar=datatype == "dateTime" or bool(zone))
        for year, fraction in (("-0042", ".000"), ("12026", ".123456789")):
            add(category, year + f"-10-03T12:{phase + 1:02d}:01" + fraction + "Z",
                source_label=year + "-10-03T12:00:01" + fraction + "Z")
        add(category, f"invalid-calendar-w{phase}")
    for zone in ("", "Z", "+00:01"):
        add("calendar-date", f"2026-11-{phase + 1:02d}" + zone, source_label="2026-10-03" + zone)
    add("custom", f"w{phase}", source_label="custom dictionary literal")
    add("core-fallback", f"w{phase}", source_label="dictionary:normalizedString")
    return cases


class Journal:
    """Pure expected-state model: has no path to an actual repository."""

    def __init__(self, directory: Path, initial: set[str] | None = None, namespaces: dict[str, str] | None = None):
        self.directory = directory
        directory.mkdir(parents=True)
        self.statements = set(initial or ())
        self.namespaces = dict(namespaces or {})
        self.pending: set[str] | None = None
        self.pending_namespaces: dict[str, str] | None = None
        self.wire: list[str] = []
        self.events: list[dict] = []
        self.checks = 0

    def emit(self, op: str, *args: str, **details):
        self.wire.append("\t".join((op, *args)))
        self.events.append({"sequence": len(self.events), "operation": op, "arguments": args, **details})

    def begin(self, isolation: str):
        assert self.pending is None
        self.pending = self.statements.copy()
        self.pending_namespaces = self.namespaces.copy()
        self.emit("B", isolation)

    def add(self, row: str):
        assert self.pending is not None
        self.pending.add(row)
        self.emit("A", *row.split("\t"))

    def remove(self, row: str):
        assert self.pending is not None
        self.pending.discard(row)
        self.emit("D", *row.split("\t"))

    def namespace(self, prefix: str, name: str | None):
        assert self.pending_namespaces is not None
        if name is None:
            self.pending_namespaces.pop(prefix, None)
            self.emit("RN", b64(prefix))
        else:
            self.pending_namespaces[prefix] = name
            self.emit("NS", b64(prefix), b64(name))

    def finish(self, commit: bool):
        assert self.pending is not None
        if commit:
            self.statements, self.namespaces = self.pending, self.pending_namespaces
        self.pending = self.pending_namespaces = None
        if commit:
            self.emit("C", self.snapshot("confirmed-commit"))
        else:
            self.emit("R")

    def snapshot(self, label: str) -> str:
        filename = f"expected-{self.checks:03d}-{label}.tsv.gz"
        self.checks += 1
        lines = ["S\t" + row for row in sorted(self.statements)]
        lines += ["NS\t" + b64(prefix) + "\t" + b64(name) for prefix, name in sorted(self.namespaces.items())]
        (self.directory / filename).write_bytes(gzip.compress(("\n".join(lines) + "\n").encode("utf-8"),
                                                             compresslevel=6, mtime=0))
        return filename

    def check(self, label: str):
        filename = self.snapshot(label)
        self.emit("V", filename, checkpoint=label, expected_statements=len(self.statements))

    def save(self, metadata: dict):
        assert self.pending is None
        (self.directory / "operations.tsv").write_text("\n".join(self.wire) + "\n", encoding="utf-8")
        write_json(self.directory / "journal.json", {"metadata": metadata, "operations": self.events})
        write_json(self.directory / "expected-manifest.json", {
            path.name: sha256(path) for path in self.directory.iterdir()
            if (path.name.startswith("expected-") and path.name.endswith(".tsv.gz")) or path.name == "writer-cases.tsv"})
        return self.statements.copy(), self.namespaces.copy()


def write_json(path: Path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def fixed_corpus_rows(selected: list[dict], owner_prefix: str = "invariant:",
                      value_predicate: str = "value", numeric_predicate: str = "fixed-numeric-valid",
                      calendar_predicate: str = "fixed-calendar-valid") -> list[str]:
    p = iri(NS + "value")
    graph = iri(NS + "graph")
    rows: list[str] = []
    for index, case in enumerate(selected):
        s = iri(NS + owner_prefix + str(index))
        term = case["term"]
        rows.extend((statement(s, iri(NS + value_predicate), term), statement(s, iri(NS + value_predicate), term, graph)))
        if case.get("query_numeric"):
            rows.append(statement(s, iri(NS + numeric_predicate), term, graph))
        if case.get("query_calendar"):
            rows.append(statement(s, iri(NS + calendar_predicate), term, graph))
        inner = triple(blank("shared-owner"), p, term)
        nested = triple(iri(NS + "inner-owner"), p, inner)
        rows.extend((statement(s, iri(NS + "nested"), nested, graph),
                     statement(blank("shared-owner"), iri(NS + "shared"), inner)))
    return rows


def seed_rows(seed: int, size: int, payload_bytes: int, scenario: str) -> tuple[list[str], list[str]]:
    p = iri(NS + "value")
    graph = iri(NS + "graph")
    selected = corpus() if scenario in ("full-corpus", "writer-corpus") else [
        {"term": literal("stable dictionary value " + "x" * 80)}, {"term": literal("plain µ 😀")},
        {"term": literal("42", XSD + "integer")}, {"term": literal("2026-10-03T12:00:01Z", XSD + "dateTime")},
    ]
    rows = fixed_corpus_rows(selected)
    mutable: list[str] = []
    for i in range(size):
        s = iri(NS + f"row:{seed}:{i}")
        obj = literal(f"seed={seed};row={i};" + "µ" * payload_bytes) if i % 8 == 0 else literal(str(i % 101), XSD + "integer")
        context = "N" if i % 3 == 0 else graph if i % 3 == 1 else blank("context-bnode")
        mutable.append(statement(s, p, obj, context))
        rows.append(statement(s, iri(NS + "number"), literal(str(i % 101), XSD + "integer"), graph))
        rows.append(statement(s, iri(NS + "date"), literal(f"2026-10-{i % 28 + 1:02d}T12:00:01Z", XSD + "dateTime"), graph))
        rows.append(statement(s, iri(NS + "edge"), iri(NS + f"row:{seed}:{(i + 1) % size}"), graph))
    rows += mutable
    return rows, mutable


def make_journal(directory: Path, state: set[str], namespaces: dict[str, str], meta: dict, args,
                 initialize: bool, mutable: list[str]):
    if meta["scenario"] == "writer-corpus":
        return make_writer_journal(directory, state, namespaces, meta, initialize)
    journal = Journal(directory, state, namespaces)
    journal.check("entry")
    if initialize:
        journal.begin("NONE")
        journal.namespace("compat", NS)
        journal.namespace("xsd", XSD)
        rows, _ = seed_rows(meta["seed"], args.statements, args.payload_bytes, meta["scenario"])
        for row in rows:
            journal.add(row)
        # Explicit duplicates in both default and named contexts are intentional.
        for row in rows[:8]:
            journal.add(row)
        journal.finish(True)
        journal.check("seed")
        journal.emit("P")
        journal.check("seed-reopen")
    rng = random.Random(meta["seed"] * 1_000_003 + meta["stage_index"] * 101 + meta["mode_index"])
    for r in range(meta["round_start"], meta["round_end"]):
        isolation = "NONE" if r % 2 == 0 else "SNAPSHOT"
        chosen = rng.sample(mutable, min(37, len(mutable)))
        journal.begin(isolation)
        for row in chosen:
            journal.remove(row)
        journal.namespace("round", NS + f"{meta['stage']}:{r}:")
        journal.finish(True)
        journal.check(f"round-{r:02d}-delete")
        # Re-add the exact owner/dependencies after deletion/GC, with many shared
        # components and nested triple terms. Never use actual IDs as input.
        journal.begin(isolation)
        for row in chosen:
            journal.add(row)
        owner = iri(NS + f"retired-owner:{meta['stage']}:{r}")
        dependency = triple(blank("retired-shared"), iri(NS + "retired-predicate"),
                            literal(f"retired dependency {meta['stage']} {r}"))
        retired = statement(owner, iri(NS + "retired"), triple(owner, iri(NS + "nested"), dependency),
                            iri(NS + "graph"))
        journal.add(retired)
        journal.finish(True)
        journal.check(f"round-{r:02d}-readd")
        snapshot = journal.events[-1]["arguments"][0]
        journal.emit("SB", snapshot)
        # NONE does not guarantee reader isolation. Both writers spanning the
        # held SNAPSHOT reader use SNAPSHOT so the asserted view is contractual.
        journal.begin("SNAPSHOT")
        journal.remove(retired)
        journal.finish(True)
        journal.check(f"round-{r:02d}-retired-delete")
        journal.begin("SNAPSHOT")
        journal.add(retired)
        journal.finish(True)
        journal.check(f"round-{r:02d}-retired-readd")
        journal.emit("SE", snapshot)
        # Verify both statement and namespace rollback, and missing-delete no-op.
        # NONE explicitly does not guarantee rollback; exercise rollback under
        # SNAPSHOT without changing any exact statement/namespace assertion.
        journal.begin("SNAPSHOT")
        journal.add(statement(owner, iri(NS + "rollback"), literal("must never survive")))
        journal.remove(chosen[0])
        journal.namespace("compat", "urn:must-not-survive:")
        journal.namespace("xsd", None)
        journal.finish(False)
        journal.check(f"round-{r:02d}-rollback")
        if (r + 1) % 3 == 0:
            journal.emit("P")
            journal.check(f"round-{r:02d}-reopen")
    journal.check("exit")
    return journal.save(meta)


def make_writer_journal(directory: Path, state: set[str], namespaces: dict[str, str], meta: dict,
                        initialize: bool):
    journal = Journal(directory, state, namespaces)
    fixed = corpus()
    fresh = writer_corpus_cases(meta["stage"])
    fixed_rows = fixed_corpus_rows(fixed)
    fresh_rows = fixed_corpus_rows(fresh, f"writer:{meta['stage']}:", "writer-value",
                                   "writer-numeric-valid", "writer-calendar-valid")
    write_json(directory / "writer-corpus.json", {"fixed_cases": len(fixed), "fresh_cases": fresh,
               "phase": meta["stage"], "canonical_boolean_owners_rewritten": True})
    (directory / "writer-cases.tsv").write_text("\n".join(
        "\t".join((case["id"], case["category"], meta["stage"], role,
                    case.get("source_case", case["id"]), case["term"]))
        for role, cases in (("fixed", fixed), ("fresh", fresh)) for case in cases) + "\n", encoding="utf-8")
    journal.check("entry")
    if initialize:
        journal.begin("NONE")
        journal.namespace("compat", NS)
        journal.namespace("xsd", XSD)
        rows, _ = seed_rows(meta["seed"], 32, 32, "writer-corpus")
        for row in rows:
            journal.add(row)
        for row in rows[:8]:
            journal.add(row)
        journal.finish(True)
        journal.check("seed")
        journal.emit("P")
        journal.check("seed-reopen")

    # Remove every fixed direct/nested/shared/function reference, including
    # canonical booleans. SNAPSHOT writers preserve the held earlier view.
    snapshot = journal.snapshot("fixed-reader")
    journal.emit("SB", snapshot)
    journal.begin("SNAPSHOT")
    for row in fixed_rows:
        journal.remove(row)
    journal.namespace("writer", NS + meta["stage"] + ":retired:")
    journal.finish(True)
    journal.check("fixed-delete")
    journal.begin("SNAPSHOT")
    for row in fixed_rows:
        journal.add(row)
    journal.namespace("writer", NS + meta["stage"] + ":revived:")
    journal.finish(True)
    journal.check("fixed-readd")
    for case in fixed:
        journal.emit("I", "fixed-readd", meta["stage"], case["category"], case["id"], case["term"])
    journal.emit("SE", snapshot)

    # Fresh input terms are unique across phases and the original 625 cases.
    # A virtual inline ID can exist before storage; dictionary lookup must not.
    for case in fresh:
        journal.emit("I", "before", meta["stage"], case["category"], case["id"], case["term"])
    journal.begin("NONE")
    for row in fresh_rows:
        journal.add(row)
    for row in fresh_rows[:8]:
        journal.add(row)
    journal.namespace("phase", NS + meta["stage"] + ":")
    journal.finish(True)
    journal.check("fresh-commit")
    for case in fresh:
        journal.emit("I", "after", meta["stage"], case["category"], case["id"], case["term"])

    snapshot = journal.snapshot("fresh-reader")
    journal.emit("SB", snapshot)
    journal.begin("SNAPSHOT")
    for row in fresh_rows:
        journal.remove(row)
    journal.finish(True)
    journal.check("fresh-retired-delete")
    journal.begin("SNAPSHOT")
    for row in fresh_rows:
        journal.add(row)
    journal.finish(True)
    journal.check("fresh-retired-readd")
    for case in fresh:
        journal.emit("I", "fresh-readd", meta["stage"], case["category"], case["id"], case["term"])
    journal.emit("SE", snapshot)
    journal.begin("SNAPSHOT")
    journal.remove(fresh_rows[0])
    journal.add(statement(iri(NS + "rollback:" + meta["stage"]), iri(NS + "rollback"),
                          literal("never-commit-" + meta["stage"], XSD + "decimal")))
    journal.namespace("compat", "urn:must-not-survive:")
    journal.namespace("phase", None)
    journal.finish(False)
    journal.check("rollback")
    journal.emit("P")
    journal.check("exit-reopen")
    for case in fresh:
        journal.emit("I", "reopen", meta["stage"], case["category"], case["id"], case["term"])
    return journal.save(meta)


def plan_writer_campaign(output: Path) -> list[dict]:
    write_json(output / "corpus.json", corpus())
    write_json(output / "writer-corpus.json", {stage: writer_corpus_cases(stage) for stage in WRITER_STAGES})
    write_json(output / "writer-selection.json", {"mode": "writer-corpus", "seed": WRITER_SEED,
               "mutable_rows": 32, "payload_repetitions": 32, "optimizer_modes": ["normal"],
               "repositories": 6, "jobs": 20, "metadata_edge_probes": False})
    jobs = []
    configurations = [("v2", inline, False) for inline in (True, False)] + [
        ("v6", inline, ordered) for inline in (True, False) for ordered in (True, False)]
    for fmt, inline, ordered in configurations:
        profile = "complete-metadata" if fmt == "v2" else "optimized"
        name = f"seed-{WRITER_SEED}/writer-corpus/{fmt}-{profile}-inline-{str(inline).lower()}-ordered-{str(ordered).lower()}"
        state, namespaces = set(), {}
        stages = WRITER_STAGES if fmt == "v2" else WRITER_STAGES[1:]
        for stage_index, stage in enumerate(stages):
            runtime = "release" if stage == "release-create" else "current" if stage == "current-downgrade" else "optimize"
            meta = {"repository": name, "seed": WRITER_SEED, "scenario": "writer-corpus", "format": fmt,
                    "inline": inline, "ordered": ordered, "profile": profile, "stage": stage, "runtime": runtime,
                    "stage_index": stage_index, "mode": "normal", "mode_index": 0, "round_start": 0, "round_end": 1}
            directory = output / "journals" / name / stage / "normal"
            state, namespaces = make_writer_journal(directory, state, namespaces, meta, stage_index == 0)
            jobs.append({**meta, "store": str(output / "repositories" / name), "journal": str(directory),
                         "result_dir": str(output / "results" / name / stage / "normal")})
    jobs.sort(key=lambda job: (WRITER_STAGES.index(job["stage"]), job["repository"]))
    write_json(output / "campaign-plan.json", jobs)
    return jobs


def plan_campaign(output: Path, args) -> list[dict]:
    if getattr(args, "writer_corpus", False):
        return plan_writer_campaign(output)
    if getattr(args, "original_v6_results", None):
        return plan_original_v6_campaign(output, args)
    jobs: list[dict] = []
    direct = getattr(args, "direct_maintenance", False)
    scenarios = args.scenarios.split(",")
    seeds = [int(value, 0) for value in args.seeds.split(",")]
    write_json(output / "corpus.json", corpus())
    for seed in seeds:
        for scenario in scenarios:
            configurations = [("v2", inline, False) for inline in (True, False)]
            if not direct:
                configurations += [("v6", inline, ordered) for inline in (True, False) for ordered in (True, False)]
            for fmt, inline, ordered in configurations:
                profile = "complete-metadata" if fmt == "v2" else "optimized"
                name = f"seed-{seed}/{scenario}/{fmt}-{profile}-inline-{str(inline).lower()}-ordered-{str(ordered).lower()}"
                store = output / "repositories" / name
                state: set[str] = set()
                namespaces: dict[str, str] = {}
                _, mutable = seed_rows(seed, args.statements, args.payload_bytes, scenario)
                stages = [("release-create", "release")] if fmt == "v2" else []
                if not direct:
                    stages.append(("optimize-first", "optimize"))
                stages += [("current-downgrade", "current"), ("optimize-reupgrade", "optimize")]
                for stage_index, (stage, runtime) in enumerate(stages):
                    modes = args.optimize_modes.split(",") if runtime == "optimize" else ["normal"]
                    for mode_index, mode in enumerate(modes):
                        start = args.rounds * mode_index // len(modes)
                        end = args.rounds * (mode_index + 1) // len(modes)
                        meta = {"repository": name, "seed": seed, "scenario": scenario, "format": fmt,
                                "inline": inline, "ordered": ordered, "profile": profile, "stage": stage, "runtime": runtime,
                                "stage_index": stage_index, "mode": mode, "mode_index": mode_index,
                                "round_start": start, "round_end": end}
                        directory = output / "journals" / name / stage / mode
                        initialize = stage_index == 0 and mode_index == 0
                        state, namespaces = make_journal(directory, state, namespaces, meta, args, initialize, mutable)
                        job = {**meta, "store": str(store), "journal": str(directory),
                               "result_dir": str(output / "results" / name / stage / mode)}
                        jobs.append(job)
    # Authentic index-metadata edge probes use public configuration only. They
    # remain named, independent repositories and cannot stand in for v2 C chains.
    probe_args = argparse.Namespace(**vars(args))
    probe_args.rounds, probe_args.statements, probe_args.payload_bytes = 0, 16, 32
    for profile in ("default-indexes", "explicit-indexes"):
        for inline in (True, False):
            name = f"seed-{seeds[0]}/metadata-edge/{profile}-inline-{str(inline).lower()}"
            state, namespaces = set(), {}
            _, mutable = seed_rows(seeds[0], 16, 32, "metadata-edge")
            stages = [("release-create", "release")]
            if not direct:
                stages.append(("optimize-first", "optimize"))
            stages += [("current-downgrade", "current"), ("optimize-reupgrade", "optimize")]
            for stage_index, (stage, runtime) in enumerate(stages):
                meta = {"repository": name, "seed": seeds[0], "scenario": "metadata-edge", "format": "v2",
                        "inline": inline, "ordered": False, "profile": profile, "stage": stage, "runtime": runtime,
                        "stage_index": stage_index, "mode": "normal", "mode_index": 0, "round_start": 0, "round_end": 0}
                directory = output / "journals" / name / stage / "normal"
                state, namespaces = make_journal(directory, state, namespaces, meta, probe_args, stage_index == 0, mutable)
                jobs.append({**meta, "store": str(output / "repositories" / name), "journal": str(directory),
                             "result_dir": str(output / "results" / name / stage / "normal")})
    # Sorting creates all authentic v2 repos before attempting their earliest
    # transition; fresh v6 chains follow even if authentic v2 admission fails.
    priority = {"release-create": 0, "optimize-first": 1, "current-downgrade": 2, "optimize-reupgrade": 3}
    jobs.sort(key=lambda j: (priority[j["stage"]], j["seed"], j["scenario"], j["repository"], j["mode_index"]))
    if args.stop_after_stage:
        jobs = [job for job in jobs if priority[job["stage"]] <= priority[args.stop_after_stage]]
    write_json(output / "campaign-plan.json", jobs)
    if direct:
        write_json(output / "direct-selection.json", {"runtime_path": ["release", "current", "optimize"],
                   "format": "v2", "jobs": len(jobs), "repositories": len({job["store"] for job in jobs}),
                   "metadata_edge_probes": True})
    return jobs


def physical_inventory(directory: Path) -> dict[str, dict]:
    """Record exact input bytes, without opening or exporting the native store."""
    inventory = {}
    for path in sorted(directory.rglob("*")):
        if path.is_symlink():
            raise ValueError(f"Original store must contain regular files, not symlinks: {path}")
        if path.is_file():
            inventory[str(path.relative_to(directory))] = {"sha256": sha256(path), "size_bytes": path.stat().st_size}
    if not inventory:
        raise ValueError(f"Original physical store is empty: {directory}")
    return inventory


def original_input(path: str, campaign: Path) -> Path:
    resolved = Path(path).resolve()
    if not resolved.is_relative_to(campaign):
        raise ValueError(f"Original input escapes its preserved campaign: {resolved}")
    return resolved


def plan_original_v6_campaign(output: Path, args) -> list[dict]:
    """Replay copied old optimize stores using only their confirmed independent oracle."""
    result_file = args.original_v6_results.resolve()
    campaign = result_file.parent
    provenance_file = campaign / "runtime-provenance.json"
    original_runtime = json.loads(provenance_file.read_text())["optimize"]
    validate_runtime("original-optimize", original_runtime, SOURCES["optimize"])
    results = json.loads(result_file.read_text())
    seeds = {int(value, 0) for value in args.seeds.split(",")}
    scenarios = set(args.scenarios.split(","))
    selected = {}
    for result in results:
        if (result["format"] == "v6" and result["stage"] == "optimize-first"
                and result["seed"] in seeds and result["scenario"] in scenarios
                and result.get("failed_stage_physical_snapshot")):
            previous = selected.get(result["repository"])
            if previous is None or result["mode_index"] > previous["mode_index"]:
                selected[result["repository"]] = result
    if not selected:
        raise ValueError("No original optimize-created v6 stores match the selected seeds/scenarios")
    jobs, inputs = [], []
    write_json(output / "corpus.json", corpus())
    for name, result in sorted(selected.items()):
        relative = Path(name)
        if relative.is_absolute() or ".." in relative.parts:
            raise ValueError(f"Unsafe original repository name: {name}")
        if (not result.get("operations_completed") or result.get("failure_kind") not in (None, "comparison")
                or not result.get("failed_stage_physical_snapshot") or not result.get("confirmed_expected_state")):
            raise ValueError(f"Original store requires a confirmed completed physical snapshot: {name}")
        if (original_runtime["revision"] not in result["command"]
                or original_runtime["lmdb_sha256"] not in result["command"]):
            raise ValueError(f"Original result does not match the immutable optimize runtime: {name}")
        original_store = original_input(result["failed_stage_physical_snapshot"], campaign)
        confirmed = original_input(result["confirmed_expected_state"], campaign)
        journal = original_input(result["journal"], campaign)
        if confirmed.parent != journal:
            raise ValueError(f"Confirmed expected snapshot escapes its recorded journal: {confirmed}")
        manifest_file = journal / "expected-manifest.json"
        expected_manifest = json.loads(manifest_file.read_text())
        if expected_manifest.get(confirmed.name) != sha256(confirmed):
            raise ValueError(f"Original expected snapshot differs from its independent manifest: {confirmed}")
        inventory = physical_inventory(original_store)
        state, namespaces = load_expected(confirmed)
        source = output / "original-inputs" / name
        source.mkdir(parents=True)
        shutil.copy2(confirmed, source / confirmed.name)
        shutil.copy2(manifest_file, source / "expected-manifest.json")
        inputs.append({"repository": name, "original_store": str(original_store), "physical_inventory": inventory,
                       "confirmed_expected_state": str(confirmed), "confirmed_expected_sha256": sha256(confirmed),
                       "original_stage": result["stage"], "original_mode": result["mode"],
                       "runtime_revision": original_runtime["revision"], "runtime_lmdb_sha256": original_runtime["lmdb_sha256"]})
        _, mutable = seed_rows(result["seed"], args.statements, args.payload_bytes, result["scenario"])
        for stage_index, (stage, runtime) in enumerate((("current-downgrade", "current"), ("optimize-reupgrade", "optimize"))):
            modes = args.optimize_modes.split(",") if runtime == "optimize" else ["normal"]
            for mode_index, mode in enumerate(modes):
                meta = {"repository": name, "seed": result["seed"], "scenario": result["scenario"], "format": "v6",
                        "inline": result["inline"], "ordered": result["ordered"], "profile": result["profile"],
                        "stage": stage, "runtime": runtime, "stage_index": stage_index + 1, "mode": mode,
                        "mode_index": mode_index, "round_start": args.rounds * mode_index // len(modes),
                        "round_end": args.rounds * (mode_index + 1) // len(modes)}
                directory = output / "journals" / name / stage / mode
                state, namespaces = make_journal(directory, state, namespaces, meta, args, False, mutable)
                jobs.append({**meta, "store": str(output / "repositories" / name), "journal": str(directory),
                             "result_dir": str(output / "results" / name / stage / mode),
                             "original_store": str(original_store), "original_inventory": inventory})
    priority = {"current-downgrade": 0, "optimize-reupgrade": 1}
    jobs.sort(key=lambda job: (priority[job["stage"]], job["repository"], job["mode_index"]))
    if args.stop_after_stage:
        if args.stop_after_stage not in priority:
            raise ValueError("Original-v6 replay only contains current-downgrade and optimize-reupgrade stages")
        jobs = [job for job in jobs if priority[job["stage"]] <= priority[args.stop_after_stage]]
    write_json(output / "original-v6-inputs.json", {"results": str(result_file), "results_sha256": sha256(result_file),
               "runtime_provenance": str(provenance_file), "runtime_provenance_sha256": sha256(provenance_file),
               "inputs": inputs, "jobs": len(jobs)})
    write_json(output / "campaign-plan.json", jobs)
    return jobs


def copy_original_store(job: dict, output: Path) -> dict:
    original, destination = Path(job["original_store"]), Path(job["store"])
    before = physical_inventory(original)
    if before != job["original_inventory"]:
        raise ValueError(f"Original physical store changed after independent planning: {original}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copytree(original, destination)
    if physical_inventory(destination) != before or physical_inventory(original) != before:
        raise ValueError(f"Original physical copy did not preserve exact bytes: {original}")
    record = {"repository": job["repository"], "original_store": str(original), "copied_store": str(destination),
              "physical_inventory": before, "status": "copied"}
    directory = output / "original-copy-evidence" / job["repository"]
    directory.mkdir(parents=True)
    write_json(directory / "copy.json", record)
    return record


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def command_run(command: list[str], logfile: Path, cwd: Path | None = None, timeout: float | None = None,
                java_diagnostics: bool = False) -> int:
    logfile.parent.mkdir(parents=True, exist_ok=True)
    record = {"command": command, "cwd": str(cwd or ROOT), "shell_command": shlex.join(command),
              "started": time.time(), "timeout_seconds": timeout}
    with logfile.open("wb") as stream:
        process = subprocess.Popen(command, cwd=cwd or ROOT, stdout=stream, stderr=subprocess.STDOUT,
                                   start_new_session=True)
        record["owned_pid"] = process.pid
        write_json(logfile.with_suffix(".command.json"), record)
        try:
            rc = process.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            # This process was started in its own session by this invocation.
            # Preserve evidence before terminating this exact owned process group.
            diagnostics = {**record, "timed_out": time.time(), "committed_state": "uncertain"}
            if java_diagnostics:
                binary = Path(command[0])
                jcmd = binary.with_name("jcmd") if binary.is_absolute() else Path(shutil.which("jcmd") or "/missing-jcmd")
                dump_command = [str(jcmd), str(process.pid), "Thread.print", "-l"]
                diagnostics["thread_dump_command"] = dump_command
                try:
                    with logfile.with_suffix(".thread-dump.txt").open("wb") as dump:
                        dump_rc = subprocess.run(dump_command, stdout=dump, stderr=subprocess.STDOUT, timeout=15).returncode
                    diagnostics["thread_dump_exit_code"] = dump_rc
                except (OSError, subprocess.TimeoutExpired) as error:
                    diagnostics["thread_dump_error"] = str(error)
            for filename in ("confirmed-commits.tsv", "completion.properties"):
                source = logfile.parent / filename
                if source.is_file():
                    shutil.copyfile(source, logfile.parent / ("timeout-last-" + filename))
            write_json(logfile.with_suffix(".timeout.json"), diagnostics)
            if process.poll() is None:
                try:
                    os.killpg(process.pid, signal.SIGTERM)
                except ProcessLookupError:
                    pass  # The owned JVM may finish while its thread dump is collected.
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    try:
                        os.killpg(process.pid, signal.SIGKILL)
                    except ProcessLookupError:
                        pass
                    process.wait()
            rc = 124
        record["exit_code"] = rc
        record["finished"] = time.time()
        write_json(logfile.with_suffix(".command.json"), record)
        return rc


def retain_harness_sources(output: Path, prepare: bool):
    paths = [Path(__file__).resolve(), SUPPORT / "package_versions.py", SUPPORT / "CompatWorkload.java",
             *(SUPPORT / label / "CompatAdapter.java" for label in SOURCES)]
    provenance = {}
    for source in paths:
        relative = source.relative_to(ROOT)
        copied = output / "harness-sources" / relative
        copied.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, copied)
        provenance[str(relative)] = {"sha256": sha256(copied), "copied_source": str(copied),
                                    "executed": source.suffix == ".java" or source == paths[0] or prepare}
    write_json(output / "harness-source-provenance.json", provenance)


def validate_runtime(label: str, runtime: dict, expected_revision: str) -> tuple[list[str], dict[str, str]]:
    runtime_root = Path(runtime["runtime_dir"]).resolve()
    cp = [str(Path(p).resolve()) for p in runtime["classpath"]]
    if len(cp) != len(set(cp)):
        raise ValueError(f"Duplicate runtime classpath entry for {label}")
    if runtime["revision"] != expected_revision:
        raise ValueError(f"Runtime {label} source mismatch: {runtime['revision']}")
    artifacts = runtime.get("artifacts")
    if not isinstance(artifacts, list) or not artifacts:
        raise ValueError(f"Runtime {label} has no recorded immutable artifact manifest")
    manifest = {str(Path(item["path"]).resolve()): item for item in artifacts}
    if len(manifest) != len(artifacts) or set(manifest) != set(cp):
        raise ValueError(f"Runtime {label} classpath does not exactly match its artifact manifest")
    hashes = {}
    for entry in cp:
        path = Path(entry)
        if not path.is_relative_to(runtime_root) or not path.is_file() or path.suffix != ".jar":
            raise ValueError(f"Runtime {label} artifact escapes its immutable runtime directory: {path}")
        item = manifest[entry]
        if path.stat().st_size != item["size_bytes"] or sha256(path) != item["sha256"]:
            raise ValueError(f"Runtime {label} artifact differs from recorded manifest: {path}")
        hashes[entry] = item["sha256"]
    if runtime.get("source_archive_sha256"):
        archive = Path(runtime["source_archive"])
        if not archive.is_file() or sha256(archive) != runtime["source_archive_sha256"]:
            raise ValueError(f"Runtime {label} source archive differs from recorded manifest")
    return cp, hashes


def compile_runtimes(runtimes: dict, output: Path, args):
    if set(runtimes) != set(SOURCES):
        raise ValueError("Runtime manifest must contain exactly release/optimize/current")
    for label, runtime in runtimes.items():
        cp, hashes = validate_runtime(label, runtime, args.sources[label])
        if any(not p.endswith(".jar") or "/target/classes" in p or "/.m2_repo/" in p for p in cp):
            raise ValueError(f"Runtime {label} is not an immutable copied-jar classpath")
        directory = output / "compiled" / label
        directory.mkdir(parents=True)
        source_relative = [Path("scripts/lmdb-compatibility/CompatWorkload.java"),
                           Path(f"scripts/lmdb-compatibility/{label}/CompatAdapter.java")]
        source_paths = [output / "harness-sources" / path for path in source_relative]
        code = command_run([args.javac, "-encoding", "UTF-8", "-cp", os.pathsep.join(cp), "-d", str(directory),
                            *map(str, source_paths)], output / "logs" / f"compile-{label}.log")
        if code:
            raise RuntimeError(f"javac failed for {label}; see {output / 'logs' / f'compile-{label}.log'}")
        lmdb = [p for p in cp if Path(p).name.startswith("rdf4j-sail-lmdb-")]
        if len(lmdb) != 1:
            raise ValueError(f"Expected exactly one LMDB jar for {label}: {lmdb}")
        runtime["lmdb_sha256"] = hashes[lmdb[0]]
        runtime["harness_classes"] = str(directory)
        runtime["runtime_hashes"] = hashes
        runtime["harness_source_hashes"] = {str(relative): sha256(path) for relative, path in zip(source_relative, source_paths)}
    write_json(output / "runtime-provenance.json", runtimes)


def java_command(runtime: dict, args, *options: str) -> list[str]:
    return [args.java, "-ea", "-Xmx768m", "-cp",
            os.pathsep.join([runtime["harness_classes"], *runtime["classpath"]]),
            "org.eclipse.rdf4j.sail.lmdb.CompatWorkload", *options]


def load_expected(snapshot: Path) -> tuple[set[str], dict[str, str]]:
    statements, namespaces = set(), {}
    for line in gzip.decompress(snapshot.read_bytes()).decode("utf-8").splitlines():
        if line.startswith("S\t"):
            statements.add(line[2:])
        elif line.startswith("NS\t"):
            _, prefix, name = line.split("\t")
            namespaces[base64.b64decode(prefix).decode()] = base64.b64decode(name).decode()
        elif line:
            raise ValueError(f"Unknown independent expected-state record: {line}")
    return statements, namespaces


def completion_values(path: Path) -> dict[str, str]:
    if not path.is_file():
        return {}
    return dict(line.split("=", 1) for line in path.read_text().splitlines() if "=" in line and not line.startswith("#"))


def execute(jobs: list[dict], runtimes: dict, output: Path, args) -> int:
    compile_runtimes(runtimes, output, args)
    checker = []
    for label, runtime in runtimes.items():
        directory = output / "checker" / label
        directory.mkdir(parents=True)
        rc = command_run(java_command(runtime, args, "selftest", str(directory), runtime["revision"],
                                      runtime["lmdb_sha256"]), directory / "run.log", timeout=args.jvm_timeout,
                         java_diagnostics=True)
        checker.append({"runtime": label, "exit_code": rc, "result_dir": str(directory)})
    write_json(output / "checker-results.json", checker)
    if any(c["exit_code"] for c in checker):
        raise RuntimeError("Java checker selftest failed; no repository campaign was started")
    if args.checker_only:
        return 0
    results = []
    blocked = {}
    active_expected: dict[str, Path] = {}
    prior_failures: dict[str, list[dict]] = {}
    original_copies = {}
    for job in jobs:
        job = job.copy()
        directory = Path(job["result_dir"])
        directory.mkdir(parents=True)
        runtime = runtimes[job["runtime"]]
        prepared_entry = Path(job["journal"]) / "expected-000-entry.tsv.gz"
        active = active_expected.setdefault(job["repository"], prepared_entry)
        if load_expected(active) != load_expected(prepared_entry) and job["repository"] not in blocked:
            # Conditional recovery starts from the independent snapshot of the
            # last CONFIRMED commit, never from a store export. Original prepared
            # journals and snapshots are immutable and remain intact.
            state, namespaces = load_expected(active)
            recovery = output / "recovery-journals" / job["repository"] / job["stage"] / job["mode"]
            _, mutable = seed_rows(job["seed"], args.statements, args.payload_bytes, job["scenario"])
            recovery_args = args
            if job["scenario"] == "metadata-edge":
                recovery_args = argparse.Namespace(**vars(args))
                recovery_args.rounds, recovery_args.statements, recovery_args.payload_bytes = 0, 16, 32
                _, mutable = seed_rows(job["seed"], 16, 32, job["scenario"])
            make_journal(recovery, state, namespaces, job, recovery_args, False, mutable)
            job["original_prepared_journal"] = job["journal"]
            job["journal"] = str(recovery)
            job["recovery_expected_parent"] = str(active)
        job["prior_failed_stages"] = prior_failures.get(job["repository"], []).copy()
        if job.get("original_store") and job["repository"] not in original_copies:
            original_copies[job["repository"]] = copy_original_store(job, output)
        Path(job["store"]).mkdir(parents=True, exist_ok=True)
        ancestor = blocked.get(job["repository"])
        command = java_command(runtime, args, "probe" if ancestor else "run", job["store"], job["journal"], str(directory),
                                   str(job["inline"]).lower(), str(job["ordered"]).lower(), job["mode"],
                                   job["format"], runtime["revision"], runtime["lmdb_sha256"], job["profile"])
        started = time.time()
        rc = command_run(command, directory / "run.log", timeout=args.jvm_timeout, java_diagnostics=True)
        expected_manifest = json.loads((Path(job["journal"]) / "expected-manifest.json").read_text())
        for filename, fingerprint in expected_manifest.items():
            if sha256(Path(job["journal"]) / filename) != fingerprint:
                raise RuntimeError(f"Independent expected snapshot changed during execution: {filename}")
        if ancestor:
            result = {**job, "status": "blocked-by-ancestor", "ancestor": ancestor, "probe_exit_code": rc,
                      "probe_command": command, "elapsed_seconds": time.time() - started}
        else:
            result = {**job, "exit_code": rc, "elapsed_seconds": time.time() - started,
                      "status": "passed" if rc == 0 else "failed", "command": command}
            completion = completion_values(directory / "completion.properties")
            result["legacy_metadata_completion"] = {
                key.removeprefix("legacy.metadata.completed."): value for key, value in completion.items()
                if key.startswith("legacy.metadata.completed.")}
            confirmed = completion.get("last.committed.snapshot")
            if confirmed:
                confirmed_path = Path(job["journal"]) / confirmed
                if not confirmed_path.is_file():
                    raise RuntimeError(f"Confirmed commit points outside independent snapshots: {confirmed}")
                active_expected[job["repository"]] = confirmed_path
            result["confirmed_expected_state"] = str(active_expected[job["repository"]])
            result["operations_completed"] = completion.get("operations.completed") == "true"
            result["failure_kind"] = None if rc == 0 else "comparison" if result["operations_completed"] else "execution-or-admission"
            if rc:
                prior_failures.setdefault(job["repository"], []).append({"stage": job["stage"], "mode": job["mode"],
                                                                         "result_dir": str(directory)})
                preserved = output / "failure-snapshots" / job["repository"] / job["stage"] / job["mode"]
                preserved.parent.mkdir(parents=True, exist_ok=True)
                shutil.copytree(job["store"], preserved)
                result["failed_stage_physical_snapshot"] = str(preserved)
            if rc and (rc == 124 or not completion or completion.get("commit.uncertain") == "true"):
                blocked[job["repository"]] = {"stage": job["stage"], "mode": job["mode"],
                                               "result_dir": str(directory), "reason": "Committed state uncertain"}
        write_json(directory / "result.json", result)
        results.append(result)
        write_json(output / "results.json", results)
        print(f"{result['status']}: {job['repository']} {job['stage']} {job['mode']}", flush=True)
    for copy in original_copies.values():
        if physical_inventory(Path(copy["original_store"])) != copy["physical_inventory"]:
            raise ValueError(f"Preserved original physical store changed during replay: {copy['original_store']}")
        copy["original_unchanged_after_replay"] = True
    if original_copies:
        write_json(output / "original-v6-copies.json", list(original_copies.values()))
    failed = [r for r in results if r["status"] != "passed"]
    write_json(output / "summary.json", {"passed": len(results) - len(failed), "failed_or_blocked": len(failed),
               "required_primary_results": [r for r in results if r["scenario"] == "full-corpus"],
               "structural_control_results": [r for r in results if r["scenario"] == "structural-control"],
               "metadata_edge_results": [r for r in results if r["scenario"] == "metadata-edge"],
               "writer_corpus_results": [r for r in results if r["scenario"] == "writer-corpus"],
               "success": not failed})
    return int(bool(failed))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", type=Path, required=True, help="Fresh directory; existing artifacts are never overwritten")
    parser.add_argument("--runtimes-json", type=Path)
    parser.add_argument("--prepare-runtimes", action="store_true")
    parser.add_argument("--seeds", default="17011,29023,47017")
    parser.add_argument("--rounds", type=int, default=12)
    parser.add_argument("--statements", type=int, default=3000, help="Mutable rows; numeric/date/edge companion rows are additional")
    parser.add_argument("--payload-bytes", type=int, default=2048, help="Unicode payload repetition count on every eighth row")
    parser.add_argument("--scenarios", default="full-corpus,structural-control")
    parser.add_argument("--optimize-modes", default="normal,native,adjacency,overlay")
    parser.add_argument("--stop-after-stage", choices=("release-create", "optimize-first", "current-downgrade", "optimize-reupgrade"))
    parser.add_argument("--source", action="append", default=[], metavar="LABEL=REVISION")
    parser.add_argument("--plan-only", action="store_true")
    parser.add_argument("--checker-only", action="store_true")
    parser.add_argument("--writer-corpus", action="store_true",
                        help="Bounded supplemental 20-job, six-repository normal-mode fresh typed-writer campaign")
    parser.add_argument("--direct-maintenance", action="store_true",
                        help="Supplemental authentic v2 release-to-maintenance path, followed by optimize reopening")
    parser.add_argument("--original-v6-results", type=Path,
                        help="Replay exact copies of confirmed original optimize-created v6 failure snapshots")
    parser.add_argument("--java", default="java")
    parser.add_argument("--javac", default="javac")
    parser.add_argument("--jvm-timeout", type=float, default=900, help="Per-owned-JVM timeout in seconds; failures retain thread/process/commit evidence")
    args = parser.parse_args()
    if args.writer_corpus:
        workload_flags = ("--seeds", "--rounds", "--statements", "--payload-bytes", "--scenarios", "--optimize-modes",
                          "--stop-after-stage", "--direct-maintenance", "--original-v6-results")
        if args.checker_only or any(argument.split("=", 1)[0] in workload_flags for argument in sys.argv[1:]):
            parser.error("--writer-corpus has a fixed bounded selection; do not combine checker-only or workload selection flags")
        args.seeds, args.scenarios, args.optimize_modes = str(WRITER_SEED), "writer-corpus", "normal"
        args.rounds, args.statements, args.payload_bytes = 1, 32, 32
    if args.direct_maintenance and args.original_v6_results:
        parser.error("Direct-maintenance and original-v6 replay are separate supplemental campaigns")
    if args.checker_only and (args.direct_maintenance or args.original_v6_results):
        parser.error("Checker-only execution does not select repository transitions")
    args.sources = SOURCES.copy()
    for item in args.source:
        label, revision = item.split("=", 1)
        if label not in SOURCES:
            parser.error(f"Unknown runtime label: {label}")
        resolved = subprocess.run(["git", "rev-parse", "--verify", "--end-of-options", revision + "^{commit}"],
                                  cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if resolved.returncode:
            parser.error(f"Cannot resolve source {label}={revision}: {resolved.stderr.strip()}")
        args.sources[label] = resolved.stdout.strip()
    if args.rounds < 1 or args.statements < 1 or args.payload_bytes < 0:
        parser.error("rounds/statements must be positive and payload-bytes nonnegative")
    if args.jvm_timeout <= 0:
        parser.error("jvm-timeout must be positive")
    allowed_scenarios = {"writer-corpus"} if args.writer_corpus else {"full-corpus", "structural-control"}
    if set(args.scenarios.split(",")) - allowed_scenarios:
        parser.error("Scenarios must be full-corpus and/or structural-control")
    if set(args.optimize_modes.split(",")) - {"normal", "native", "adjacency", "overlay"}:
        parser.error("Unknown optimize mode")
    output = args.output_dir.resolve()
    if output.exists():
        parser.error("Output directory already exists. Choose a fresh path to preserve every artifact.")
    output.mkdir(parents=True)
    replay = [sys.executable, str(Path(__file__).resolve()), *sys.argv[1:]]
    replay[replay.index("--output-dir") + 1] = str(output) + "-replay"
    for label, revision in args.sources.items():
        replay += ["--source", label + "=" + revision]
    write_json(output / "invocation.json", {"argv": sys.argv, "sources": args.sources,
               "exact_fresh_rerun": shlex.join(replay), "output": str(output)})
    try:
        retain_harness_sources(output, args.prepare_runtimes)
        if args.checker_only:
            jobs = []
            write_json(output / "campaign-plan.json", jobs)
            write_json(output / "checker-selection.json", {"checker_only": True, "repository_jobs": 0})
            print("Prepared checker-only execution; no repository journals or oracle trees generated.", flush=True)
        else:
            jobs = plan_campaign(output, args)
            print(f"Prepared {len(jobs)} jobs, {len(corpus())} literal cases; independent snapshots are persisted.", flush=True)
        if args.plan_only:
            return 0
        if args.prepare_runtimes:
            spec = importlib.util.spec_from_file_location("package_versions", output / "harness-sources" / "scripts" /
                                                         "lmdb-compatibility" / "package_versions.py")
            module = importlib.util.module_from_spec(spec)
            sys.modules["package_versions"] = module
            spec.loader.exec_module(module)
            runtimes = module.prepare_runtimes(ROOT, output / "runtimes", args.sources)
        elif args.runtimes_json:
            runtimes = json.loads(args.runtimes_json.read_text())
        else:
            parser.error("Use --runtimes-json or --prepare-runtimes (or --plan-only)")
        return execute(jobs, runtimes, output, args)
    except Exception as error:
        write_json(output / "harness-error.json", {"type": type(error).__name__, "message": str(error)})
        print(f"Harness failed: {error}; preserved {output}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
