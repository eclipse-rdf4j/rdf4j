#!/usr/bin/env python3
"""
Parses and summarizes the output of SourceSelectionPerformanceTest#benchmarkSourceSelection.

Usage
-----
Run the parameterized tests and pipe (or redirect) their stdout into this script:

    mvn -pl tools/federation test -Dtest=SourceSelectionPerformanceTest 2>&1 | python3 tools/federation/src/test/scripts/summarize_benchmark.py

Or point it at a saved log file:

    python3 tools/federation/src/test/scripts/summarize_benchmark.py benchmark.log

The script prints a single table with one row per (members, latency, patterns) combination,
showing the average execution time and the total number of endpoint requests across all
measured runs (including the warm-up run).
"""

import re
import sys

# ---------------------------------------------------------------------------
# Patterns
# ---------------------------------------------------------------------------

# "Source selection benchmark (5 members, 2 patterns, 0ms latency): average execution time over 5 runs = 38 ms"
RE_AVG = re.compile(
    r"Source selection benchmark \((\d+) members, (\d+) patterns, (\d+)ms latency\):"
    r" average execution time over (\d+) runs = (\d+) ms"
)

# "Source selection benchmark (5 members, 2 patterns, 0ms latency): total endpoint requests = 60"
RE_REQ = re.compile(
    r"Source selection benchmark \((\d+) members, (\d+) patterns, (\d+)ms latency\):"
    r" total endpoint requests = (\d+)"
)

# ---------------------------------------------------------------------------
# Parse
# ---------------------------------------------------------------------------

def parse(lines):
    """
    Returns:
        averages  : dict  (members, latency, patterns) -> avg_ms
        requests  : dict  (members, latency, patterns) -> total_requests
        n_runs    : int   number of measured runs (excluding warm-up), from output
    """
    averages = {}
    requests = {}
    n_runs   = None

    for line in lines:
        line = line.rstrip()

        m = RE_AVG.search(line)
        if m:
            # groups: (1)=members  (2)=patterns  (3)=latency  (4)=n_runs  (5)=avg_ms
            # store key as (members, latency, patterns) for consistent unpacking
            key      = (int(m.group(1)), int(m.group(3)), int(m.group(2)))
            n_runs   = int(m.group(4))
            averages[key] = int(m.group(5))
            continue

        m = RE_REQ.search(line)
        if m:
            # groups: (1)=members  (2)=patterns  (3)=latency  (4)=total_requests
            key = (int(m.group(1)), int(m.group(3)), int(m.group(2)))
            requests[key] = int(m.group(4))
            continue

    return averages, requests, n_runs

# ---------------------------------------------------------------------------
# Format helpers
# ---------------------------------------------------------------------------

def col_widths(rows, headers):
    widths = [len(h) for h in headers]
    for row in rows:
        for i, cell in enumerate(row):
            widths[i] = max(widths[i], len(str(cell)))
    return widths

def print_table(title, headers, rows, align=None):
    if not rows:
        return
    widths = col_widths(rows, headers)
    align  = align or ["<"] * len(headers)
    sep    = "+-" + "-+-".join("-" * w for w in widths) + "-+"
    fmt    = "| " + " | ".join(f"{{:{a}{w}}}" for a, w in zip(align, widths)) + " |"

    print(f"\n{title}")
    print(sep)
    print(fmt.format(*headers))
    print(sep)
    for row in rows:
        print(fmt.format(*[str(c) for c in row]))
    print(sep)

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    if len(sys.argv) > 1:
        print("Parsing benchmark log file …")
        with open(sys.argv[1], encoding="utf-8") as fh:
            lines = fh.readlines()
    else:
        print("Running benchmark – this may take up to a few minutes …", flush=True)
        lines = sys.stdin.readlines()

    averages, requests, n_runs = parse(lines)

    if not averages:
        print("No benchmark results found in input.", file=sys.stderr)
        sys.exit(1)

    runs_label = f"{n_runs} measured run(s) + 1 warm-up" if n_runs else "? runs + 1 warm-up"

    # --- Table: average execution times + total endpoint requests ---
    headers = ["Members", "Latency (ms)", "Patterns", "Avg time (ms)", "Total endpoint requests"]
    rows = [
        (members, latency, patterns, avg_ms, requests.get((members, latency, patterns), "n/a"))
        for (members, latency, patterns), avg_ms in sorted(averages.items())
    ]
    print_table(
        f"=== Source-selection benchmark results ({runs_label}) ===",
        headers, rows,
        align=[">", ">", ">", ">", ">"],
    )

    # --- Summary: best / worst ---
    print("\n=== Summary ===")
    best  = min(averages.items(), key=lambda kv: kv[1])
    worst = max(averages.items(), key=lambda kv: kv[1])
    print(f"  Fastest: members={best[0][0]}, latency={best[0][1]}ms, "
          f"patterns={best[0][2]}  →  {best[1]} ms avg")
    print(f"  Slowest: members={worst[0][0]}, latency={worst[0][1]}ms, "
          f"patterns={worst[0][2]}  →  {worst[1]} ms avg")

    # latency impact: compare latency=0 vs latency>0 for same (members, patterns)
    zero_lat = {(m, p): v for (m, l, p), v in averages.items() if l == 0}
    nonz_lat = {(m, p): (l, v) for (m, l, p), v in averages.items() if l > 0}
    shared   = sorted(set(zero_lat) & set(nonz_lat))
    if shared:
        print("\n  Latency overhead (latency=0ms vs latency>0ms):")
        print(f"    {'Members':>7}  {'Patterns':>8}  {'0ms (ms)':>10}  "
              f"{'latency>0 (ms)':>14}  {'overhead':>10}")
        for (m, p) in shared:
            lat, val = nonz_lat[(m, p)]
            base     = zero_lat[(m, p)]
            overhead = val - base
            print(f"    {m:>7}  {p:>8}  {base:>10}  {val:>14}  {overhead:>+10}")

if __name__ == "__main__":
    main()
