/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials are made
 * available under the Eclipse Distribution License v1.0.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

/** Regression tests run the real model and chooser with scripted durations, never wall-time assertions. */
public class LmdbNativeAdaptiveLaneTransitionTest {
	private static final long PACKED_NANOS = 719_000L;
	private static final long TAIL_NANOS = 13_690_000L;

	@Test
	public void fullRunPriceSurvivesMachineReadinessTransition() {
		Fixture f = new Fixture();
		var packed = estimate("packedFtreeAggregate");
		for (int i = 0; i < 6; i++) f.complete(packed, PACKED_NANOS);
		check(f.model.predict(packed).latestObservedNanos() == PACKED_NANOS, "initial full-run price missing");
		f.trainMachine(packed);
		var after = f.model.predict(packed);
		check(after.latestObservedNanos() == PACKED_NANOS,
				"machine readiness orphaned the latest complete execution: " + after);
		check(after.evidenceSource() == LmdbNativeCostPrediction.EvidenceSource.EXACT_VARIANT,
				"measured physical arm must not become an unmeasured machine estimate");
		check(after.exactCompletedCount() == 6L, "completed physical observations disappeared");
	}

	@Test
	public void trainedMachineCannotColdRescueSlowTailOverMeasuredPackedTree() {
		Fixture f = new Fixture();
		var packed = estimate("packedFtreeAggregate");
		var tail = estimate("factorizedTail");
		for (int i = 0; i < 6; i++) f.complete(packed, PACKED_NANOS);
		f.trainMachine(packed);
		for (int i = 0; i < 6; i++) f.complete(tail, TAIL_NANOS);
		// No harness: isolate selection from exploration. Both arms really completed; neither is cold.
		var result = LmdbNativeAdaptiveArbitration.choose(List.of(
				new LmdbNativeAdaptiveArbitration.Candidate<String>(packed, 0, o -> "packed"),
				new LmdbNativeAdaptiveArbitration.Candidate<String>(tail, 1, o -> "tail")), f.model, null);
		var normal = (LmdbNativeAdaptiveArbitration.DispatchPlan.Normal<String>) result;
		check(normal.candidate().estimate() == packed,
				"a known 719us arm was replaced by a 13690us cold-start rescue: " + normal.reason());
	}

	@Test
	public void exactDirectPosteriorSurvivesWithoutFullRunLedger() {
		Fixture f = new Fixture();
		var packed = estimate("packedFtreeAggregate");
		for (int i = 0; i < 6; i++) f.model.record(packed, packed.total(), PACKED_NANOS, true);
		check(f.model.predict(packed).latestObservedNanos() == 0L, "test must not use the full-run ledger");
		f.trainMachine(packed);
		var price = f.model.predict(packed);
		check(price.evidenceSource() == LmdbNativeCostPrediction.EvidenceSource.EXACT_VARIANT,
				"DIRECT posterior evidence was dropped when feature support became available");
		check(price.exactCompletedCount() == 6L, "direct completions were not retained");
		check(Math.abs(price.expectedNanos() - PACKED_NANOS) < PACKED_NANOS * 0.05,
				"direct log latency was mistaken for a feature residual");
	}

	@Test
	public void predictionFallbackDoesNotPreventResidualTraining() {
		Fixture f = new Fixture();
		var arm = estimate("packedFtreeAggregate");
		for (int i = 0; i < 6; i++) f.model.record(arm, arm.total(), PACKED_NANOS, true);
		f.trainMachine(arm);
		for (int i = 0; i < 3; i++) f.model.record(arm, arm.total(), PACKED_NANOS, true);
		var residual = f.store.posteriors().read(key(arm, LmdbNativeCostPosteriorStore.Lane.RESIDUAL),
				f.store.regimeTracker().epoch(), f.wall.get());
		check(residual.exact().completedCount == 3L, "prediction fallback accidentally pins updates to DIRECT");
		var prediction = f.model.predict(arm);
		check(prediction.components().lane() == LmdbNativeCostPosteriorStore.Lane.RESIDUAL,
				"new exact residual evidence must be consumed");
		check(prediction.exactCompletedCount() == 9L, "confirmation must count disjoint completions in both lanes");
	}

