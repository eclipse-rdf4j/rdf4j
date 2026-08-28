# Implement paged CSF direct adjacency for LMDB

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must stay current while implementation proceeds. Maintain this document according to
`.agent/PLANS.md`.

## Purpose / Big Picture

RDF4J's LMDB direct-adjacency accelerator currently stores its immutable base as legacy encoded runs in native
arenas. This change introduces a compact sparse-fibre (CSF) representation: immutable native pages group rows by
predicate and direction, factor repeated neighbor and context data, and keep update overlays above the immutable
base. Bound-predicate queries use the paged base directly. Queries that need to enumerate every predicate for a
node remain correct by falling back to authoritative LMDB because the compact base deliberately omits that reverse
locator.

After completion, paged CSF is the default base. Setting
`-Dremoved direct-adjacency rollback selector=true` selects the prior format. Consolidation rewrites only
affected CSF shards and retains untouched shards for pinned snapshots. Tests demonstrate exact query parity,
revision behavior, and memory-account cleanup; matched JMH and sizing probes provide evidence before making
performance claims.

## Progress

- [x] (2026-08-01 11:11Z) Inspected the August 1 archive against commit `321cc04eca` and current HEAD; identified
  the source delta, stale artifacts, missing ownership APIs, and the newer `LmdbSailStore` work that must survive.
- [x] (2026-08-01 11:11Z) Ran the mandatory root quick clean install; the reactor completed with `BUILD SUCCESS` in
  33.214 seconds and the full output is in `maven-build.log`.
- [x] (2026-08-01 11:14Z) Added the default-selection regression and preserved the intended one-test Surefire failure
  in `initial-evidence.txt`: expected one predicate-enumeration fallback but the retired implementation recorded zero.
- [x] (2026-08-01 11:25Z) Ported the immutable page/vector/shard/index core and passed seven focused JUnit tests,
  including an August 1 copy-on-write structural-sharing scenario; evidence is in
  `post-evidence-paged-csf-core.txt`.
- [x] (2026-08-01 11:32Z) Implemented exact precharged native/metadata pools, per-root/per-shard reservation owners,
  reference-counted dictionary charges, and CAS-safe release; 15 focused tests pass with evidence in
  `post-evidence-paged-csf-memory.txt`.
- [x] (2026-08-01 11:39Z) Integrated default paged construction, legacy rollback, virtual handles, cursor dispatch,
  and predicate-enumeration fallback; 59 focused query/catalog/codec tests pass with evidence in
  `post-evidence-paged-csf-query.txt`.
- [x] (2026-08-01 12:03Z) Integrated page/shard copy-on-write consolidation, append-only predicate evolution,
  context-extension copies, publication identity checks, pinned-root sharing, and overlay-only refusal fallback;
  the focused consolidation suite passes 11 tests.
- [x] (2026-08-01 12:50Z) Hardened malformed page/vector validation and replaced allocation-heavy random-access
  decoding with reusable sequential cursors; format/index tests pass and JFR evidence is retained.
- [x] (2026-08-01 13:01Z) Completed matched JDK 25 legacy/paged JMH, retained-probe JMH, and exact five-million-row
  sizing. The synthetic projection is 9.8957 bytes/statement and 168.23 decimal GB persistent at 17 billion rows.
- [x] (2026-08-01 14:12Z) Completed the second module verification, copyright/format checks, the formatted-tree
  four-plane builder test, and the final full-reactor quick clean install. Surefire passed all 2,807 tests; one of
  118 Failsafe tests hit a pre-existing MEDICAL_RECORDS planner timeout documented in six July 30-31 logs.
- [x] (2026-08-01 18:26Z) Removed the reported JDK 26 query regression with random-access FOR row keys, bounded
  native loads, compressed row starts, block/range/page caches, single-page cursor specialization, common-context
  bulk materialization, and a tail-safe streaming FOR decoder. Final JDK 26 q0 is 2.285 ms/op paged versus
  2.837 ms/op legacy; 52 post-format cross-layer tests pass.

