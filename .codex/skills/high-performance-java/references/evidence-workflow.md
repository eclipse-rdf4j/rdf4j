# Evidence Workflow

Use this workflow before making strong performance claims.

## RDF4J path

1. Reproduce with the local benchmark wrapper.

```bash
scripts/run-single-benchmark.sh --module <module> --class <fqcn> --method <benchmarkMethod>
```

2. If the benchmark moves but cause is unclear:
   - use `--enable-jfr` for benchmark-side JFR capture
   - or use `async-profiler-java-macos` for cpu / alloc / wall evidence on macOS
3. If code shape or JIT behavior is the question:
   - use [hotspot-jit-forensics](../hotspot-jit-forensics/SKILL.md)
   - capture compilation tier, inlining decisions, and method-scoped C2 evidence

## Generic Java path

1. Build the smallest reproducible JMH or app-level benchmark.
2. Capture baseline result.
3. Change code shape.
4. Capture candidate result with same JVM, flags, input size, and warmup assumptions.
5. If the delta matters, inspect JIT evidence:

```bash
java   -XX:+UnlockDiagnosticVMOptions   -XX:+LogCompilation   -XX:LogFile=jit.xml   -XX:+PrintCompilation   -jar app.jar
```

If assembly or per-method diagnostics are needed, move to focused compiler directives and the `hotspot-jit-forensics` workflow.

## Additional workflow for runtime codegen / Janino

If the change introduces generated Java or runtime compilation, do not stop at a single warm benchmark.

Also capture:
- cold compile + first execute time
- warm cached execute time
- generated source size or code-shape proxy
- cache hit/miss behavior if caching is part of the design
- fallback behavior on compile failure or code-size overflow
- classloader / metaspace symptoms if repeated compilation is involved

## Output contract

Report these five items:
- benchmark delta: throughput/latency before vs after
- allocation delta: lower / unchanged / unknown
- JIT evidence: inline success/failure, tier, bailout, intrinsic, vectorization clue, or “not inspected”
- exact command or benchmark selector
- confidence: high / medium / low

If runtime codegen is involved, also report:
- cold compile cost: measured / unknown
- warm cache behavior: hit / miss / unknown
- fallback path exercised: yes / no / unknown

## Confidence rules

- High: repeatable benchmark delta plus matching profile/JIT evidence
- Medium: repeatable benchmark delta without definitive low-level proof
- Low: one run, noisy run, or JVM explanation not verified

For runtime codegen, confidence drops if cold-start cost, cache reuse, or fallback behavior were not inspected.

## Fallback when assembly is unavailable

Do not stop at “assembly unavailable”.

Still collect:
- `jit.xml`
- compiler directives output
- `PrintCompilation` / inlining diagnostics
- async-profiler or JFR evidence

Then say the exact missing piece: for example `hsdis` not installed or assembly printing not enabled.
