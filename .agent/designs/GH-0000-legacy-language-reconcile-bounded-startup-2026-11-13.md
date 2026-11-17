# Bounded startup for legacy (format-2) stores: prove the language-tag reconcile is still current (2026-11-13)

Status: specified, not implemented. Routine A (behaviour change at open; adds one persisted property).

## Observation

`LmdbBoundedStartupTest[3]` (LEGACY) is red: about 5.6k cursor reads between constructor and ready against a
budget of 1,000 (`triage-storefmt/report.txt`, follow-up section; attribution probe by the triage agent). 4,432 of
them come from `ValueStore.planLanguageTagVariantMergeOnce` (`normalizeLegacyLanguageOwnerKeys`,
`planLanguageLiteralGroups`), called by `LmdbLanguageTagVariantMerge.mergeIfRequired` on every open of a format-2
store. About 400 more are alternative-IRI probes from core-datatype seeding (not part of this change unless the
budget still fails after it; then report the remaining attribution and stop).

## Why the scan runs on every open

Deliberate (class comment of `LmdbLanguageTagVariantMerge`): "A legacy format-2 store is reconciled on each open
because an old writer can introduce raw language variants while leaving the canonical property intact." The
merge-on-open (942b2aec8b, 2026-09-29) met upstream's bounded-startup contract (52992a2ee8, 2026-10-25) in merge
a9b477c32a. Both requirements are legitimate: an old writer must not leave duplicate ids for one term, and an
untouched store must open in bounded work.

## Required behaviour

1. A format-2 store whose value environment has not been committed to by anyone since this build last finished a
   reconcile and closed cleanly opens without planning the merge: no language-literal scan, no owner-key
   normalisation scan. `LmdbBoundedStartupTest[3]` goes green without changing its budget or its fixture's
   meaning (if its fixture is a store written by an old-format writer that was never reconciled, the first open
   legitimately scans; then the test must be read carefully — report what the fixture is before changing
   anything).
2. Any commit to the value environment by a writer other than a clean session of this build makes the next open
   scan as today: an old release writing the store, a crash of this build (no clean close), a restored or copied
   value environment whose identity does not match. The reconcile's roll-forward property is unchanged.
3. The proof is a durable witness in the pattern of `RdfTermDomainQualification` (`publishClosed` /
   `matches`): written only after the value store has closed cleanly with a trusted final transaction id, and
   consumed at open by comparing it with the value environment actually found (transaction id, and the native
   generation where the store has one). It is a new `store.properties` key; an old writer that drops or keeps the
   key is safe either way because its commit changes the transaction id. No witness, a malformed one, or a
   mismatch means "scan". The witness never authorises anything except skipping this scan.
4. No extra durable write on the open path when the witness matches. If the witness does not match, it need not be
   withdrawn eagerly (a stale witness can never match a later state of the same environment), unless reading the
   code shows a way for a transaction id to repeat; then say so and withdraw.
5. Format 7 is unchanged (it already skips: canonical keys, no old writer).

## The invariant that must be proven before the witness is allowed

"After a completed reconcile, a session in which only this build writes leaves the value store in a state where a
fresh plan is empty and `normalizeLegacyLanguageOwnerKeys` has nothing to change." The write path keeps raw
owner keys for format 2 (`retainLegacyLanguageOwnerKey`), so this is not obvious. Establish it by reading the write
path and the planner, and by a test: reconcile, then write through the sail a mix that exercises it (the same
literal in several case variants of the language tag, in both orders, in one and in several transactions, long
labels that take the hashed path, triple terms containing such literals, removals followed by re-adds, value GC),
close, reopen with a step observer, and assert the plan is empty and the migration made no value-store commit.
If the invariant does not hold, stop and hand back what the next open has to repair after our own writes; do not
introduce the witness on top of a false invariant.

## Tests (red first; store level through `LmdbStore` unless noted)

- `LmdbBoundedStartupTest[3]` as is (existing red).
- Clean close then reopen of a reconciled format-2 store: the planner is not invoked (seam: the existing
  `stepObserverForTesting` never reports PLANNED, or a counter on the planning entry point), and reads stay in
  budget.
- An old-writer commit between two opens (simulate with a raw LMDB write to the value environment, or the
  existing `disabledForTesting` path that writes legacy variants): the next open plans, merges the variants, and
  the open after that skips again.
- Crash: no clean close (existing crash seams) → the next open plans.
- Witness tampering: absent, malformed, transaction id off by one, generation differing → plan.
- The invariant test above.
- Existing `LmdbLanguageTagVariantMergeTest` and `LmdbLanguageTagVariantMergeStorageTest` stay green; if any of
  them relied on "every open scans", list them with the reason before touching them.
- One mutation per rule (skip without checking the transaction id; publish the witness before close completes;
  skip when the witness is malformed).

## Implementation hand-back (2026-11-13 03:30; merged into the working tree, uncommitted)

Evidence: `/private/tmp/rdf4j-phase1-continue-20261109-2OK1vQjv/impl-langwitness/` (`report.txt`, `changes.diff`,
`surefire/`, `mutations/`, `attribution/`, `compare-r1.txt`).

- Invariant holds (reading + `ownWritesAfterACompletedReconcileLeaveAnEmptyPlan[1,2]`, green on the unmodified
  product, red under a write-path mutation).
- Deviation, accepted: the witness is a sidecar file `<store dir>/.rdf4j-language-tag-reconciliation`
  (`1:<valuesTransaction>:<generation>`, generation = 32 hex of `gc_meta` `refcounts-generation`), not a
  `store.properties` key. A key changed the bytes of an original legacy `store.properties`, which this build
  guarantees not to do (`LmdbLegacyUnmarkedDomainLifecycleTest` went red).
- Read in `ValueStore.open()` before any startup write; a non-matching or malformed witness is deleted eagerly
  (a restored older image could otherwise reach the witnessed transaction again). Adopted in `mergeIfRequired`
  only if the env's last transaction is still this owner's. Written by `LmdbSailStore.closeResources` after the
  value store closed cleanly with a synced, owned final transaction and a current reconcile. A 6.1.0 sidecar has
  no `language-tag-key`: the skip adopts the same in-memory canonical mode as the empty-plan path
  (`adoptLegacyCanonicalLanguageLookup`).
- `LmdbLanguageTagReconcileWitnessTest`: 18 cases, 15 red on the base product, 18 green after; six mutations
  caught. Two-tree comparison over 92 classes: nothing green→red that is stable.
- Not achieved: `LmdbBoundedStartupTest[3]` is still red, 5,649 → 1,207 reads against 1,000. 973 of the rest are
  alternative-IRI probes in `ValueStore.seedCoreDatatypes` (the design guessed ~400). Follow-up in flight
  (`impl-langwitness/seeding/`): attribute per datatype, then remove repeated probes without new persisted state.
- Residual risk no witness covers: an older image of the same environment restored and advanced by an old release
  to exactly the witnessed transaction with the same generation. Unverified: the Windows directory-force path.

## Not in this change

Core-datatype seeding probes; moving the scan to background; any change to format 7; the predicate-domain witness.
