# Generated-value keys: implementation and evidence

## Scope and baseline

This patch is against `value-overlay-c2-source.zip`, the latest optimized overlay snapshot. It changes generated-value ingress and terminal DISTINCT/GROUP BY identity; it does not change value-overlay formats, retained-memory budgets, reverse indexes, or LMDB storage. All files from that source snapshot are retained.

The previous path asked the value resolver whether each computed term existed in the store, then potentially asked again when canonicalizing a synthetic ID for a key table. A bounded resolver cache reduced some calls, but high-cardinality generated values could churn the cache. A terminal whose keys are generated does not need an answer to this membership question.

The delivered path hashes the already-created RDF Value, confirms equality after hash collisions, and gives its equivalence class an evaluation-local key. The same primitive hash/group/spill operators consume these keys. The key is NOT a hash digest used as identity and NOT a claimed LMDB term ID. No persistent membership lookup is performed during admitted generated ingress or its subsequent key normalization.

## Two separate identity contracts

`NativeGeneratedKeyAuthority` is a terminal-only authority. `NativeTermAuthority` remains storage-facing for general resolution, joins, comparison expressions and probes. `RowState.keyAuthority()` and `KernelHooks.keySemantics()` make the distinction explicit.

On ingress, a concurrent exact term map (`Value` hash plus `Value.equals`) selects a semantic representative. Matching exact spelling reuses its original local ID immediately. New spellings enter the existing evaluation-local interner without consulting the store or compiled-constant catalog. `NativeValueKey` preserves lexical form, datatype, language spelling, base direction and nested triple spelling. RDF-equal spelling variants can therefore share a grouping key without replacing the Value that an original result row returns.

The canonical-ID memoization is a primitive paged `long` directory rather than a boxed ID-to-ID map for ordinary dense runtime ordinals. A populated cell proves membership in this authority; a numeric interval alone never proves ownership, identity or database absence. A bounded directory covers the first 2^24 ordinals; larger/sparse/exceptional IDs use an exact map. Directory/page/cell publication uses release/acquire access. The defensive exceptional path can decode a stored term to compare content, but never performs value-to-ID membership resolution. The automatic planner proof avoids that stored-content path for the intended all-generated keys.

Dictionary-free keying is not dictionary-free query execution. An expression such as LCASE must still read its operand, and the query must still scan or join its source data. The optimization removes resolution of the expression's newly produced result for key identity.

## Planner admission: proof, not sampling

`NativeGeneratedKeyPlan` inspects the physical producer suffix once. It marks the specific `CopyBinding` assignment objects whose outputs can use local identity. The proof is immutable and attaches to an evaluation, not mutable global plan flags. Both UNION arms must prove the property. First-row/first-batch observations are never used to assume the rest of a stream is generated.

Supported admission includes computed or semantic BIND/SELECT-expression outputs, generated group-key tuples, generated aggregate DISTINCT arguments, simple projection aliases and independent filters with known dependencies. A GROUP BY plan additionally requires every DISTINCT aggregate channel to have generated keys; ordinary non-DISTINCT aggregate inputs can remain stored IDs. Independent semantic-expression dependency metadata is separate from generic BIND argument lowering, so its prior required-mask/reorderability behavior is not changed.

Admission deliberately declines mixed generated/stored key tuples, mixed-provenance UNION arms, entry-bound/rebound targets, term-checked assignments, joins/OPTIONAL/opaque operators between the assignment and terminal, and expressions or filters that consume an unresolved selected assignment. A multi-stage expression that computes one terminal key from another can therefore miss the optimization in this revision. Inline/constant-only keys retain their already-cheap existing path. This is a conservative sufficient proof, not recognition of every logically possible generated-only query.

The key scope does not mark the whole upstream pipeline as generated-only. Storage-facing hooks remain ordinary even when a terminal has a local key scope. Child evaluations do not borrow an active unresolved-key context across native operator boundaries; they preserve the outer query-scope owner for query-scoped values. An unresolved runtime ID is explicitly distinct from a value proved absent from the store. Probe classification returns VALUE_GUARDED_PROBE for such IDs; direct raw probes fail closed if an unresolved terminal ID escapes the admitted suffix. This guard is not an implementation of arbitrary generated joins.

The default-on switch is `rdf4j.lmdb.generatedKeys.enabled`. Set it to `false` before evaluation to retain the store-first path. No reverse-index enlargement or overlay-budget change enables this feature. `NativeGeneratedKeyPlan.ACTIVATIONS` counts activated evaluations once per scope, not per row. It is a package-local diagnostic, not a row-level hit counter.

## Native and IR integration

The ordinary row DISTINCT trackers and sorted-row DISTINCT use key authority. Native group tuples normalize hashing/equality but retain original payload IDs; generated single-column grouping uses that exact tuple path instead of the raw single-ID map. Aggregate DISTINCT sets and bounded count-group storage use the same canonical keys.

