# Compact generated-value keys: implementation, evidence and production limitations

Baseline: `generated-value-keys-source.zip`, not the original dictionary-resolving implementation. Both sides already avoid dictionary calls for admitted generated keys. This revision optimizes the retained identity machinery and the remaining ingress/key-table work.

## Status

The production component suites pass **975,697 counted checks** (43,337 existing plus 932,360 added). The final offline build compiles **55 Java files**: 25 complete production files, three extracted-production units, 21 explicitly labeled test doubles and six test/benchmark files. The extracted units contain complete classes/methods including the actual native DISTINCT tables, CopyBinding and the SyntheticValueSource ingress/boundary paths.

**This is not a full RDF4J build or a production-release certification.** In particular the whole query planner, generated Janino source, model implementations, LMDB I/O and complete `LmdbNativeValueCodec` are not dependency-compiled by this offline harness. Its DecodedValue and SimpleValueFactory are explicit doubles. The changed plain-string admission method was checked against its actual fields and syntax parsed; the full-repository test exercises it using the actual model, but that JUnit test has not been compiled/executed here. No original end-to-end query benchmark or AArch64 benchmark has been run.

## 1. Remove per-value duplication

`NativeRuntimeValueTable` replaces the two runtime maps and their structural keys/boxed IDs/wrappers. `NativeGeneratedKeyAuthority` no longer owns a second Value-to-representative map and a separate normal-case canonical-key cache. Ordinary and generated runtime interning share this table, preserving exact-ID interop in both directions. Store-facing lookup itself is unchanged.

A hash slot is one long: cached 32-bit hash plus an unsigned ordinal-plus-one. Stable payload pages contain the original Value reference and one metadata long. The metadata word publishes the canonical term ordinal and unresolved-membership flag together. Most runtime ordinals are dense; exceptionally sparse/high ordinals retain a map fallback. Full dictionary IDs are never narrowed into the ordinal space. An allocated numeric interval is never used as an ownership proof.

Exact spelling and term equality remain distinct. Equal language-tag case variants may share a canonical key while keeping different payload IDs and their original output spelling. Triples preserve this recursively. Datatypes, lexical forms, base directions and node kinds are not collapsed. Hash collisions only select candidates; an exact spelling/term comparison determines identity. The table's exact-spelling match runs on duplicates, while semantic coalescing runs only when actually inserting a new spelling.

The new ordinal limit is `0xffff_fffe` entries in a runtime-ID space; it fails before truncation. Dense payload pages cover the first 2^24 ordinals; sparse/high ordinals use an explicitly charged exceptional map. These are query-local runtime ordinals, not global store cardinality or stored dictionary IDs.

## 2. Probe generated xsd:string labels before allocating Literals

The actual SyntheticValueSource decoded-value overload recognizes an exact xsd:string result of an admitted CopyBinding and probes the table by its already-produced String. A duplicate does not need a Literal object. A miss materializes a Literal once and uses the same table/IDs as Value ingress.

This does not eliminate producing or hashing the String, the expression's decoded carrier, or resolving input values. Already-materialized values still use the general Value path. Language-tagged, typed, directional, resource and triple results keep their existing conversion path. The shortcut is also disabled when query-scoped authoritative Values exist, preserving the previous representative protocol. On a hardened hash stripe the string path conservatively uses the common Value implementation and can again allocate a duplicate Literal.

The precise allocation regression test uses the real CopyBinding and SyntheticValueSource methods with an instrumented factory double: one Literal construction for 25,001 repeated decoded inputs. It is not a claim about total allocations for the full application query.

## 3. Canonical probe keys, original output payloads

`KernelRuntime.LongIntMap` now normalizes a key once per operation when its hooks support exact canonical keys. Its probe array stores those canonical longs; its pre-existing ordinal-to-key array retains the first original payload. Probes and growth use primitive equality/hashing without repeatedly invoking normalization or sameRdfTerm on occupied buckets. No additional per-entry array was introduced.

