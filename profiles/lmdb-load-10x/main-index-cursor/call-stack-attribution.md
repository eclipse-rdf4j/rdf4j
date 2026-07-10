# Post-cursor call-stack attribution and lower bounds

Date: 2026-07-10

Fresh async-profiler CPU JFR captures were taken at commit `334ddc5246` plus evidence-only commits, with `automaticEvaluationStrategy=false`, JDK 26, G1, a 2 GiB heap, two one-second warmups, four one-second measurements, and the exact `loadDatagovFileSingleTransaction$` selector. The repository wrapper could not launch on macOS Bash 3 because two empty arrays are expanded under `set -u`; the exact underlying async-profiler agent command was therefore run directly against the existing benchmark jar.

The profiled scores were 766.466 ms/op for `NONE` and 796.061 ms/op for `READ_COMMITTED`. These runs are for attribution, not the acceptance comparison, because profiling and short warmup affect the absolute score.

Async-profiler JFRs were converted to collapsed stacks with:

```text
jfrconv --cpu --simple -o collapsed <capture.jfr> <capture.collapsed>
```

Inclusive sample attribution:

| Path | NONE | READ_COMMITTED |
| --- | ---: | ---: |
| `TripleStore.storeTriplesAligned` | 34.23% | 30.25% |
| `ValueStore` | 19.52% | 19.71% |
| Sail source ingestion | 29.04% | 29.97% |
| Native LMDB, all callers | 36.58% | 32.73% |
| Native LMDB below aligned triple writes | 25.42% | 21.80% |
| Native LMDB below value storage | 9.98% | 9.90% |
| Leading-field radix sorting | 4.67% | 4.28% |
| Flush | 7.20% | 6.37% |
| Transaction commit | 0.98% | 0.87% |

`NONE` spends 18.46% in batched `ValueStore.storeValues`; `READ_COMMITTED` spends 18.86% in scalar `ValueStore.getId`. Their total value-resolution cost is nevertheless nearly identical. The four-index packed-block probe can remove most of the triple-index native share, but Amdahl's law limits that change alone to far less than 10x.

## Pure Java model and value-resolution floor

A scratch JDK 26 probe parsed the benchmark model once, then timed reusable-array passes over all 613,157 statements. A plain scan that calls each component's `hashCode()` averaged 19.429 ms. Building a transaction-local equality-based `ObjectLongHashMap<Value>` and resolving the four IDs averaged 124.345 ms; looking up all IDs in an already populated map still averaged 106.831 ms.

An identity-keyed primitive map was faster at 46.304 ms to build and 42.238 ms to reuse, but it produced 1,226,197 IDs for only 179,732 equal values. The model therefore creates or exposes many equal-but-not-identical values, making identity caching incorrect. A correct fast path needs a two-pass collision-aware content-hash partition or another way to resolve each distinct equality value once rather than once per statement occurrence.

## Repository lifecycle floor

A second scratch probe ran fresh directory creation, LMDB repository initialization, begin/commit, `hasStatement`, connection close, repository shutdown, and directory deletion with no statements. After ten warmups, both isolation modes were generally 2.8-3.8 ms per complete lifecycle. Repository setup and cleanup therefore leave roughly 74-80 ms of the acceptance threshold available for model traversal, value persistence, four indexes, and transaction bookkeeping; lifecycle is not the obstacle.

## Implication

The measured path has three independent costs of comparable size. Reaching 10x requires a bulk-ingestion entry point that avoids per-statement Sail changeset work, resolves distinct values through a collision-safe two-pass scheme, and persists sorted transaction segments as page-sized packed blocks. Optimizing any one of those layers in isolation cannot satisfy both thresholds.

## Rejected public-hash partition

A follow-up scratch probe attempted the proposed two-pass resolver using `Model.subjects()`, `predicates()`, `objects()`, and `contexts()` to enumerate distinct terms, group them by `Value.hashCode()`, record unequal collisions, and then skip equality checks for collision-free statement components. It was both collision-heavy and slow:

```text
values:             179,732
collision hashes:     2,517
collision values:     2,659
distinct build:      129-137 ms (stable early rounds)
statement resolve:    89-103 ms (stable early rounds)
combined:            219-236 ms
```

The distinct-view calls force `DynamicModel` to upgrade to `LinkedHashModel`; later rounds rose to 448-481 ms under allocation/GC pressure. The high collision count also disproves the assumption that RDF4J's public 32-bit equality hash is nearly unique across term kinds.

After the upgrade, statement components are canonical object identities (179,732 identities for 179,732 equal values), but the indexed model's iterator and node indirections are slower: scans rose from 19 ms to 43-51 ms and identity resolution took 62-81 ms. Forcing an upgrade during loading would therefore be a benchmark-warmup artifact, not a real ingestion improvement. The next candidate must preserve the lightweight map-backed iteration while arranging canonical term identities during model construction or source parsing.
