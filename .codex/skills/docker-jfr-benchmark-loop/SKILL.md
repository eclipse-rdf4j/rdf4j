---
name: docker-jfr-benchmark-loop
description: Run a repeatable RDF4J performance loop against one JMH benchmark or a benchmark class main method in Docker with Linux Java 26 and JFR CPU-time profiling. Use when working in this repo on benchmark-guided performance changes, hotspot triage, JFR reading, CPU bottleneck analysis, or repeated baseline, fix, and rerun loops. Trigger on requests mentioning benchmark, profiling, JFR, hotspot, perf loop, CPU bottleneck, or Docker benchmark runs in RDF4J.
---

# Docker JFR Benchmark Loop

Use this skill for one-benchmark perf work in this repo. Default runner: `scripts/run-docker-jfr-loop.sh`, not ad hoc Maven or raw JMH commands.

## Quick start

Dry-run a known selector:

```bash
.codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh \
  org.eclipse.rdf4j.model.benchmark.ValueCreationBenchmark.createBNode \
  --dry-run
```

Explicit selector plus params:

```bash
.codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh \
  --module core/model \
  --class org.eclipse.rdf4j.model.benchmark.ValueCreationBenchmark \
  --method createBNode \
  --param samples=1000
```

Invoke a benchmark class's own main method when that main owns the JMH configuration:

```bash
.codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh \
  --module core/sail/lmdb \
  --main-class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark
```

Main-class mode launches the class from the packaged benchmark jar and attaches the Java 26/JFR options to that JVM.
It does not select or invoke a JMH benchmark method itself. Use `--main-arg` for application arguments; JMH `--param`
and raw JMH arguments are intentionally unavailable because the main method owns those choices.

## Repo-grounded defaults

- The wrapper calls repo helper `scripts/run-single-benchmark-docker.sh`.
- The Docker helper already forces Linux Java 26 plus `--enable-jfr --enable-jfr-cpu-times`.
- The inner helper already enforces:
  - `settings=profile`
  - `dumponexit=true`
  - `duration=120s`
  - `warmup=0`
  - `measurement=10` iterations of `10s`
  - `forks=1`
  - `jdk.CPUTimeSample#enabled=true`
  - `report-on-exit=cpu-time-hot-methods`
- This skill wrapper adds the missing fidelity flags:
  - `-XX:FlightRecorderOptions=stackdepth=1024`
  - `-XX:+UnlockDiagnosticVMOptions`
  - `-XX:+DebugNonSafepoints`
- The repo helper sets `-XX:StartFlightRecording:method-profiling=max` for CPU-time JFR runs.

## Core loop

1. Pick one benchmark selector, or main-class mode when the class must own its JMH setup. Keep the entrypoint, params,
   Docker image, and profiling flags constant for the whole comparison.
2. Capture a baseline run with `scripts/run-docker-jfr-loop.sh`.
3. Read the `.jfr` using [references/jfr-reading.md](references/jfr-reading.md).
4. Choose one candidate fix with material CPU share. Prefer the fix most likely to move total runtime, not just local self time.
5. Re-run the exact same selector and params.
6. Compare benchmark delta plus hotspot shift.
7. Repeat until:
   - the hotspot shifts,
   - CPU share falls below a meaningful threshold,
   - or GC / locks / memory / I/O / JIT behavior dominates instead.

## Operating rules

- Route benchmark variation through `--param`.
- Route JVM tuning through `--jvm-arg`.
- Do not use raw `--jmh-arg` during JFR runs; the helper rejects extra JMH args when JFR is enabled.
- Do not switch to a second `StartFlightRecording`; stay aligned with repo helper behavior.
- Do not treat a small benchmark gain as proof the hotspot fix failed. First check whether another bottleneck surfaced and now caps the total speedup.
- If the run is not CPU-bound, say so and pivot to lock, GC, memory, I/O, or JIT evidence instead of forcing a CPU-only story.

## Output contract

Answers using this skill should usually include:

- exact run command
- `.jfr` path
- top CPU hotspots
- confidence notes
- next-fix hypothesis
- whether a second bottleneck likely masks part of the gain

## Escalate cleanly

- Use [high-performance-java](../high-performance-java/SKILL.md) for algorithm, data-structure, and code-shape changes.
- Use [hotspot-jit-forensics](../hotspot-jit-forensics/SKILL.md) when inlining, tiering, C2, or codegen behavior is in question.
- Use [jmh-benchmark-compare](../jmh-benchmark-compare/SKILL.md) for multi-run diffs, history, and exported reports.

## Reference map

- Loop workflow: [references/perf-loop.md](references/perf-loop.md)
- JFR reading guide: [references/jfr-reading.md](references/jfr-reading.md)