Noncanonical hooks retain the previous semantic path. Canonical zero is handled as a real key even when the original input ID is nonzero; the original zero ID can conversely normalize to a nonzero key. Regression hooks deliberately throw if canonical probing tries a second normalization or semantic comparison.

Native DISTINCT and the shared/spilling group components benefit from the compact authority without being replaced by a separate execution strategy. Generated-key provenance/admission rules are unchanged. Mixed-provenance and storage-facing consumers continue through their existing authority; this revision does not speculate that a generated term is absent from LMDB.

## 4. Publication, synchronization and closing

Duplicate probes acquire a published hash slot but acquire no monitor. A new spelling locks one of 16 hash stripes, rechecks the observed descriptor and empty terminator, and reuses the probe when still valid. Normal growth redistributes cached hashes, without rereading every payload. There is no claim that unique insertion is lock-free: memory admission, allocation and counters are shared, and resizing is serialized within a stripe.

An acquired entry already publishes its Value, so internal payload access no longer repeats a metadata ownership read. External ID lookup still uses the published metadata word. This follows VarHandle's release/acquire ordering contract [3]. Page directories and replaced indexes are published rather than modified destructively under readers. Java bounds checks remain.

Runtime-table/budget initialization uses a dedicated lock, not the context-close monitor. This prevents a nativeState factory holding a ConcurrentHashMap bin from waiting on that monitor while close waits to clear the map. The test proves nativeState construction can complete while another thread holds the context monitor. Eight-writer publication/resize tests and 20 concurrent-close rounds also pass. These are stress/contract tests, not a formal concurrency proof or measured parallel speedup.

Closing one generated authority does not close the shared runtime table. The execution context owns the table; nested contexts retain distinct ID spaces but share the query admission budget. Cross-authority imports still obey ownership rules. Closing the root prevents new child runtime initialization.

## 5. Collision policy: an important rejected implementation

The first compact-table version triggered expensive seeded content hashing after a long run of occupied slots. That was wrong as a performance heuristic: ordinary 75%-full tables have clusters. The normal 8,192-term, two-language-case diagnostic hardened 4 of 16 stripes before aliases and 11 after aliases. This caused a material regression.

The retained policy distinguishes identical full hashes from ordinary occupied buckets: 48 full-hash candidates trigger hardening, while a much longer 512-slot chain remains a defensive trigger. Normal test input now hardens **zero** stripes. Seeded content hashing is independent of the Java Aa/BB polynomial collision family, includes semantic term fields, and retains exact equality checks. It is not a cryptographic/DoS-resistance claim.

A 4,096-probe guard explicitly refuses pathological residual chains rather than permitting unlimited work. In particular, thousands of output spellings of the same case-insensitive language term cannot be dispersed by a semantic hash. The guard fails the query without silently dropping rows or treating colliding terms as equal. The test covers that refusal and verifies the prior representative remains intact.

Historical table snapshots and reverse patches are in `experiments/`. They are rejected/earlier implementations for inspection and controlled experiments, not alternatives recommended for production. The final measurements below use one frozen production tree; earlier logs are screening evidence only.

## 6. Final paired component benchmarks

JDK: supplied Zulu 26.0.2.1+1, Linux x86-64, AMD EPYC 9V74 virtual machine (five exposed vCPUs, cgroup quota four CPUs). Flags: `-ea -Xms256m -Xmx2g`. Normal tiered compilation; no forced inlining, -Xcomp or threshold changes. No CPU pinning. Timing was run separately from C2 printing and object-graph inspection.

Each case uses 131,072 input rows, a new evaluation per pass, ten warmup passes and nine measured passes. Five independent JVM forks per side per case, with side order alternated between forks: **120 timed JVM executions**. Tables and the initial context/authority are constructed before the timer; growth, new retained entries, fresh Literal objects, canonical keying and insertion are timed. Context/authority closing is outside the timer. Each pass verifies exact cardinality/checksum and forbidden dictionary activity.

Both sides use the same explicit factory/model/decoded-value doubles and unchanged benchmark source. The factory's text hash follows the actual SimpleLiteral lexical-only hash [4], rather than the old harness's allocating Objects.hash varargs implementation. Labels are precomputed; input reads, string transformation and LMDB/overlay latency are excluded. This improves comparability but does not establish application-level times or literal object sizes.

