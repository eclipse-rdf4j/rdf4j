# Early Frontier operator feedback: separate follow-up

Status: the existing untracked test records a desired early-provider backlog that is not implemented. The source/history audit did not establish that this synthetic observation represents a lost, admitted compiled-query receipt. Preserve the test, its expected result, and the red report; do not mute it or introduce a queue merely to make the current integration green. This question is distinct from the actively investigated selected native FILTER-feedback defect.

## Evidence and provenance

`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbDeferredFrontierFeedbackTest.java` was already untracked when this integration began. Its initial bytes are preserved at `.agent/reviews/deferred-frontier-feedback-20261031/original-source.java.txt`, SHA-256 `928ba54bac71d90e29a21a49a9c709d8099eb214048b5789e7f86b6221298134`. Its current paired-source fixture bytes are preserved at `qualified-source.java.txt` beside that copy, SHA-256 `16b2224289116e2119745ec7f7cc5f08c5328dee5d3c548edfdeafa18978fe3a`. The working source was not replaced by either copy. The qualified fixture's copied Surefire report is `first-red-surefire.txt` in the same directory:

    Tests run: 5, Failures: 1, Errors: 0, Skipped: 0
    earlyFrontierObservationIsRetained:
      completed current-snapshot Frontier feedback reaches its provider
      expected: <true> but was: <false>

The fuller diagnostic run and command remain under `/tmp/rdf4j-authority-test-lock-20261031.LejzLV/runs/lmdb-authority-and-diagnostics/`. This failure must be reported if the untracked test is discovered by a full local module run.

## What was established

`LmdbStore.init` returns before post-ready maintenance publishes optional providers; `LmdbSailStore.startBackgroundMaintenanceAfterInitialization` starts that work asynchronously, so a real query can run during the window. The untracked test opens a real paired planning source, but then directly calls `LmdbEvaluationStatistics.recordOperatorOutcome` with a detached JOIN and manually supplied prediction, outcome, completion and costing fields. It does not optimize, compile or execute that JOIN, and has no compiler-owned runtime feedback contract or execution target. The ready-provider positive verifies the legacy importer; the early positive additionally asks it to retain a call made while no provider exists.

Current `LmdbEvaluationStatistics.shouldTrackCostFeedback` declines ordinary JOIN tracking while `runtime.feedback()` is absent, and `recordOperatorOutcome` declines a direct observation in that state. Generic execution resolves its target during compilation. FILTER has a separate early qualified outcome route in `LmdbEvaluationFilterServices`; that route is not evidence of an operator backlog. Published provider views permit later operations to see a provider but do not replay earlier direct calls. A source/history review found the same non-null provider guards before asynchronous startup and no committed history for this untracked test. Commit `52992a2ee8` introduced post-ready startup; the merge retained those JOIN admission guards while adding the separate FILTER path.

## Required next investigation

If early JOIN feedback retention becomes a requested contract, first create an in-repository regression that holds the existing provider-publication gate after preceding metadata maintenance, commits a real JOIN dataset, and optimizes, compiles and drains an ordinary query under a real paired source. Do not enable result telemetry solely to force a recorder. Assert its exact result bag, actual source currentness, generated contract and target admission, and compare with the same query prepared after provider publication. This determines whether a previously admitted completed receipt was lost or whether no producer was admitted at all. Capture its first-red before changing production.

Any future backlog must contain bounded, compiler-owned immutable receipts with their source, owner, logical epoch, input-frame authority, predictions, completed invocation and root/cleanup settlement; charge the established memory budget, reject safely when full, revalidate at publication and publish at most once. A closed source cannot be made current merely by retaining its object, and mutable query algebra must not be replayed as authority. These are design constraints to evaluate, not an implementation in this integration.