## Surprises & Discoveries

- Observation: the archive is a review bundle, not a clean checkout. Its hardening patch is already commit
  `321cc04eca`, and its report/results predate the August 1 copy-on-write source.
  Evidence: the archive timestamps `CsfShard.java`, `ImmutablePagedQuadCsfIndex.java`,
  `LmdbPagedCsfConsolidator.java`, and `LmdbDirectAdjacencyStore.java` on August 1, while the migration report and
  JUnit tests are dated July 31.
- Observation: the archive's consolidator is not compile-ready in isolation. It calls absent catalog extension and
  ownership APIs and refers to an absent shared-charge type.
  Evidence: static comparison found calls to `baseOnlyWithCsf`, `retainSharedBaseCharge`, `extendAppend`,
  `extensionCopyPlan`, `modeledJavaBytes`, and `withAdditionalSelected` without matching definitions.
- Observation: current HEAD is newer than the archive in `LmdbSailStore`.
  Evidence: commit `82a7227f76` adds join and parallel snapshot improvements after the source baseline used by the
  archive. That file must be merged semantically, never replaced from the bundle.
- Observation: `mvnf`'s mandatory root install compiles all LMDB test sources, so a staged test class cannot be
  isolated with Maven compiler include properties.
  Evidence: the first CSF run stopped in test compilation on `LmdbPagedCsfBaseBuilderTest`; adding the builder-facing
  APIs allowed the focused CSF selector to run normally.
- Observation: the archive freezes the shard target-page property in a static final field, which makes its standalone
  COW test depend on a JVM launch flag and prevents deterministic per-test configuration.
  Evidence: the first JUnit COW run produced one shard and failed its `>= 2` precondition. Sampling the bounded value
  once per sizing/rewrite pass produced deterministic four-page test shards and all seven tests passed.
- Observation: the first matched paged JMH run was dominated by decoder object allocation and repeated validation,
  not by native page access itself.
  Evidence: the paged scalar JFR attributes 65.44% of sampled allocation to `PackedLongVector.reader` and 33.44% to
  `CsfShard.page`; CPU samples concentrate in `validateBlock`, `decode`, and block-directory reconstruction. Reusable
  readers, sequential copies, and immutable-page identity caching reduced the matched paged scalar result from
  22,502 ns/op to 621 ns/op and bulk traversal from 21,772 ns/op to 474 ns/op.
- Observation: the first complete module run exposed contracts that encoded legacy-only behavior rather than CSF
  defects.
  Evidence: unbound-predicate commit probes expected direct enumeration, snapshot telemetry expected two builder
  threads and uncovered source visits, and a synchronous benchmark hook sampled a later queued `BUILDING` state
  after a `ROW_EXACT` base had published. The five reports are preserved in `initial-evidence.txt`; the corrected
  focused suites pass 3, 6, and 13 tests respectively.
- Observation: the second complete module run has no new failure attributable to paged CSF, but the branch's known
  MEDICAL_RECORDS query-planner timeout prevents an all-green Failsafe summary.
  Evidence: Surefire reports 2,807 tests with zero failures or errors; Failsafe reports 118 tests with only
  `medicalPatientsWithMedsOrObservationsExcludingCodeAvoidsUnboundLeftGuards` failing after its 30-second retry
  window. The exact same test and message occur in six retained logs dated July 30-31, before this implementation.
- Observation: JDK 26 makes the remaining packed-vector costs query-dominant after the asynchronous CSF base is
  published.
  Evidence: MEDICAL_RECORDS q0 changes from roughly 10 ms to 125-130 ms when the paged base publishes, while the
  matched legacy run settles near 2.7 ms. In the steady-state JMH worker, `readBits` and `decodeNative` account for
  56.34% self CPU and `CompactCsfPageReader.findRow` accounts for 44.4% inclusive CPU.
