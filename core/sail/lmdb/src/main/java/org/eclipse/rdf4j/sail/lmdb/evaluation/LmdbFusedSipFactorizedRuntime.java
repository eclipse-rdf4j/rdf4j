/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Query-owned runtime shared by generated SIP guards, factorized batches and bind-time filter placement.
 * <p>
 * The root session owns one bounded memory account and one telemetry accumulator. Parallel workers inherit that root:
 * they keep worker-confined mutable batches, but share immutable SIP domains, placement schedules and counters. This is
 * important both for correctness (all workers see the same no-false-negative domain) and observability (worker counters
 * are merged before the query plan is rendered instead of disappearing when a worker thread exits).
 */
final class LmdbFusedSipFactorizedRuntime {

	private static final int DEFAULT_SLOTS = 16;
	private static final int MAX_DOMAIN_ID = 1 << 20;
	private static final long DEFAULT_QUERY_BUDGET = Long.getLong("rdf4j.lmdb.native.fused.maxBytes", 8L << 20);
	private static final ThreadLocal<Session> CURRENT = new ThreadLocal<>();
	private static final LmdbRuntimeFilterRelocator RELOCATOR = new LmdbRuntimeFilterRelocator();
	private static final Session NOOP = Session.noop();

	private LmdbFusedSipFactorizedRuntime() {
	}

	static Scope enter(long queryKey) {
		return enter(queryKey, DEFAULT_QUERY_BUDGET, true);
	}

	static Scope enter(long queryKey, long maximumBytes) {
		return enter(queryKey, maximumBytes, true);
	}

	static Scope enter(long queryKey, long maximumBytes, boolean telemetryEnabled) {
		Session session = create(queryKey, maximumBytes, telemetryEnabled);
		return new Scope(CURRENT.get(), session, true);
	}

	static Session create(long queryKey, boolean telemetryEnabled) {
		return create(queryKey, DEFAULT_QUERY_BUDGET, telemetryEnabled);
	}

	static Session create(long queryKey, long maximumBytes, boolean telemetryEnabled) {
		return new Session(queryKey, maximumBytes, telemetryEnabled, null);
	}

	/** Attaches an existing session to the current thread without taking ownership of it. */
	static Scope attach(Session session) {
		return new Scope(CURRENT.get(), session == null ? NOOP : session, false);
	}

	/** Creates a worker-confined child whose counters and immutable domains are shared with {@code parent}. */
	static Scope inherit(Session parent) {
		Session child = parent == null || parent.noop ? NOOP
				: new Session(parent.queryKey, parent.maximumBytes, parent.shared.telemetryEnabled, parent);
		return new Scope(CURRENT.get(), child, child != NOOP);
	}

	static Session currentOrNull() {
		Session session = CURRENT.get();
		return session == NOOP ? null : session;
	}

	static Session current() {
		Session session = CURRENT.get();
		return session == null ? NOOP : session;
	}

	static boolean mayContain(int domainId, long value) {
		return current().mayContain(domainId, value);
	}

	static final class Scope implements AutoCloseable {
		private final Session previous;
		private final Session session;
		private final boolean closeSession;
		private boolean closed;

		Scope(Session previous, Session session, boolean closeSession) {
			this.previous = previous;
			this.session = session;
			this.closeSession = closeSession;
			CURRENT.set(session);
		}

		Session session() {
			return session;
		}

		@Override
		public void close() {
			if (!closed) {
				closed = true;
				if (closeSession) {
					session.close();
				}
				if (previous == null) {
					CURRENT.remove();
				} else {
					CURRENT.set(previous);
				}
			}
		}
	}

	static final class Session implements AutoCloseable {
		private final long queryKey;
		private final long maximumBytes;
		private final Session parent;
		private final Shared shared;
		private final boolean noop;
		private LmdbFactorizedBatch batch;
		private LmdbRuntimeFilterRelocator.Schedule schedule;
		private boolean closed;

		private Session(long queryKey, long maximumBytes, boolean telemetryEnabled, Session parent) {
			this.queryKey = queryKey;
			this.maximumBytes = Math.max(0, maximumBytes);
			this.parent = parent;
			this.noop = false;
			this.shared = parent == null ? new Shared(this.maximumBytes, telemetryEnabled) : parent.shared;
			this.schedule = parent == null ? new LmdbRuntimeFilterRelocator.Schedule(DEFAULT_SLOTS) : parent.schedule;
		}