Interpreted IR, emitted Janino IR and `KernelGroupSink` receive a separate key-hooks adapter for their terminal tables. The enclosing kernel retains original hooks for expressions, ordering and probes. BIND lowering now retains `CopyBinding` identity instead of discarding it for only the evaluator; this lets the exact same admission proof reach a compiled or interpreted assignment. Ordinary key-hook behavior defaults to the existing hooks, and the raw default of ordinary row dedup remains unchanged when no generated-key scope is active.

This does not introduce another mutually exclusive query strategy. Existing native cursors, primitive tuple/hash tables, generated/interpreted IR and spill/sort boundaries share the local identity contract. Computed aggregate plans already use the serial dispatch restrictions in the baseline; this patch does not advertise new parallel aggregate execution. The local interner/cache is thread-safe and independently stress-tested.

## Executed validation

`tools/generated-keys/run.py` performs an explicit offline compilation and regression run with JDK 25 or newer. The harness accepts both the repository source layout (`core/sail/lmdb/src/main/java/`) and the legacy standalone `java/` layout through one resolver. It compiles complete production key, resolver, context, plan, primitive kernel-table, bounded count-store, grouping-sink, ordering-sink, spill/sort and memory-manager files. It also extracts complete production CopyBinding and tuple/DISTINCT classes and the exact SyntheticValueSource ingress/scope/guard methods. Missing RDF model, plan-shell, query-source and other external APIs are explicit test doubles, including the context-aware evaluator overload used by the copied production code. `KernelPeerCancelledException` is copied from production when present and falls back to a labeled double for older source bundles. `build-manifest.json` identifies every source, origin and hash. The harness currently compiles 57 Java files in total.

**Historical offline snapshot:** the original regression run passed **43,337 counted checks**. Tests include generated results that already exist in the fake dictionary and ones absent from it; forbidden membership and value-read calls; adversarial equal hashes; exact duplicate reuse; language case and output spelling; datatype/lexical distinctions; base direction; nested triples; unbound/error outputs; composite keys; forced all-bucket collisions; table growth; scalar/batch DISTINCT; real primitive IR group maps and sets; real bounded spill/sort/merge counts and COUNT DISTINCT; representative preservation; concurrent publication; page and 2^24 directory boundaries; foreign-key import; close/new evaluation; ordinary store-first/probe behavior. The real KernelGroupSink test makes the ordinary key proof and normalizer throw, verifying use of keySemantics at that boundary.

**Historical offline snapshot:** all 20 changed/new production Java files plus the full-repository test file were syntax-parsed as complete compilation units (21 files). Syntax parsing is not full typechecking.

The full-repository `GeneratedValueKeysIntegrationTest` has since been compiled and passed in the normal LMDB module test tree: 11 tests, 0 failures, 0 errors and 0 skips. It uses the actual RDF4J model, LMDB store and native query path. Its differential matrix exercises `GENERIC`, `STORE_FIRST`, `LOCAL_NATIVE`, `LOCAL_INTERPRETED` and `LOCAL_COMPILED`; every non-generic mode is run twice against the generic bag result. The focused activation test also observes `NativeGeneratedKeyPlan.ACTIVATIONS` increasing for an eligible local-native query. The six-class native evidence report is retained at `logs/review-20260917/r5-native-all-six-green/REPORT.md`.

**Still not executed:** the full RDF4J repository test suite, the user's ThemeQueryBenchmark, the original application query/dataset workload, or AArch64 profiling. The offline harness continues to use explicit test doubles and remains separate from the actual repository integration selector. The current 11-test result compares the configured `LOCAL_COMPILED` mode and the other listed modes against the generic result, and proves one eligible `LOCAL_NATIVE` activation; it does not prove Janino or native activation for every configured mode or query shape.

## Key-path micro-experiment

Five independent JVM forks, five warmup rounds and seven measured rounds per case, JDK 26.0.2.1 x86-64. Each measured pass constructs 131,072 fresh literal objects and inserts keys into the actual primitive KernelRuntime.LongIntMap. The strings backing the labels are prepared before timing. The control is the existing store-first resolver and canonicalizer in the same patched tree, not an execution of the original complete application. Its empty oracle merely increments a counter and returns UNKNOWN; it performs no I/O, encodes no LMDB key and models no LMDB/overlay latency. Generated-local takes the new ingress and local canonical-key path. The timed interval includes lazy key/resolver allocations but excludes context creation before timing and close afterwards. The test is not JMH.

Medians of five per-fork medians:

| Unique terms | Store-first ns/row | Local ns/row | Store-first membership calls/pass | Local calls/pass | Store-first allocated bytes/row | Local allocated bytes/row |
|---:|---:|---:|---:|---:|---:|---:|
| 64 | 45.644 | 31.519 | 64 | 0 | 104.429 | 64.270 |
| 8,192 | 206.115 | 101.162 | 76,266 | 0 | 208.988 | 91.560 |
| 131,072 | 1561.184 | 778.365 | 243,351 | 0 | 981.457 | 503.703 |

