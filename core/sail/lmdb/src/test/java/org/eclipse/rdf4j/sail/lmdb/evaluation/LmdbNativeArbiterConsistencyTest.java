/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

/** Actual r9 counterexamples; no elapsed-time correctness assertions. */
class LmdbNativeArbiterConsistencyTest {
	static LmdbNativePhysicalVariantKey key(String name) {
		return LmdbNativePhysicalVariantKey.builder(name).physicalOrderAndProperties(name).build();
	}

	static LmdbNativeCostEstimate estimate(String name) {
		return new LmdbNativeCostEstimate(key(name), LmdbNativeCostVector.zero(),
				LmdbNativeCostVector.point(LmdbNativeCostVector.Feature.SCANNED_ROW, 100), 100, 0.1);
	}

	static LmdbNativeAdaptiveArbitration.Candidate<String> candidate(String name, int rank) {
		return new LmdbNativeAdaptiveArbitration.Candidate<>(estimate(name), rank, ignored -> name);
	}

	static LmdbNativeCostPosteriorStore.ExactKey exact(String name) {
		return LmdbNativeCostPosteriorStore.ExactKey.of(LmdbNativeRegimeKey.STEADY,
				LmdbNativeCostPosteriorStore.Lane.DIRECT, key(name));
	}

	static LmdbNativeProbeScheduler scheduler(AtomicLong clock) {
		return new LmdbNativeProbeScheduler(LmdbNativeProbeConfig.defaults(), clock::get);
	}

	@Test
	void admissionRechecksCooldownAtomically() {
		AtomicLong clock = new AtomicLong(1_000);
		var s = scheduler(clock);
		var k = key("race");
		var r = LmdbNativeRegimeKey.STEADY;
		assertTrue(s.mayProbe(k, r, 0));
		s.censored(k, r, 0, false); // other query finishes between precheck and acquisition
		assertFalse(s.beginProbe(k, r, 0), "beginProbe must not bypass a concurrent cooldown");
	}

	@Test
	void admissionRechecksQuarantineAtomically() {
		var s = scheduler(new AtomicLong(1_000));
		var k = key("race");
		var r = LmdbNativeRegimeKey.STEADY;
		assertTrue(s.mayProbe(k, r, 0));
		s.quarantine(k, r, 0);
		assertFalse(s.beginProbe(k, r, 0), "beginProbe must not bypass quarantine");
	}

	@Test
	void fullSchedulerMustDeclineUntrackableProbes() {
		var s = scheduler(new AtomicLong(1_000));
		var r = LmdbNativeRegimeKey.STEADY;
		for (int i = 0; i < 4096; i++)
			assertTrue(s.beginProbe(key("arm" + i), r, 0));
		assertFalse(s.beginProbe(key("overflow"), r, 0), "successful untracked admissions violate single flight");
		assertEquals(4096, s.trackedArms());
	}

	@Test
	void staleAbandonCannotReleaseNewEpochFlight() {
		var s = scheduler(new AtomicLong(1_000));
		var k = key("race");
		var r = LmdbNativeRegimeKey.STEADY;
		assertTrue(s.beginProbe(k, r, 0));
		// The policy may either block the new epoch until the old flight drains or replace it. In the latter case,
		// the old callback must not release its successor.
		boolean renewed = s.beginProbe(k, r, 1);
		s.probeAbandoned(k, r, 0);
		if (renewed)
			assertFalse(s.mayProbe(k, r, 1), "old abandon released the new flight");
		else
			assertTrue(s.beginProbe(k, r, 1));
	}

	@Test
	void staleCompletionCannotRemoveNewQuarantine() {
		var s = scheduler(new AtomicLong(1_000));
		var k = key("race");
		var r = LmdbNativeRegimeKey.STEADY;
		s.quarantine(k, r, 2);
		s.completed(k, r, 1, false);
		assertEquals(LmdbNativeProbeScheduler.State.QUARANTINED, s.state(k, r, 2));
	}

	@Test
	void staleCensorCannotRegressSchedulerEpoch() {
		var s = scheduler(new AtomicLong(1_000));
		var k = key("race");
		var r = LmdbNativeRegimeKey.STEADY;
		s.quarantine(k, r, 2);
		s.censored(k, r, 1, false);
		assertEquals(LmdbNativeProbeScheduler.State.QUARANTINED, s.state(k, r, 2));
	}