		private Session() {
			queryKey = 0L;
			maximumBytes = 0L;
			parent = null;
			noop = true;
			shared = Shared.noop();
			schedule = new LmdbRuntimeFilterRelocator.Schedule(1);
		}

		static Session noop() {
			return new Session();
		}

		long queryKey() {
			return queryKey;
		}

		boolean telemetryEnabled() {
			return !noop && shared.telemetryEnabled;
		}

		boolean claim(long bytes) {
			if (noop || bytes <= 0) {
				return true;
			}
			for (;;) {
				long current = shared.usedBytes.get();
				long next = current + bytes;
				if (next < current || next > maximumBytes) {
					shared.memoryRefusals.increment();
					return false;
				}
				if (shared.usedBytes.compareAndSet(current, next)) {
					return true;
				}
			}
		}

		/** Query-ledger bytes not currently claimed; advisory because sibling workers may claim concurrently. */
		long remainingBytes() {
			return noop ? Long.MAX_VALUE : Math.max(0L, maximumBytes - shared.usedBytes.get());
		}

		void release(long bytes) {
			if (!noop && bytes > 0) {
				shared.usedBytes.addAndGet(-bytes);
			}
		}

		void installDomain(int domainId, LmdbNativeSipFilter.Domain domain) {
			installDomain(domainId, "domain[" + domainId + "]", "unspecified", domain, 0L, 0L);
		}

		void installDomain(int domainId, String label, String producer, LmdbNativeSipFilter.Domain domain,
				long buildNanos, long claimedBytes) {
			if (noop || domainId < 0 || domainId >= MAX_DOMAIN_ID) {
				return;
			}
			LmdbNativeSipFilter.Domain installed = domain == null ? LmdbNativeSipFilter.ALL : domain;
			shared.installDomain(domainId, label, producer, installed, buildNanos, claimedBytes);
		}

		void registerDomainConsumer(int domainId, String consumer) {
			if (!noop) {
				DomainStats stats = shared.domain(domainId);
				if (stats != null && consumer != null && !consumer.isBlank()) {
					stats.consumers.add(consumer);
				}
			}
		}

		LmdbNativeSipFilter.Domain domain(int domainId) {
			DomainStats stats = noop ? null : shared.domain(domainId);
			return stats == null ? LmdbNativeSipFilter.ALL : stats.domain;
		}

		boolean mayContain(int domainId, long value) {
			if (noop) {
				return true;
			}
			DomainStats stats = shared.domain(domainId);
			if (stats == null || stats.domain == LmdbNativeSipFilter.ALL) {
				shared.sipBypasses.increment();
				return true;
			}
			shared.sipTests.increment();
			stats.tests.increment();
			boolean accepted = stats.domain.mayContain(value);
			if (!accepted) {
				shared.sipRejects.increment();
				stats.rejects.increment();
			}
			return accepted;
		}

		void recordSipBatch(int domainId, String consumer, long tested, long rejected) {
			if (noop || tested <= 0) {
				return;
			}
			long boundedRejected = Math.max(0L, Math.min(tested, rejected));
			DomainStats stats = shared.domain(domainId);
			if (stats == null || stats.domain == LmdbNativeSipFilter.ALL) {
				shared.sipBypasses.add(tested);
				return;
			}
			if (consumer != null && !consumer.isBlank()) {
				stats.consumers.add(consumer);
			}
			shared.sipTests.add(tested);
			stats.tests.add(tested);
			if (boundedRejected > 0) {
				shared.sipRejects.add(boundedRejected);
				stats.rejects.add(boundedRejected);
			}
		}

		void recordDomainDriven(int domainId, String consumer, long rows) {
			if (noop || rows <= 0) {
				return;
			}
			DomainStats stats = shared.domain(domainId);
			if (stats == null || stats.domain == LmdbNativeSipFilter.ALL) {
				return;
			}
			if (consumer != null && !consumer.isBlank()) {
				stats.consumers.add(consumer);
			}
			stats.drivenRows.add(rows);
			shared.sipDrivenRows.add(rows);
		}

