# Perf Loop

Use this loop when one benchmark should drive one optimization cycle.

## 1. Baseline

Run one selector with stable inputs:

```bash
.codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh \
  <fqcn.method> \
  [--param name=value]...
```

Keep these fixed across baseline and candidate runs:

- benchmark selector
- `--param` values
- Docker image / platform
- injected JFR fidelity flags
- any extra `--jvm-arg`

## 2. Read the recording

Open the emitted `.jfr` and identify:

- hottest CPU paths
- whether cost is concentrated in one thread or spread across many
- whether samples are lost
- whether time is really CPU time, not blocked / waiting / lock / GC dominated

Use [jfr-reading.md](jfr-reading.md) for CLI and JMC paths.

## 3. Pick one candidate fix

Choose one change with material total-cost share.

Good candidates:

- top hot leaf with meaningful self cost
- wide parent frame repeated across many hot leaves
- avoidable allocation or copying inside the hot path
- redundant dispatch / decoding / branching in the dominant path

Bad candidates:

- tiny leaf under 1% total share
- visually hot method that is mostly inherited inclusive cost
- code outside the dominant thread when one thread clearly dominates

## 4. Re-run the same selector

After the code change, rerun the exact same benchmark command shape. Do not change selector or parameters just to make the result look better.

## 5. Compare both benchmark and profile

Check both:

- benchmark moved in the expected direction
- hotspot share moved in the expected direction

Interpretation:

- benchmark up, hotspot down: likely real win
- hotspot down, benchmark flat: another bottleneck may now dominate
- hotspot flat, benchmark flat: fix likely missed the real limiter
- benchmark down, hotspot shifted elsewhere: regression or measurement drift; inspect both runs

## 6. Repeat until stop condition

Stop when one of these is true:

- hotspot shifts to a new dominant path
- CPU share of the original issue is no longer material
- the workload is no longer CPU-bound
- locks, GC, memory, I/O, or JIT behavior now dominate

## 7. Report honestly

Say:

- exact command
- benchmark delta
- `.jfr` path
- top hotspot shift
- confidence
- next-fix hypothesis

Remember: fixing one bottleneck often reveals the next one before the first fix shows its full end-to-end value. That is normal. Treat it as loop progress, not contradiction.