`value` = Value ingress plus actual LongIntMap grouping. `decoded` = actual CopyBinding + synthetic decoded ingress plus LongIntMap (decoded carrier/factory are doubles). `distinct` = Value ingress plus actual NativeDistinctTracker. `language` = two retained language spellings per semantic term. `store` = ordinary store-first control with an immediate-return unknown oracle, not LMDB. `collision` = deliberately colliding Java strings, not a representative throughput workload.

Medians of per-fork medians:

| Operation | Unique terms | Before ns/row | After ns/row | Before / after | Before / after allocated bytes per row |
|---|---:|---:|---:|---:|---:|
| value | 64 | 28.932 | 26.908 | 1.08x | 16.236 / 16.152 |
| value | 8,192 | 58.308 | 36.806 | 1.58x | 39.538 / 22.748 |
| value | 131,072 | 558.546 | 133.163 | 4.19x | 395.530 / 124.078 |
| decoded | 64 | 22.751 | 24.747 | 0.92x | 16.238 / 1.660 |
| decoded | 8,192 | 63.725 | 38.016 | 1.68x | 39.542 / 7.748 |
| decoded | 131,072 | 505.346 | 146.269 | 3.45x | 395.618 / 124.078 |
| language | 8,192 | 101.329 | 72.601 | 1.40x | 99.001 / 49.507 |
| distinct | 64 | 29.225 | 31.562 | 0.93x | 16.214 / 16.128 |
| distinct | 8,192 | 62.329 | 47.925 | 1.30x | 38.206 / 21.419 |
| distinct | 131,072 | 457.424 | 134.466 | 3.40x | 375.501 / 103.999 |
| store | 8,192 | 208.275 | 92.767 | 2.25x | 188.734 / 72.385 |
| collision | 1,024 | 3314.594 | 110.878 | 29.89x | 20.124 / 16.843 |

The all-unique grouping path is 4.19x faster in this experiment; native DISTINCT is 3.40x faster. Small duplicate-dominated cases are not uniformly improved: decoded ingress at cardinality 64 is about 8.8% slower and native DISTINCT at 64 is about 8.0% slower in median elapsed time. Low-cardinality fork variability is large. The second pass does not claim a universal win.

Generated cases perform zero membership lookups and zero dictionary value reads in both versions. The store-first control makes 75,270 membership-oracle calls per pass in both versions; its speedup does not represent eliminating those calls. Local mode already avoided them before this revision.

All per-fork medians (no discarded outliers):

| Operation / cardinality | Baseline fork medians (ns/row) | Optimized fork medians (ns/row) |
|---|---|---|
| value / 64 | 31.617, 50.026, 28.932, 20.837, 17.958 | 20.231, 30.735, 26.908, 27.824, 20.997 |
| value / 8,192 | 57.995, 59.154, 59.435, 58.308, 54.641 | 36.775, 38.797, 36.534, 37.582, 36.806 |
| value / 131,072 | 558.546, 516.43, 569.904, 580.262, 516.411 | 128.446, 131.604, 135.029, 133.163, 134.398 |
| decoded / 64 | 25.859, 21.55, 22.056, 22.751, 45.18 | 28.512, 22.378, 24.747, 25.258, 24.665 |
| decoded / 8,192 | 95.806, 63.725, 63.223, 58.635, 66.024 | 38.5, 49.056, 38.016, 37.478, 37.126 |
| decoded / 131,072 | 508.21, 505.346, 536.435, 473.668, 505.339 | 134.442, 146.269, 153.412, 141.855, 191.667 |
| language / 8,192 | 95.099, 101.329, 100.188, 102.748, 103.602 | 72.601, 71.867, 76.38, 75.062, 72.416 |
| distinct / 64 | 38.806, 29.225, 27.948, 29.214, 38.656 | 31.562, 30.082, 36.4, 31.546, 35.951 |
| distinct / 8,192 | 62.329, 58.049, 69.892, 60.599, 64.557 | 47.925, 48.484, 47.548, 48.902, 47.612 |
| distinct / 131,072 | 484.65, 479.797, 457.424, 430.199, 394.635 | 134.466, 140.591, 131.126, 135.501, 128.657 |
| store / 8,192 | 185.89, 203.445, 208.275, 212.451, 219.899 | 90.41, 97.652, 84.314, 102.837, 92.767 |
| collision / 1,024 | 3394.44, 3237.501, 3268.94, 3314.594, 3328.442 | 115.691, 115.073, 110.878, 108.633, 109.383 |