		/**
		 * Publishes SIP work performed by an existing specialized operator. Keeping these descriptors outside the
		 * generated-domain ordinal array avoids sparse/high synthetic ids and lets interpreted, bare-EXISTS and
		 * factorized-tail routes participate in the same query telemetry without allocating a duplicate filter.
		 */
		void recordImplicitSip(String label, String producer, String consumer, String representation, boolean exact,
				long cardinalityUpperBound, long tested, long rejected, long drivenRows) {
			if (noop || (tested <= 0 && drivenRows <= 0)) {
				return;
			}
			long boundedTested = Math.max(0L, tested);
			long boundedRejected = Math.max(0L, Math.min(boundedTested, rejected));
			DomainStats stats = shared.implicitDomain(label, producer, representation, exact,
					cardinalityUpperBound);
			if (consumer != null && !consumer.isBlank()) {
				stats.consumers.add(consumer);
			}
			if (boundedTested > 0) {
				stats.tests.add(boundedTested);
				shared.sipTests.add(boundedTested);
			}
			if (boundedRejected > 0) {
				stats.rejects.add(boundedRejected);
				shared.sipRejects.add(boundedRejected);
			}
			if (drivenRows > 0) {
				stats.drivenRows.add(drivenRows);
				shared.sipDrivenRows.add(drivenRows);
			}
		}

		int compactSorted(int domainId, long[] values, int from, int length) {
			if (noop) {
				return length;
			}
			DomainStats stats = shared.domain(domainId);
			if (stats == null || stats.domain == LmdbNativeSipFilter.ALL) {
				shared.sipBypasses.add(length);
				return length;
			}
			shared.sipTests.add(length);
			stats.tests.add(length);
			int retained = stats.domain.compactSorted(values, from, length);
			long rejected = length - retained;
			if (rejected > 0) {
				shared.sipRejects.add(rejected);
				stats.rejects.add(rejected);
			}
			return retained;
		}

		LmdbFactorizedBatch batch(int rootCapacity, int levels) {
			if (noop) {
				return new LmdbFactorizedBatch(rootCapacity, levels);
			}
			if (batch == null) {
				try {
					batch = new LmdbFactorizedBatch(rootCapacity, levels, this::claim);
					markFactorizationMode("CSR_BATCH");
				} catch (LmdbFactorizedBatch.FactorizationRefused refused) {
					shared.factorizationRefusals.increment();
					return null;
				}
			}
			batch.clear();
			return batch;
		}

		void markFactorizationMode(String mode) {
			if (!noop && mode != null) {
				shared.factorizationModes.add(mode);
			}
		}

		/** Counts one kernel bind under {@code "<identity> route=<route>"} so telemetry names what actually ran. */
		void recordKernelBind(String route, String identity) {
			if (!noop && route != null && identity != null) {
				shared.kernelBinds.computeIfAbsent(identity + " route=" + route, ignored -> new LongAdder())
						.increment();
			}
		}

		void recordFactorizedLevel(long rootRows, long parentRows, long candidates, long storedLanes,
				long rejectedBeforeMaterialization, long optionalNullLanes, long flatEquivalentRows,
				long multiplicityExtra) {
			if (noop) {
				return;
			}
			shared.factorRootRows.add(Math.max(0, rootRows));
			shared.factorParentRows.add(Math.max(0, parentRows));
			shared.factorCandidateRows.add(Math.max(0, candidates));
			shared.factorStoredLanes.add(Math.max(0, storedLanes));
			shared.factorRejected.add(Math.max(0, rejectedBeforeMaterialization));
			shared.factorOptionalNulls.add(Math.max(0, optionalNullLanes));
			shared.factorFlatEquivalent.add(Math.max(0, flatEquivalentRows));
			shared.factorMultiplicityExtra.add(Math.max(0, multiplicityExtra));
		}

		void recordMaterializationBoundary(long rows) {
			if (!noop) {
				shared.materializationBoundaries.increment();
				shared.flattenedValues.add(Math.max(0, rows));
			}
		}

		void addFlattenedValues(long count) {
			if (!noop && count > 0) {
				shared.flattenedValues.add(count);
			}
		}

		void addRelocatedFilterTests(long count) {
			if (!noop && count > 0) {
				shared.relocatedFilterTests.add(count);
			}
		}

		LmdbRuntimeFilterRelocator.Schedule schedule() {
			return schedule;
		}

