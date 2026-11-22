#!/usr/bin/env python3
"""Run the DCAT wildcard query matrix against an existing RDF4J server."""

import argparse
from collections import Counter
import csv
from datetime import datetime, timezone
from fnmatch import fnmatchcase
from hashlib import sha256
import json
from pathlib import Path
import subprocess
import sys
from urllib.parse import quote

CORPUS = (Path(__file__).resolve().parents[1] /
          "core/sail/lmdb/src/test/resources/wildcard-query-matrix")
DEFAULT_STRATEGIES = ("auto", "nestedLoop", "wildcardPredicateBatch", "irKernel")


def request(url, params, accept, timeout):
    command = [
        "curl", "--silent", "--show-error", "--fail-with-body", "--get",
        "--connect-timeout", "2", "--max-time", str(timeout + 3),
        "--max-filesize", str(32 * 1024 * 1024),
        "--header", f"Accept: {accept}",
        "--write-out", r"\n%{time_starttransfer}\t%{time_total}",
    ]
    for key, value in params.items():
        command += ["--data-urlencode", f"{key}={value}"]
    response = subprocess.run([*command, url], capture_output=True, check=False)
    body, _, timings = response.stdout.rpartition(b"\n")
    if response.returncode:
        detail = body if response.returncode == 22 and body else response.stderr
        return None, {
            "status": "request-error",
            "exitCode": response.returncode,
            "error": detail.decode(errors="replace")[:300],
        }
    first_byte, total = map(float, timings.split(b"\t"))
    return body, {"status": "ok", "firstByteMs": first_byte * 1000, "totalMs": total * 1000}


def rows(endpoint, params, timeout):
    body, metrics = request(endpoint, {**params, "timeout": str(timeout)},
                            "text/tab-separated-values", timeout)
    if body is None:
        return metrics, None
    header, _, data = body.partition(b"\n")
    if not header.startswith(b"?"):
        return {"status": "invalid-response", "error": header.decode(errors="replace")[:300]}, None
    lines = data.splitlines()
    bag = Counter(lines)
    digest = sha256()
    for line, count in sorted(bag.items()):
        digest.update(sha256(line).digest())
        digest.update(count.to_bytes(8, "big"))
    return {**metrics, "rows": len(lines), "bagSha256": digest.hexdigest()}, bag


def telemetry(endpoint, params, timeout):
    body, metrics = request(endpoint, {
        **params, "action": "explain", "query-language": "SPARQL",
        "query-timeout": str(timeout), "explain": "Telemetry", "explain-format": "json",
    }, "application/json", timeout)
    if body is None:
        return metrics
    try:
        response = json.loads(body)
        content = response["content"]
        plan = json.loads(content) if isinstance(content, str) else content
        if response.get("timedOut"):
            return {**metrics, "status": "query-timeout"}
        invocations = [invocation for physical in plan.get("physicalPlans", [])
                       for invocation in physical.get("invocations", [])]
        if not invocations:
            invocations = [stage for stage in plan.get("executionProfile", {}).get("stages", [])
                           if stage.get("operator") == "Native invocation" and stage.get("engine") == "lmdb-native"]
        actual = plan.get("longMetricsActual", {})
        attributes = plan.get("stringMetricsActual", {})
        routes = [invocation["strategy"] for invocation in invocations]
        if not routes and attributes.get("nativeExecutionPath"):
            routes.append(attributes["nativeExecutionPath"])
        return {
            **metrics,
            "routes": routes,
            "invocationStatuses": [invocation.get("status") for invocation in invocations],
            "rows": actual.get("outputRowsActual"),
            "firstRowNanos": actual.get("firstRowTimeNanosActual"),
            "lastRowNanos": actual.get("lastRowTimeNanosActual"),
            "decision": attributes.get("nativeDecisionOutcome"),
        }
    except (KeyError, TypeError, ValueError):
        return {**metrics, "status": "invalid-response", "error": body[:300].decode(errors="replace")}


def emit(**record):
    print(json.dumps(record), flush=True)


def read_baseline(path):
    baseline = {}
    with Path(path).open() as source:
        for line in source:
            row = json.loads(line)
            if row["phase"] != "rows" or row["status"] != "ok":
                continue
            previous = baseline.get(row["query"])
            if previous is None or (previous["strategy"] != "nestedLoop" and row["strategy"] == "nestedLoop"):
                baseline[row["query"]] = row
    return baseline