Allocated bytes use ThreadMXBean for the measuring thread. These include harness model objects and table growth but not externally precomputed labels. The small decoded case varies between scalar-replaced and allocated carrier behavior (1.660 to 24.160 bytes/row across final forks); duplicate Literal-construction elimination does not guarantee zero total allocation. Native-memory allocation and RSS were not measured by this counter. These are exploratory standalone measurements, not JMH confidence intervals or full SPARQL results.

## 7. Retained footprint

The supplied javaagent performs identity-deduplicated traversal of objects reachable from the authority (and optionally the actual group map), summing Instrumentation.getObjectSize. Static graphs/VM globals are not recursively traversed; model Values and retained Strings are counted. The model objects are offline doubles, not actual production SimpleLiteral instances. This is a JVM-layout-specific reachable footprint, not RSS or a complete production query heap.

At 131,072 unique strings:

| Reachable graph | Before bytes | After bytes |
|---|---:|---:|
| Key authority/context including payloads | 46,141,336 | 14,165,840 |
| Above plus actual group map | 50,335,776 | 18,360,280 |
| Key state excluding model objects/String/byte arrays | 35,655,448 | 3,680,080 |

Including payloads this is about **69.3% less**; the separated key-state component is about **89.7% less** (roughly 272 versus 28 bytes per unique key on this layout). The latter isolates the duplicated nodes, keys and wrappers rather than pretending the factory double is a production memory measurement. Per-class counts are in the evidence. The baseline retains 131,072 structural NativeValueKeys, 131,072 representatives, 131,072 runtime wrappers, 262,144 boxed Longs and 393,216 CHM nodes. The optimized generated-only graph retains none of these per-term objects.

There is a small fixed-cost tradeoff: an empty generated authority grows from 1,256 to 1,784 reachable bytes. At 64 terms it shrinks from 31,664 to 23,696; at 8,192 terms from 2,885,176 to 887,600. No persistent LMDB data, value-overlay pages or reverse-index entries are added.

## 8. Memory admission and operational behavior

New property: `rdf4j.lmdb.runtimeValues.maxBytes`, a positive decimal byte count. Default is `max(16 MiB, min(512 MiB, maxHeap / 8))`, evaluated when a query first creates its runtime table. For the benchmark's 2 GiB heap this is 256 MiB. An explicit example is `-Drdf4j.lmdb.runtimeValues.maxBytes=268435456`.

It is a **shared admission account for runtime-value tables under one query owner**, covering ordinary and generated runtime values, not a process-wide or full-query RSS limit. Standard Value payloads are conservatively estimated, and arrays/directories are explicitly charged. Shared datatype/text references may be charged more than once. Arbitrary custom Values can retain additional object graphs not modeled by the estimate. Existing group/sort/spill budgets and other query allocations remain separate. Old arrays can remain transiently reachable until readers/GC finish after replacement; accounting release is not synchronous physical deallocation.

Admission failure throws QueryEvaluationException instead of evicting live identities, dropping a grouping key, or pretending a digest is identity. Limits for pathological hash work and excessive triple nesting (256 levels) likewise fail explicitly. No dictionary fallback is introduced to work around these limits.

**Generated payload spilling is still not implemented.** Spilling the primitive group table does not make this interner unbounded/disk-backed. High-cardinality long-string workloads may require a larger runtime-values budget or a future payload-spill implementation. This is an outstanding production-scale capability, not something silently hidden by the memory cap. An end-to-end JUnit test is supplied to verify that resource refusal propagates as a failed query rather than an unbound BIND; it is not executed by the offline harness.