- Observation: this is a data-layout and access-shape problem, not delayed JIT compilation.
  Evidence: both `readBits(JJI)J` and `decodeNative(JI)J` reach C2 on JDK 26. Row IDs use delta blocks, so every
  binary-search probe replays up to 255 preceding deltas; native extraction then invokes `Unsafe.getByte` once per
  payload byte, including the JDK 26 native-access guard on every invocation.
- Observation: independent decoding alone exposed a second O(page-count) search tax that the original profile hid.
  Evidence: after row-key FOR encoding and prefix-offset storage, `CsfShard.floorPage` still consumed 4.69% of the
  JMH worker. A lookup-cursor page-interval cache reduced it to 0.03% and made 256 paged finds 1,682.779 ns/op versus
  5,069.407 ns/op for the matched retired implementation.
- Observation: after search was removed, bulk FOR extraction became the next bounded optimization target.
  Evidence: the pre-streaming steady profile attributed 5.74% self CPU to `readLittleEndian`, 3.07% to `readBits`,
  and 2.58% to `decodeNativeRange`. An adjacent old/new bulk control measured 162.750 versus 152.632 ns/op, a 6.22%
  improvement, and final q0 measured 2.285 ms/op.
- Observation: ChatGPT Pro's independent review agreed with the streaming direction only after its first unsafe
  reservoir was challenged.
  Evidence: the corrected review derives the same logical-payload-bounded word loader, lazy crossing-word load,
  no-post-final transition, and width-zero/width-64 specializations used here. It also corrects padding from an
  unconditional eight bytes to 0..7 bytes for partial blocks and recommends no layout change without another
  measured win.

## Decision Log

- Decision: treat the August 1 Java source as the intended behavior and the July 31 prose/results as historical
  evidence only.
  Rationale: the user selected the latest source literally, and the newer source contains the requested shard-level
  copy-on-write path.
  Date/Author: 2026-08-01 / Codex.
- Decision: make paged CSF the default and retain the legacy implementation behind the documented system property.
  Rationale: this is the requested rollout and gives operators an immediate reversible fallback without changing a
  supported RDF4J configuration interface.
  Date/Author: 2026-08-01 / Codex.
- Decision: split memory charges by physical lifetime rather than copy the archive's monolithic base charge.
  Rationale: pinned roots can share old shards after a rewrite; one monolithic charge would undercount shared live
  memory or double-count replacements. The account must follow the lifetime of each native allocation.
  Date/Author: 2026-08-01 / Codex.
- Decision: preserve the current `LmdbSailStore` and layer only the direct-adjacency integration points onto it.
  Rationale: replacing it would regress post-archive query execution improvements unrelated to this feature.
  Date/Author: 2026-08-01 / Codex.
- Decision: keep validation at page resolution but cache the resolved immutable page and reuse vector readers for all
  cursor operations.
  Rationale: malformed data must still fail closed, while validating and allocating on every scalar access made the
  compact format unusably expensive. The cache is scoped by owning index and page id, so retained roots cannot alias.
  Date/Author: 2026-08-01 / Codex.
- Decision: report paged build telemetry using the archive's accepted CSF groups and one materializer thread.
  Rationale: the August 1 source explicitly records group counts and single-threaded construction; retaining legacy
  test expectations would misdescribe the shipped builder.
  Date/Author: 2026-08-01 / Codex.
- Decision: encode binary-searched row IDs as random-access frame-of-reference blocks and keep delta modes available
  only to callers that consume sorted vectors sequentially.
  Rationale: row lookup is logarithmic only when each probe is O(1); retaining delta replay makes a nominal binary
  search perform repeated linear block-prefix scans. Frame-of-reference packing preserves compression without an
  eight-byte-per-row side table.
  Date/Author: 2026-08-01 / Codex.
- Decision: read packed native fields with exact-width little-endian loads that never cross the encoded payload.
  Rationale: a bounded composition of byte, short, int, and long loads removes most native-access calls while
  retaining the malformed-layout safety property; unconditional eight-byte loads could overread a page allocation.
  Date/Author: 2026-08-01 / Codex.
