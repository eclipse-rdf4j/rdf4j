# Preserve exact native root relations through factor algebra

This is the live ExecPlan for the native packed root-factor work, maintained according to `.agent/PLANS.md` in `/Users/havardottestad/Documents/Programming/rdf4j-small-things`. It derives from the frozen read-only audit at `/private/tmp/rdf4j-factor-fiber-audit-20261103/`; that audit remains unchanged. The current authorization is limited to adding and running the smallest real-source test before any production change. The initial census is preserved separately; this plan records only the new test-first qualification and subsequent reviewed implementation path.

## Purpose / Big Picture


An OPTIONAL relation can have many values for one bound subject. When two independent OPTIONALs have many values, an aggregate such as COUNT DISTINCT of each branch should read each independent relation rather than construct their complete pair product as an intermediate. RDF4J already represents independent native child relations as borrowed descriptors: a descriptor points to immutable source-owned storage while each consumer has its own bounded reader. A bound-subject single-pattern relation currently becomes the f-tree's root and loses that representation, even though the actual source can lend the whole relation. Preserve that producer's existing exact run and feed the existing factor and marginal consumers. Final flat output still emits every demanded result, and a later correlated operation still expands its dependencies.

The first implementation delivers that general native root capability plus proof of real work and lifecycle. It does not enable a new automatic public candidate. Automatic composed candidates need a later source-bound complete physical quote; no guessed cardinality or forced strategy can replace it.

## Progress


- [x] (2026-11-03) Complete current source and receipt audit.
- [x] (2026-11-03) Review root preservation scope and invariants.
- [x] Capture smallest native root red evidence.
- [x] Review real-source first-red evidence.
- [x] Draft exact root ownership proposal.
- [in_progress] Qualify borrowed reader ownership failures.
- [todo] Preserve exact root run and ownership.
- [todo] Verify composition lifecycle and negative controls.
- [todo] Review finite public candidate pricing separately.

Exactly one step is in progress. The smallest real-source test reached its factor-export assertion after proving that the selected GroupedRowCursor uses an unconsumed RunRootProducer and matching the independent MemoryStore bag. Its one-test failure report and full invocation logs are preserved in `.agent/artifacts/packed-root-factor-first-red-20261103T2300Z/first-red/`. The initial scratch root implementation is held: common consumers can advance and close the factor Source before their exhausted Reader is closed. Test-only qualification of that separately identified ownership contract precedes a revised implementation; applying production changes requires independent review.

## Surprises & Discoveries


