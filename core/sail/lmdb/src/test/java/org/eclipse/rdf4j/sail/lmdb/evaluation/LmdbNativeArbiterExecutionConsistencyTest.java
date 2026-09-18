/* Copyright (c) 2026 Eclipse RDF4J contributors. SPDX-License-Identifier: BSD-3-Clause */
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelQueryCancelledException;
import org.junit.jupiter.api.Test;

class LmdbNativeArbiterExecutionConsistencyTest {
	@Test
	void externalCancellationSettlesFlightAndReservationWithoutFallback() throws Exception {
		var store = new LmdbNativeStoreCostModel(null, LmdbNativePosteriorConfig.defaults(),
				LmdbNativeProbeConfig.defaults(), () -> 1000L, () -> 1000000000L);
		var model = new LmdbNativeAdaptiveCostModel(new LmdbNativeMachineCostModel(), store,
				new LmdbNativeAdaptiveCostModel.Configuration(true, true));
		var incumbent = new LmdbNativeStrategyProposal<String>(() -> "fallback", LmdbNativeWork.exact(1000),
				LmdbNativeAttemptMetrics.PATH_BATCH, () -> {
				});
		var estimate = incumbent.adaptiveEstimate(-1);
		for (int i = 0; i < 30; i++)
			model.recordCompleted(estimate, estimate.total(), 5_000_000, 1, model.currentRegime());
		store.safetyLedger().earn(1_000_000_000_000L);
		AtomicInteger opens = new AtomicInteger(), discarded = new AtomicInteger();
		var trial = new LmdbNativeStrategyProposal<String>(() -> {
			opens.incrementAndGet();
			return "private";
		},
				LmdbNativeWork.UNKNOWN, LmdbNativeAttemptMetrics.PATH_PARALLEL_PIPELINES, () -> {
				});
		long before = store.safetyLedger().availableNanos();
		try (var arbiter = LmdbNativeStrategyArbiter.<String>forModel(null, -1, model)
				.probeHarness(new LmdbNativeProbeHarness<>() {
					public String drainBounded(String v, LmdbNativeCostObservation o, int cap) {
						throw KernelQueryCancelledException.INSTANCE;
					}

					public void discard(String v) {
						discarded.incrementAndGet();
					}
				})) {
			arbiter.offer(() -> incumbent).offer(() -> trial);
			try {
				arbiter.select();
				throw new AssertionError("cancellation swallowed");
			} catch (KernelQueryCancelledException expected) {
			}
		}
		assertEquals(1, opens.get());
		assertEquals(1, discarded.get(), "cancelled private output leaked");
		var state = store.probeScheduler()
				.state(trial.directTimingEstimate().variantKey(), model.currentRegime(),
						store.regimeTracker().epoch());
		assertFalse(state == LmdbNativeProbeScheduler.State.PROBING, "cancelled flight stuck forever");
		// Cancellation charges actual elapsed time, never leaves the full reservation outstanding.
		assertTrue(store.safetyLedger().availableNanos() <= before);
	}

	@Test
	void legacyTieBreakDoesNotDependOnDeclarationOrder() {
		var a = new LmdbNativeStrategyProposal<String>(() -> "a", LmdbNativeWork.UNKNOWN, "a-custom", () -> {
		});
		var z = new LmdbNativeStrategyProposal<String>(() -> "z", LmdbNativeWork.UNKNOWN, "z-custom", () -> {
		});
		for (var list : java.util.List.of(java.util.List.of(a, z), java.util.List.of(z, a)))
			assertEquals("a-custom", list.get(LmdbNativeStrategyArbiter.rank(list)).tag);
	}

	@Test
	void bindTimeDeclinesAreTriedAtMostOnceInStableOrder() throws Exception {
		var opened = new java.util.ArrayList<String>();
		try (var a = LmdbNativeStrategyArbiter.<String>forSlice(null, -1)) {
			for (String tag : new String[] { "z-last", "b-declines", "a-declines" })
				a.offer(() -> new LmdbNativeStrategyProposal<>(() -> {
					opened.add(tag);
					return tag.equals("z-last") ? "value" : null;
				},
						LmdbNativeWork.UNKNOWN, tag, () -> {
						}));
			assertEquals("value", a.select());
			assertEquals(java.util.List.of("a-declines", "b-declines", "z-last"), opened);
		}
	}

	@Test
	void closingOneFaultyCandidateStillClosesTheRestExactlyOnce() throws Exception {
		var a = LmdbNativeStrategyArbiter.<String>forSlice(null, -1);
		AtomicInteger released = new AtomicInteger();
		var failure = new IllegalStateException("release");
		a.offer(() -> new LmdbNativeStrategyProposal<>(() -> null, LmdbNativeWork.UNKNOWN, "a", () -> {
			released.incrementAndGet();
			throw failure;
		}));
		a.offer(() -> new LmdbNativeStrategyProposal<>(() -> null, LmdbNativeWork.UNKNOWN, "b",
				released::incrementAndGet));
		try {
			a.close();
			throw new AssertionError("cleanup error swallowed");
		} catch (IllegalStateException actual) {
			assertSame(failure, actual);
		}
		a.close();
		assertEquals(2, released.get());
		assertEquals(0, a.candidateCount());
	}