- Decision: store compressed starts for rows but omit the redundant terminal element and derive it from the fixed
  page `fiberCount`; retain sequential count encoding for contexts.
  Rationale: random row materialization needs O(1) starts, while full context offsets widened the dominant vectors
  enough to slow q0 and threaten the tight memory gate. The implicit terminal recovers the common one-entry cost.
  Date/Author: 2026-08-01 / Codex.
- Decision: cache a non-continuation page's unsigned row interval in the caller-owned lookup cursor.
  Rationale: repeated probes in one page do not need partition/shard/page binary searches. Continuation pages are
  deliberately excluded so supernode extent semantics continue through the checked generic path.
  Date/Author: 2026-08-01 / Codex.
- Decision: use a no-layout-change streaming FOR range decoder with bounded word loads and exact partial-word tails.
  Rationale: it amortizes native loads across packed values, specializes widths zero and 64, and never reads after
  the final output. Padding would consume scarce allocator headroom and is unnecessary for the measured 6.22% win.
  Date/Author: 2026-08-01 / Codex.

## Outcomes & Retrospective

The original implementation, validation, and JDK 26 performance follow-up are complete. Paged CSF is the default,
the legacy property remains a tested
rollback, exact charge ownership follows dictionary/root/shard/context-extension lifetimes, and consolidation now
rewrites affected shards while retaining untouched shards for pinned roots. The initial behavioral failures and the
subsequent compatibility failures are preserved in `initial-evidence.txt`; their focused post-change selections are
green.

The reported regression is removed. On the matched JDK 26 degree-128 fixture, 256 paged finds fell from 14,131.523
to 1,682.779 ns/op and are about three times faster than the 5,069.407 ns/op legacy pair. Paged bulk traversal fell
from 510.454 to 152.632 ns/op. Most importantly, the MEDICAL_RECORDS q0 post-publication phase fell from roughly
127-130 ms to 2.285 +/- 0.028 ms/op; the current legacy pair is 2.837 +/- 0.043 ms/op, making paged CSF 19.5% faster
in the reported workload. The exact synthetic sizing remains 11.0874 bytes/statement and 188.53 GB build peak at 17
billion statements because the final streaming decoder has no layout or allocation change. A production-like
Wikidata sizing run remains an explicit rollout gate.

The earlier second module verification passed all 2,807 Surefire tests and 117 of 118 Failsafe tests. The sole
failure is a
pre-existing MEDICAL_RECORDS planner timeout reproduced in six retained July 30-31 logs; no CSF or direct-adjacency
test failed. After the JDK 26 performance patch, a final post-format selection spanning packed vectors, page format,
immutable index/extents, and direct-adjacency queries passes 52/52 tests on JDK 26. A later broad run generated only
zero-failure reports but its wrapper stalled before Maven's summary, so it is recorded as inconclusive rather than
green. Copyright/SPDX validation, repository formatting, and the root JDK 26 quick install pass.

## Context and Orientation

The implementation lives in `core/sail/lmdb`. `LmdbDirectAdjacencyStore` publishes immutable base generations and
append-only update overlays. `LmdbInMemoryAdjacencyIndex` owns one base generation; `LmdbAdjacencyArenaCatalog`,
`LmdbAdjacencyPredicateCatalog`, and `LmdbAdjacencyContextCatalog` resolve encoded handles and ordinals.
`LmdbAdjacencyRunCodec.RunCursor` is the hot query cursor used by direct-adjacency iterators.

CSF means compact sparse fibre. Here, each page stores a hierarchy of predicate plane, row key, distinct neighbor,
and sorted raw context identifiers. A plane is one of outgoing explicit, incoming explicit, outgoing inferred, or
incoming inferred. A shard is an independently retained immutable group of pages. Copy-on-write consolidation
creates replacement shards only where committed overlay rows changed and shares every untouched shard with older
snapshot roots.