	@Test
	void staleCapacityOutcomeCannotRegressSchedulerEpoch() {
		var s = scheduler(new AtomicLong(1_000));
		var k = key("race");
		var r = LmdbNativeRegimeKey.STEADY;
		s.quarantine(k, r, 2);
		s.capacityExceeded(k, r, 1);
		assertEquals(LmdbNativeProbeScheduler.State.QUARANTINED, s.state(k, r, 2));
	}

	@Test
	void quarantineDeadlineCannotWrap() {
		AtomicLong clock = new AtomicLong(Long.MAX_VALUE - 1);
		var s = scheduler(clock);
		var k = key("race");
		var r = LmdbNativeRegimeKey.STEADY;
		s.quarantine(k, r, 0);
		assertEquals(LmdbNativeProbeScheduler.State.QUARANTINED, s.state(k, r, 0));
	}

	@Test
	void staleLatestMeasurementCannotOverwriteNewEpoch() {
		var l = new LmdbNativeLatestObservedLedger(true);
		var e = exact("arm");
		l.observe(e, 200, 2);
		l.observe(e, 900, 1);
		assertEquals(200L, l.latestNanos(e, 2), "late old callback erased valid latest evidence");
	}

	@Test
	void concurrentPosteriorUpdatesMustNotLoseCompletedExecutions() throws Exception {
		var p = new LmdbNativeCostPosteriorStore(LmdbNativePosteriorConfig.defaults());
		var e = exact("same");
		int threads = 8, each = 2000;
		CountDownLatch start = new CountDownLatch(1);
		AtomicReference<Throwable> error = new AtomicReference<>();
		Thread[] workers = new Thread[threads];
		for (int t = 0; t < threads; t++)
			workers[t] = Thread.ofPlatform().start(() -> {
				try {
					start.await();
					for (int i = 0; i < each; i++)
						p.updateCompleted(e, Math.log(1000), 1, 0, 1000);
				} catch (Throwable x) {
					error.compareAndSet(null, x);
				}
			});
		start.countDown();
		for (Thread w : workers)
			w.join();
		if (error.get() != null)
			throw new AssertionError(error.get());
		var read = p.read(e, 0, 1000);
		assertEquals((long) threads * each, read.exact().completedCount, "lost exact completion updates");
		assertEquals((long) threads * each, read.family().completedCount, "lost family completion updates");
		assertEquals((long) threads * each, read.global().completedCount, "lost global completion updates");
		assertEquals(1, p.admittedVariants(e.subPopulation()), "same variant consumed multiple admission slots");
	}

	@Test
	void repeatRestoreDoesNotConsumeAnotherAdmissionSlot() {
		var p = new LmdbNativeCostPosteriorStore(LmdbNativePosteriorConfig.defaults());
		var e = exact("same");
		var node = p.read(e, 0, 1000).exact();
		for (int i = 0; i < 5000; i++)
			p.restoreExact(e, node);
		assertEquals(1, p.admittedVariants(e.subPopulation()));
		var other = exact("other");
		p.restoreExact(other, node);
		assertEquals(2, p.admittedVariants(e.subPopulation()));
	}

	@Test
	void predictionPassDoesNotReadIncumbentTwice() {
		AtomicInteger reads = new AtomicInteger();
		var p = LmdbNativePosteriorConfig.defaults();
		var store = new LmdbNativeStoreCostModel(null, p, LmdbNativeProbeConfig.defaults(),
				() -> {
					reads.incrementAndGet();
					return 1000L;
				}, () -> 1_000_000L);
		var model = new LmdbNativeAdaptiveCostModel(new LmdbNativeMachineCostModel(), store,
				new LmdbNativeAdaptiveCostModel.Configuration(true, true));
		var arms = List.of(candidate("a", 0), candidate("b", 1), candidate("c", 2));
		LmdbNativeAdaptiveArbitration.choose(arms, model, LmdbNativeAdaptiveArbitration.ProbeContext.disabled());
		assertTrue(reads.get() <= arms.size(), "incumbent priced twice in one decision; reads=" + reads.get());
	}

