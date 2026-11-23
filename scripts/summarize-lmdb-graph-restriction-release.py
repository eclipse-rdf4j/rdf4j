#!/usr/bin/env python3
"""Assess the raw results from run-lmdb-graph-restriction-release.sh."""

import argparse
import csv
import json
import math
import statistics
from pathlib import Path


FIXTURES = {
    "native-fusion": ("VARIABLE_GRAPH", "SUBJECT", 10000, 32, 20),
    "no-graph": ("NO_GRAPH", "SUBJECT", 10000, 32, 20),
    "rank": ("SEQUENCE_BARRIER", "SUBJECT", 10000, 32, 5),
    "partial": ("BODY_BINDS_SELECTOR", "PARTIAL", 10000, 32, 5),
    "selective": ("BODY_BINDS_SELECTOR", "SUBJECT", 1000000, 100, 5),
}
PROFILES = ("experimental", "standard")
MODES = ("OFF", "AUTO")
CONTROLS = tuple(fixture for fixture in FIXTURES if fixture != "selective")
LEGACY_99_9_PERCENT_T = 3.7676268043150865  # 24 iteration samples, for comparability with earlier JMH runs.
PAIRED_95_PERCENT_T = 4.302652729696142  # Three independent fork pairs.
GEOMETRIC_MEAN_LIMIT = 1.05
INDIVIDUAL_LIMIT = 1.10


def require(condition, message):
    if not condition:
        raise ValueError(message)


def load_fork(directory, row):
    profile, fixture, pair, mode = (row[key] for key in ("profile", "fixture", "pair", "mode"))
    require(profile in PROFILES and fixture in FIXTURES and pair in ("1", "2", "3") and mode in MODES,
            f"Unexpected run identity {row}")
    filename = f"{profile}-{fixture}-{pair}-{mode}.json"
    require(row["file"] == filename, f"Unexpected result filename {row['file']}")
    result, = json.loads((directory / filename).read_text(encoding="utf-8"))
    require(result["benchmark"].endswith(".GraphRestrictionJoinBenchmark.runQuery"), f"Wrong benchmark in {filename}")
    shape, key_shape, statements, keys, warmup_minimum = FIXTURES[fixture]
    expected = {
        "experimentalOptimizations": "true" if profile == "experimental" else "false",
        "shape": shape, "keyShape": key_shape, "statementCount": str(statements),
        "outerOccurrences": str(keys), "distinctKeys": str(keys), "strategy": mode,
        "graphCount": "1", "indexes": "spoc+posc+ospc", "selectivity": "1",
        "missKind": "DICTIONARY_ABSENT", "telemetry": "false",
    }
    require(result["params"] == expected, f"Fixture parameters differ in {filename}")
    require(result["forks"] == 1 and result["threads"] == 1 and result["mode"] == "avgt"
            and result["warmupTime"] == result["measurementTime"] == "1 s"
            and result["warmupIterations"] == int(row["warmup"]) >= warmup_minimum
            and result["measurementIterations"] == 8, f"Unexpected JMH protocol in {filename}")
    primary = result["primaryMetric"]
    latency, = primary["rawData"]
    allocation = result["secondaryMetrics"]["gc.alloc.rate.norm"]
    allocated, = allocation["rawData"]
    require(primary["scoreUnit"] == "ms/op" and len(latency) == 8
            and all(math.isfinite(value) and value > 0 for value in latency), f"Invalid latency data in {filename}")
    require(allocation["scoreUnit"] == "B/op" and len(allocated) == 8
            and all(math.isfinite(value) and value >= 0 for value in allocated), f"Invalid allocation data in {filename}")
    if row["worker_cpu"]:
        log = (directory / filename).with_suffix(".log").read_text(encoding="utf-8")
        require(f"Benchmark worker pinned to Linux CPU {row['worker_cpu']}" in log.splitlines(),
                f"Missing requested worker affinity confirmation in {filename}")
    return latency, allocated


def read_cohort(directory):
    with (directory / "run-order.tsv").open(encoding="utf-8", newline="") as manifest:
        rows = list(csv.DictReader(manifest, delimiter="\t"))
    require(rows and len(rows) % 6 == 0, "The manifest must contain three complete pairs per selected fixture")
    require({row["file"] for row in rows} == {file.name for file in directory.glob("*.json")},
            "The manifest and raw result files differ")
    affinity = {row["worker_cpu"] for row in rows}
    require(len(affinity) == 1, "Use the same benchmark worker affinity throughout a release cohort")
    cohorts = {profile: {} for profile in PROFILES}
    for start in range(0, len(rows), 6):
        block = rows[start:start + 6]
        profile, fixture = (block[0][key] for key in ("profile", "fixture"))
        require(profile in PROFILES and fixture in FIXTURES, f"Unknown fixture {profile}/{fixture}")
        require(fixture not in cohorts[profile], f"Duplicate fixture {profile}/{fixture}")
        require(all(row["profile"] == profile and row["fixture"] == fixture and row["warmup"] == block[0]["warmup"]
                    for row in block), f"Mixed protocol for {profile}/{fixture}")
        rotation = PROFILES.index(profile) + tuple(FIXTURES).index(fixture)
        modes = {mode: [] for mode in MODES}
        for pair in range(3):
            order = MODES if (pair + rotation) % 2 == 0 else MODES[::-1]
            for row, expected_mode in zip(block[2 * pair:2 * pair + 2], order):
                require(row["mode"] == expected_mode and row["pair"] == str(pair + 1),
                        f"Broken pair order for {profile}/{fixture}")
                modes[expected_mode].append(load_fork(directory, row))
        cohorts[profile][fixture] = modes
    return cohorts, affinity.pop()


