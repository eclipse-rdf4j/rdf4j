#!/usr/bin/env python3
"""Summarise a strategy-regret TSV from LmdbNativeStrategyRegretHarnessTest.

Prints, per decision (query, store configuration, decision point): every measured strategy with its median, the
selector's choice, the measured fastest and the regret ratio; then aggregates: how often each strategy is fastest,
how often the selector's expected-price ranking agrees with the measured ranking, and the decisions whose regret
exceeds a threshold (default 1.5x), which are the assumptions to revisit.

Usage: lmdb-regret-report.py core/sail/lmdb/target/strategy-regret-3000.tsv [--threshold 1.5]
"""
import sys
from collections import defaultdict


def load(path):
    rows = []
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            line = line.rstrip("\n")
            if not line or line.startswith("#"):
                continue
            c = line.split("\t")
            rows.append({
                "query": c[0], "indexes": c[1], "adjacency": c[2], "point": c[3], "strategy": c[4],
                "chosen": c[5] == "chosen",
                "median": None if c[6] == "NaN" else float(c[6]),
                "rows": int(c[8]),
                "expected": None if c[9] == "NaN" else float(c[9]),
                "note": c[12] if len(c) > 12 else "",
            })
    return rows


def main(argv):
    path = argv[0]
    threshold = float(argv[argv.index("--threshold") + 1]) if "--threshold" in argv else 1.5
    rows = load(path)
    decisions = defaultdict(list)
    for row in rows:
        decisions[(row["query"], row["indexes"], row["adjacency"], row["point"])].append(row)
    fastest_count = defaultdict(int)
    chosen_count = defaultdict(int)
    ratios = defaultdict(list)
    agree, total, bad = 0, 0, []
    rank_pairs_ok, rank_pairs = 0, 0
    for key, entries in sorted(decisions.items()):
        measured = [e for e in entries if e["median"] is not None]
        if not measured:
            continue
        fastest = min(measured, key=lambda e: e["median"])
        chosen = next((e for e in measured if e["chosen"]), None)
        fastest_count[fastest["strategy"]] += 1
        print(f"\n{key[0]} [{key[1]} {key[2]}] {key[3]}")
        for e in sorted(measured, key=lambda e: e["median"]):
            flag = "<= chosen" if e["chosen"] else ""
            exp = "" if e["expected"] is None else f"quoted {e['expected'] / 1e6:9.3f} ms"
            print(f"    {e['strategy']:32s} {e['median'] / 1e6:9.3f} ms  rows={e['rows']:<7d} {exp} {flag}")
        for e in entries:
            if e["median"] is None:
                print(f"    {e['strategy']:32s}   declined  {e['note']}")
            elif e["expected"] is not None and e["expected"] > 0:
                ratios[e["strategy"]].append(e["median"] / e["expected"])
        # pairwise agreement between quoted expected price and measured median
        priced = [e for e in measured if e["expected"] is not None]
        for i in range(len(priced)):
            for j in range(i + 1, len(priced)):
                a, b = priced[i], priced[j]
                if abs(a["median"] - b["median"]) / max(a["median"], b["median"]) < 0.1:
                    continue  # too close to call
                rank_pairs += 1
                if (a["expected"] < b["expected"]) == (a["median"] < b["median"]):
                    rank_pairs_ok += 1
        if chosen is None:
            continue
        total += 1
        chosen_count[chosen["strategy"]] += 1
        regret = chosen["median"] / fastest["median"]
        if regret <= 1.05:
            agree += 1
        if regret > threshold:
            bad.append((regret, key, chosen["strategy"], fastest["strategy"]))
    print("\n=== fastest strategy frequency ===")
    for strategy, count in sorted(fastest_count.items(), key=lambda kv: -kv[1]):
        print(f"  {strategy:32s} fastest {count:3d}x   chosen {chosen_count.get(strategy, 0):3d}x")
    print(f"\ndecisions={total} chosenWithin5pctOfFastest={agree} "
          f"quotedOrderAgreesWithMeasured={rank_pairs_ok}/{rank_pairs} pairs")
    print(f"\n=== regret above {threshold}x ===")
    for regret, key, chosen, fastest in sorted(bad, reverse=True):
        print(f"  {regret:6.2f}x  {key[0]} [{key[1]} {key[2]}] {key[3]}: chose {chosen}, fastest {fastest}")
    ratio_table(ratios)
    return 0



def ratio_table(entries_by_strategy):
    """Per strategy: the median, lower and upper quartile of measured over quoted time (1.0 is an exact quote)."""
    print("\n=== measured / quoted per strategy (median, p25, p75, n) ===")
    rows = []
    for strategy, ratios in entries_by_strategy.items():
        ratios = sorted(ratios)
        if not ratios:
            continue
        rows.append((ratios[len(ratios) // 2], ratios[len(ratios) // 4], ratios[3 * len(ratios) // 4],
                     len(ratios), strategy))
    for median, p25, p75, n, strategy in sorted(rows, reverse=True):
        print(f"    {strategy:34s} median={median:6.2f} p25={p25:6.2f} p75={p75:6.2f} n={n}")


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