	@Test
	void coldRescueUsesConfiguredDisplacementMargin() {
		var p = LmdbNativePosteriorConfig.defaults();
		var strict = new LmdbNativePosteriorConfig(p.noiseFloorLog(), p.noiseInitialLog(), p.varianceFloorExact(),
				p.varianceFloorFamily(), p.varianceFloorGlobal(), p.priorVarianceExact(), p.priorVarianceFamily(),
				p.priorVarianceGlobal(), p.huberSigmas(), p.decayHalfLifeMillis(), 1.0, 1.0, p.cooldownTtlMillis(),
				p.changeDetectorDelta(), p.changeDetectorLambda(), p.persistStaleHalfLifeMillis(), p.persistKappa(),
				p.persistPrecisionCap());
		var store = new LmdbNativeStoreCostModel(null, strict, () -> 1000L);
		var model = new LmdbNativeAdaptiveCostModel(new LmdbNativeMachineCostModel(), store,
				new LmdbNativeAdaptiveCostModel.Configuration(true, true));
		for (int i = 0; i < 6; i++) {
			store.posteriors().updateCompleted(exact("preferred"), Math.log(1_000_000), 1, 0, 1000);
			store.posteriors().updateCompleted(exact("rival"), Math.log(800_000), 1, 0, 1000);
		}
		store.latestObserved().observe(exact("preferred"), 1_000_000, 0);
		store.latestObserved().observe(exact("rival"), 800_000, 0);
		var result = LmdbNativeAdaptiveArbitration.choose(
				List.of(candidate("unknown", 0), candidate("preferred", 1), candidate("rival", 2)),
				model, LmdbNativeAdaptiveArbitration.ProbeContext.disabled());
		assertTrue(result instanceof LmdbNativeAdaptiveArbitration.DispatchPlan.Normal<?>);
		var chosen = (LmdbNativeAdaptiveArbitration.DispatchPlan.Normal<String>) result;
		assertEquals("preferred", chosen.candidate().estimate().variantKey().strategyFamily(),
				"rescue silently replaced configured log-margin=1 with default 0.1");
	}

	@Test
	void completionFromEarlierEpochCannotBecomeANewEpochMeasurement() {
		AtomicLong time = new AtomicLong(1000);
		var store = new LmdbNativeStoreCostModel(null, LmdbNativePosteriorConfig.defaults(), () -> 1000L);
		var model = new LmdbNativeAdaptiveCostModel(new LmdbNativeMachineCostModel(), store,
				new LmdbNativeAdaptiveCostModel.Configuration(true, true));
		var arm = estimate("oldFlight");
		var observation = new LmdbNativeCostObservation(arm, model, time::get);
		store.regimeTracker().forceEpochBump();
		time.addAndGet(10000);
		observation.exhausted();
		assertEquals(0L, model.predict(arm).latestObservedNanos(), "old run relabelled with new epoch");
		assertEquals(0L, model.predict(arm).exactCompletedCount(), "old run trained the new epoch");
	}

	@Test
	void corruptedTailWithAValidChecksumCannotPartiallyRestoreTheModel() throws Exception {
		var directory = java.nio.file.Files.createTempDirectory("arbiter-checkpoint-");
		String property = "rdf4j.lmdb.costModel.persist.enabled", previous = System.getProperty(property);
		System.setProperty(property, "true");
		try {
			var context = new LmdbNativeCostModelContext(directory, new java.util.UUID(0, 76),
					() -> 0L, () -> "DISABLED", () -> 0L, () -> 0L);
			var store = new LmdbNativeStoreCostModel(context, LmdbNativePosteriorConfig.defaults(), () -> 1000L);
			var e = exact("persisted");
			store.posteriors().updateCompleted(e, Math.log(5000), 1, 0, 1000);
			store.persistence().persistIfDue(true);
			var path = directory.resolve(LmdbNativeCostModelPersistence.FILE_NAME);
			byte[] bytes = java.nio.file.Files.readAllBytes(path);
			// Last section originally has zero entries. Claim one absent entry, then repair the checksum.
			java.nio.ByteBuffer.wrap(bytes).putInt(bytes.length - 12, 1);
			var crc = new java.util.zip.CRC32C();
			crc.update(bytes, 0, bytes.length - 8);
			java.nio.ByteBuffer.wrap(bytes).putLong(bytes.length - 8, crc.getValue());
			java.nio.file.Files.write(path, bytes);
			var restored = new LmdbNativeStoreCostModel(context, LmdbNativePosteriorConfig.defaults(), () -> 1000L);
			assertFalse(restored.posteriors().read(e, 0, 1000).anyEvidence(),
					"parse failure restored a prefix of a corrupt checkpoint");
		} finally {
			if (previous == null)
				System.clearProperty(property);
			else
				System.setProperty(property, previous);
			try (var files = java.nio.file.Files.walk(directory)) {
				for (var f : files.sorted(java.util.Comparator.reverseOrder()).toList())
					java.nio.file.Files.delete(f);
			}
		}
	}