The greater-than-input membership count in the all-unique control comes from separate ingress and canonicalization work plus bounded-cache behavior, not simulated disk reads. Local key work makes zero membership and zero dictionary value-read calls in every measured case. Five-fork timing ranges and all logs are in `benchmark-summary.json`. There is substantial VM/GC/host variability, especially for the all-unique case; one final all-unique fork is slower with the new path. These measurements support the call-elimination and allocation claims, not an end-to-end query speedup or a universal speedup for every key distribution.

## Assembly inspection and optimization iteration

The first local implementation used a boxed concurrent ID-to-key memoization map. The retained implementation uses a bounded paged primitive cache and checks an exact-spelling representative before allocating a structural spelling key. It shares the newly captured spelling key with the runtime interner on the miss path. These changes remove boxing on common terminal canonicalization and reduce repeated structural-key construction. Historical first-pass evidence is separated from final measurements.

Actual final C2 machine bytes were captured with normal tiered compilation on the supplied JDK, not inferred from source. The included disassembler verifies complete byte coverage before decoding with objdump. `canonicalTermKey` has 680 main-code bytes; `cached` has 360 in the captured normal C2 compilation. The populated-cache fast path has ordinal arithmetic, directory/page bounds and loads, then returns the primitive canonical key. It has no dictionary invocation or per-hit key allocation. It still has bounds checks and cold exceptional/uncommon-trap paths. Acquire loads compile to ordinary loads on this x86-64 capture; that is not evidence of identical AArch64 code. The distinct Value map remains a hash table with real equality checks and synchronized admission on misses, not a branchless universal string hash.

Capture reproduction (after the offline build):

```bash
"$JAVA_HOME/bin/java" -Xms256m -Xmx2g -XX:+UnlockDiagnosticVMOptions \
  '-XX:CompileCommand=print,org.eclipse.rdf4j.sail.lmdb.evaluation.NativeGeneratedKeyAuthority::canonicalTermKey' \
  '-XX:CompileCommand=print,org.eclipse.rdf4j.sail.lmdb.evaluation.NativeGeneratedKeyAuthority::cached' \
  '-XX:CompileCommand=print,org.eclipse.rdf4j.sail.lmdb.evaluation.NativeGeneratedKeyAuthority::intern' \
  -cp /tmp/generated-keys/build/classes \
  org.eclipse.rdf4j.sail.lmdb.evaluation.GeneratedKeyBenchmark > /tmp/generated-keys/c2.log 2>&1
python3 tools/value-overlay/disassemble.py /tmp/generated-keys/c2.log /tmp/generated-keys/assembly
```

## Memory and limitations

No persistent bytes or overlay indexes are added. There is evaluation-local memory proportional to unique generated RDF terms and retained spelling variants: Values/spelling keys, interner entries, semantic representatives and canonical-key cache. The paged cache charges about eight payload bytes per dense ordinal plus page/directory overhead; the interner and maps cost more than that. A RuntimeValue wrapper now records unresolved membership in the existing runtime context; even feature-disabled ordinary runtime interning can pay that small metadata-allocation cost. Pure stored IDs do not enter that runtime interner.

Primitive group spilling bounds its existing group/sort workspace, NOT the query-local Value interner. This revision does not implement spilling of generated string payloads, an overall bounded-memory DISTINCT, or zero-copy hashing of an unevaluated expression. High-cardinality keys can still use substantial heap. The allocation measurements are bytes allocated during the kernel test, not retained heap/RSS measurements.

Mixed sources, semantic comparisons, joins and raw storage probes continue to need their existing storage-facing identity path. All-generated keying works regardless of whether equal terms also happen to exist elsewhere in LMDB: existence is irrelevant unless a consumer asks a storage-facing question.

## Reproduction and deployment

Apply `generated-value-keys.patch` to the source snapshot it names. The production-only patch is an alternative that omits tools/docs; do not apply both. The cumulative patch, when supplied, is against the original judges' archive, not the latest C2 tree. The runner accepts either the repository source layout or a legacy root containing `java/`.

```bash
git apply --check generated-value-keys.patch
git apply generated-value-keys.patch
export JAVA_HOME=/path/to/jdk-25-or-newer
python3 tools/generated-keys/run.py --out /tmp/generated-keys --bench --forks 5
```

The runner refuses to replace an existing unmarked build directory. It writes an explicitly disposable build under the chosen output path.

The full integration test is part of the normal LMDB module test source tree. Run it with the branch's required JDK and installed dependencies:

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py GeneratedValueKeysIntegrationTest
```

The verification manifest records fresh patch application, byte comparison against the complete source ZIP, and rerunning the offline tests from that freshly patched tree. The offline runner and the normal LMDB module test remain separate checks: the former uses explicit doubles, while the latter uses actual RDF4J dependencies.

## Primary references

The implementation preserves the existing RDF-term key semantics rather than replacing equality with digest equality. SPARQL's DISTINCT and grouping algebra provide the semantic contract: https://www.w3.org/TR/sparql11-query/ .

DuckDB's implementation discussion describes separating hash-table probing from exact group-key equality and payload storage; this is related design guidance, not a copied implementation: https://duckdb.org/2022/03/07/aggregate-hashtable .
