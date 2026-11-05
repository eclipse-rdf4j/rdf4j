#!/usr/bin/env python3
"""Fit the LMDB native selector's static prior from regret-harness measurements.

Input: the TSV written by LmdbNativeStrategyRegretHarnessTest (one row per forced strategy run with its measured
median nanoseconds and the priced expected feature vector). Output: non-negative least-squares coefficients
(nanoseconds per feature), the fit quality, and a comparison with the current hand-set prior, so that every
coefficient the selector relies on is a measured number.

Usage: lmdb-fit-prior.py core/sail/lmdb/target/strategy-regret.tsv [more.tsv ...] [--pin FEATURE=ns,...]
       [--strategies tag,tag] [--exclude tag,tag]

--pin holds the named coefficients at the given nanoseconds (their contribution is subtracted from every target
before the fit), so the remaining coefficients are fitted around prices that are already measured elsewhere.
--strategies / --exclude restrict the rows to (or drop) the named strategy families.
"""
import math
import sys

# the static prior as LmdbNativeStaticPrior.defaultCoefficients sets it (2026-11-03); keep in step with that method
HAND_PRIOR = {
    "OPEN": 5000.0, "WORKER_START": 500000.0, "INDEX_SEEK": 200.0, "BRANCH_OPEN": 100.0, "SCANNED_ROW": 1.0,
    "DECODED_ROW": 2.0, "INTERPRETED_ROW": 120.0, "BATCH_ROW": 150.0, "GENERATED_ROW": 110.0, "MEMBERSHIP_TEST": 14.0,
    "HASH_BUILD_ROW": 45.0, "HASH_PROBE": 40.0, "GROUP_UPDATE": 150.0, "DISTINCT_UPDATE": 35.0, "SORT_N_LOG2_N": 22.0,
    "OUTPUT_MATERIALIZATION": 250.0, "GENERATED_SOURCE_BYTE_CACHE_MISS": 1.5,
}


def load(paths):
    names, rows = None, []
    for path in paths:
        file_names = None
        with open(path, encoding="utf-8") as handle:
            for line_number, line in enumerate(handle, 1):
                line = line.rstrip("\n")
                if not line:
                    continue
                if line.startswith("#"):
                    header = line.split("\t")[-1]
                    if not header.startswith("features("):
                        continue
                    if not header.endswith(")"):
                        raise ValueError(f"Feature header is malformed at {path}:{line_number}")
                    file_names = header[len("features("):-1].split(",")
                    if (any(not name or name != name.strip() for name in file_names)
                            or len(set(file_names)) != len(file_names)):
                        raise ValueError(f"Feature names must be nonempty and unique at {path}:{line_number}")
                    if names is None:
                        names = file_names
                    elif file_names != names:
                        raise ValueError(f"Feature schema mismatch at {path}:{line_number}: "
                                         f"expected {names}, found {file_names}")
                    continue
                if file_names is None:
                    raise ValueError(f"Feature header is required before measurements at {path}:{line_number}")
                cells = line.split("\t")
                median, features = cells[6], cells[13] if len(cells) > 13 else ""
                if not features:
                    continue
                elapsed = float(median)
                if not math.isfinite(elapsed):
                    continue  # Censored/failed runs are not exact targets for least-squares fitting.
                if elapsed < 0.0:
                    raise ValueError(f"Elapsed nanoseconds must be nonnegative at {path}:{line_number}")
                if not math.isfinite(float(cells[11])):
                    continue  # The quote ceiling, not its finite expected floor, establishes boundedness.
                vector = [float(value) for value in features.split(",")]
                if len(vector) != len(names):
                    raise ValueError(f"Feature count mismatch at {path}:{line_number}: "
                                     f"expected {len(names)}, found {len(vector)}")
                if any(not math.isfinite(value) or value < 0.0 for value in vector):
                    raise ValueError(f"Feature counts must be finite and nonnegative at {path}:{line_number}")
                rows.append((vector, elapsed, cells))
    return names, rows


def nnls(rows, dimension, iterations=20000):
    """Projected gradient descent on sum((x.c - t)^2) with c >= 0, columns scaled by their maxima."""
    if dimension == 0:
        return []  # Every coefficient may be pinned.
    scale = [max(1e-9, max(abs(vector[i]) for vector, _, _ in rows)) for i in range(dimension)]
    scaled = [([value / scale[i] for i, value in enumerate(vector)], target) for vector, target, _ in rows]
    coefficients = [0.0] * dimension
    # The squared-error gradient has Lipschitz bound 2 * rows * columns after scaling.
    # Use its reciprocal: twice this step oscillates forever for identical constant columns.
    step = 0.5 / (len(scaled) * dimension)
    for _ in range(iterations):
        gradient = [0.0] * dimension
        for vector, target in scaled:
            residual = sum(c * v for c, v in zip(coefficients, vector)) - target
            for i, value in enumerate(vector):
                gradient[i] += 2.0 * residual * value
        coefficients = [max(0.0, c - step * g) for c, g in zip(coefficients, gradient)]
    return [c / s for c, s in zip(coefficients, scale)]