	@Test
	void frontierAndColdRescueAreIndependentOfCallerIterationOrder() {
		var a = new LmdbNativeAdaptiveArbitration.Priced<>(candidate("a", 0),
				LmdbNativeCostPrediction.ordinalOnly("cold"));
		var b = new LmdbNativeAdaptiveArbitration.Priced<>(candidate("b", 1), numerical("b", 100, 0, 100));
		var c = new LmdbNativeAdaptiveArbitration.Priced<>(candidate("c", 2), numerical("c", 105, 0, 105));
		assertSame(a, LmdbNativeAdaptiveArbitration.selectWithinFrontier(List.of(c, b, a)),
				"ordinal tie used input order");
		assertSame(b, LmdbNativeAdaptiveArbitration.coldStartRescue(List.of(c, a, b)),
				"rescue assumed element zero is incumbent");
	}

	private static LmdbNativeCostPrediction numerical(String family, double location, double variance, long latest) {
		var components = new LmdbNativeCostPrediction.Components(LmdbNativeRegimeKey.STEADY,
				LmdbNativeCostPosteriorStore.Lane.DIRECT, LmdbNativeFamilyShapeKey.of(key(family)), 0,
				Math.log(location), 0, 0, variance, 0);
		return new LmdbNativeCostPrediction(0, location, Double.MAX_VALUE, 0, Double.MAX_VALUE, 0, Double.MAX_VALUE,
				LmdbNativeCostPrediction.PriceBasis.DIRECT_POSTERIOR, true, false, 10, 10, latest,
				LmdbNativeCostPrediction.EvidenceSource.EXACT_VARIANT, components, "");
	}

	@Test
	void mixedLatestAndEstimatedComparisonsUseOneAcyclicLocation() {
		var a = numerical("a", 1000, 0, 10);
		var b = numerical("b", 1, 0, 20);
		var c = numerical("c", 100, 0, 0);
		assertFalse(LmdbNativeCostPrediction.displaces(a, b, .1)
				&& LmdbNativeCostPrediction.displaces(b, c, .1)
				&& LmdbNativeCostPrediction.displaces(c, a, .1), "three valid quotes form a dominance cycle");
	}

	@Test
	void positiveSystemSettingsCannotOverflowIntoNegativeProbeLimits() {
		String[] suffix = { "maxDeadlineMillis", "minDeadlineMicros", "cancelBoundMillis", "minSpacingMillis",
				"bufferRows", "maxPerQuery", "minSpacingDecisions" };
		java.util.Map<String, String> saved = new java.util.HashMap<>();
		try {
			for (String x : suffix) {
				String k = "rdf4j.lmdb.adaptiveProbe." + x;
				saved.put(k, System.getProperty(k));
				System.setProperty(k, Long.toString(Long.MAX_VALUE));
			}
			var c = LmdbNativeProbeConfig.system();
			assertTrue(c.maxDeadlineNanos() > 0 && c.minDeadlineNanos() > 0 && c.cancelBoundNanos() > 0
					&& c.minSpacingNanos() > 0 && c.bufferRows() > 0 && c.maxPerQuery() > 0
					&& c.minSpacingDecisions() > 0,
					"positive properties wrapped during unit conversion or int narrowing");
		} finally {
			saved.forEach((k, v) -> {
				if (v == null)
					System.clearProperty(k);
				else
					System.setProperty(k, v);
			});
		}
	}
}
