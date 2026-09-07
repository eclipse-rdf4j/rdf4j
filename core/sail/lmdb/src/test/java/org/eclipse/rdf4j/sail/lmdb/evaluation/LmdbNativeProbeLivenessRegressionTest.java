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

/** Real policy/posteriors with scripted completed durations and deterministic scheduler clocks. */
public class LmdbNativeProbeLivenessRegressionTest {
	private static final String COMPILED = "irAggregate";
	private static final String INTERPRETED = "irAggregateInterpreted";
	private static final String PACKED = "packedFtreeAggregate";
	private static final String TAIL = "factorizedTail";

	@Test
	public void subFloorRescueStillOffersABoundedTrialOfThePreferredKernel() {
		Fixture f = new Fixture(INTERPRETED, 544_541L);
		var trial = estimate(COMPILED);
		var plan = f.choose(trial, true);
		check(plan instanceof LmdbNativeAdaptiveArbitration.DispatchPlan.Probe<?>,
				"preferred compiled kernel is starved behind a 544541ns interpreted winner: " + plan);
		var probe = (LmdbNativeAdaptiveArbitration.DispatchPlan.Probe<String>) plan;
		try {
			check(probe.trial().estimate() == trial, "wrong trial");
			check(probe.fallback().estimate() == f.anchor, "measured fallback must remain available");
			long ceiling = (long) (probe.fallbackPrediction().expectedNanos() * f.config.maxSlowdownFraction());
			check(probe.deadlineNanos() > 0L && probe.deadlineNanos() <= ceiling, "deadline exceeds slowdown cap");
			check(probe.deadlineNanos() < f.config.minDeadlineNanos(), "test did not hit the floor");
		} finally {
			probe.reservation().refund();
		}
	}

	@Test
	public void twoFlagValuesProduceTheSameBoundedRecoveryDecision() {
		String property = "rdf4j.lmdb.janinoCodegen.factorGuardPeeling";
		String previous = System.getProperty(property);
		try {
			for (String flag : List.of("false", "true")) {
				System.setProperty(property, flag);
				Fixture f = new Fixture(INTERPRETED, 544_541L);
				var plan = f.choose(estimate(COMPILED), true);
				check(plan instanceof LmdbNativeAdaptiveArbitration.DispatchPlan.Probe<?>, "flag=" + flag + ": " + plan);
				((LmdbNativeAdaptiveArbitration.DispatchPlan.Probe<?>) plan).reservation().refund();
			}
		} finally {
			if (previous == null) System.clearProperty(property);
			else System.setProperty(property, previous);
		}
	}

	@Test
	public void completedFasterKernelIsNotStarvedDuringConfirmation() {
		Fixture f = new Fixture(INTERPRETED, 544_541L);
		var trial = estimate(COMPILED);
		f.model.record(trial, trial.total(), 251_291.0, true);
		var priced = List.of(new LmdbNativeAdaptiveArbitration.Priced<>(
				new LmdbNativeAdaptiveArbitration.Candidate<String>(trial, 0, o -> "trial"), f.model.predict(trial)),
				new LmdbNativeAdaptiveArbitration.Priced<>(
						new LmdbNativeAdaptiveArbitration.Candidate<String>(f.anchor, 1, o -> "anchor"),
						f.model.predict(f.anchor)));
		var probe = LmdbNativeAdaptiveArbitration.maybeProbe(priced, priced.get(1), f.model, f.context(true));
		check(probe != null, "a faster under-confirmed preferred kernel still needs bounded evidence");
		probe.reservation().refund();
	}

	@Test
	public void lowerPreferenceOptionalExplorationStillHonorsTheFloor() {
		Fixture f = new Fixture(INTERPRETED, 544_541L);
		var candidate = estimate("optionalCandidate");
		var plan = LmdbNativeAdaptiveArbitration.choose(List.of(
				new LmdbNativeAdaptiveArbitration.Candidate<String>(f.anchor, 0, o -> "anchor"),
				new LmdbNativeAdaptiveArbitration.Candidate<String>(candidate, 1, o -> "candidate")),
				f.model, f.context(true));
		check(plan instanceof LmdbNativeAdaptiveArbitration.DispatchPlan.Normal<?>,
				"optional exploration must remain gated");
	}

