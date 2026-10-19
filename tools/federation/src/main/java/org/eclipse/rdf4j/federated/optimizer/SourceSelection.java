/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.optimizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.algebra.EmptyStatementPattern;
import org.eclipse.rdf4j.federated.algebra.ExclusiveStatement;
import org.eclipse.rdf4j.federated.algebra.FedXStatementPattern;
import org.eclipse.rdf4j.federated.algebra.FilterValueExpr;
import org.eclipse.rdf4j.federated.algebra.StatementSource;
import org.eclipse.rdf4j.federated.algebra.StatementSource.StatementSourceType;
import org.eclipse.rdf4j.federated.algebra.StatementSourcePattern;
import org.eclipse.rdf4j.federated.cache.SourceSelectionCache;
import org.eclipse.rdf4j.federated.cache.SourceSelectionCache.StatementSourceAssurance;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.TripleSource;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ControlledWorkerScheduler;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelExecutor;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelTaskBase;
import org.eclipse.rdf4j.federated.exception.ExceptionUtil;
import org.eclipse.rdf4j.federated.exception.OptimizationException;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.structures.SubQuery;
import org.eclipse.rdf4j.federated.util.QueryStringUtil;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs source selection during query optimization by determining, for each statement pattern in a BGP, which
 * federation members (endpoints) can contribute results.
 *
 * <h2>Algorithm</h2>
 * <p>
 * For each statement pattern and each endpoint the {@link SourceSelectionCache} is consulted first. Patterns whose
 * source membership is already known from a previous query are resolved without any remote communication. Only patterns
 * with a {@link org.eclipse.rdf4j.federated.cache.SourceSelectionCache.StatementSourceAssurance#POSSIBLY_HAS_STATEMENTS
 * POSSIBLY_HAS_STATEMENTS} assurance require a remote check. All remote checks are executed in parallel using the FedX
 * worker-thread infrastructure ({@link SourceSelectionExecutorWithLatch}), and the calling thread blocks until every
 * check has completed or the query timeout is reached.
 * </p>
 *
 * <h2>Remote check strategies</h2>
 * <p>
 * Two strategies are supported for performing the remote checks, selected per endpoint based on the configuration:
 * </p>
 * <dl>
 * <dt>Grouped source selection (default, {@link org.eclipse.rdf4j.federated.FedXConfig#isEnableGroupedSourceSelection()
 * enableGroupedSourceSelection=true})</dt>
 * <dd>All statement patterns that require a remote check for a given endpoint are batched into a single SPARQL SELECT
 * query of the form:
 *
 * <pre>
 * SELECT * WHERE {
 *   BIND(EXISTS { &lt;pattern_0&gt; } AS ?stmt_0)
 *   BIND(EXISTS { &lt;pattern_1&gt; } AS ?stmt_1)
 *   ...
 * }
 * </pre>
 *
 * This reduces the number of remote requests from <em>O(S &times; M)</em> to <em>O(M)</em>, where <em>S</em> is the
 * number of statement patterns and <em>M</em> the number of federation members. It is particularly effective in
 * high-latency settings. Grouped checks are only used when more than two patterns require a check for the same
 * endpoint; otherwise, individual ASK queries are sent. The grouped check is implemented in
 * {@link ParallelGroupedCheckTask}.</dd>
 * <dt>Individual ASK queries (classic FedX,
 * {@link org.eclipse.rdf4j.federated.FedXConfig#isEnableGroupedSourceSelection()
 * enableGroupedSourceSelection=false})</dt>
 * <dd>One ASK query is sent per statement pattern and endpoint, yielding up to <em>S &times; M</em> remote requests.
 * Implemented in {@link ParallelCheckTask}.</dd>
 * </dl>
 *
 * <h2>Configuration</h2>
 * <p>
 * The source selection strategy is controlled via {@link org.eclipse.rdf4j.federated.FedXConfig}:
 * </p>
 * <ul>
 * <li>{@link org.eclipse.rdf4j.federated.FedXConfig#withEnableGroupedSourceSelection(boolean)
 * withEnableGroupedSourceSelection(boolean)} — enables or disables grouped source selection (default:
 * {@code true})</li>
 * <li>{@link org.eclipse.rdf4j.federated.FedXConfig#withSourceSelectionCacheSpec(String)
 * withSourceSelectionCacheSpec(String)} — configures the Guava {@code CacheBuilderSpec} for the source selection
 * cache</li>
 * <li>{@link org.eclipse.rdf4j.federated.FedXConfig#withSourceSelectionCacheFactory(org.eclipse.rdf4j.federated.cache.SourceSelectionCacheFactory)
 * withSourceSelectionCacheFactory(...)} — supplies a custom
 * {@link org.eclipse.rdf4j.federated.cache.SourceSelectionCacheFactory}</li>
 * </ul>
 *
 * @author Andreas Schwarte
 */
