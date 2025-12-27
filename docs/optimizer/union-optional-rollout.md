# UNION + OPTIONAL Rollout

This document explains how to enable or disable the UNION/OPTIONAL optimizer changes, how to interpret harness output, and how to extend the harness.

## Enable or disable

The optimizer rules are guarded by system properties:

- `rdf4j.optimizer.unionOptional.enabled` enables or disables all rules.
- `rdf4j.optimizer.unionOptional.flatten.enabled` enables UNION flattening (U1).
- `rdf4j.optimizer.unionOptional.unionReorder.enabled` enables UNION arm reordering (U2).
- `rdf4j.optimizer.unionOptional.unionReorder.minRatio` sets the minimum cost ratio (default 1.5) required to reorder arms.

Optional LHS-only improvements (O1/O2) are still planned; no runtime flag is wired yet.

Set these properties on the JVM command line or in the harness runner before executing queries.

## Harness output

Each harness run writes baseline and candidate CSV files under `tools/optimizer-harness/target/harness/run-<timestamp>/`. Each CSV row includes a stable node id (path index), operator type, estimates, actual row counts, and total time in milliseconds. Summary files list the worst estimate errors so the estimator can be refined with evidence. Use `--profile tiny|small|medium` to scale dataset and query sizes.

Plan dumps are written when mismatches or regressions occur. Use the `plans/` folder to inspect the before/after algebra and confirm which rewrite fired.

## Adding new sweeps

To add new sweeps, update `QueryGenerator` with a new query family and add a deterministic dataset pattern in `DatasetGenerator` if needed. Keep changes deterministic by using the harness seed and avoid adding non-deterministic functions.

## Known good and bad patterns

Known good: skewed UNION arms, OPTIONAL with selective left-hand patterns, and filters scoped correctly inside OPTIONAL.

Known risky: OPTIONAL with filters outside that reference optional variables, and UNION/OPTIONAL distributive rewrites that change correlation or filter scope. These should remain disabled unless a dedicated rule and tests are added.