def mode_summary(forks):
    latency = [sample for iteration_values, _ in forks for sample in iteration_values]
    allocated = [sample for _, iteration_values in forks for sample in iteration_values]
    means = [statistics.mean(iteration_values) for iteration_values, _ in forks]
    score = statistics.mean(latency)
    margin = LEGACY_99_9_PERCENT_T * statistics.stdev(latency) / math.sqrt(len(latency))
    drift = [statistics.mean(samples[4:]) / statistics.mean(samples[:4]) - 1 for samples, _ in forks]
    trending = sum(value > .10 for value in drift) >= 2 or sum(value < -.10 for value in drift) >= 2
    return score, score - margin, score + margin, statistics.mean(allocated), means, drift, trending


def assess(fixtures):
    controls = []
    historical_regressions = []
    supported = []
    trending = []
    disagreements = []
    historical_selective = False
    paired_selective = False
    print("Fixture       Mode  ms/op     Historical 99.9% CI       allocated B/op   Per-fork late/early movement")
    for fixture, modes in fixtures.items():
        summary = {mode: mode_summary(modes[mode]) for mode in MODES}
        for mode, (score, low, high, allocation, _, drift, unstable) in summary.items():
            movement = " ".join(f"{delta:+.1%}" for delta in drift)
            print(f"{fixture:13} {mode:5} {score:8.3f}    [{low:9.3f}, {high:9.3f}]  {allocation:14.0f}   {movement}")
            if unstable:
                trending.append(f"{fixture}/{mode}")
        off, auto = (summary[mode] for mode in MODES)
        ratios = [math.log(a / b) for a, b in zip(auto[4], off[4])]
        center = statistics.mean(ratios)
        margin = PAIRED_95_PERCENT_T * statistics.stdev(ratios) / math.sqrt(len(ratios))
        ratio = auto[0] / off[0]
        paired_lower, paired_upper = math.exp(center - margin), math.exp(center + margin)
        historical_lower = auto[1] / off[2]
        print(f"  AUTO/OFF {ratio:.4f}  paired {math.exp(center):.4f} "
              f"[{paired_lower:.4f}, {paired_upper:.4f}] (95% CI)  "
              f"historical interval-ratio lower bound {historical_lower:.4f}")
        if fixture != "selective":
            controls.append(ratio)
            historical_regression = historical_lower > INDIVIDUAL_LIMIT
            paired_regression = paired_lower > INDIVIDUAL_LIMIT
            if historical_regression:
                historical_regressions.append(fixture)
            if paired_regression:
                supported.append(fixture)
            if historical_regression != paired_regression:
                disagreements.append(fixture)
        else:
            historical_selective = auto[2] < off[1]
            paired_selective = paired_upper < 1
            if historical_selective != paired_selective:
                disagreements.append(fixture)
            print("  Selective latency gate " + ("passed" if historical_selective else "failed")
                  + " under the historical calculation and " + ("passed" if paired_selective else "failed")
                  + " across independent pairs")
    if len(controls) == len(CONTROLS):
        geometric_mean = math.exp(statistics.mean(math.log(ratio) for ratio in controls))
        result = "passed" if geometric_mean <= GEOMETRIC_MEAN_LIMIT else "failed"
        print(f"Four-control geometric mean {geometric_mean:.4f} {result} the {GEOMETRIC_MEAN_LIMIT:.2f} limit")
        if "selective" in fixtures:
            historical_pass = geometric_mean <= GEOMETRIC_MEAN_LIMIT and not historical_regressions and historical_selective
            paired_pass = geometric_mean <= GEOMETRIC_MEAN_LIMIT and not supported and paired_selective
            print("Historical numeric latency gates " + ("passed" if historical_pass else "failed"))
            print("Independent-pair numeric latency gates " + ("passed" if paired_pass else "failed"))
    else:
        print("Partial control matrix. The release geometric mean requires all four controls.")
    print("Controls above the historical individual threshold ", ", ".join(historical_regressions) or "none", sep="")
    print("Controls with independently supported regressions above 10% ", ", ".join(supported) or "none", sep="")
    if disagreements:
        print("Historical and independent checks disagree for " + ", ".join(disagreements) + ". Review both.")
    if trending:
        print("Measurement trends in " + ", ".join(trending) + ". Extend warmup symmetrically and retain both cohorts.")
    else:
        print("No consistent measurement drift above 10% across a majority of one mode's forks.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("results_directory", type=Path)
    args = parser.parse_args()
    try:
        cohorts, affinity = read_cohort(args.results_directory)
    except (KeyError, OSError, ValueError) as error:
        parser.exit(2, f"Invalid release results: {error}\n")
    if any(set(fixtures) != set(FIXTURES) for fixtures in cohorts.values()):
        print("Partial release matrix")
    if affinity:
        print(f"Benchmark worker affinity verified on Linux logical CPU {affinity}")
    for profile, fixtures in cohorts.items():
        if fixtures:
            print(f"\n{profile}")
            assess(fixtures)


if __name__ == "__main__":
    main()