def run_case(case, args, row_endpoint, explain_endpoint, references, needed_references, baseline):
    name = case["query"]
    comparison = case["comparison"]
    params = {"query": (CORPUS / f"{name}.rq").read_text()}
    results = []
    preferred = None
    reference_bag = None
    for repeat in range(args.warmup + args.runs):
        offset = repeat % len(args.strategy)
        strategies = args.strategy[offset:] + args.strategy[:offset]
        for strategy in strategies:
            forced = {"lmdb-forced-strategy": strategy} if strategy != "auto" else {}
            metrics, bag = rows(row_endpoint, {**params, **forced}, args.timeout)
            warmup = repeat < args.warmup
            if bag is not None:
                rank = (warmup, strategy != "nestedLoop")
                if comparison == "bag" and (preferred is None or rank < preferred[0]):
                    preferred = (rank, metrics["bagSha256"])
                    if name in needed_references:
                        reference_bag = bag
                elif comparison in references:
                    metrics["withinReferenceBag"] = bag <= references[comparison]
            results.append((strategy, repeat - args.warmup + 1, warmup, metrics))
    if reference_bag is not None:
        references[name] = reference_bag
    for strategy, repeat, warmup, metrics in results:
        if metrics["status"] == "ok":
            if comparison == "bag":
                metrics["matchesBag"] = metrics["bagSha256"] == preferred[1]
            if baseline is not None:
                metrics["matchesBaselineRows"] = metrics["rows"] == baseline["rows"]
                if comparison == "bag":
                    metrics["matchesBaselineBag"] = metrics["bagSha256"] == baseline["bagSha256"]
        emit(query=name, strategy=strategy, phase="warmup" if warmup else "rows",
             repeat=repeat, **metrics)
    for strategy in args.telemetry:
        forced = {"lmdb-forced-strategy": strategy} if strategy != "auto" else {}
        emit(query=name, strategy=strategy, phase="telemetry",
             **telemetry(explain_endpoint, {**params, **forced}, args.timeout))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server", default="http://localhost:8080")
    parser.add_argument("--repository", default="dcat-lmdb")
    parser.add_argument("--query", action="append", help="Case name or glob (repeatable)")
    parser.add_argument("--runs", type=int, default=3, help="Measured requests per strategy per case")
    parser.add_argument("--warmup", type=int, default=1, help="Warmup requests per strategy per case")
    parser.add_argument("--timeout", type=int, default=4, help="Server execution timeout in seconds")
    parser.add_argument("--strategy", nargs="+", default=DEFAULT_STRATEGIES)
    parser.add_argument("--telemetry", nargs="+", default=(), help="Strategies to explain separately")
    parser.add_argument("--reference", help="Previous JSON Lines capture for cross-run checks")
    args = parser.parse_args()
    if args.runs < 1 or args.warmup < 0 or args.timeout < 1:
        parser.error("Use positive runs and timeout and a nonnegative warmup")

    with (CORPUS / "manifest.tsv").open(newline="") as source:
        cases = list(csv.DictReader(source, delimiter="\t"))
    if args.query:
        cases = [case for case in cases
                 if any(fnmatchcase(case["query"], pattern) for pattern in args.query)]
    if not cases:
        parser.error("No query cases selected")
    server = args.server.rstrip("/")
    repo = quote(args.repository, safe="")
    row_endpoint = f"{server}/rdf4j-server/repositories/{repo}"
    explain_endpoint = f"{server}/rdf4j-workbench/repositories/{repo}/query"
    baseline = read_baseline(args.reference) if args.reference else {}
    query_digests = {case["query"]: sha256((CORPUS / (case["query"] + ".rq")).read_bytes()).hexdigest()
                     for case in cases}
    emit(phase="configuration", started=datetime.now(timezone.utc).isoformat(),
         endpoint=row_endpoint, **vars(args), cases=len(cases), queryDigests=query_digests)
    references = {}
    needed_references = {case["comparison"] for case in cases}
    for case in cases:
        run_case(case, args, row_endpoint, explain_endpoint, references, needed_references,
                 baseline.get(case["query"]))
    return 0


if __name__ == "__main__":
    sys.exit(main())