`rdf4j.lmdb.generatedKeys.enabled` retains its previous meaning and defaults to true. Setting it false disables generated-only admission, but does not revert the shared runtime-table implementation for ordinary runtime IDs. Compare the baseline source tree to isolate this implementation change. No extra overlay capacity or reverseSlots setting is needed.

## 9. Actual final C2 evidence

Print captures cover Value, decoded and language profiles. Every byte of each C2 main-code range is checked before objdump disassembly; the logs and binaries are supplied. Code is not inferred from Java source. The final source hashes match the measurement/build manifests.

In the Value profile, the last captured LongIntMap.getOrInsert main code falls from **4,192 to 1,832 bytes**: canonical normalization is outside the collision loop, and probe/growth use canonical primitive keys. That is supporting structural evidence, not an instruction-latency estimate.

The new publishedValue helper is 368 main-code bytes; its normal dense path loads a page and a Value without a second metadata ownership probe. External metadata lookup remains 384 bytes and performs the publication read. Release/acquire semantics are retained; on this x86-64 capture the ordinary reads are loads, but that says nothing about which instructions AArch64 will emit.

Not every function gets smaller. The last generated-authority canonicalTermKey capture is 864 bytes versus 680 before, and the interner now distributes code between a small authority wrapper and table helpers. Comparing only the wrapper would be misleading. Full before/after listings and every recompilation are retained, including cold and exceptional paths.

## 10. Verification scope and reproduction

Coverage includes 180,000 seeded reference-map operations, exact and semantic spelling identity, datatypes, direction, nested triples, Java-hash collisions, Unicode case folding, ordinary/runtime interop, decoded ingress, canonical zero, native scalar/batched DISTINCT, IR group maps, real bounded count-group spilling/merging, scope imports, sparse/full-width IDs, ordinal refusal, nested admission accounting, factory lock ordering, concurrent publication/resize and concurrent closing. The backing oracle throws on dictionary lookup/read in the generated tests.

Run from the updated source root:

```bash
export JAVA_HOME=/path/to/jdk-26
python3 tools/generated-keys/run.py --out /tmp/generated-keys-tests
python3 tools/generated-keys/compare.py   --baseline /path/to/extracted/generated-value-keys-source   --out /tmp/generated-keys-comparison --forks 5 --bench --memory --c2
```

The runner deletes only its marked disposable build directory. Do not install any `stubs/` class into the application. Full-repository JUnit source is under `tools/generated-keys/integration/`; copy it to the matching LMDB test package and run the branch's normal build. It adds actual-model compact-string tests, conversion-shape checks and budget-failure propagation to the existing generic/native/interpreted/compiled differential coverage.

Production changes are restricted to six files: NativeRuntimeValueTable (new), NativeExecutionContext, NativeGeneratedKeyAuthority, KernelRuntime, LmdbNativeValueCodec and LmdbSyntheticValueSource. No producer-proof broadening or new isolated query strategy is included.

Before production adoption, the full branch build, actual model/JMH workload, generated Janino execution, original query/dataset and target architecture still need validation. Multiwriter tests do not prove universal concurrency safety, and per-query admission does not protect against all server-wide concurrent memory demand. The measured improvements are established for the stated components and synthetic workloads only.

## Primary references

1. Fastutil's primitive open-addressed maps (separate primitive arrays and load-bounded growth): https://raw.githubusercontent.com/vigna/fastutil/master/drv/OpenHashMap.drv
2. DuckDB's grouped aggregate hash-table design (separate lookup metadata and payload): https://duckdb.org/2022/03/07/aggregate-hashtable
3. Java SE 26 VarHandle release/acquire contract: https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/lang/invoke/VarHandle.html
4. RDF4J SimpleLiteral's actual lexical-only hash and exact literal equality: https://raw.githubusercontent.com/eclipse-rdf4j/rdf4j/main/core/model/src/main/java/org/eclipse/rdf4j/model/impl/SimpleLiteral.java

These informed the design and benchmark model. The implementation is independently written; no third-party hash-map source is bundled as a dependency.
