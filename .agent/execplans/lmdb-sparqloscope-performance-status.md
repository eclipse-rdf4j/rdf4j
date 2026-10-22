# LMDB native execution draft checkpoint — GH-6205

This draft implements the missing native execution capabilities exposed by the SPARQLoscope investigation. It remains in progress until the current-source full correctness and controlled full-data acceptance gates finish.

The changes connect query deadlines to cooperative native cancellation, poll worker/coordinator completion and preserve drain-before-close ownership. Recursive scalar compilation and worker-local correlated predicates use shared capability/binding proofs. Typed SAMPLE, GROUP_CONCAT and DISTINCT state, generated-value storage and physical result replay are bounded by the query memory ledger and can spill. A native typed result channel carries length summaries through proven aliases into STRLEN without exporting summaries as RDF values. Dependency-domain execution preserves exact input multiplicities and offers conservative uncached execution when reuse benefit is unproved. Common candidate offering compares legal typed and FULL_VALUE alternatives while preserving forcing, actual-route reporting and streaming output.

## Verification and upstream integration

The private integration checkout combines the published implementation with develop at `61ce262c47f3221188869c515d4071db71e529aa`. Ten conflicting files were resolved to preserve both branches' semantics. The integration connects complete worker-local LATERAL preparation, projected condition entry frames and typed cells, HAVING physical inventory, and replay leases across the independent query-scope, entry-frame and term owners.

Production snapshot89 passes all ten matching regressions, including exact generic/compiled/interpreted results and actual parallel workers for the composed LATERAL/GRAPH/nested-EXISTS query. The expanded 55-class run has 801 passes and one setup error among 802 invocations: an upstream direct-filter fixture omitted query-entry initialization. Snapshot90 corrects that fixture with explicit captured-entry admission while preserving all semantic, native-ID and graph-provenance assertions. Its separate focused rerun passes 1/1 with zero failures or errors; the root quick install also passed. These counts are separate executions and are not a complete module result. Exact source manifests and first-red/matching reports remain retained outside the published source scope.

The private snapshot82 checkout passed the nine matching regressions and 652 neighboring tests across 45 classes, with zero failures, errors or skips. Offline quick installs, the module Spotless check and source copyright checks passed. The focused cases include the unchanged 5,000/15,000-byte query budgets, exact private kernel cancellation, actual FULL serial/parallel execution, typed alias STRLEN and explicit bounded result-owner cleanup. First-red reports and subsequent matching reports remain retained locally under `benchmark-runs/sparqloscope-20261020/implementation-evidence/`; raw evidence, build outputs and benchmark stores are excluded from the commit.

The last complete LMDB unit/integration gate was source66: 9,772 XML-backed cases with 138 skips and zero failures/errors. That result is historical evidence. It does not verify the subsequent worker-polling, physical arbitration, output-owner or upstream integration changes. The current complete module gate remains outstanding.

## Required continuation

- Run current-source complete LMDB and affected-module verification, with source/report identity recorded.
- Refresh the benchmark runtime and collect causally joined wall/allocation, execution, cancellation acknowledgement and cleanup evidence for the previously timed-out shapes.
- Qualify all 21 investigated queries and run three interleaved AUTO/alternative pairs; warm AUTO must be within 10% of the best correctly executed applicable candidate.
- Execute all 105 canonical queries over HTTP with full response consumption in one fresh-JVM suite and three warm suites; every AUTO execution must finish within 90 seconds while the configured timeout remains 180 seconds.
- Validate complete RDF result identity, boundness and multiplicities against the same store; use semantic SAMPLE membership and LIMIT-tie checks where appropriate.

These gates use the existing 503,865,752-triple format-6 DBLP store, Java26 and the accepted host/memory budgets. The sketch estimator and context-pair sketches stay disabled. Existing loaded-host diagnostics are diagnostic evidence only and do not approve performance targets. Canonical data, original reports and calibration state remain preserved.

The branch retains the two earlier GH-0000 commits without rewriting them. Further implementation and validation continue after draft publication. The complete accepted specification and living decisions are in [the ExecPlan](lmdb-sparqloscope-performance.md).
