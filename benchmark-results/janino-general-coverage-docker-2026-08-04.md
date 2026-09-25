# Janino general-coverage Docker gate — 2026-08-04

Linux JDK 26, one fork, zero warmup, ten 10-second measurements per cell, complete result-digest validation.

| Workload | LMDB | Janino | Decision |
| --- | ---: | ---: | --- |
| 49 formerly unsupported PlanRows queries | 3.892 ± 0.249 ms/op | 4.189 ± 0.449 ms/op | default off |
| generated SUM(DISTINCT) query | 0.015 ± 0.001 ms/op | 0.017 ± 0.001 ms/op | default off |
| AVG(DISTINCT) companion | 0.011 ± 0.001 ms/op | 0.014 ± 0.001 ms/op | default off |

Commands used the repository Docker/JFR runner:

    .codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeGeneratedCorpusBenchmark.executeQueries --param workload=PLAN_BRIDGE_49 --jfr-output profiles/lmdb/janino-plan-bridge-49-d49fb95e9c.jfr
    .codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeGeneratedCorpusBenchmark.executeQueries --param workload=DISTINCT_SUM --jfr-output profiles/lmdb/janino-distinct-sum-d49fb95e9c.jfr
    .codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeGeneratedCorpusBenchmark.executeQueries --param workload=DISTINCT_AVG --jfr-output profiles/lmdb/janino-distinct-avg-d49fb95e9c.jfr

The PlanRows recording shows why it loses: generated execution wraps the unchanged engine plan. Allocation pressure
includes `LmdbNativeKernelExecution.KernelRowCursor` (2.44%), `lowerRowsWithPlanBridge` (1.31%), and
`LmdbNativeKernelBindings` (1.30%), in addition to staging arrays and the isolated row-state copy. This is a correct
semantic escape hatch but not a fused replacement, so it cannot default on under the parity-or-faster rule.
