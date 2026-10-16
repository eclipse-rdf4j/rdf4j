# Earlier table variants, not production alternatives

Each patch independently replaces the FINAL NativeRuntimeValueTable with an earlier table snapshot. Apply only to a fresh final source tree, not cumulatively. Later wrapper/group/lifecycle changes stay in place, so historical timing logs are not claimed as measurements of these exact reconstructed combinations.

Use `run.py --compile-only --out ...` then invoke GeneratedKeyOptimizationBenchmark or request C2 printing manually. Some final regression tests intentionally reject these variants (normal clustering and resource-work safeguards), so passing the final suite is not expected. Undo the patch or start from another fresh final tree afterward. Never deploy an experimental reverse patch.

01 repeats metadata ownership probes and term comparisons. 02 removes the redundant publication probe but still performs semantic matching during duplicate lookup. 03 moves semantic coalescing to insertion but still mistakes ordinary occupied buckets for adversarial full-hash collisions. The final version counts full-hash candidates separately and retains a residual work guard.