		void recordFilterPlacement(int filterKey, String label, int originalStage, int selectedStage,
				boolean learned) {
			if (noop) {
				return;
			}
			FilterStats stats = shared.filters.computeIfAbsent(filterKey,
					key -> new FilterStats(filterKey, label == null ? "filter#" + filterKey : label));
			stats.originalStage = originalStage;
			stats.selectedStage = selectedStage;
			stats.learned = learned;
			stats.placements.increment();
			if (selectedStage >= 0 && selectedStage != originalStage) {
				stats.moves.increment();
				shared.filtersRelocated.increment();
			}
		}

		void observeFilter(long filterKey, long tested, long accepted, long elapsedNanos) {
			RELOCATOR.observe(filterKey, tested, accepted, elapsedNanos);
			observeFilter((int) filterKey, "filter#" + filterKey, -1, -1, tested, accepted, elapsedNanos, 0L);
		}

		void observeFilter(int filterKey, String label, int originalStage, int selectedStage, long tested,
				long accepted, long elapsedNanos, long downstreamRowsAvoided) {
			if (noop || tested <= 0 || accepted < 0 || accepted > tested || elapsedNanos < 0) {
				return;
			}
			FilterStats stats = shared.filters.computeIfAbsent(filterKey,
					key -> new FilterStats(filterKey, label == null ? "filter#" + filterKey : label));
			if (originalStage >= 0) {
				stats.originalStage = originalStage;
			}
			if (selectedStage >= 0) {
				stats.selectedStage = selectedStage;
			}
			stats.tested.add(tested);
			stats.accepted.add(accepted);
			stats.elapsedNanos.add(elapsedNanos);
			stats.downstreamRowsAvoided.add(Math.max(0, downstreamRowsAvoided));
			stats.observations.increment();
			shared.relocatedFilterTests.add(tested);
		}

		Telemetry telemetry() {
			if (noop) {
				return Telemetry.EMPTY;
			}
			return shared.snapshot(queryKey);
		}

		@Override
		public void close() {
			if (!closed) {
				closed = true;
				batch = null;
				if (parent == null && !noop) {
					shared.clear();
				}
			}
		}
	}

	private static final class Shared {
		private final boolean telemetryEnabled;
		private final AtomicLong usedBytes = new AtomicLong();
		private final LongAdder memoryRefusals = new LongAdder();
		private volatile DomainStats[] domains = new DomainStats[DEFAULT_SLOTS];
		private final ConcurrentHashMap<String, DomainStats> implicitDomains = new ConcurrentHashMap<>();
		private final LongAdder sipTests = new LongAdder();
		private final LongAdder sipRejects = new LongAdder();
		private final LongAdder sipBypasses = new LongAdder();
		private final LongAdder sipDrivenRows = new LongAdder();
		private final Set<String> factorizationModes = ConcurrentHashMap.newKeySet();
		private final LongAdder factorRootRows = new LongAdder();
		private final LongAdder factorParentRows = new LongAdder();
		private final LongAdder factorCandidateRows = new LongAdder();
		private final LongAdder factorStoredLanes = new LongAdder();
		private final LongAdder factorRejected = new LongAdder();
		private final LongAdder factorOptionalNulls = new LongAdder();
		private final LongAdder factorFlatEquivalent = new LongAdder();
		private final LongAdder factorMultiplicityExtra = new LongAdder();
		private final LongAdder factorizationRefusals = new LongAdder();
		private final LongAdder materializationBoundaries = new LongAdder();
		private final LongAdder flattenedValues = new LongAdder();
		private final ConcurrentHashMap<Integer, FilterStats> filters = new ConcurrentHashMap<>();
		private final LongAdder relocatedFilterTests = new LongAdder();
		private final LongAdder filtersRelocated = new LongAdder();
		/** Bind counts per {@code "<kernel identity> route=<route>"}; workers share the query's map. */
		private final ConcurrentHashMap<String, LongAdder> kernelBinds = new ConcurrentHashMap<>();

		private Shared(long maximumBytes, boolean telemetryEnabled) {
			this.telemetryEnabled = telemetryEnabled;
		}

		private static Shared noop() {
			return new Shared(0L, false);
		}

