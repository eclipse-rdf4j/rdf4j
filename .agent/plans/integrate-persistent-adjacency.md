# Integrate the supplied persistent adjacency implementation

This living ExecPlan follows `.agent/PLANS.md`. The two companion plans,
`persist-paged-adjacency.md` and `optimize-persistent-adjacency.md`, are imported
historical records from the supplied archive, not validation of this integration.

## Purpose / Big Picture

Integrate the user's supplied persistent paged adjacency implementation into fresh
upstream main without reverting newer upstream work. Statements are stored as
bounded adjacency pages, transaction-local deltas, and durable metadata rather than
physical statement permutation indexes. See `core/sail/lmdb/PERSISTENT_ADJACENCY.md`
for the supplied format and transaction contracts. Publish a draft first, then
independently test and review the integrated code, as explicitly requested.

## Progress

- [x] (2026-11-01) Resolve and safely inspect supplied archive.
- [x] (2026-11-01) Verify checksums and focused patch application.
- [x] (2026-11-01) Preserve current upstream main changes.
- [ ] in_progress: Commit, push, and create draft PR.
- [ ] Build and test integrated implementation.
- [ ] Review persistence, migration, snapshots, and recovery.
- [ ] Report verified results and remaining limitations.

## Surprises & Discoveries

The archive includes a cumulative patch based on
`467b285c5345d590d5e7726ced9fe057e825b428`. Fresh upstream main is
`62727ee4aa44e95027689b692dc5423724d02be6`. The cumulative patch applies cleanly to
that main. Three patch paths also have newer upstream changes: `LmdbSailStore.java`,
`LmdbDirectAdjacencyCommitTest.java`, and `LmdbSailStoreTest.java`. Those changes are
retained; the other 91 imported paths match the archive's final source hashes.

## Decision Log

On 2026-11-01 the user explicitly requested implementation, commit/push, and a draft
PR before testing and review. Therefore initial publication deliberately precedes
the repository's normal test-first and build gates. It is not a ready-to-merge claim.
Use only the cumulative patch, not a stale module-tree overwrite or both supplied
patches. Do not import archived binaries, databases, or historical test reports.
Keep the unrelated earlier hardening effort paused and its source/cache untouched.
No GitHub issue number was supplied, so use the required `GH-0000` fallback.

## Outcomes & Retrospective

Initial integration has passed archive integrity, path/symlink safety, payload
checksum verification, `git apply --check`, and `git diff --cached --check`.
Compilation, tests, performance verification, and independent code review have not
yet been performed on this integrated tree. Historical archive results are not
substitutes for those checks.

## Context and Orientation

The implementation is scoped to `core/sail/lmdb`, plus its documentation and plans.
`TripleStore` owns native LMDB transaction integration;
`LmdbPersistentAdjacency` owns page/delta/metadata operations;
`PersistentCsfPageCodec` encodes bounded pages. New tests cover format validation,
migration, snapshots, map growth, replay, compaction, caching, and query results.

## Plan of Work and Milestones

First publish the staged 94-file cumulative patch plus this integration record as
a draft against upstream main. Verify remote tree identity and exact branch SHA.
Then use an isolated local Maven cache and Java 25, installing dependencies with
tests skipped before focused LMDB tests. Expand to the module and compatibility,
recovery, isolation, and query suites after focused success. Review migration
atomicity, old-writer rejection, dictionary/triple publication, cache ownership,
cursor lifetime, counts, and compaction failure paths. Record failures distinctly
from infrastructure limitations; do not imply the historical benchmark claims were
reproduced. Any follow-up fix must include a reproducing regression test first.

## Concrete Steps

From the integration checkout, publication checks are:

    git diff --cached --check
    git diff --cached --stat
    git commit -m "GH-0000 Integrate persistent paged adjacency implementation"

After the draft exists, install current reactor artifacts in a private `.m2_repo`
using the repository's quick profile with tests skipped. Then run focused tests:

    mvn -o -B -ntp -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb \
      -Dtest=LmdbPersistentAdjacencyTest,LmdbPersistentAdjacencyContractTest,\
LmdbPersistentAdjacencyAdmissionTest,LmdbPersistentAdjacencyFenceTest \
      -DskipITs -DtrimStackTrace=false test

Use resource-safe test fork settings for this environment, retaining assertions and
the Mockito agent. Never use `-am` or `-q` when tests are enabled. Preserve initial
and subsequent logs/reports outside `target` before additional runs overwrite them.

## Validation and Acceptance

Publication requires the remote branch commit to have the exact local tree and
fresh-main parent, and the PR to remain draft. Runtime acceptance requires passing
focused persistence/migration/recovery tests and the relevant broader tests on the
integrated code. Report every failed, skipped, blocked, and not-run stage honestly.

## Idempotence and Recovery

Apply the cumulative patch once to a clean fresh-main checkout. Never apply it on
top of itself or replace the module from the archive. Preserve unrelated work and
untracked artifacts. Do not test migration against any real user database: fixtures
must be disposable copies. No merge, release, or deployment is authorized.

## Artifacts and Notes

Input: `rdf4j-persistent-adjacency-optimized.zip`, 26,763,036 bytes.
SHA-256: `60f4615c35622767109d9cda0ad4fe30ea625be5dd8557202c696183fa769c0f`.
Cumulative patch SHA-256:
`3d5c7032b8f4f1d55485109b7a838395cfc06d09771db0b0fd38b00e8d83175c`.
The archive has 2,544 safe entries, with verified ZIP CRC and payload checksums.

## Interfaces and Dependencies

No new external dependencies are introduced by the supplied patch. The existing
LMDB and RDF4J APIs remain the integration boundary. Persistent format and migration
behavior require independent compatibility review before considering a merge.

Initial revision: recorded provenance, publication-first instruction, and pending
verification so historical package evidence cannot be mistaken for current results.