The source bundle is `/Users/havardottestad/Downloads/lmdb-direct-adjacency-paged-csf-latest-source (1).zip`. Stage
its source under `/tmp`; do not copy files blindly over the checkout. The hardening patch, reports, compiled classes,
standalone test runners, profiles, and verification results are not implementation inputs. New Java files require the
exact repository copyright header with year 2026 and `// Some portions generated by Codex` immediately below it.

## Plan of Work

First, change `LmdbDirectAdjacencyQueryTest` so its bound-node/unbound-predicate case expects correct LMDB results
and one `PREDICATE_ENUMERATION_INCOMPLETE` fallback under the new default. Run that method against untouched
production code and preserve its failing Surefire report in `initial-evidence.txt`.

Next, add the archive's compact page format, little-endian access, packed vectors, native slab allocator, page
encoder/reader, page data, shard, and immutable index beneath `org.eclipse.rdf4j.sail.lmdb.csf`. Port standalone
coverage into JUnit before production behavior for each unit: vector modes and boundaries, malformed pages, all four
planes, page splitting, supernode continuation extents, exact two-pass parity, and copy-on-write isolation. Pages use
a fixed 160-byte header, capacity classes from 256 bytes to 64 KiB, and at most 1,024 rows per virtual handle page.

Then add exact ownership. A package-private `LmdbAdjacencySharedCharge` reference-counts the dictionary arena charge.
`LmdbAdjacencyMemoryAccount` gains an internal precharged reservation pool, or equivalently safe split charges, so
the exact planned native and Java-metadata totals can be divided between the root and individual shards without
changing the account total. Each shard owns its child reservations; a copied context-extension arena owns a separate
native charge. All failure and publication-abandon paths close only what they acquired.

Integrate `LmdbPagedCsfBaseBuilder` as a two-pass size/materialize build. Extend the arena catalog with CSF-aware
copies; reserve slot `0xFE` for CSF handles and `0xFF` for the legacy chunk sentinel. Extend predicate catalogs with
append-only sorted segments and stable ordinal bases, context catalogs with exact extension copy plans and modeled
heap bytes, and coverage with unsigned selected-predicate merging. Dispatch the existing run cursor's scalar, bulk,
lower-bound, context, count, and ordered-domain methods to a reusable CSF row cursor. Default to the paged builder;
sample the legacy property per store construction so tests can cover both modes without class-loading interference.

Finally, integrate `LmdbPagedCsfConsolidator`. Fold the newest overlay value for each unsigned key/plane/raw-predicate
tuple, append new predicate ordinals, translate contexts to raw pairs, and invoke the CSF rewrite. Publish only when
the base, overlays, and applied revision still match. A successful publication has no overlays and advances the base
and minimum snapshot revisions. On a page-rewrite refusal, try existing overlay-only coalescing while preserving the
paged base; if that also refuses, keep the current exact state for later retry. Preserve current HEAD's
`LmdbSailStore` code and modify only the minimum direct-adjacency boundaries needed.

## Concrete Steps

Run commands from the repository root. The mandatory baseline command is:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install \
      2>&1 | tee maven-build.log

Run focused tests through the repository runner, never adding `-am` or `-q`:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbDirectAdjacencyQueryTest#boundSubjectUnboundPredicateEnumeratesEveryGroup \
      --module core/sail/lmdb --retain-logs

Persist the first failure before another run can overwrite it:

    python3 scripts/agent-evidence.py --command "<focused command>" --log <retained-verify-log> \
      core/sail/lmdb/target/surefire-reports > initial-evidence.txt

After each milestone, rerun its focused selector and record the Surefire report snippet. At the end run:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs
    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

Use `scripts/run-single-benchmark.sh` with `DirectAdjacencyBenchmark` selectors for matched legacy/paged JMH. Use
`--enable-jfr` for allocation evidence if a repeatable regression needs diagnosis.

## Validation and Acceptance