		private synchronized void installDomain(int domainId, String label, String producer,
				LmdbNativeSipFilter.Domain domain, long buildNanos, long claimedBytes) {
			DomainStats[] current = domains;
			if (domainId < current.length && current[domainId] != null) {
				// Generated kernels and parallel workers may bind the same logical domain more than once. The first
				// installation owns the counters and full-domain metadata; replacing it would erase already observed
				// tests/rejections and a worker's ordinal window could accidentally narrow the no-false-negative
				// filter.
				return;
			}
			int next = current.length;
			while (next <= domainId && next < MAX_DOMAIN_ID) {
				next = Math.min(MAX_DOMAIN_ID, next << 1);
			}
			DomainStats[] updated = Arrays.copyOf(current, next);
			updated[domainId] = new DomainStats(domainId,
					label == null ? "domain[" + domainId + "]" : label,
					producer == null ? "unspecified" : producer, domain, buildNanos, claimedBytes);
			// Copy-on-write publication keeps the membership hot path lock-free while giving worker threads a
			// proper volatile happens-before edge for newly installed immutable domain descriptors.
			domains = updated;
		}

		private DomainStats domain(int domainId) {
			DomainStats[] snapshot = domains;
			return domainId >= 0 && domainId < snapshot.length ? snapshot[domainId] : null;
		}

		private DomainStats implicitDomain(String label, String producer, String representation, boolean exact,
				long cardinalityUpperBound) {
			String resolvedLabel = label == null || label.isBlank() ? "implicit-sip" : label;
			String resolvedProducer = producer == null || producer.isBlank() ? "specialized-operator" : producer;
			String resolvedRepresentation = representation == null || representation.isBlank()
					? "SPECIALIZED"
					: representation;
			String key = resolvedLabel + '\u0000' + resolvedProducer + '\u0000' + resolvedRepresentation;
			return implicitDomains.computeIfAbsent(key, ignored -> new DomainStats(
					-1 - implicitDomains.size(), resolvedLabel, resolvedProducer,
					LmdbNativeSipFilter.descriptor(resolvedRepresentation, exact, cardinalityUpperBound), 0L, 0L));
		}

		private Telemetry snapshot(long queryKey) {
			ArrayList<DomainTelemetry> domainTelemetry = new ArrayList<>();
			for (DomainStats stats : domains) {
				if (stats != null) {
					domainTelemetry.add(stats.snapshot());
				}
			}
			for (DomainStats stats : implicitDomains.values()) {
				domainTelemetry.add(stats.snapshot());
			}
			domainTelemetry.sort(Comparator.comparingInt(DomainTelemetry::id));
			ArrayList<FilterTelemetry> filterTelemetry = new ArrayList<>();
			for (FilterStats stats : filters.values()) {
				filterTelemetry.add(stats.snapshot());
			}
			filterTelemetry.sort(Comparator.comparingInt(FilterTelemetry::id));
			List<String> modes = factorizationModes.stream().sorted().toList();
			FactorizationTelemetry factor = new FactorizationTelemetry(modes, factorRootRows.sum(),
					factorParentRows.sum(), factorCandidateRows.sum(), factorStoredLanes.sum(), factorRejected.sum(),
					factorOptionalNulls.sum(), factorFlatEquivalent.sum(), factorMultiplicityExtra.sum(),
					materializationBoundaries.sum(), flattenedValues.sum(), factorizationRefusals.sum());
			java.util.TreeMap<String, Long> binds = new java.util.TreeMap<>();
			kernelBinds.forEach((key, count) -> binds.put(key, count.sum()));
			return new Telemetry(queryKey, sipTests.sum(), sipRejects.sum(), sipBypasses.sum(),
					sipDrivenRows.sum(), usedBytes.get(), memoryRefusals.sum(), List.copyOf(domainTelemetry), factor,
					List.copyOf(filterTelemetry), relocatedFilterTests.sum(), filtersRelocated.sum(),
					java.util.Collections.unmodifiableMap(binds));
		}

		private synchronized void clear() {
			domains = new DomainStats[DEFAULT_SLOTS];
			implicitDomains.clear();
			usedBytes.set(0L);
			filters.clear();
			factorizationModes.clear();
			kernelBinds.clear();
		}
	}