	@Test
	void forcedFilteringReleasesAllRejectedCandidatesEvenWhenOneCloseFails() throws Exception {
		AtomicInteger released = new AtomicInteger();
		var failure = new IllegalStateException("reject close");
		try (var a = LmdbNativeStrategyArbiter.<String>forSlice(null, -1)
				.forcingWhereApplicable("not-present", "test")) {
			a.offer(() -> new LmdbNativeStrategyProposal<>(() -> null, LmdbNativeWork.UNKNOWN, "a", () -> {
				released.incrementAndGet();
				throw failure;
			}));
			a.offer(() -> new LmdbNativeStrategyProposal<>(() -> null, LmdbNativeWork.UNKNOWN, "b",
					released::incrementAndGet));
			try {
				a.select();
				throw new AssertionError("cleanup error swallowed");
			} catch (IllegalStateException actual) {
				assertSame(failure, actual);
			}
			assertEquals(2, released.get(), "removed candidate lost before cleanup sweep");
		}
	}

	@Test
	void unreturnedNormalValueIsClosedWhenLoserReleaseFails() throws Exception {
		AtomicInteger released = new AtomicInteger(), valueClosed = new AtomicInteger();
		var failure = new IllegalStateException("loser close");
		try (var a = LmdbNativeStrategyArbiter.<AutoCloseable>forSlice(null, -1)) {
			a.offer(() -> new LmdbNativeStrategyProposal<>(() -> valueClosed::incrementAndGet, LmdbNativeWork.exact(1),
					"a", () -> {
					}));
			a.offer(() -> new LmdbNativeStrategyProposal<>(() -> null, LmdbNativeWork.exact(100), "b", () -> {
				released.incrementAndGet();
				throw failure;
			}));
			a.offer(() -> new LmdbNativeStrategyProposal<>(() -> null, LmdbNativeWork.exact(100), "c",
					released::incrementAndGet));
			try {
				a.select();
				throw new AssertionError("cleanup error swallowed");
			} catch (IllegalStateException actual) {
				assertSame(failure, actual);
			}
			assertEquals(2, released.get());
			assertEquals(1, valueClosed.get(), "opened but unreturned cursor leaked");
		}
	}

	@Test
	void realProbeDrainFailureDiscardsPrivateValueAndDoesNotFallback() throws Exception {
		var store = new LmdbNativeStoreCostModel(null, LmdbNativePosteriorConfig.defaults(),
				LmdbNativeProbeConfig.defaults(), () -> 1000L, () -> 1000000000L);
		var model = new LmdbNativeAdaptiveCostModel(new LmdbNativeMachineCostModel(), store,
				new LmdbNativeAdaptiveCostModel.Configuration(true, true));
		AtomicInteger fallback = new AtomicInteger(), discarded = new AtomicInteger();
		var incumbent = new LmdbNativeStrategyProposal<String>(() -> {
			fallback.incrementAndGet();
			return "fallback";
		},
				LmdbNativeWork.exact(1000), LmdbNativeAttemptMetrics.PATH_BATCH, () -> {
				});
		var e = incumbent.adaptiveEstimate(-1);
		for (int i = 0; i < 30; i++)
			model.recordCompleted(e, e.total(), 5_000_000, 1, model.currentRegime());
		store.safetyLedger().earn(1_000_000_000_000L);
		var failure = new java.io.IOException("drain failed");
		try (var a = LmdbNativeStrategyArbiter.<String>forModel(null, -1, model)
				.probeHarness(new LmdbNativeProbeHarness<>() {
					public String drainBounded(String v, LmdbNativeCostObservation o, int cap)
							throws java.io.IOException {
						throw failure;
					}

					public void discard(String v) {
						discarded.incrementAndGet();
					}
				})) {
			a.offer(() -> incumbent)
					.offer(() -> new LmdbNativeStrategyProposal<>(() -> "private", LmdbNativeWork.UNKNOWN,
							LmdbNativeAttemptMetrics.PATH_PARALLEL_PIPELINES, () -> {
							}));
			try {
				a.select();
				throw new AssertionError("failure swallowed");
			} catch (java.io.IOException actual) {
				assertSame(failure, actual);
			}
		}
		assertEquals(1, discarded.get());
		assertEquals(0, fallback.get());
	}

	@Test
	void wholeArbiterPricesEachArmFromOneClockCapture() {
		AtomicInteger reads = new AtomicInteger();
		var store = new LmdbNativeStoreCostModel(null, LmdbNativePosteriorConfig.defaults(),
				() -> 1000L + reads.incrementAndGet());
		var model = new LmdbNativeAdaptiveCostModel(new LmdbNativeMachineCostModel(), store,
				new LmdbNativeAdaptiveCostModel.Configuration(true, true));
		var a = new LmdbNativeStrategyProposal<String>(() -> "a", LmdbNativeWork.exact(100), "a", () -> {
		});
		var b = new LmdbNativeStrategyProposal<String>(() -> "b", LmdbNativeWork.exact(101), "b", () -> {
		});
		int before = reads.get();
		LmdbNativeStrategyArbiter.rankAdaptive(java.util.List.of(a, b), -1, model);
		assertEquals(1, reads.get() - before, "cold gate and final decision used different quotes");
	}
}