The focused default-selection regression must fail before production changes and pass afterward. CSF unit tests must
cover vector boundaries 255/256/257, typed payloads and odd doubles, unsigned high-bit values, malformed layouts,
row/neighbor/context splits, 64-KiB pages, multi-extent supernodes, lookup/bulk/lower-bound/context behavior, exact
size parity, rewrite updates/inserts/deletes, retained old roots, and reservation cleanup.

Store-level tests must prove all four planes, FULL and SELECTED coverage, inline and referenced keys, bound subject
and object queries, exact misses, context filtering, legacy deltas over a paged base, predicate-enumeration fallback,
legacy-property acceleration, pending commits, gaps, revision floors, publication races, pinned snapshots, refusal
fallbacks, and zero BASE/JAVA_METADATA/DELTA leakage. The full `core/sail/lmdb` module verify must report zero new
Surefire or Failsafe failures.

Matched JMH runs must use the same JDK 25 runtime, data, forks, and warmup for legacy and paged modes. Do not claim a
speedup without repeatable measurements. Re-run the exact five-million-statement sizing probe for both explicit
directions; the conservative 17-billion-statement projection must remain below 200 decimal GB for persistent and
peak memory, equivalent to 11.7647 bytes per statement. A production-like Wikidata run remains a rollout gate rather
than a claim supported by the synthetic probe.

## Idempotence and Recovery

All tests and builds are repeatable. Preserve unrelated tracked and untracked files. If a production edit occurs
before its smallest failing test, revert only that edit with an explicit patch and restart from the regression. If
offline dependency resolution fails, retry the exact command once without `-o`, then return to offline mode. A
candidate CSF generation must remain unpublished until fully constructed and validated; closing an abandoned
candidate must leave both the live state and every memory-account category unchanged.

## Artifacts and Notes

`maven-build.log` contains the final full quick-install output. `initial-evidence.txt` contains the first focused
failures. Subsequent compact evidence files and retained `logs/mvnf` logs are named by milestone. These are
untracked evidence artifacts and must be kept.

## Interfaces and Dependencies

No supported RDF4J public API, persistent LMDB format, or dependency changes. New CSF types are internal even where
Java visibility must be public across the `lmdb` and `lmdb.csf` packages. The only operational switch is
`removed direct-adjacency rollback selector`; absent or false means paged, true means legacy. The in-memory
page format is versioned and validated. Target the repository's JDK 25 environment and the existing `Unsafe`
convention.

Revision note (2026-08-01, Codex): created the implementation ExecPlan from the approved plan and the inspected
August 1 source bundle; recorded the archive/source conflicts and exact memory-ownership correction before the first
production edit.

Revision note (2026-08-01 11:14Z, Codex): captured the focused pre-change failure and advanced the sole active item
to the immutable CSF core.

Revision note (2026-08-01 11:25Z, Codex): recorded the green CSF core milestone and advanced to exact memory
ownership; added the Maven test-compilation and shard-property discoveries.

Revision note (2026-08-01 11:32Z, Codex): recorded exact physical-lifetime accounting and advanced to default
builder/query integration.

Revision note (2026-08-01 11:39Z, Codex): recorded the green default/rollback query integration and advanced to
store-level copy-on-write consolidation.

Revision note (2026-08-01 13:24Z, Codex): recorded completed copy-on-write integration, exact sizing, JFR-directed
cursor optimization, matched benchmark results, and the first module-run compatibility findings; advanced the sole
active item to final validation.

Revision note (2026-08-01 14:12Z, Codex): recorded the complete module audit, classified the sole Failsafe failure
against six pre-task reproductions, and closed final copyright, formatting, focused test, and reactor-install gates.

Revision note (2026-08-01 18:26Z, Codex): closed the JDK 26 regression follow-up with matched current paged/legacy
q0, adjacent decoder control, final threaded CPU profile, layout-neutral memory proof, and 52 post-format JDK 26
tests; retained the inconclusive broad wrapper run without calling it green.
