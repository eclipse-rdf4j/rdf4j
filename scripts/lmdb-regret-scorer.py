#!/usr/bin/env python3
"""Measure dispatch regret for the LMDB native engine.

Regret answers one question: how much slower is the strategy the engine actually picks than the
best strategy available to it?

    regret(q) = time(default dispatch) / min over arms of time(arm)

Each "arm" disables one strategy family with an existing kill switch, so the query falls to
whatever the ladder would have chosen next. An arm that beats the default is direct evidence that
dispatch made the wrong call for that query, and the arm name says which strategy was at fault.

Two measurement rules are enforced rather than left to the reader:

  * every arm of a query runs with identical JMH settings, so scores are comparable;
  * an arm is only reported as a real difference when its confidence interval is DISJOINT from the
    default's. Overlapping intervals are recorded but flagged "no measurable effect" -- with JMH at
    n=3 the 99.9% interval is wide, and treating an overlap as a result is how you end up chasing
    noise. Overlapping arms still take part in the minimum, where they are harmless.

Results are cached per (query, arm) so a long sweep can be interrupted and resumed.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import platform
import re
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
RUNNER = REPO / "scripts" / "run-single-benchmark.sh"

# Each arm disables one strategy family. The query then runs on whatever the ladder picks instead,
# which is the counterfactual we want: "what if this strategy had not been chosen?"
ARMS: dict[str, list[str]] = {
    "default": [],
    "no-kernel": ["-Drdf4j.lmdb.janinoCodegen.enabled=false"],
    "no-wcoj": ["-Drdf4j.lmdb.wcoj.enabled=false"],
    "no-batch": ["-Drdf4j.lmdb.nativeBatch.enabled=false"],
    "no-parallel": ["-Drdf4j.lmdb.parallel.enabled=false"],
    "no-factorized": [
        "-Drdf4j.lmdb.factorizedRows.enabled=false",
        "-Drdf4j.lmdb.factorizedSink.enabled=false",
        "-Drdf4j.lmdb.factorizedTail.enabled=false",
    ],
    "no-adaptive": ["-Drdf4j.lmdb.adaptiveFilterPlacement.enabled=false"],
    "no-prefixrun": ["-Drdf4j.lmdb.prefixRun.enabled=false"],
    # Not a strategy family: disables the learned cost model, so default-vs-this measures directly
    # whether runtime calibration improves wall-clock on this query.
    "no-calibration": ["-Drdf4j.lmdb.costCalibration.enabled=false"],
}

SCORE = re.compile(r"avgt\s+\d+\s+([0-9.]+)\s*±\s*([0-9.]+)")


def run_arm(query: str, arm: str, warmup: int, measure: int, seconds: int,
            build: bool) -> tuple[float, float] | None:
    """Runs one (query, arm) and returns (score_ms, half_width_ms), or None when it did not run."""
    cmd = [
        str(RUNNER),
        "--module", "core/sail/lmdb",
        "--class", "org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark",
        "--method", "executeQuery",
        "--theme-query", query,
        "--warmup-iterations", str(warmup),
        "--measurement-iterations", str(measure),
    ]
    if not build:
        cmd.append("--no-build")
    for flag in ARMS[arm]:
        cmd += ["--jvm-arg", flag]
    cmd += ["--", "-w", str(seconds), "-r", str(seconds)]

    completed = subprocess.run(cmd, cwd=REPO, capture_output=True, text=True)
    if completed.returncode != 0:
        # a partial score printed before a failure must never be recorded as a measurement
        stderr_tail = "\n".join(completed.stderr.splitlines()[-15:])
        sys.stderr.write(f"  !! {query} [{arm}] exited with {completed.returncode}\n")
        if stderr_tail:
            sys.stderr.write(f"     stderr tail:\n{stderr_tail}\n")
        return None
    matches = SCORE.findall(completed.stdout)
    if not matches:
        stderr_tail = "\n".join(completed.stderr.splitlines()[-15:])
        sys.stderr.write(f"  !! {query} [{arm}] produced no score\n")
        if stderr_tail:
            sys.stderr.write(f"     stderr tail:\n{stderr_tail}\n")
        return None
    score, error = matches[-1]
    return float(score), float(error)


def measurement_fingerprint(warmup: int, measure: int, seconds: int) -> dict:
    """Complete measurement configuration + build identity. Cached results are only comparable
    (and only reusable) when every field matches: JMH settings, the arm flag table, the JVM, and
    the exact source state of the benchmarked module (HEAD revision plus a digest of uncommitted
    changes)."""

    def git(*argv: str) -> str:
        try:
            return subprocess.run(["git", *argv], cwd=REPO, capture_output=True, text=True,
                                  check=False).stdout.strip()
        except OSError:
            return "unavailable"

    diff = git("diff", "HEAD", "--", "core/sail/lmdb")
    return {
        "warmup": warmup,
        "measure": measure,
        "seconds": seconds,
        "arms": ARMS,
        "java": platform.java_ver()[0] or subprocess.run(
            ["java", "-version"], capture_output=True, text=True, check=False).stderr.splitlines()[:1],
        "head": git("rev-parse", "HEAD"),
        "lmdb_diff_sha256": hashlib.sha256(diff.encode()).hexdigest(),
    }


def disjoint(a: tuple[float, float], b: tuple[float, float]) -> bool:
    """True when two JMH results do not overlap, i.e. the difference is resolvable."""
    return a[0] + a[1] < b[0] - b[1] or b[0] + b[1] < a[0] - a[1]


def load_average() -> float:
    """One-minute load average, or NaN where unavailable."""
    try:
        return __import__("os").getloadavg()[0]
    except (OSError, AttributeError):
        return float("nan")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--queries", required=True,
                        help="file of THEME:INDEX targets, one per line ('#' comments allowed)")
    parser.add_argument("--cache", default="target/regret-cache.json")
    parser.add_argument("--report", default="target/regret-report.txt")
    parser.add_argument("--arms", default=",".join(ARMS), help="comma-separated arm names")
    parser.add_argument("--warmup", type=int, default=3)
    parser.add_argument("--measure", type=int, default=3)
    parser.add_argument("--seconds", type=int, default=2)
    parser.add_argument("--build", action="store_true", help="package before the first run")
    args = parser.parse_args()

    arms = [a.strip() for a in args.arms.split(",") if a.strip()]
    unknown = [a for a in arms if a not in ARMS]
    if unknown:
        parser.error(f"unknown arms: {unknown}")
    if "default" not in arms:
        parser.error("the 'default' arm is required -- it is the numerator of regret")

    queries = []
    for line in Path(args.queries).read_text().splitlines():
        line = line.split("#", 1)[0].strip()
        if line:
            queries.append(line)

    cache_path = REPO / args.cache
    cache_path.parent.mkdir(parents=True, exist_ok=True)
    fingerprint = measurement_fingerprint(args.warmup, args.measure, args.seconds)
    cache: dict = {}
    if cache_path.exists():
        stored = json.loads(cache_path.read_text())
        if isinstance(stored, dict) and stored.get("fingerprint") == fingerprint:
            cache = stored.get("results", {})
        else:
            # a cache produced under different JMH settings, JVM, or source state must never be
            # mixed into this sweep's comparisons; keep it aside for inspection instead
            stale = cache_path.with_suffix(cache_path.suffix + ".stale")
            cache_path.rename(stale)
            print(f"  cache fingerprint changed; previous results moved to {stale}", flush=True)

    def persist() -> None:
        cache_path.write_text(
                json.dumps({"fingerprint": fingerprint, "results": cache}, indent=1, sort_keys=True))

    build_pending = args.build
    for query in queries:
        for arm in arms:
            key = f"{query}|{arm}"
            if key in cache:
                continue
            print(f"  running {query} [{arm}] ...", flush=True)
            result = run_arm(query, arm, args.warmup, args.measure, args.seconds, build_pending)
            if result is not None:
                # only a confirmed successful build-and-run clears the pending build; only
                # successful measurements are cached
                build_pending = False
                cache[key] = {"score": result[0], "error": result[1]}
                persist()

    lines = ["=== LMDB dispatch regret ===",
             f"arms: {', '.join(arms)}",
             f"JMH: {args.warmup} warmup / {args.measure} measurement iterations, {args.seconds}s each",
             f"load average at report time: {load_average():.2f}"]
    lines.append("")
    regrets = []
    measured = 0
    for query in queries:
        base = cache.get(f"{query}|default")
        if base is None:
            lines.append(f"{query:<34} SKIPPED (no default measurement)")
            continue
        measured += 1
        base_pair = (base["score"], base["error"])
        best_arm, best_pair = "default", base_pair
        detail = []
        for arm in arms:
            if arm == "default":
                continue
            entry = cache.get(f"{query}|{arm}")
            if entry is None:
                continue
            pair = (entry["score"], entry["error"])
            if pair[0] < best_pair[0]:
                best_arm, best_pair = arm, pair
            if disjoint(pair, base_pair):
                direction = "faster" if pair[0] < base_pair[0] else "slower"
                detail.append(f"{arm} {pair[0]:.3f}ms {direction}")
        regret = base_pair[0] / best_pair[0] if best_pair[0] > 0 else float("nan")
        # Only a disjoint improvement counts as real regret; otherwise the default is fine.
        real = best_arm != "default" and disjoint(best_pair, base_pair)
        if real:
            regrets.append(regret)
        flag = f"REGRET {regret:.2f}x (best: {best_arm})" if real else "ok"
        lines.append(f"{query:<34} default {base_pair[0]:9.3f}ms  {flag}")
        if detail:
            lines.append(f"{'':<34}   resolvable: {'; '.join(detail)}")

    lines.append("")
    if measured == 0:
        # A sweep that measured nothing must never read as a sweep that found nothing wrong. The most
        # common cause is another JMH instance holding the global lock, which makes every run exit
        # silently -- "no effect" and "no regression" are indistinguishable unless this is checked.
        lines.append(f"INCONCLUSIVE: no query produced a measurement ({len(queries)} attempted).")
        lines.append("Nothing was measured, so nothing can be concluded. Check that no other JMH")
        lines.append("instance holds /tmp/jmh.lock, then re-run.")
        report = "\n".join(lines)
        print(report)
        (REPO / args.report).write_text(report + "\n")
        return 2

    if measured < len(queries):
        lines.append(f"WARNING: only {measured}/{len(queries)} queries were measured; the rest are "
                     f"excluded from the figures below.")
    if regrets:
        geo = math.exp(sum(math.log(r) for r in regrets) / len(regrets))
        lines.append(f"queries with resolvable regret: {len(regrets)}/{measured}")
        lines.append(f"max regret:            {max(regrets):.3f}x   (bar: <= 1.30)")
        lines.append(f"geometric mean regret: {geo:.3f}x   (bar: <= 1.05)")
    else:
        lines.append(f"no resolvable regret across {measured} measured queries -- dispatch picked "
                     f"the best measurable strategy everywhere")

    report = "\n".join(lines)
    print(report)
    (REPO / args.report).write_text(report + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