PatternPlan is not missing an entry point: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeFactorAlgebra.java:374–383` already calls packed openLeaf before scalar fallback. Both fixture LeftJoins are ordinary, with null lexical arrays. Subject specialization turns each RHS into one unary root. Only non-root terminals acquire borrowed payloads; RunRootProducer copies values into scalar root lanes. The existing projection consumer preserves factors it receives, but receives none from this shape.

The earlier composed diagnostic's actual per-leaf producer remains unknown: its mask-zero prefixes cannot distinguish RunRootProducer, ScanRootProducer and a declined packed leaf followed by PatternPlan scalar execution. A separate real-source singleton test has now identified an actual RunRootProducer before reaching its export failure; it does not retroactively identify the composed diagnostic's producers. The earlier census has three restricted tests plus one PRIMARY test passing, not a full benchmark.

Composed row AUTO offering is already blocked in `LmdbNativeRowStep.java:1816–1824`; only direct MultiJoin is automatic. Composed aggregate offering remains legal but UNKNOWN-priced at `LmdbNativeGroupStep.java:1632–1649`. The PRIMARY Group receipt chose irAggregate and reported packedFtreeAggregate Infinity. Retain those facts as separate from producer improvements.

## Decision Log


Decision: retain the existing full constant-key RunRootProducer instead of reopening storage or inventing another singleton iterator. Rationale: it supplies an exact originating pattern and resolved handle from the real source, and avoids duplicating native probes. Date/author: 2026-11-03, read-only audit worker.

Decision: use an explicit root-factor mode in the existing grouped transport, not rootData.borrowed. Rationale: current GroupedRowCursor borrowed exports assume a real parent lane and dereference node.parent. A root is an entire relation for the current scalar entry frame. Date/author: 2026-11-03, audit worker.

Decision: retain existing OPTIONAL unmatched scalar UNDEF lane. Rationale: one empty mapping preserves left multiplicity and factors; an empty borrowed relation would erase the OPTIONAL solution. No new nullable batch container is needed. Date/author: 2026-11-03, audit worker.

Decision: make no new AUTO candidate offer in the first slice. Rationale: representation correctness and direct diagnostics do not certify a finite source-bound work recipe. Existing source degree metadata does not itself certify borrowing coverage. Date/author: 2026-11-03, audit worker.

## Outcomes & Retrospective


Audit complete; the separately frozen census is available in `.agent/artifacts/composed-factor-census-20261102T225342Z/`. The first real-source implementation test is now captured. The native source, singleton packed leaf and direct-adjacency run all qualified; the exact ordinary/MemoryStore bag passed. The first failure is the expected root-factor export assertion: the free object relation was not exposed as a nonempty borrowed root factor. Surefire reports one test, one failure, zero errors. No production file changed. Implementation, actual root-group export, exact serving counts, quote integration, Q11 actual route and throughput remain undone. No speedup or per-leaf probe count is established. See `.agent/artifacts/packed-root-factor-first-red-20261103T2300Z/first-red/classification.md` for the exact command, report, and source hashes.

## Context and Orientation


The working directory is `/Users/havardottestad/Documents/Programming/rdf4j-small-things`. Main Java files are under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/`. LmdbNativeFactorAlgebra composes a bag, meaning repeated solutions count repeatedly, over scalar bindings plus FactorEnvironment descriptors. LmdbNativePackedFtree plans a connected fixed-predicate region, specializes bound inputs, selects a root producer, stages scalar roots and optionally borrows child relations. A RunRootProducer already owns one resolved constant-key adjacency run. A borrowed factor contains exact logical quad count and source coordinates, not a binding-set row or a mutable native cursor.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/factor/FactorEnvironment.java` attaches independent unary groups or explicitly zipped multicolumn groups. Dependencies must be scalar in the current row, groups must not overlap, and generations prevent advancing a producer while a consumer retains its descriptors. `BorrowedFactorBatch.java` owns no native view lease itself; its Source does. `BorrowedTupleBatch.java` preserves correlation between columns of a single relation; do not split such columns into independent factors. `LmdbBorrowedAdjacencySource.java` and `LmdbDirectNativeAdjacency.java` in the parent LMDB package implement the actual source read-view authority and lease.

`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbComposedFactorCensusTest.java:425–442` constructs VALUES subjects with two single-pattern OPTIONALs. Its frozen diagnostic has 30 scalar prefixes and no groups, while exact bag and Group values match the independent MemoryStore fixture. PRIMARY has benchmark-like AUTO properties but chose other public strategies. Existing `LmdbNativePackedFtreeTest.java:169–259,811–907` proves borrowed child support and native singleton correctness.

## Plan of Work


Milestone 1: add and observe one smallest real-source failing root-export test. Add `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedRootFactorTest.java` with the project license and Codex signature. Use the same real explicit-source access and completed direct-adjacency setup as the census. Bind one subject in a RowState, leave the object free, open one fixed-predicate context-unrestricted PatternPlan through singleton packed openLeaf, and record the actual returned grouped cursor's Runtime/root-producer kind using package-visible fields. No reflection or source wrapper is needed. Require the actual RunRootProducer native positive control; an unavailable native capability is a setup failure, not a skipped export assertion. Assert that the emitted object is deferred in a factor, remains UNKNOWN in row.slots, depends on its bound subject, and reconstructs exactly the expected RDF multiset through the existing projection reader. Before implementation, the nonempty factor assertion must fail; capture its actual Surefire snippet at repository root `initial-evidence.<workspace>.txt` before any rerun overwrites it. This proves the native root/export gap without new production telemetry. Add related variants only after the smallest red is frozen.

Milestone 2: retain the selected native relation. In LmdbNativePackedFtree, add a root-export capability owned by the existing Runtime/GroupedRowCursor lifecycle. Admit a full unconsumed RunRootProducer, one variable node, the single unary relation already covered by that exact producer, no other per-member constraints/filters/witnesses/children, contextFree(pattern), borrowing/factor transport enabled, and permitted encounter policy. Preserve original-to-specialized pattern origin, source/id-space authority, fixed input dependency slots and resolved run identity. Obtain a Source from that same adjacency and export that same handle into one-lane BorrowedFactorBatch. Never call find again or advance RunRootProducer to construct the descriptor. If either source or export declines, close temporary factor ownership and retain the same unconsumed scalar Runtime. No storage fallback is relabeled as borrowed native work.

Keep the current one-row-per-member behavior of ordinary packed row consumers and all unsupported root kinds. Explicit grouped root mode emits one scalar prefix with the borrowed variable and Runtime constant multiplicity; it must not both count the factor and multiply it into scalar weight. It binds fixed output slots and preserves entry bindings using existing trail compatibility. A missing native run yields no RHS solution; existing Joined then emits its one unmatched UNDEF mapping. Root factor EOF or close invalidates row descriptors before closing source and Runtime; readers must close before factor source, including early close and exceptions. Reuse FactorEnvironment and native physical Source, not a fanout-sized heap copy. Apply ledger accounting to new retained state, preserve bounded windows and cancellation.

Milestone 3: prove composition and physical work. Add test-only observations of factor prefixes and projection demand as in the census. Where real serving source work is unavailable, add opt-in production observations only after the qualifying red is observed. Record actual leaf Runtime kind, one-to-one producer origin, frame/demand, probe/key operations, descriptor export, source reader windows/weighted entries/logical weight, projection visits, final bags, ledger and native lease deltas. The observer must neither wrap the source nor advance it or substitute missing capabilities. Separate metadata reads, speculative trials and committed serving. Run independent/nullable/correlated/UNION/effect/scope/lifecycle controls from Validation. Freeze measured actual receipts before deriving exact source-work assertions. Do not assert guessed 4+4 probes or a guessed 28 projection total.

Milestone 4 is a separate design/review gate: add a naturally offered composed candidate only when an immutable source-bound executable recipe supplies a finite complete quote. Use `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeAggregatePhysicalPlan.java` Producer.quote and `NativeAggregateCostQuotes.java:376–404` for producer grain and reducer/retention work. Give the row route equivalent ownership before changing its current AUTO gate. Exact metadata may price finite known VALUES entries and native independent unary relations, but borrowing authority must come from the actual source/provider contract. Missing capability or unsupported scope/effects/correlation yields a declined new offer. Quote acquisition cannot open the row producer. A selected native recipe that loses authority must decline before consumption or use a previously priced bounded fallback; it cannot silently execute arbitrary scalar work under the native quote. Existing ordinary candidates remain and may win. This milestone is not included in the first production patch.

## Concrete Steps


After review and the shared-source lock is granted, inspect git status/diff, read the current mvnf skill, and revalidate frozen hashes for touched/depended-on files. Preserve every unrelated tracked and untracked file. Use a named workspace distinct from the Group worker. Run the repository-mandated initial root quick clean install before tests, using current Java 25+ and the checkout-local `.m2_repo`; no tests may use -am or -q. Prefer the mvnf runner, which performs the required install before verify. Run, in order:

    cd /Users/havardottestad/Documents/Programming/rdf4j-small-things
    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace packed-root-fiber LmdbNativePackedRootFactorTest#boundNativeRunExportsBorrowedRoot --retain-logs

Copy the initial failure evidence to `initial-evidence.packed-root-fiber.txt`, including exact run log and full-GAV workspace report paths printed by the runner. Do not overwrite existing evidence; append a clearly dated new section if a file already exists. Publish a concise Evidence block with command/report/actual failure lines, then implement. Rerun the same method and then the class:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace packed-root-fiber LmdbNativePackedRootFactorTest#boundNativeRunExportsBorrowedRoot --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace packed-root-fiber LmdbNativePackedRootFactorTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace packed-root-fiber LmdbNativePackedFtreeTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace packed-root-fiber LmdbNativePackedAlgebraTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace packed-root-fiber LmdbComposedFactorCensusTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace packed-root-fiber LmdbComposedFactorPrimaryCensusTest --retain-logs

Commands are proposed, not run by the auditor. Evolve names/counts to the actual added tests and retain their results. Run copyright check before formatter; use the serialized repository formatting route with `.m2_repo`, never concurrent workspace formatting. A whole LMDB module test run is the required broader completion check after focused greens, with its long job delegated to the existing worker. Coordinate root install and any source changes with the parent. Do not build or format while the separate Group source lock is active.

## Validation and Acceptance


Acceptance for the first slice is that a real native bound-key run survives as a borrowed root group and reconstructs the identical RDF bag; independent OPTIONAL factors remain independent until demanded; all fallback, effect and scope semantics match MemoryStore or the existing generic/native oracle. Actual source/projection work is recorded with unavailable values kept unknown. No automatic composed candidate becomes newly offered in this slice.

Use data-driven positive tests for arbitrary fanout and both subject-key/object-key directions, native formats that really support borrowing, repeated VALUES entries and equal neighbors with exact context weights. Verify final projection with neither, one and both factor columns; Group key demand, COUNT(*), COUNT(?x), COUNT DISTINCT and supported SUM/AVG; empty grouped input and empty global Group. Retain the existing positive borrowed multivariable BGP controls. The final row path may still enumerate the result product when all variables are requested; that is correct output work.

Test empty VALUES, absent p/q/both, actual NOT_FOUND, actual NOT_COVERED, export false, prebound output compatibility, UNDEF subject and nullable prior binding filled by a later producer. A nullable key cannot be assumed bound from producedMask. Shared variables, a later arm reading p, nested OPTIONAL, and a p/q joint filter must expand dependencies instead of asserting false independence. Test UNION duplicate branches and missing columns. UUID/RAND/BNODE and opaque/custom effects must observe one invocation per logical row, preserve encounter guarantees and take the scalar boundary where required. Test lexical subquery OPTIONAL and non-well-designed OPTIONAL as distinct cases. Fixed graph/default-dataset constraints and context variables retain exact scope; the context-free slice must not borrow an unrestricted run over a restricted source pattern.

Close before next, close with active marginal reader, cancellation before/during decode, generation change, owner closure, source snapshot mismatch, budget refusal, partial failed export and throwing cleanup must restore row trail and release reservations/native leases without hiding the primary failure or publishing partial completion. Preserve exact weights and avoid multiplying unopened factors for presence-only/null aggregate arguments. Existing generated-borrowed tests and PRIMARY with Janino/parallel on are controls against shared abstraction regressions; new parallel root export may remain out of scope.

For the later pricing milestone, first-red tests must prove the new finite recipe is honestly offered in AUTO, no row producer opens during quote, missing source/scope/quote declines, no unpriced composed row offer is introduced, and a priced alternative can win. Public final actual route and direct diagnostic route are separate invocations and assertions. Do not require the packed tag merely to make a plan test green.

## Idempotence and Recovery


Keep edits additive and source-locked. If the smallest method is green before implementation because a supported route already exists, inspect that route and document it; do not force a new one. If actual setup lacks native borrowing, record its capability/coverage receipt and choose an actual supported native source fixture rather than mocking the desired answer. If a production patch preceded the observed red, follow AGENTS recovery using the saved exact owned patch; never revert unrelated changes or delete artifacts. A local borrowing decline can reuse the same unconsumed Runtime. Once a result/effect escapes, lost coverage is failure/cancellation according to existing contract, not replay. Keep all logs, initial reports, scratch evidence and original receipts.

## Artifacts and Notes


The audit bundle includes frozen REPORT.md, this EXECPLAN.md, SOURCE_MANIFEST.json, RECEIPT_EXTRACT.json and 53 exact input files. The restricted diagnostic currently records factorPrefixCount 30, masks [0], projectionEvents [30,30,30,30], all leaf-work/lease fields null. PRIMARY records nativeProjectionBoundary for rows and irAggregate for Group. Those are preexisting receipts. There is no new red/green evidence in this audit.

Medical Q11 is in `testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/common/ThemeQueryCatalog.java:169–206`: SELECT DISTINCT *, ORDER BY, nested OPTIONAL and catalog count 199461. Its deeper dependencies force some factors to expand. A 30-row aggregate fixture cannot establish that Q11 uses the new capability or improves throughput. Before benchmarking preserve exact query/data/head/config, actual public route and full semantic oracle. Any performance claim requires the supported benchmark/JFR workflow and actual measured serving work at that head.

## Interfaces and Dependencies


Use existing BorrowedFactorBatch.Source, NativeAdjacency.openFactorSource/borrowRun, FactorEnvironment.bind, FactorProductCursor, LmdbNativeFactorProjections and the current Runtime lifecycle. No new external dependency is needed. The root-export internal carrier must retain its original/specialized producer identity, actual owning Runtime/source, exact root slot/dependency mask, one-lane borrowed batch and one factor-source lease; it must expose the existing LmdbNativeFactorCursor contract with exact scalar multiplicity. Do not expose physical handles as scalar term IDs or public API fields. If observations are added, use invocation-local producer IDs with explicit serving/metadata/attempt phase and opt-in no-op default, without source wrappers.

Revision note (2026-11-03): initial read-only proposal. The first slice ends at preserved native root groups and observed work. Automatic public candidate admission is held for its own finite recipe and quote, because existing scalar LeftJoin estimates are intentionally unbounded.
