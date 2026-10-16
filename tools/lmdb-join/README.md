# Native and IR hash-join performance experiments

This revision is based on `lmdb-packed-performance-source.zip`. There are no new configuration flags,
external dependencies, persistent formats, or join strategies. It changes the existing primitive
build/probe data structures and their native/factorized consumers. All prior source is retained.

## Reproduction

Use a complete JDK 26. The measured runs used the supplied Zulu 26.0.2.1 x86-64 build.
From the source root containing `java/`:

```bash
export JAVA_HOME=/path/to/jdk26
python3 tools/lmdb-join/run.py --source /path/to/packed-baseline \
  --out /tmp/join-base --compile-only
python3 tools/lmdb-join/run.py --out /tmp/join-tuned
python3 tools/lmdb-join/compare.py --base /tmp/join-base --tuned /tmp/join-tuned \
  --out /tmp/join-results --forks 5
python3 tools/lmdb-join/capture_c2.py --build /tmp/join-base --out /tmp/join-c2-base
python3 tools/lmdb-join/capture_c2.py --build /tmp/join-tuned --out /tmp/join-c2-tuned
python3 tools/lmdb-packed/regressions.py --baseline /path/to/packed-baseline \
  --out /tmp/join-inherited-tests
```

Use different output paths for different builds. The inherited builder only clears marked disposable
build directories. `compare.py --filter case-one,case-two` selects named cases without changing their
parameters; `--cpu` optionally pins a process, but the reported final run did not pin a CPU. Run the
measurements without concurrent compilation, regression tests, or other benchmark processes.

## Compilation boundaries

The offline harness compiles the complete production `PrimitiveHashJoinTable`,
`PrimitiveHashJoinFactorSource`, `KernelRuntime`, and real factor reader/selection components. It
retains the previous generated-key component build and its labeled external API doubles.
`NativeBatch` is the exact complete class extracted from its multi-class source file.
`estimateBuildBytes` and the three production refill/probe methods are copied without algorithmic
changes into small test shells. `JoinProbeDriver` tests refill, selected physical rows and transitions
between head/count/bucket consumers. The timed `JoinBatchDriver` is selected when compiling: it
invokes the baseline or candidate's real batch-preparation signatures, with no reflective branch or
reimplementation of probing in the measured loop.

`SourceSyntaxCheck` uses the JDK compiler's parse phase on all five changed complete production files.
This is syntax checking, not dependency type checking. The build does not compile the complete
RDF4J application, Janino-generated queries, the full HashJoinFactorCursor/HashJoinBatchCursor type
graph, or the real repository integration tests. Every copied/extracted/doubled unit is recorded in
`build-manifest.json`. Existing repository integration suites, including
`LmdbNativeHashJoinChainStatsTest`, `LmdbNativeBatchedProbeTest`,
`LmdbNativeAdaptiveHashJoinTest`, and `LmdbNativeKernelIrEmitterTest`, remain relevant full-branch checks.

## Measurement boundaries

Inputs are preconstructed primitive ID arrays. There is no input generation, query parsing,
optimization, dictionary access, LMDB I/O or result Value materialization inside a timed operation.
Build cases allocate and insert the entire table, including capacity growth. Probe cases reuse a
built table and repeat a 16,384-lane selection. Batch cases include hashing and entry staging, not
just successful lookups. Chain cases also visit the linked payloads. Misses, three-column keys,
scattered selections, 75% occupancy, small tables and alternating key shapes are separate cases.
Do not treat hot repeated probes into a modest resident table as a random cold-RAM database benchmark.

Each independent JVM warms for at least ten operations and one second, then calibrates repetition
counts toward 50 ms per pass and measures seven passes. Five forks are run per version per case,
alternating the version order. The median of each process is retained; the report uses the median
of the five process medians. Wall time, current-thread CPU time and current-thread allocated bytes
are recorded. This is a standalone diagnostic harness, not JMH or a confidence-interval analysis.

`native-bytes` and `ir-bytes` are exact primitive backing-array payload counts, checked independently
by reflection in the regression suite. They exclude object/array headers, source batches, probe
scratch and inputs, old arrays awaiting GC, JVM code and all other query state. The table's memory
reservation remains conservative during a build and is not shrunk to the eventual distinct count.
Probe scratch has a separate deterministic 4-byte-per-lane reduction in the real cursor.

## C2

Only diagnostic method-print commands are supplied. There is no forced inlining, forced compilation,
changed compilation threshold, `-Xcomp`, or explicit Vector API. When hsdis is unavailable, the
inherited `disassemble.py` validates every emitted main-code byte before decoding with objdump.
Missing machine bytes cause a failure rather than a fabricated instruction listing. All captured
recompilations are kept; capture runs are not timed benchmark forks.

Historical experiment patches are documented in `experiments/README.md`. They are source-level
independent alternatives against the same baseline, not sequential patches to apply to the release.
