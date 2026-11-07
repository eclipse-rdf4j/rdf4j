# LMDB query cost learning and adaptation

Strategy selection is a pure function of each candidate's cost quote and a checked-in table of prices. The learned layer is advisory, off by default, and can only narrow a price inside the interval the quote stands behind. Nothing here is a semantic requirement: every strategy that can run gives the same results.

## The static prior

A candidate's [`LmdbNativeCostQuote`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeCostQuote.java) counts physical features (index seeks, scanned rows, interpreted, batch and generated rows, hash and group operations, output materialization) separately for start-up and streaming, each with a low, expected and high count. [`LmdbNativeStaticPrior`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeStaticPrior.java) holds one nanosecond price per feature. The prices are constants fitted by the regret harness ([`LmdbNativeStrategyRegretHarnessTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/LmdbNativeStrategyRegretHarnessTest.java), `scripts/lmdb-fit-prior.py`) on an Apple silicon developer machine, so their ratios matter more than their absolute values. The prior also holds a measured fixed cost per strategy and the rule for how much a parallel strategy must save.

The product of counts and prices is the candidate's interval and expected price in nanoseconds, which makes candidates of different families comparable. [`LmdbNativeSelector`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeSelector.java) ranks on those numbers; see [strategy arbitration](query-arbitration-and-explanation.md).

## The advisory learner

[`LmdbNativeAdvisoryLearner`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAdvisoryLearner.java) is enabled with `rdf4j.lmdb.advisoryLearner.enabled`. With it off, no evidence is recorded, nothing is written, and a decision depends on the quotes alone.

Evidence is the ratio of the measured time of a complete run to its quoted expected price, kept as a mean of logarithms with a forgetting factor per key. A key is the strategy id and variant, the store's index generation, the access source of the plan's root pattern, the class of the entry bindings, and the size class of the quoted output, so a price learned on one index set is never consulted on another.

[`LmdbNativeCostObservation`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeCostObservation.java) measures a run. It times the pulls of the selected strategy, the first ones individually and later ones by sampling, so a slow consumer of the results is not charged to the strategy. Strategies that materialize while they open are timed by their open. A run that fails, is cancelled, or closes before its end (a `LIMIT` that was satisfied) reports nothing. Runs always compare with the quoted price, never the advised one, so advice cannot feed on itself.

After eight complete runs of a key, the learned price is the quoted expectation scaled by the observed ratio and clamped into the quoted interval. The expectation and both ends of the interval then move towards the learned price by the confidence of the evidence. This gives the layer its guarantees:

- An interval is never widened, and a price never leaves the quoted interval, however far the measurements went.
- An unknown ceiling stays unknown.
- Structural dominance is applied by the selector regardless of price, so learned prices cannot promote a strategy over one that dominates it.

## Persistence

A store's evidence is written to `native-advisory-learner.tsv` in its data directory when the store closes, through a temporary file and an atomic move. Loading keeps only the rows of the current index generation, and a file that cannot be read in full starts the learner cold. A failed write is logged and does not stop the store from closing.

## What is gone

Earlier versions learned machine coefficients, hierarchical runtime posteriors and workload regimes, and ran bounded probes and hedged races. All of it was replaced by the measured static prior and this learner, and the matching `rdf4j.lmdb.costCalibration.*`, `rdf4j.lmdb.costModel.*`, `rdf4j.lmdb.adaptiveProbe.enabled` and `rdf4j.lmdb.adaptiveHedge.*` properties no longer exist. A `cost-model.lncm` file left in an older store's data directory is ignored.

Source contracts: [`LmdbNativeAdvisoryLearnerTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAdvisoryLearnerTest.java) and [`LmdbNativeSelectorTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeSelectorTest.java).