public class SourceSelection {

	private static final Logger log = LoggerFactory.getLogger(SourceSelection.class);

	protected final List<Endpoint> endpoints;
	protected final SourceSelectionCache cache;
	protected final FederationContext federationContext;
	protected final QueryInfo queryInfo;

	public SourceSelection(List<Endpoint> endpoints, SourceSelectionCache cache, FederationContext federationContext,
			QueryInfo queryInfo) {
		this.endpoints = endpoints;
		this.cache = cache;
		this.federationContext = federationContext;
		this.queryInfo = queryInfo;
	}

	/**
	 * Map statements to their sources. Use synchronized access!
	 */
	protected Map<StatementPattern, List<StatementSource>> stmtToSources = new ConcurrentHashMap<>();

	/**
	 * Perform source selection for the provided statements using cache or remote ASK queries.
	 *
	 * Remote ASK queries are evaluated in parallel using the concurrency infrastructure of FedX. Note, that this method
	 * is blocking until every source is resolved.
	 *
	 * The statement patterns are replaced by appropriate annotations in this optimization.
	 *
	 * @param stmts
	 */
	public void doSourceSelection(List<StatementPattern> stmts) {

		// map remote check tasks per endpoint
		Map<Endpoint, List<CheckTaskPair>> endpointToRemoteCheckTasks = new HashMap<>();

		// for each statement determine the relevant sources
		for (StatementPattern stmt : stmts) {

			// jump over the statement (e.g. if the same pattern is used in two union branches)
			if (stmtToSources.containsKey(stmt)) {
				continue;
			}

			stmtToSources.put(stmt, new ArrayList<>());

			SubQuery q = new SubQuery(stmt, queryInfo.getDataset());

			// check for each current federation member (cache or remote ASK)
			for (Endpoint e : endpoints) {
				StatementSourceAssurance a = cache.getAssurance(q, e);
				if (a == StatementSourceAssurance.HAS_REMOTE_STATEMENTS) {
					addSource(stmt, new StatementSource(e.getId(), StatementSourceType.REMOTE));
				} else if (a == StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS) {
					endpointToRemoteCheckTasks.computeIfAbsent(e, (_) -> new ArrayList<>())
							.add(new CheckTaskPair(e, stmt, queryInfo));
				} else if (a == StatementSourceAssurance.NONE) {
					// cannot provide any statements
					continue;
				} else {
					throw new IllegalStateException("Unexpected statement source assurance: " + a);
				}
			}
		}

		// if remote checks are necessary, execute them using the concurrency
		// infrastructure and block until everything is resolved
		if (!endpointToRemoteCheckTasks.isEmpty()) {
			SourceSelectionExecutorWithLatch.run(this, endpointToRemoteCheckTasks, cache);
		}

		// iterate over input statements, BGP might be uses twice
		// resulting in the same entry in stmtToSources
		for (StatementPattern stmt : stmts) {

			List<StatementSource> sources = stmtToSources.get(stmt);

			// if more than one source -> StatementSourcePattern
			// exactly one source -> OwnedStatementSourcePattern
			// otherwise: No resource seems to provide results

			if (sources.size() > 1) {
				var stmtNode = stmt instanceof FedXStatementPattern ? (FedXStatementPattern) stmt
						: new StatementSourcePattern(stmt, queryInfo);
				for (StatementSource s : sources) {
					stmtNode.addStatementSource(s);
				}
				stmt.replaceWith(stmtNode);
			} else if (sources.size() == 1) {
				if (stmt instanceof FedXStatementPattern fstmt) {
					fstmt.addStatementSource(sources.get(0));
				} else {
					stmt.replaceWith(new ExclusiveStatement(stmt, sources.get(0), queryInfo));
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Statement " + QueryStringUtil.toString(stmt)
							+ " does not produce any results at the provided sources, replacing node with EmptyStatementPattern.");
				}
				stmt.replaceWith(new EmptyStatementPattern(stmt));
			}
		}
	}

	/**
	 * Retrieve a set of relevant sources for this query.
	 *
	 * @return the relevant sources
	 */
	public Set<Endpoint> getRelevantSources() {
		Set<Endpoint> endpoints = new HashSet<>();
		for (List<StatementSource> sourceList : stmtToSources.values()) {
			for (StatementSource source : sourceList) {
				endpoints
						.add(queryInfo.getFederationContext().getEndpointManager().getEndpoint(source.getEndpointID()));
			}
		}
		return endpoints;
	}

	/**
	 * Add a source to the given statement in the map (synchronized through map)
	 *
	 * @param stmt
	 * @param source
	 */
	protected void addSource(StatementPattern stmt, StatementSource source) {
		// The list for the stmt mapping is already initialized
		List<StatementSource> sources = stmtToSources.get(stmt);
		synchronized (sources) {
			sources.add(source);
		}
	}

	protected static class SourceSelectionExecutorWithLatch implements ParallelExecutor<BindingSet> {

		/**
		 * Execute the given list of tasks in parallel, and block the thread until all tasks are completed.
		 * Synchronization is achieved by means of a latch. Results are added to the map of the source selection
		 * instance. Errors are reported as {@link OptimizationException} instances.
		 *
		 * @param tasks
		 */
		public static void run(SourceSelection sourceSelection,
				Map<Endpoint, List<CheckTaskPair>> endpointToRemoteTasks, SourceSelectionCache cache) {
			var sourceSelectionWithLatch = new SourceSelectionExecutorWithLatch(sourceSelection);

			boolean groupedSourceSelectionEnabled = sourceSelection.federationContext.getConfig()
					.isEnableGroupedSourceSelection();

			List<ParallelSourceSelectionTask> tasks = new ArrayList<>();
			for (var endpointEntry : endpointToRemoteTasks.entrySet()) {

				var remoteCheckTasks = endpointEntry.getValue();

				if (groupedSourceSelectionEnabled && remoteCheckTasks.size() > 2) {
					// variant 1: grouped check per endpoint (single SELECT with BIND(EXISTS{}) per pattern)
					List<StatementPattern> stmts = remoteCheckTasks.stream().map(c -> c.t).toList();
					tasks.add(new ParallelGroupedCheckTask(endpointEntry.getKey(), stmts, sourceSelection.queryInfo,
							sourceSelectionWithLatch));
				} else {
					// variant 2: individual ASK queries per statement pattern (classic behavior)
					for (CheckTaskPair taskPair : remoteCheckTasks) {
						tasks.add(new ParallelCheckTask(taskPair.e, taskPair.t, taskPair.queryInfo,
								sourceSelectionWithLatch));
					}
				}
			}

			sourceSelectionWithLatch.executeRemoteSourceSelection(tasks, cache);
		}

		private final SourceSelection sourceSelection;
		private final ControlledWorkerScheduler<BindingSet> scheduler;
		private CountDownLatch latch;
		private boolean finished = false;
		protected List<Exception> errors = new CopyOnWriteArrayList<>();

		private SourceSelectionExecutorWithLatch(SourceSelection sourceSelection) {
			this.sourceSelection = sourceSelection;
			this.scheduler = sourceSelection.federationContext.getManager().getJoinScheduler();
		}

		/**
		 * Execute the given list of tasks in parallel, and block the thread until all tasks are completed.
		 * Synchronization is achieved by means of a latch
		 *
		 * @param tasks
		 */
		private void executeRemoteSourceSelection(List<ParallelSourceSelectionTask> tasks, SourceSelectionCache cache) {
			if (tasks.isEmpty()) {
				return;
			}

			latch = new CountDownLatch(tasks.size());
			for (ParallelSourceSelectionTask task : tasks) {
				scheduler.schedule(task);
			}

			try {
				boolean completed = latch.await(getQueryInfo().getMaxRemainingTimeMS(), TimeUnit.MILLISECONDS);
				if (!completed) {
					throw new OptimizationException("Source selection has run into a timeout");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.debug("Error during source selection. Thread got interrupted.");
				errors.add(e);
			}

			finished = true;

			// check for errors:
			if (!errors.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				sb.append(errors.size())
						.append(" errors were reported while optimizing query ")
						.append(getQueryInfo().getQueryID());

				for (Exception e : errors) {
					sb.append("\n").append(ExceptionUtil.getExceptionString("Error occured", e));
				}

				log.debug(sb.toString());

				Exception ex = errors.get(0);
				errors.clear();
				if (ex instanceof OptimizationException) {
					throw (OptimizationException) ex;
				}

				throw new OptimizationException(ex.getMessage(), ex);
			}
		}

		@Override
		public void run() {
			/* not needed */
		}

		@Override
		public void addResult(CloseableIteration<BindingSet> res) {
			latch.countDown();
		}

		@Override
		public void toss(Exception e) {
			latch.countDown();
			errors.add(e);
			getQueryInfo().abort();
		}

		@Override
		public void done() {
			/* not needed */
		}

		@Override
		public boolean isFinished() {
			return finished;
		}

		@Override
		public QueryInfo getQueryInfo() {
			return sourceSelection.queryInfo;
		}
	}

	protected class CheckTaskPair {
		public final Endpoint e;
		public final StatementPattern t;
		public final QueryInfo queryInfo;

		public CheckTaskPair(Endpoint e, StatementPattern t, QueryInfo queryInfo) {
			this.e = e;
			this.t = t;
			this.queryInfo = queryInfo;
		}
	}

	protected static abstract class ParallelSourceSelectionTask extends ParallelTaskBase<BindingSet> {

		protected final Endpoint endpoint;
		protected final SourceSelectionExecutorWithLatch control;
		protected final QueryInfo queryInfo;

		public ParallelSourceSelectionTask(Endpoint endpoint,
				QueryInfo queryInfo, SourceSelectionExecutorWithLatch control) {
			super();
			this.endpoint = endpoint;
			this.control = control;
			this.queryInfo = queryInfo;
		}

		@Override
		public ParallelExecutor<BindingSet> getControl() {
			return control;
		}

		@Override
		public void cancel() {
			control.latch.countDown();
			super.cancel();
		}
	}

	/**
	 * Task for sending an ASK request to the endpoints (for source selection)
	 *
	 * @author Andreas Schwarte
	 */
	protected static class ParallelCheckTask extends ParallelSourceSelectionTask {

		protected final StatementPattern stmt;

		public ParallelCheckTask(Endpoint endpoint, StatementPattern stmt, QueryInfo queryInfo,
				SourceSelectionExecutorWithLatch control) {
			super(endpoint, queryInfo, control);
			this.stmt = stmt;
		}

		@Override
		protected CloseableIteration<BindingSet> performTaskInternal() throws Exception {
			try {
				TripleSource t = endpoint.getTripleSource();
				boolean hasResults = t.hasStatements(stmt, EmptyBindingSet.getInstance(), queryInfo,
						queryInfo.getDataset());

				SourceSelection sourceSelection = control.sourceSelection;
				sourceSelection.cache.updateInformation(new SubQuery(stmt, queryInfo.getDataset()), endpoint,
						hasResults);

				if (hasResults) {
					sourceSelection.addSource(stmt, new StatementSource(endpoint.getId(), StatementSourceType.REMOTE));
				}

				return null;
			} catch (Exception e) {
				throw new OptimizationException(
						"Error checking results for endpoint " + endpoint.getId() + ": " + e.getMessage(), e);
			}
		}

	}

	protected static class ParallelGroupedCheckTask extends ParallelSourceSelectionTask {
		protected final List<StatementPattern> stmts;

		public ParallelGroupedCheckTask(Endpoint endpoint, List<StatementPattern> stmts, QueryInfo queryInfo,
				SourceSelectionExecutorWithLatch control) {
			super(endpoint, queryInfo, control);
			this.stmts = stmts;
		}

		@Override
		protected CloseableIteration<BindingSet> performTaskInternal() throws Exception {

			CloseableIteration<BindingSet> innerResult = null;
			try {
				TripleSource t = endpoint.getTripleSource();
				SourceSelection sourceSelection = control.sourceSelection;

				String preparedGroupedCheck = QueryStringUtilExt.selectQueryStringGroupedSourceSelection(stmts,
						EmptyBindingSet.getInstance());

				innerResult = t.getStatements(preparedGroupedCheck, EmptyBindingSet.getInstance(),
						(FilterValueExpr) null, queryInfo);
				if (!innerResult.hasNext()) {
					throw new IllegalStateException("Inner result for grouped source selection is empty.");
				}
				var sourceSelectionBs = innerResult.next();
				int stmtId = 0;
				for (var stmt : stmts) {

					boolean hasResults = ((Literal) sourceSelectionBs.getValue("stmt_" + stmtId++)).booleanValue();
					sourceSelection.cache.updateInformation(new SubQuery(stmt, queryInfo.getDataset()), endpoint,
							hasResults);

					if (hasResults) {
						sourceSelection.addSource(stmt,
								new StatementSource(endpoint.getId(), StatementSourceType.REMOTE));
					}
				}

				return null;
			} catch (Exception e) {
				throw new OptimizationException(
						"Error checking results for endpoint " + endpoint.getId() + ": " + e.getMessage(), e);
			} finally {
				if (innerResult != null) {
					innerResult.close();
				}
			}

		}

	}

	static class QueryStringUtilExt extends QueryStringUtil {

		public static String selectQueryStringGroupedSourceSelection(List<StatementPattern> stmts,
				BindingSet bindings) {

			StringBuilder sb = new StringBuilder();
			sb.append("SELECT * WHERE { ");

			Set<String> varNames = new HashSet<>();
			int stmtId = 0;
			for (var stmt : stmts) {
				sb.append("BIND(EXISTS { ")
						.append(constructStatement(stmt, varNames, bindings))
						.append(" } AS ?stmt_")
						.append(stmtId++)
						.append(") ");
			}
			sb.append("}");
			return sb.toString();
		}
	}
}