	@Test
	public void directCensorMustNotBecomeAnUnmeasuredOptimisticPrior() {
		Fixture f = new Fixture();
		var arm = estimate("irAggregate");
		f.model.recordCensored(arm, 100_000_000L, arm.total(), LmdbNativeRegimeKey.STEADY);
		f.trainMachine(arm);
		var prediction = f.model.predict(arm);
		check(prediction.evidenceSource() == LmdbNativeCostPrediction.EvidenceSource.EXACT_VARIANT,
				"representation changes must not erase censoring either");
		check(prediction.exactCompletedCount() == 0L, "censoring is not a completion");
		check(prediction.latestObservedNanos() == 0L, "censoring is not a full-run time");
	}

	@Test
	public void residualCensorIsNotHiddenByOldDirectEvidence() {
		Fixture f = new Fixture();
		var arm = estimate("packedFtreeAggregate");
		for (int i = 0; i < 6; i++) f.model.record(arm, arm.total(), PACKED_NANOS, true);
		f.trainMachine(arm);
		f.model.recordCensored(arm, 4_000_000L, arm.total(), LmdbNativeRegimeKey.STEADY);
		var prediction = f.model.predict(arm);
		check(prediction.components().lane() == LmdbNativeCostPosteriorStore.Lane.RESIDUAL,
				"a new exact censor must not be hidden by the old direct posterior");
		check(prediction.exactCompletedCount() == 6L, "censor does not erase or add completed executions");
	}

	@Test
	public void latestLedgerIsLaneIndependentButStillEpochAndVariantScoped() {
		var ledger = new LmdbNativeLatestObservedLedger(true);
		var arm = estimate("irAggregate");
		var direct = key(arm, LmdbNativeCostPosteriorStore.Lane.DIRECT);
		var residual = key(arm, LmdbNativeCostPosteriorStore.Lane.RESIDUAL);
		ledger.observe(direct, PACKED_NANOS, 7L);
		check(ledger.latestNanos(residual, 7L) == PACKED_NANOS, "DIRECT/RESIDUAL is not an execution identity");
		ledger.observe(residual, TAIL_NANOS, 7L);
		check(ledger.latestNanos(direct, 7L) == TAIL_NANOS, "newer slower observations must also replace the price");
		check(ledger.size() == 1, "one physical execution identity must occupy one ledger entry");
		check(ledger.latestNanos(direct, 8L) == 0L, "stale epoch leaked");
		check(ledger.latestNanos(key(estimate("irAggregateInterpreted"),
				LmdbNativeCostPosteriorStore.Lane.DIRECT), 7L) == 0L, "sibling variant leaked");
	}

	@Test
	public void probesDoNotReplaceFullRunPricesAcrossLanes() {
		Fixture f = new Fixture();
		var arm = estimate("irAggregate");
		f.complete(arm, PACKED_NANOS);
		f.trainMachine(arm);
		f.observe(arm, 20_000L, LmdbNativeCostObservation.Role.PROBE);
		check(f.model.predict(arm).latestObservedNanos() == PACKED_NANOS,
				"a probe must neither orphan nor replace the real full-run time after a lane switch");
	}

