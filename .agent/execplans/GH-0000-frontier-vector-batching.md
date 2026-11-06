# Integrate bit-exact Frontier SIMD batching

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. Maintain this document in accordance with
`.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

Frontier statistics currently hashes one RDF statement at a time. This change keeps every persisted hash and bucket
bit-for-bit identical, but batches independent statements into primitive structure-of-arrays buffers and evaluates
them with the Java 25 incubating Vector API when the runtime resolves `jdk.incubator.vector`. A scalar implementation
remains the authoritative fallback, handles small batches and tails, and lets the same LMDB artifact run in custom or
OSGi environments where the incubator module is unavailable.

The observable proof is threefold. Differential tests must produce the scalar oracle's exact values with the module
absent, with SIMD enabled, and with the module present but vector execution disabled. End-to-end statistics builds
must retain the same witnesses and query evidence across batch boundaries. A focused JMH benchmark must separately
report scalar and dispatched batch costs on the active JDK 25 runtime without claiming end-to-end RDF4J throughput.

## Progress

- [x] (2026-08-23 07:24Z) Read repository, SIMD, Maven, and HotSpot instructions; audited the dirty index/worktree.
- [x] (2026-08-23 07:24Z) Ran the required JDK-25 root quick clean install; the existing candidate built successfully.
- [x] (2026-08-23 07:34Z) Normalized candidate sources; JDK-25 compilation is green with explicit Vector arguments.
- [x] (2026-08-23 07:38Z) Added deterministic generated-source checking and green arbitrary-lane batcher coverage.
- [x] (2026-08-23 07:34Z) Routed pass two through plane-local batchers and accounted for their peak heap.
- [x] (2026-08-23 07:44Z) Added Java-25 compile/Javadoc/OSGi wiring and official launcher activation.
- [x] (2026-08-23 07:56Z) Added differential, batching, builder, launcher, and benchmark coverage.
- [x] (2026-08-23 08:22Z) Ran all focused runtime modes, the LMDB selection, benchmark, and C2 inspection.
- [x] (2026-08-23 08:36Z) Finished module formatter, Javadoc, final quick install, packaging, and artifact checks.
- [x] (2026-08-23 08:36Z) Completed the artifact audit and retrospective without staging or committing.
- [x] (2026-08-23 09:40Z) Enabled the Vector module in both Theme query JMH forks with focused red/green evidence.

## Surprises & Discoveries

- Observation: The four supplied candidate sources were already present, but the two new classes were staged without
  package declarations while their worktree versions added packages. `FrontierStatisticsHash` and
  `FrontierLeafProbe` had been replaced by standalone formatting without repository headers. The unrelated
  `ThemeQueryBenchmark.java` worktree content equals `HEAD`, but its index contains a stale deletion.
  Evidence: `git status --short` reports `AM` for both new classes and `MM` for the benchmark; staged and unstaged
  statistics are disjoint. Do not alter the index.

- Observation: Contrary to the initial expectation, the current candidate completes the repository's JDK-25
  `-Pquick clean install` without explicit LMDB POM wiring.
  Evidence: `maven-build.log` reports `RDF4J: LmdbStore SUCCESS [8.028 s]`, `RDF4J: Runtime - OSGi SUCCESS [5.081 s]`,
  and `BUILD SUCCESS` in 40.892 seconds. Inspect the effective compiler command and produced manifest before deciding
  which explicit configuration remains necessary.

- Observation: The default build configuration has four design lanes and two audit lanes, so an eight-lane-only
  adapter would not be a general repository solution. User configuration does not impose a maximum lane count.
  Evidence: `FrontierStatisticsBuildConfig.forDiskBudget` selects 4 + 2 lanes and validation requires only positive
  lane counts.

- Observation: Maven's in-process compiler compiled the Vector imports without an explicit module argument, even
  though direct JDK-25 `javac` rejects them as not in the module graph. The effective Maven compiler command contained
  no hidden module option, and the generated OSGi repository metadata treated `jdk.incubator.vector` as mandatory.
  Evidence: `/tmp/frontier-vector-maven-compile-debug.log` shows only inherited lint arguments; direct `javac` passes
  only with `--add-modules jdk.incubator.vector`; `.m2_repo/repository.xml` recorded `optional='false'`.

- Observation: The first post-edit compile accidentally used the shell's JDK 26 with `--release 25`; it passed but is
  not acceptance evidence. The identical compile was rerun under Temurin 25.0.1 and passed in 8.799 seconds.
  Evidence: The appended `maven-build.log` contains both Maven runtime banners and the final JDK-25 `BUILD SUCCESS`.

- Observation: Top-level `initial-evidence.txt` already contains ignored evidence from the earlier shard optimization.
  Preserving it requires a task-specific evidence filename rather than overwriting an unrelated artifact.
  Evidence: Its retained commands are the 2026-08-20 `FrontierStatisticsShardTest` and LMDB module selections; current
  adapter evidence is in `initial-evidence.frontier-vector-batching.txt`.

- Observation: Ordinary jars on this branch do not inherit an OSGi manifest merely because the root declares the
  Maven Bundle Plugin. The LMDB artifact therefore needed an explicit `manifest` execution and Jar Plugin manifest
  binding, not just an `Import-Package` instruction.
  Evidence: The pre-change jar had only Maven implementation entries. The JDK-25 `clean package` now produces a
  `Bundle-ManifestVersion: 2` jar whose first calculated import is
  `jdk.incubator.vector;resolution:=optional`; no `jdk.incubator.vector` package is exported.

- Observation: The live batched builder and the retained scalar `FrontierOmniBuilder.addWitnesses` oracle produce
  byte-identical Omni cell-directory, tuple, and posting shards across 255/256/257 rows per plane and 2/6/17 lanes.
  Evidence: Focused JDK-25 builder selections passed 3 + 2 cases in 1.946 and 1.920 seconds respectively; a second-pass
  failure before the first flush also passed and left no published `.fs2m` manifest.

- Observation: All new focused selections pass in three fresh JVM modes. The hash facade passed 5 tests in each
  unresolved, resolved-SIMD, and resolved-forced-scalar process; the adapter passed 13 tests in each mode; the
  capacity-focused builder selection passed 3 tests in each mode. Additional 2/17-lane and failure/publication
  selections passed 2 and 1 tests respectively.
  Evidence: Retained logs are under `logs/mvnf/20260823-073751-verify.log` through
  `logs/mvnf/20260823-081156-verify.log`; the exact command/report summary is preserved in
  `initial-evidence.frontier-vector-batching.txt`.

- Observation: The broad LMDB selection is not green for unrelated branch state. It completed 2,261 tests with one
  failure, five errors, and seventeen skips. The failure is the preserved staged/worktree mismatch in
  `ThemeQueryBenchmarkSparseParamTest`; all errors report `A join telemetry side cannot have overlapping evaluations`
  from existing planning/benchmark tests. No new SIMD, batcher, or builder test failed.
  Evidence: `logs/mvnf/20260823-081300-verify.log` contains the final Surefire aggregate and each exact failure.

- Observation: This Apple Silicon JDK-25 runtime prefers only two long lanes (`S_128_BIT`). Unlike the supplied
  external AVX-512 measurements, dispatched SIMD is slower here: at 8,192 records the scalar oracle is 68.162
  ns/record, forced-scalar batching is 111.640 ns/record, and dispatched SIMD is 158.387 ns/record. Allocation is
  effectively zero after construction in every benchmark path.
  Evidence: `profiles/lmdb/frontier-vector-{scalar,forced-scalar,dispatched}-jdk25.txt` contains the JMH and GC
  profiler results. These are hot-loop measurements, not end-to-end RDF4J throughput.

- Observation: HotSpot compiled both fused `omniPriorities8` and `omniBucketSequences` kernels at C2, including OSR
  versions, but this JDK installation could not decode machine assembly because an `hsdis` disassembler was not
  available.
  Evidence: `profiles/lmdb/frontier-vector-jit.xml` contains the C2 nmethods and
  `profiles/lmdb/frontier-vector-c2-jdk25.txt` contains the C2-only run.

- Observation: A top-level quiet formatter invocation became detached from its tool session without a live Maven or
  Java process and produced no trustworthy exit status. It was stopped and is not acceptance evidence.
  Evidence: Process inspection found no Maven/JPS child; rerun the formatter visibly before final acceptance.

- Observation: The same repository-wide formatter behavior recurred with visible output after it reached the root
  Spotless execution. Module-scoped Spotless runs for every touched Java/Maven module then completed normally and
  reported zero changed files; the final JDK-25 root clean install also completed every lifecycle-bound Spotless
  execution successfully.
  Evidence: The LMDB/server-boot `process-resources` run finished `BUILD SUCCESS` with 557 LMDB Java files and all
  server-boot sources already clean; final `maven-build.log` reports a green 123-project reactor.

- Observation: Separating platform Vector capability from the execution-enable property is required for stable
  capacity rounding and heap accounting. A resolved module with execution forced off must remain scalar while still
  exposing the actual preferred species to the batcher.
  Evidence: The final forced-scalar facade and adapter tests pass with the resolved two-lane species, while
  `vectorEnabled()` remains false.

- Observation: The Theme benchmarks' forked JVMs did not inherit the LMDB compiler or launcher module arguments;
  both `ThemeQueryBenchmark` and `ThemeQueryPlanRunBenchmark` explicitly declared only heap arguments in `@Fork`.
  Evidence: The new reflection test failed before the edit and passed after both annotations gained
  `--add-modules=jdk.incubator.vector`.

- Observation: During post-change verification, an unrelated concurrent edit appeared in
  `FrontierOmniLayout.java`; Spotless reformatted one of its new lines, another writer restored the unformatted line,
  and the final Spotless check failed after the focused Surefire method had passed.
  Evidence: `logs/mvnf/20260823-093735-verify.log` reports 1/0/0 for Surefire followed by the unrelated format error.
  The concurrent file was preserved, and an isolated direct Surefire rerun finished `BUILD SUCCESS` with 1/0/0.

## Decision Log

- Decision: Use Routine D and this ExecPlan instead of manufacturing a pre-fix TDD failure for user-supplied
  production candidates.
  Rationale: The change combines generated low-level code, build/runtime module behavior, OSGi packaging, streaming
  batching, launchers, and performance evidence. Each milestone remains independently testable.
  Date/Author: 2026-08-23 / Codex

- Decision: Preserve all unrelated tracked, staged, and untracked state; edit intended worktree files with narrow
  patches and never run restore/reset/clean/unstage or bulk staging.
  Rationale: The checkout contains unrelated research and a stale benchmark index entry owned by the user.
  Date/Author: 2026-08-23 / Codex

- Decision: Batch only the second-pass Omni witness workflow in this tier. Keep population, heavy-predicate, and center
  updates in their current order.
  Rationale: This realizes the supplied fused priorities and bucket-sequence kernels while keeping the next direct
  sketch-update fusion and broader builder batching out of scope.
  Date/Author: 2026-08-23 / Codex

- Decision: Generalize the adapter to every positive configured lane count. Use 4/8/16 fixed priority kernels up to
  sixteen lanes, then vectorize row priorities and bucket sequences while deriving arbitrary-lane priorities through
  the scalar oracle.
  Rationale: Six lanes is the repository default and configuration supports totals above sixteen.
  Date/Author: 2026-08-23 / Codex

- Decision: Official launchers resolve the incubator module when it exists; custom launchers and unresolved OSGi
  packages retain scalar fallback.
  Rationale: This makes the optimization active in supported distributions without making the optional runtime
  dependency a hard artifact requirement.
  Date/Author: 2026-08-23 / Codex

- Decision: Clear the adapter's logical batch count immediately before invoking the sink and permanently fail the
  adapter on any hashing, sink, runtime, or linkage failure.
  Rationale: Reusable arrays remain visible for synchronous consumption, while a partial sink cannot be retried or
  duplicated and no failure after partial output is converted into scalar fallback.
  Date/Author: 2026-08-23 / Codex

- Decision: Generate and bind a Bnd manifest during the LMDB jar lifecycle while retaining `jar` packaging.
  Rationale: This makes the produced LMDB artifact itself OSGi-resolvable without changing its Maven artifact type;
  the explicit optional Vector import is followed by `*` so all other imports remain calculated.
  Date/Author: 2026-08-23 / Codex

- Decision: Detect the preferred species independently of `rdf4j.frontier.vector.enabled`, and apply that property
  only to execution dispatch.
  Rationale: Disabling execution must not change allocation size or heap-governor accounting when the Vector module
  is otherwise resolved. Module-unresolved environments still use a one-lane capacity multiple.
  Date/Author: 2026-08-23 / Codex

- Decision: Put Theme benchmark activation in the JMH `@Fork` configurations, not only in the shell benchmark
  wrapper.
  Rationale: This covers the main Theme benchmark, the plan/run benchmark, direct JMH-jar invocation, and the existing
  supervised Theme campaigns, all of which create their actual measurement JVM from JMH fork metadata.
  Date/Author: 2026-08-23 / Codex

## Outcomes & Retrospective

The repository-native SIMD tier is complete. The scalar hash remains the oracle, schema identifiers remain 2 and 1,
and `FrontierLeafProbe` has no worktree diff. Generated Java-25 kernels are deterministic under `--check`; the sole
runtime facade remains loadable with the module unresolved and performs SIMD only for a capable, enabled runtime above
the configured crossover. The general reusable adapter supports every positive lane count and the live second witness
pass is batch-driven without changing population order or publication failure semantics.

Fresh-JVM differential evidence is green in unresolved, resolved-SIMD, and resolved-forced-scalar modes. Final facade
logs report 5/0/0 in each mode; adapter logs report 13/0/0 in each mode, with the final forced-scalar rerun confirming
preferred-species capacity rounding; capacity-focused builder logs report 3/0/0 in each mode. Additional two- and
seventeen-lane comparisons and failure/non-publication coverage pass. The final JDK-25 root `-Pquick clean install`
completed all 123 projects in 39.620 seconds.

Packaging is complete: the LMDB jar has a Bnd OSGi manifest whose first import is
`jdk.incubator.vector;resolution:=optional`, while no Vector package is exported. The server-boot distribution ZIP
contains the conditional launcher. Fake-Java launcher tests cover module-present and module-absent paths; shell syntax,
Docker static checks, generated-source equality, touched-file headers, Javadoc generation, and `git diff --check` pass.
The Javadoc goal reports four existing unsupported-tag warnings outside the changed Frontier code.

The broad LMDB suite is honestly classified broad-red: 2,261 tests, one failure, five errors, and seventeen skips.
The failure reflects the preserved `ThemeQueryBenchmark.java` staged/worktree mismatch, and the five errors are the
existing overlapping-evaluation telemetry invariant in planning/benchmark tests. None of the new SIMD, adapter, or
builder tests failed. No index entry was changed; all unrelated tracked and untracked research remains untouched.

Performance is hardware-dependent. On this Apple Silicon JDK-25 runtime, preferred vectors are only 128-bit/two-lane,
and dispatched SIMD is slower than forced-scalar batching: at 8,192 records, 158.387 versus 111.640 ns/record; the
direct scalar oracle is 68.162 ns/record. Allocation is effectively zero after construction. Both fused priority and
bucket-sequence kernels reached C2, but decoded assembly was unavailable without `hsdis`. These hot-loop figures do
not establish end-to-end RDF4J throughput and do not reproduce the external AVX-512 result.

Both Theme query JMH benchmarks now resolve the Vector module in their measurement forks while retaining `-Xms1G`
and `-Xmx16G`. The focused fork contract was captured red before the annotations changed and green afterward. The
direct Surefire post-fix command is green; the corresponding full `mvnf` lifecycle reached a green Surefire phase but
is not classified green because a concurrent unrelated `FrontierOmniLayout.java` edit failed its later Spotless gate.

## Context and Orientation

`core/sail/lmdb` owns the LMDB store and its Frontier statistics implementation. `FrontierStatisticsHash.java` is the
package-private scalar hash contract and defines persisted schema identifiers. `FrontierStatisticsHashVector.java` is
the generated Vector API implementation. `FrontierStatisticsHashBatch.java` chooses vector or scalar execution once
per batch. `FrontierOmniBuilder.java` currently calculates row priorities, lane priorities, and bucket sequences for
one statement at a time during the second snapshot pass. `FrontierStatisticsBuilder.java` owns both snapshot scans and
the global heap-governor estimate.

The input layout is four contiguous `long[]` columns for subject, predicate, object, and context IDs. Multi-output
priority arrays are lane-major: logical lane `L`, record `R` is at `L * stride + R`. Bucket sequences are domain-major:
the domain is `(lane * 4 + component)` and the record address is `domain * stride + R`. A bucket sequence packs the
low 32-bit start position and odd 32-bit delta from which all Omni rows are derived exactly.

The Java Vector API is an incubating JDK module. Compiling source that imports it and launching code that resolves it
are distinct operations. `FrontierStatisticsHashBatch` must remain loadable without resolving the vector class. The
facade checks the boot module layer, the enabled property, the preferred long-vector lane count, and a minimum batch
threshold before invoking a vector method.

RDF4J packages LMDB as an OSGi bundle as well as a classpath jar. Any calculated `jdk.incubator.vector` package import
must be optional so the entire bundle remains resolvable when an OSGi framework does not export the package. Official
server, console, and Docker launchers can resolve the JDK module at process startup; arbitrary embedders cannot be
changed from inside the library.

## Plan of Work

First normalize the supplied sources. Reconstruct the repository headers and formatting around the semantic candidate
instead of accepting the standalone wholesale rewrites. `FrontierLeafProbe` should end with no semantic change.
`FrontierStatisticsHash` should preserve its current scalar methods and schema IDs, widening only constants referenced
by the generated vector class. Add correct headers and generated-source comments to the two new classes. Tighten
minimum-batch property parsing so malformed, zero, and negative values cannot lower the threshold below the preferred
vector length.

Add `scripts/generate-frontier-statistics-hash-vector.py`. It must deterministically own the tracked vector source and
support a non-mutating `--check` mode. The generated source includes the RDF4J header, package declaration, imports,
class documentation, fixed kernels, exact scalar tails, and the source-generator warning. Regeneration and repository
formatting must converge to the tracked source with no diff.

Add `FrontierStatisticsOmniBatcher.java`. Its constructor accepts a constant plane, positive lane count, and positive
requested capacity. Round capacity up to the vector length exposed by the dispatch facade. Allocate and reuse four
input columns, lane-major priorities, domain-major bucket sequences, and a row-priority scratch array only when more
than sixteen logical lanes require it. The package-private sink signature is:

    void accept(int plane,
            long[] subjects, long[] predicates, long[] objects, long[] contexts,
            int count,
            long[] priorities, int priorityStride,
            long[] bucketSequences, int bucketSequenceStride) throws IOException;

`add` flushes a full buffer before storing the next record. `flush` is a no-op for an empty buffer and otherwise hashes
then invokes the sink once. The batch count is cleared before invoking the sink so a thrown exception cannot be
retried or duplicated; a failed flag rejects every later operation. For one through four lanes call the fixed four-lane
kernel, for five through eight call the eight-lane kernel, and for nine through sixteen call the sixteen-lane kernel.
For larger layouts call the vectorized row-priority facade, then fill actual lane-major priorities through
`FrontierStatisticsHash.omniPriorityFromRowPriority`. For every actual lane and component call the batch facade's
vectorized bucket-sequence method. `estimatedHeapBytes` must use checked arithmetic and match the allocated primitive
arrays.

Add `FrontierOmniBuilder.addWitnessBatch`. Consume precomputed arrays with record as the outer loop, then lane,
component, and row, preserving the current event order and admission formula. Retain `addWitnesses` unchanged as the
one-record scalar oracle. In the second scan in `FrontierStatisticsBuilder`, create one capacity-256 batcher per plane,
run heavy and center updates synchronously, enqueue Omni records, and flush after `scanChecked` succeeds. Include the
largest single batcher's bytes in `estimatedHeapBytes`; the two plane-local instances do not coexist.

Inspect the effective Maven compiler configuration and built manifest before changing `core/sail/lmdb/pom.xml`. Add
only missing explicit Java-25 compiler and Javadoc module options. Configure Bnd with
`jdk.incubator.vector;resolution:=optional,*` if the generated manifest otherwise makes the package mandatory. Verify
both module-absent loading and the packaged manifest rather than assuming Maven configuration behavior.

Update `tools/server-boot/src/main/dist/bin/rdf4j-server.sh`, SDK console shell and Windows batch scripts, and the two
JDK-25 Dockerfiles. Shell and batch launchers probe `--list-modules` and append
`--add-modules=jdk.incubator.vector` only when present. The pinned Docker images add the module to their existing Java
option variables. Update the server-boot and Docker documentation with the automatic behavior, incubator warning,
disable/minimum-batch properties, and manual flag for custom WAR or embedded launchers.

Add comprehensive scalar-oracle tests for every batch facade operation and boundary, adapter layout and lifecycle
tests for lane counts 1, 2, 4, 6, 8, 16, and 17, and focused builder tests immediately below, at, and above batch
capacity. Add a launcher regression script using fake Java commands for module-present and module-absent paths. Add a
JMH benchmark comparing the scalar witness hash workflow with the dispatched adapter for batch sizes 7, 8, 256, and
8192 at six logical lanes.

## Concrete Steps

Run every command from the repository root with JDK 25 selected. Do not use `-am` or `-q` for tests. Preserve full
Maven install output in `maven-build.log`.

The initial build command, already green, is:

    JAVA_HOME=/Users/havardottestad/.sdkman/candidates/java/25.0.1-tem \
    PATH=/Users/havardottestad/.sdkman/candidates/java/25.0.1-tem/bin:$PATH \
    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
      -Dmaven.repo.local=.m2_repo -Pquick clean install

After each source milestone, run the generated-source check and focused compilation/tests. Use these fresh-JVM test
modes through `mvnf` with `--retain-logs`:

    python3 .codex/skills/mvnf/scripts/mvnf.py FrontierStatisticsHashBatchTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py FrontierStatisticsHashBatchTest --retain-logs -- \
      -DargLine="--add-modules jdk.incubator.vector"
    python3 .codex/skills/mvnf/scripts/mvnf.py FrontierStatisticsHashBatchTest --retain-logs -- \
      -DargLine="--add-modules jdk.incubator.vector -Drdf4j.frontier.vector.enabled=false"

Repeat the same modes for `FrontierStatisticsOmniBatcherTest` and the new focused builder method. After focused green,
run `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs` once. Persist the first compact test report
in top-level `initial-evidence.txt` and keep the retained Maven logs.

Before formatting, run the copyright checker and correct only touched files. Then run the repository formatter,
generated-source check, `git diff --check`, Javadoc, package, and manifest inspection. Shell syntax and launcher tests
must cover both module branches. Do not alter the index during any check.

Use the repository benchmark wrapper with `--jvm-arg --add-modules=jdk.incubator.vector` for SIMD and add the disable
property for forced scalar. Keep JFR, compiler directives, `jit.xml`, and any assembly under existing untracked profile
locations; do not add them to the intended patch. Inspect the fused priority and bucket-sequence methods on JDK 25 and
state exactly when assembly is unavailable.

## Validation and Acceptance

Acceptance requires exact equality with the scalar oracle for all facade methods, offsets, strides, masks, components,
vector boundaries, tails, and arbitrary supported lane counts. `HASH_SCHEMA_ID` must remain 2 and `BUCKET_SCHEMA_ID`
must remain 1. The module-unresolved and forced-scalar JVMs must report scalar dispatch. A module-resolved JVM with more
than one preferred long lane must report vector dispatch at or above the crossover.

The adapter must invoke its sink once per full or final partial batch, preserve record/lane/component/row order, allocate
nothing after construction, and become permanently failed after any hashing or sink exception. End-to-end builds must
publish the same exact/query-visible evidence below, at, and above capacity, including six- and seventeen-lane layouts.

The packaged LMDB jar must remain usable without the Vector module. Its OSGi manifest must not contain a mandatory
vector import. Official launchers must add the module when the selected JDK exposes it and omit the option when a fake
or custom runtime does not.

Formatting, generated-source checking, Javadoc, quick install, focused tests, and the LMDB module suite must be green.
The benchmark is evidence, not a correctness gate: report scalar, forced-scalar batching, and dispatched SIMD numbers
with the active JDK/CPU and make no end-to-end throughput claim from a hot-loop result.

## Idempotence and Recovery

The generator check, tests, packaging, and benchmark commands are repeatable. The batcher owns only heap arrays and
does not change persistence. No migration or cleanup is required. If Maven offline resolution fails, rerun the exact
command once online, then return offline. If parallel Maven fails for another reason, retry the same command serially.

Never recover by resetting, restoring, cleaning Git files, unstaging, or deleting untracked artifacts. Repair intended
files with narrow patches. The unrelated staged benchmark deletion and every unrelated diagnostic remain user-owned.

## Artifacts and Notes

The initial baseline is in `maven-build.log`. Focused Maven logs belong under `logs/mvnf/`; compact initial evidence
belongs in `initial-evidence.txt`. Performance artifacts belong under untracked `profiles/lmdb` paths. The final handoff
must distinguish focused green, LMDB-module green, package/Javadoc results, benchmark evidence, and anything aborted,
blocked, or unrun.

## Interfaces and Dependencies

No public Java API or third-party dependency is added. New package-private types are `FrontierStatisticsHashVector`,
`FrontierStatisticsHashBatch`, and `FrontierStatisticsOmniBatcher`. The adapter's sink consumes reusable primitive
arrays synchronously. External runtime controls are:

    -Drdf4j.frontier.vector.enabled=false
    -Drdf4j.frontier.vector.minBatch=<positive count>

The only platform dependency is the Java 25 JDK's `jdk.incubator.vector` module. Compilation and SIMD-enabled launches
resolve it explicitly. Module-absent, single-lane, disabled, and below-crossover execution remains scalar and bit-exact.