def r_squared(rows, coefficients):
    targets = [target for _, target, _ in rows]
    mean = sum(targets) / len(targets)
    total = sum((t - mean) ** 2 for t in targets) or 1.0
    residual = sum((sum(c * v for c, v in zip(coefficients, vector)) - target) ** 2 for vector, target, _ in rows)
    return 1.0 - residual / total


def base_id(tag):
    paren = tag.find("(")
    return tag[:paren] if paren > 0 else tag


def parse(argv):
    paths, pins, keep, drop = [], {}, None, set()
    i = 0
    while i < len(argv):
        if argv[i] == "--pin":
            for item in argv[i + 1].split(","):
                name, value = item.split("=")
                coefficient = float(value)
                if not name.strip() or not math.isfinite(coefficient) or coefficient < 0.0:
                    raise ValueError(f"Pinned coefficient must be named, finite and nonnegative: {item}")
                pins[name.strip()] = coefficient
            i += 2
        elif argv[i] == "--strategies":
            keep = set(argv[i + 1].split(","))
            i += 2
        elif argv[i] == "--exclude":
            drop = set(argv[i + 1].split(","))
            i += 2
        else:
            paths.append(argv[i])
            i += 1
    return paths, pins, keep, drop


def main(argv):
    paths, pins, keep, drop = parse(argv)
    names, rows = load(paths)
    rows = [row for row in rows if (keep is None or base_id(row[2][4]) in keep) and base_id(row[2][4]) not in drop]
    if not rows:
        print("no priced measurements found", file=sys.stderr)
        return 1
    unknown_pins = set(pins) - set(names)
    if unknown_pins:
        raise ValueError(f"Pinned features are absent from the measurements: {sorted(unknown_pins)}")
    hand = [HAND_PRIOR.get(name, 0.0) for name in names]
    pinned = [pins.get(name) for name in names]
    free = [i for i, value in enumerate(pinned) if value is None]
    # the pinned contribution leaves the fit; the free coefficients explain what remains
    reduced = []
    for vector, target, cells in rows:
        held = sum(vector[i] * pinned[i] for i in range(len(names)) if pinned[i] is not None)
        # Negative residual targets are valid: an overprediction by the pinned features must
        # still oppose increasing the free coefficients in the original least-squares objective.
        reduced.append(([vector[i] for i in free], target - held, cells))
    fitted_free = nnls(reduced, len(free))
    fitted = list(pinned)
    for index, value in zip(free, fitted_free):
        fitted[index] = value
    print(f"measurements: {len(rows)}" + (f"   pinned: {pins}" if pins else ""))
    print(f"R^2 hand prior: {r_squared(rows, hand):.3f}   R^2 fitted (shared slopes): {r_squared(rows, fitted):.3f}")
    print(f"{'feature':32s} {'hand ns':>12s} {'fitted ns':>12s} {'observed':>10s}")
    for i, name in enumerate(names):
        support = sum(1 for vector, _, _ in rows if vector[i] > 0)
        mark = "  (pinned)" if pinned[i] is not None else ""
        print(f"{name:32s} {hand[i]:12.1f} {fitted[i]:12.1f} {support:10d}{mark}")
    rows = reduced if pins else rows
    names_fit = [names[i] for i in free] if pins else names
    # second model: shared slopes plus one fixed intercept per strategy (the cost a strategy pays before its first
    # row: compilation, worker group, catalog acquisition), fitted jointly
    strategies = sorted({base_id(cells[4]) for _, _, cells in rows})
    index = {s: i for i, s in enumerate(strategies)}
    extended = []
    for vector, target, cells in rows:
        one_hot = [0.0] * len(strategies)
        one_hot[index[base_id(cells[4])]] = 1.0
        extended.append((vector + one_hot, target, cells))
    joint = nnls(extended, len(names_fit) + len(strategies))
    print(f"\nR^2 fitted (shared slopes + per-strategy fixed cost): {r_squared(extended, joint):.3f}")
    print(f"{'feature':32s} {'fitted ns':>12s}")
    for i, name in enumerate(names_fit):
        print(f"{name:32s} {joint[i]:12.1f}")
    print(f"\n{'strategy':32s} {'fixed ms':>10s} {'n':>5s}")
    for s in strategies:
        n = sum(1 for _, _, cells in rows if base_id(cells[4]) == s)
        print(f"{s:32s} {joint[len(names_fit) + index[s]] / 1e6:10.3f} {n:5d}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