	@Test
	public void persistedDirectEvidenceRemainsUsableAfterRestartWithTrainedMachine() throws Exception {
		java.nio.file.Path directory = java.nio.file.Files.createTempDirectory("rdf4j-lane-evidence-");
		String previous = System.getProperty("rdf4j.lmdb.costModel.persist.enabled");
		System.setProperty("rdf4j.lmdb.costModel.persist.enabled", "true");
		try {
			AtomicLong wall = new AtomicLong(1_000_000L);
			var context = new LmdbNativeCostModelContext(directory, new java.util.UUID(0L, 9L),
					() -> 0L, () -> "DISABLED", () -> 0L, () -> 3L);
			var original = new LmdbNativeStoreCostModel(context, LmdbNativePosteriorConfig.defaults(),
					LmdbNativeProbeConfig.defaults(), wall::get, () -> 0L);
			var arm = estimate("packedFtreeAggregate");
			var direct = key(arm, LmdbNativeCostPosteriorStore.Lane.DIRECT);
			for (int i = 0; i < 6; i++) original.posteriors().updateCompleted(direct,
					Math.log(PACKED_NANOS), 1.0, 0L, wall.get());
			original.persistence().persistIfDue(true);
			check(java.nio.file.Files.isRegularFile(directory.resolve(LmdbNativeCostModelPersistence.FILE_NAME)),
					"real posterior sidecar was not written");
			var restored = new LmdbNativeStoreCostModel(context, LmdbNativePosteriorConfig.defaults(),
					LmdbNativeProbeConfig.defaults(), wall::get, () -> 0L);
			var machine = new LmdbNativeMachineCostModel();
			for (int i = 0; i < 32; i++) machine.update(arm.total(), PACKED_NANOS);
			var model = new LmdbNativeAdaptiveCostModel(machine, restored,
					new LmdbNativeAdaptiveCostModel.Configuration(true, true));
			var prediction = model.predict(arm);
			check(prediction.latestObservedNanos() == 0L, "full-run ledger must not be persisted");
			check(prediction.evidenceSource() == LmdbNativeCostPrediction.EvidenceSource.EXACT_VARIANT
					&& prediction.exactCompletedCount() == 6L,
					"a compatible persisted DIRECT arm became unmeasured on a warm machine");
		} finally {
			if (previous == null) System.clearProperty("rdf4j.lmdb.costModel.persist.enabled");
			else System.setProperty("rdf4j.lmdb.costModel.persist.enabled", previous);
			try (var paths = java.nio.file.Files.walk(directory)) {
				for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
					java.nio.file.Files.deleteIfExists(path);
				}
			}
		}
	}

	@Test
	public void directHistoryDoesNotLeakToNewShapesOrRegimes() {
		Fixture f = new Fixture();
		var arm = estimate("packedFtreeAggregate");
		for (int i = 0; i < 6; i++) f.complete(arm, PACKED_NANOS);
		f.trainMachine(arm);
		var other = new LmdbNativeCostEstimate(arm.variantKey().toBuilder().cardinalityBucket(31).build(),
				LmdbNativeCostVector.zero(), arm.total(), arm.estimatedOutputRows(), arm.structuralUncertainty());
		check(f.model.predict(other).exactCompletedCount() == 0L, "different variant inherited exact evidence");
		var building = new LmdbNativeRegimeKey(LmdbNativeRegimeKey.Phase.BUILDING, 0L, false, 0L);
		check(!f.store.posteriors().read(LmdbNativeCostPosteriorStore.ExactKey.of(building,
				LmdbNativeCostPosteriorStore.Lane.DIRECT, arm.variantKey()), 0L, f.wall.get()).exactPresent(),
				"different regime inherited evidence");
	}

	private static LmdbNativeCostPosteriorStore.ExactKey key(LmdbNativeCostEstimate arm,
			LmdbNativeCostPosteriorStore.Lane lane) {
		return LmdbNativeCostPosteriorStore.ExactKey.of(LmdbNativeRegimeKey.STEADY, lane, arm.variantKey());
	}

	private static LmdbNativeCostEstimate estimate(String family) {
		return new LmdbNativeCostEstimate(LmdbNativePhysicalVariantKey.builder(family)
				.physicalOrderAndProperties(family).build(), LmdbNativeCostVector.zero(),
				LmdbNativeCostVector.point(LmdbNativeCostVector.Feature.SCANNED_ROW, 100), 100, 0.1);
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	private static final class Fixture {
		final AtomicLong wall = new AtomicLong(1_000_000L);
		final LmdbNativeStoreCostModel store = new LmdbNativeStoreCostModel(null,
				LmdbNativePosteriorConfig.defaults(), LmdbNativeProbeConfig.defaults(), wall::get, () -> 0L);
		final LmdbNativeMachineCostModel machine = new LmdbNativeMachineCostModel();
		final LmdbNativeAdaptiveCostModel model = new LmdbNativeAdaptiveCostModel(machine, store,
				new LmdbNativeAdaptiveCostModel.Configuration(true, true));

		void complete(LmdbNativeCostEstimate arm, long nanos) {
			observe(arm, nanos, LmdbNativeCostObservation.Role.NORMAL);
		}

		void observe(LmdbNativeCostEstimate arm, long nanos, LmdbNativeCostObservation.Role role) {
			AtomicLong clock = new AtomicLong();
			var observation = new LmdbNativeCostObservation(arm, model, clock::get, role);
			clock.set(nanos);
			observation.exhausted();
		}

		void trainMachine(LmdbNativeCostEstimate arm) {
			// Other arms/stores share the machine model: they can cross its readiness threshold without
			// executing this particular arm again. No sleep or wall-clock warmup is needed.
			while (machine.snapshot().sampleCount() < 32L) machine.update(arm.total(), PACKED_NANOS);
			check(machine.predict(arm.total()).adaptiveReady(), "fixture did not cross the real readiness threshold");
		}
	}
}