	@Test
	public void absentHarnessNeverRunsAnUnboundedPreferredTrial() {
		Fixture f = new Fixture(INTERPRETED, 544_541L);
		var plan = f.choose(estimate(COMPILED), false);
		check(plan instanceof LmdbNativeAdaptiveArbitration.DispatchPlan.Normal<?>, "missing harness");
		check(((LmdbNativeAdaptiveArbitration.DispatchPlan.Normal<?>) plan).candidate().estimate() == f.anchor,
				"fallback lost");
	}

	@Test
	public void schedulerCooldownAndSpacingRemainAuthoritative() {
		Fixture f = new Fixture(INTERPRETED, 544_541L);
		var candidate = estimate(COMPILED);
		f.store.probeScheduler().censored(candidate.variantKey(), f.model.currentRegime(), 0L, false);
		check(f.choose(candidate, true) instanceof LmdbNativeAdaptiveArbitration.DispatchPlan.Normal<?>,
				"cooldown bypassed");
		f.wall.addAndGet(f.config.cooldownBaseMillis() + 1);
		f.store.safetyLedger().noteProbeDecision();
		check(f.choose(candidate, true) instanceof LmdbNativeAdaptiveArbitration.DispatchPlan.Normal<?>,
				"spacing bypassed");
	}

	@Test
	public void slowTailAllowsFreshPackedTrialWhenSchedulerIsEligible() {
		Fixture f = new Fixture(TAIL, 13_690_000L);
		var candidate = estimate(PACKED);
		var plan = f.choose(candidate, true);
		check(plan instanceof LmdbNativeAdaptiveArbitration.DispatchPlan.Probe<?>,
				"a healthy slow-tail case already probes packed; the supplied last-invocation log omits its blocker");
		var probe = (LmdbNativeAdaptiveArbitration.DispatchPlan.Probe<?>) plan;
		check(probe.trial().estimate() == candidate, "wrong candidate");
		probe.reservation().refund();
	}

	@Test
	public void slowTailRemainsSelectedWhilePackedIsInCooldown() {
		Fixture f = new Fixture(TAIL, 13_690_000L);
		var candidate = estimate(PACKED);
		f.store.probeScheduler().capacityExceeded(candidate.variantKey(), f.model.currentRegime(), 0L);
		var plan = f.choose(candidate, true);
		check(plan instanceof LmdbNativeAdaptiveArbitration.DispatchPlan.Normal<?>,
				"cooldown should retain safe winner");
		var normal = (LmdbNativeAdaptiveArbitration.DispatchPlan.Normal<?>) plan;
		check(normal.candidate().estimate() == f.anchor && normal.reason().contains("cold-start"),
				"not the observed branch");
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
		final AtomicLong nano = new AtomicLong(1_000_000_000L);
		final AtomicLong wall = new AtomicLong(1_000_000L);
		final LmdbNativeProbeConfig config = LmdbNativeProbeConfig.defaults();
		final LmdbNativeStoreCostModel store = new LmdbNativeStoreCostModel(null,
				LmdbNativePosteriorConfig.defaults(), config, wall::get, nano::get);
		final LmdbNativeAdaptiveCostModel model = new LmdbNativeAdaptiveCostModel(new LmdbNativeMachineCostModel(),
				store, new LmdbNativeAdaptiveCostModel.Configuration(true, true));
		final LmdbNativeCostEstimate anchor;

		Fixture(String family, long nanos) {
			anchor = estimate(family);
			for (int i = 0; i < 6; i++) {
				AtomicLong runClock = new AtomicLong();
				var observation = new LmdbNativeCostObservation(anchor, model, runClock::get);
				runClock.set(nanos);
				observation.exhausted();
			}
		}

		LmdbNativeAdaptiveArbitration.ProbeContext context(boolean harness) {
			return new LmdbNativeAdaptiveArbitration.ProbeContext(config, new LmdbNativeQueryProbeBudget(), harness);
		}

		LmdbNativeAdaptiveArbitration.DispatchPlan<String> choose(LmdbNativeCostEstimate trial, boolean harness) {
			return LmdbNativeAdaptiveArbitration.choose(List.of(
					new LmdbNativeAdaptiveArbitration.Candidate<String>(trial, 0, o -> "trial"),
					new LmdbNativeAdaptiveArbitration.Candidate<String>(anchor, 1, o -> "anchor")),
					model, context(harness));
		}
	}
}