	private static final class DomainStats {
		private final int id;
		private final String label;
		private final String producer;
		private final LmdbNativeSipFilter.Domain domain;
		private final long buildNanos;
		private final long claimedBytes;
		private final Set<String> consumers = ConcurrentHashMap.newKeySet();
		private final LongAdder tests = new LongAdder();
		private final LongAdder rejects = new LongAdder();
		private final LongAdder drivenRows = new LongAdder();

		private DomainStats(int id, String label, String producer, LmdbNativeSipFilter.Domain domain,
				long buildNanos, long claimedBytes) {
			this.id = id;
			this.label = label;
			this.producer = producer;
			this.domain = domain;
			this.buildNanos = Math.max(0, buildNanos);
			this.claimedBytes = Math.max(0, claimedBytes);
		}

		private DomainTelemetry snapshot() {
			return new DomainTelemetry(id, label, producer, domain.representation(), domain.exact(),
					domain.cardinalityUpperBound(), claimedBytes, domain.estimatedBytes(), buildNanos,
					consumers.stream().sorted().toList(), tests.sum(), rejects.sum(), drivenRows.sum());
		}
	}

	private static final class FilterStats {
		private final int id;
		private final String label;
		private volatile int originalStage = -1;
		private volatile int selectedStage = -1;
		private volatile boolean learned;
		private final LongAdder placements = new LongAdder();
		private final LongAdder moves = new LongAdder();
		private final LongAdder observations = new LongAdder();
		private final LongAdder tested = new LongAdder();
		private final LongAdder accepted = new LongAdder();
		private final LongAdder elapsedNanos = new LongAdder();
		private final LongAdder downstreamRowsAvoided = new LongAdder();

		private FilterStats(int id, String label) {
			this.id = id;
			this.label = label;
		}

		private FilterTelemetry snapshot() {
			return new FilterTelemetry(id, label, originalStage, selectedStage, learned, placements.sum(), moves.sum(),
					observations.sum(), tested.sum(), accepted.sum(), elapsedNanos.sum(), downstreamRowsAvoided.sum());
		}
	}

	record DomainTelemetry(int id, String label, String producer, String representation, boolean exact,
			long cardinalityUpperBound, long claimedBytes, long estimatedBytes, long buildNanos,
			List<String> consumers, long tests, long rejects, long drivenRows) {
		boolean active() {
			return !"ALL".equals(representation);
		}
	}

	record FactorizationTelemetry(List<String> modes, long rootRows, long parentRows, long candidateRows,
			long storedLanes, long rejectedBeforeMaterialization, long optionalNullLanes, long flatEquivalentRows,
			long multiplicityExtra, long materializationBoundaries, long flattenedRows, long refusals) {
		boolean used() {
			return !modes.isEmpty() || storedLanes > 0 || rejectedBeforeMaterialization > 0;
		}

		double compressionRatio() {
			return storedLanes == 0 ? 1.0 : (double) Math.max(storedLanes, flatEquivalentRows) / storedLanes;
		}
	}

	record FilterTelemetry(int id, String label, int originalStage, int selectedStage, boolean learned,
			long placements, long moves, long observations, long tested, long accepted, long elapsedNanos,
			long downstreamRowsAvoided) {
		boolean used() {
			return placements > 0 || tested > 0;
		}
	}

	record Telemetry(long queryKey, long sipTests, long sipRejects, long sipBypasses, long sipDrivenRows,
			long retainedBytes, long memoryRefusals, List<DomainTelemetry> domains,
			FactorizationTelemetry factorization, List<FilterTelemetry> filters, long relocatedFilterTests,
			long filtersRelocated, java.util.Map<String, Long> kernelBinds) {

		static final Telemetry EMPTY = new Telemetry(0L, 0L, 0L, 0L, 0L, 0L, 0L, List.of(),
				new FactorizationTelemetry(List.of(), 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L),
				List.of(), 0L, 0L, java.util.Map.of());

		boolean runtimeSipUsed() {
			return (sipTests > 0 || sipDrivenRows > 0)
					&& domains.stream().anyMatch(DomainTelemetry::active);
		}

		long activeDomainCount() {
			return domains.stream().filter(DomainTelemetry::active).count();
		}

		double sipRejectionRatio() {
			return sipTests == 0 ? 0 : (double) sipRejects / sipTests;
		}
	}
}
