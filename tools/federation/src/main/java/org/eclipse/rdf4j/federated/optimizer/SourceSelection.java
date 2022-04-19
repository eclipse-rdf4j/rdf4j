/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.optimizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.algebra.EmptyStatementPattern;
import org.eclipse.rdf4j.federated.algebra.ExclusiveStatement;
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
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Perform source selection during optimization
 *
 * @author Andreas Schwarte
 *
 */
public class SourceSelection {

	private static final Logger log = LoggerFactory.getLogger(SourceSelection.class);

	protected final List<Endpoint> endpoints;
	protected final SourceSelectionCache cache;
	protected final QueryInfo queryInfo;

	public SourceSelection(List<Endpoint> endpoints, SourceSelectionCache cache, QueryInfo queryInfo) {
		this.endpoints = endpoints;
		this.cache = cache;
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

		List<CheckTaskPair> remoteCheckTasks = new ArrayList<>();

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
					remoteCheckTasks.add(new CheckTaskPair(e, stmt, queryInfo));
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
		if (remoteCheckTasks.size() > 0) {
			SourceSelectionExecutorWithLatch.run(this, remoteCheckTasks, cache);
		}

		// iterate over input statements, BGP might be uses twice
		// resulting in the same entry in stmtToSources
		for (StatementPattern stmt : stmts) {

			List<StatementSource> sources = stmtToSources.get(stmt);

			// if more than one source -> StatementSourcePattern
			// exactly one source -> OwnedStatementSourcePattern
			// otherwise: No resource seems to provide results

			if (sources.size() > 1) {
				StatementSourcePattern stmtNode = new StatementSourcePattern(stmt, queryInfo);
				for (StatementSource s : sources) {
					stmtNode.addStatementSource(s);
				}
				stmt.replaceWith(stmtNode);
			} else if (sources.size() == 1) {
				stmt.replaceWith(new ExclusiveStatement(stmt, sources.get(0), queryInfo));
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
		public static void run(SourceSelection sourceSelection, List<CheckTaskPair> tasks, SourceSelectionCache cache) {
			new SourceSelectionExecutorWithLatch(sourceSelection).executeRemoteSourceSelection(tasks, cache);
		}

		private final SourceSelection sourceSelection;
		private final ControlledWorkerScheduler<BindingSet> scheduler;
		private CountDownLatch latch;
		private boolean finished = false;
		protected List<Exception> errors = new CopyOnWriteArrayList<>();

		private SourceSelectionExecutorWithLatch(SourceSelection sourceSelection) {
			this.sourceSelection = sourceSelection;
			// TODO simpler access pattern
			this.scheduler = sourceSelection.queryInfo.getFederationContext().getManager().getJoinScheduler();
		}

		/**
		 * Execute the given list of tasks in parallel, and block the thread until all tasks are completed.
		 * Synchronization is achieved by means of a latch
		 *
		 * @param tasks
		 */
		private void executeRemoteSourceSelection(List<CheckTaskPair> tasks, SourceSelectionCache cache) {
			if (tasks.isEmpty()) {
				return;
			}

			latch = new CountDownLatch(tasks.size());
			for (CheckTaskPair task : tasks) {
				scheduler.schedule(new ParallelCheckTask(task.e, task.t, task.queryInfo, this));
			}

			try {
				boolean completed = latch.await(getQueryInfo().getMaxRemainingTimeMS(), TimeUnit.MILLISECONDS);
				if (!completed) {
					throw new OptimizationException("Source selection has run into a timeout");
				}
			} catch (InterruptedException e) {
				log.debug("Error during source selection. Thread got interrupted.");
				errors.add(e);
			}

			finished = true;

			// check for errors:
			if (errors.size() > 0) {
				StringBuilder sb = new StringBuilder();
				sb.append(
						errors.size() + " errors were reported while optimizing query " + getQueryInfo().getQueryID());

				for (Exception e : errors) {
					sb.append("\n" + ExceptionUtil.getExceptionString("Error occured", e));
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
		public void addResult(CloseableIteration<BindingSet, QueryEvaluationException> res) {
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

	/**
	 * Task for sending an ASK request to the endpoints (for source selection)
	 *
	 * @author Andreas Schwarte
	 */
	protected static class ParallelCheckTask extends ParallelTaskBase<BindingSet> {

		protected final Endpoint endpoint;
		protected final StatementPattern stmt;
		protected final SourceSelectionExecutorWithLatch control;
		protected final QueryInfo queryInfo;

		public ParallelCheckTask(Endpoint endpoint, StatementPattern stmt, QueryInfo queryInfo,
				SourceSelectionExecutorWithLatch control) {
			this.endpoint = endpoint;
			this.stmt = stmt;
			this.queryInfo = queryInfo;
			this.control = control;
		}

		@Override
		protected CloseableIteration<BindingSet, QueryEvaluationException> performTaskInternal() throws Exception {
			try {
				TripleSource t = endpoint.getTripleSource();
				boolean hasResults;
				hasResults = t.hasStatements(stmt, EmptyBindingSet.getInstance(), queryInfo, queryInfo.getDataset());

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

}
