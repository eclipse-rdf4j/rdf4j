/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql.federation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.helpers.TupleExprs;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.query.InsertBindingSetCursor;
import org.eclipse.rdf4j.repository.sparql.query.QueryStringUtil;
import org.eclipse.rdf4j.repository.sparql.query.SPARQLQueryBindingSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Federated Service wrapping the {@link Repository} to communicate with a SPARQL endpoint.
 *
 * @author Andreas Schwarte
 */
public class RepositoryFederatedService implements FederatedService {

	private static final String ROW_IDX_VAR = "__rowIdx";

	final static Logger logger = LoggerFactory.getLogger(RepositoryFederatedService.class);

	/**
	 * A convenience iteration for SERVICE expression which evaluates intermediate results in batches and manages all
	 * results. Uses {@link JoinExecutorBase} facilities to guarantee correct access to the final results
	 *
	 * @author as
	 */
	private class BatchingServiceIteration extends JoinExecutorBase<BindingSet> {

		private final int blockSize;

		private final Service service;

		private final ExecutorService threadExecutor = Executors.newSingleThreadExecutor();

		private final Future<?> querySubmissionTask;

		/**
		 * @param inputBindings
		 * @throws QueryEvaluationException
		 */
		public BatchingServiceIteration(CloseableIteration<BindingSet> inputBindings,
				int blockSize, Service service) throws QueryEvaluationException {
			super(inputBindings, null, EmptyBindingSet.getInstance());
			this.blockSize = blockSize;
			this.service = service;

			// Set up a consumer task to send HTTP requests in parallel. This must be done in a
			// separate thread, because submitting HTTP requests may block if the HTTP pool is full.
			// In that case, we would enter a deadlock, with the main thread waiting for both the
			// pool to yield, and the consumer of the bindings to read from the queue.
			// See: https://github.com/eclipse-rdf4j/rdf4j/discussions/5120
			// Test case: https://github.com/tkuhn/rdf4j-timeout-test
			try {
				querySubmissionTask = threadExecutor.submit(wrap(this::run));
			} catch (Exception e) {
				throw new QueryEvaluationException("Failed to start a thread for batched federated query submission",
						e);
			}
		}

		@Override
		protected void handleBindings() throws Exception {
			// Note: any exceptions here will be intercepted by the caller and tossed asynchronously
			// via the rightQueue.
			while (!isClosed() && leftIter.hasNext()) {
				ArrayList<BindingSet> blockBindings = new ArrayList<>(blockSize);
				for (int i = 0; i < blockSize; i++) {
					if (!leftIter.hasNext()) {
						break;
					}
					blockBindings.add(leftIter.next());
				}
				CloseableIteration<BindingSet> materializedIter = new CollectionIteration<>(
						blockBindings);
				// evaluateInternal is BLOCKING if the HTTP pool is exhausted
				addResult(evaluateInternal(service, materializedIter, service.getBaseURI()));
			}
		}

		@Override
		public void handleClose() throws QueryEvaluationException {
			super.handleClose();
			if (querySubmissionTask != null) {
				querySubmissionTask.cancel(true);
			}
			threadExecutor.shutdownNow();
		}
	}

	/**
	 * Wrapper iteration which closes a {@link RepositoryConnection} upon {@link #close()}
	 *
	 * @author Andreas Schwarte
	 */
	private static class CloseConnectionIteration implements CloseableIteration<BindingSet> {

		private final CloseableIteration<BindingSet> delegate;
		private final RepositoryConnection connection;

		private CloseConnectionIteration(CloseableIteration<BindingSet> delegate,
				RepositoryConnection connection) {
			super();
			this.delegate = delegate;
			this.connection = connection;
		}

		@Override
		public boolean hasNext() throws QueryEvaluationException {
			return delegate.hasNext();
		}

		@Override
		public BindingSet next() throws QueryEvaluationException {
			return delegate.next();
		}

		@Override
		public void remove() throws QueryEvaluationException {
			delegate.remove();

		}

		@Override
		public void close() throws QueryEvaluationException {
			try {
				delegate.close();
			} finally {
				closeQuietly(connection);
			}
		}
	}

	private final Repository rep;

	/**
	 * Whether the wrapped endpoint has been declared tolerant to partitioning one logical SERVICE Invocation into
	 * several remote requests. See {@link #setPartitionToleranceDeclared(boolean)}.
	 */
	private boolean partitionToleranceDeclared = false;

	/**
	 * The number of bindings sent in a single subquery in {@link #evaluate(Service, CloseableIteration, String)} If
	 * blockSize is set to 0, the entire input stream is used as block input the block size effectively determines the
	 * number of remote requests
	 */
	protected int boundJoinBlockSize = 15;

	/**
	 * Whether to use a fresh repository connection for individual queries
	 */
	private boolean useFreshConnection = true;

	// flag indicating whether the repository shall be closed in #shutdown()
	protected boolean shutDown;

	private RepositoryConnection managedConn = null;

	/**
	 * @param repo the repository to be used
	 */
	public RepositoryFederatedService(Repository repo) {
		this(repo, true);
	}

	/**
	 * @param repo     the repository to be used
	 * @param shutDown a flag indicating whether the repository shall be closed in {@link #shutdown()}
	 */
	public RepositoryFederatedService(Repository repo, boolean shutDown) {
		super();
		this.rep = repo;
		this.shutDown = shutDown;
	}

	/**
	 * Evaluate the provided sparqlQueryString at the initialized {@link Repository} of this {@link FederatedService}.
	 * Insert bindings into SELECT query and evaluate
	 */
	@Override
	public CloseableIteration<BindingSet> select(Service service, Set<String> projectionVars,
			BindingSet bindings, String baseUri) throws QueryEvaluationException {

		RepositoryConnection conn = null;
		try {
			String sparqlQueryString = service.getSelectQueryString(projectionVars);

			conn = useFreshConnection ? freshConnection() : getConnection();
			TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, sparqlQueryString, baseUri);

			Iterator<Binding> bIter = bindings.iterator();
			while (bIter.hasNext()) {
				Binding b = bIter.next();
				if (service.getServiceVars().contains(b.getName())) {
					query.setBinding(b.getName(), b.getValue());
				}
			}

			TupleQueryResult res = query.evaluate();

			// insert original bindings again
			CloseableIteration<BindingSet> result = new InsertBindingSetCursor(res, bindings);

			if (useFreshConnection) {
				result = new CloseConnectionIteration(result, conn);
			}

			if (service.isSilent()) {
				// buffer the complete result before exposure: a failed SILENT invocation is the singleton
				// empty mapping, so a mid-stream failure must discard all partial rows and pass the input
				// binding through unchanged
				return bufferSilently(result, List.of(bindings));
			} else {
				return result;
			}
		} catch (MalformedQueryException e) {
			if (useFreshConnection) {
				closeQuietly(conn);
			}
			throw new QueryEvaluationException(e);
		} catch (RepositoryException e) {
			if (useFreshConnection) {
				closeQuietly(conn);
			}
			throw new QueryEvaluationException(
					"Repository for endpoint " + rep.toString() + " could not be initialized.", e);
		} catch (RuntimeException e) {
			if (useFreshConnection) {
				closeQuietly(conn);
			}
			throw e;
		}
	}

	/**
	 * Evaluate the provided sparqlQueryString at the initialized {@link Repository} of this {@link FederatedService}.
	 * Insert bindings, send ask query and return final result
	 */
	@Override
	public boolean ask(Service service, BindingSet bindings, String baseUri) throws QueryEvaluationException {

		RepositoryConnection conn = null;
		try {
			String sparqlQueryString = service.getAskQueryString();

			conn = useFreshConnection ? freshConnection() : getConnection();
			BooleanQuery query = conn.prepareBooleanQuery(QueryLanguage.SPARQL, sparqlQueryString, baseUri);

			Iterator<Binding> bIter = bindings.iterator();
			while (bIter.hasNext()) {
				Binding b = bIter.next();
				if (service.getServiceVars().contains(b.getName())) {
					query.setBinding(b.getName(), b.getValue());
				}
			}

			return query.evaluate();
		} catch (MalformedQueryException e) {
			throw new QueryEvaluationException(e);
		} catch (RepositoryException e) {
			throw new QueryEvaluationException(
					"Repository for endpoint " + rep.toString() + " could not be initialized.", e);
		} finally {
			if (useFreshConnection) {
				closeQuietly(conn);
			}
		}
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(Service service,
			CloseableIteration<BindingSet> bindings, String baseUri)
			throws QueryEvaluationException {

		// SPARQL defines a constant-IRI SERVICE as ONE logical Invocation producing one result multiset (or,
		// for SILENT, one empty mapping on failure). Splitting the input bindings into blocks and sending one
		// remote request per block partitions that Invocation: separate requests may observe different remote
		// state, generate distinct fresh values (UUID/BNODE), and fail partially — outcomes no single
		// Invocation can produce. Partitioned (batched) evaluation is therefore only used when the endpoint
		// has been explicitly declared partition-tolerant; otherwise the input is either pushed down in one
		// single request (when small enough) or the service pattern is evaluated once and joined locally.
		if (partitionToleranceDeclared && boundJoinBlockSize > 0) {
			return new BatchingServiceIteration(bindings, boundJoinBlockSize, service);
		}
		return evaluateInternal(service, bindings, service.getBaseURI());
	}

	/**
	 * Evaluate the SPARQL query that can be constructed from the SERVICE node at the initialized {@link Repository} of
	 * this {@link FederatedService}. Use specified bindings as constraints to the query. Try to evaluate using VALUES
	 * clause, if this yields an exception fall back to the naive implementation. This method deals with SILENT
	 * SERVICEs.
	 */
	protected CloseableIteration<BindingSet> evaluateInternal(Service service,
			CloseableIteration<BindingSet> bindings, String baseUri)
			throws QueryEvaluationException {

		// materialize all bindings (to allow for fallback in case of errors)
		// note that this may be blocking depending on the underlying iterator
		List<BindingSet> allBindings = new LinkedList<>();
		while (bindings.hasNext()) {
			allBindings.add(bindings.next());
		}

		if (allBindings.isEmpty()) {
			return new EmptyIteration<>();
		}

		// projection vars
		Set<String> projectionVars = new HashSet<>(service.getServiceVars());
		projectionVars.removeAll(allBindings.get(0).getBindingNames());

		// Without a declared partition tolerance, one logical Invocation may not be split into several remote
		// requests. When the input binding set does not fit a single request, evaluate the original service
		// pattern once and join locally instead.
		int singleRequestLimit = boundJoinBlockSize > 0 ? boundJoinBlockSize : Integer.MAX_VALUE;
		if (!partitionToleranceDeclared && allBindings.size() > singleRequestLimit) {
			return evaluateOnceAndJoinLocally(service, allBindings, baseUri);
		}

		// Pushing input bindings into a service pattern that contains a subquery is not observationally
		// equivalent to the one logical Invocation the algebra defines: pre-bound variables pierce the
		// subquery's scope (changing aggregates), and a VALUES clause injected around a subselect constrains
		// its solutions BEFORE any LIMIT/ORDER inside it. Evaluate such patterns once and join locally —
		// UNLESS the user explicitly projects the ?__rowIdx correlation variable in the service pattern,
		// which is the documented opt-in to row-correlated vectored evaluation (an RDF4J extension contract;
		// a degenerate use that duplicates the VALUES variable falls through to the single-invocation
		// fallback via the malformed-query handling).
		boolean userRequestedRowCorrelation = service.getServiceExpressionString() != null
				&& service.getServiceExpressionString().contains("?" + ROW_IDX_VAR);
		if (!userRequestedRowCorrelation && TupleExprs.containsSubquery(service.getArg())) {
			return evaluateOnceAndJoinLocally(service, allBindings, baseUri);
		}

		// below we need to take care for SILENT services
		RepositoryConnection conn = null;
		CloseableIteration<BindingSet> result = null;
		try {
			// fallback to simple evaluation (just a single binding)
			if (allBindings.size() == 1) {
				// select() handles SILENT atomically (complete buffering before exposure)
				return select(service, projectionVars, allBindings.get(0), baseUri);
			}

			// To be able to insert the input bindings again later on, we need some
			// means to identify the row of each binding. hence, we use an
			// additional
			// projection variable, which is also passed in the VALUES clause
			// with the value of the actual row. The value corresponds to the index
			// of the binding in the index list. The variable is freshly generated: a fixed helper name could
			// collide with a user variable of the same name in the service pattern or the input bindings.
			// When the user explicitly projects ?__rowIdx as the vectored-evaluation opt-in, that exact name
			// IS the correlation contract and is used as-is.
			String rowIdxVar = userRequestedRowCorrelation ? ROW_IDX_VAR
					: freshRowIndexVariable(service, allBindings);
			projectionVars.add(rowIdxVar);

			String queryString = service.getSelectQueryString(projectionVars);

			List<String> relevantBindingNames = getRelevantBindingNames(allBindings, service.getServiceVars());

			if (!relevantBindingNames.isEmpty()) {
				// insert VALUES clause into the query
				queryString = insertValuesClause(queryString,
						buildVALUESClause(allBindings, relevantBindingNames, rowIdxVar), rowIdxVar);
			}

			conn = useFreshConnection ? freshConnection() : getConnection();
			TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString, baseUri);
			TupleQueryResult res;
			query.setMaxExecutionTime(60); // TODO how to retrieve max query value
			// from actual setting?
			res = query.evaluate();

			if (relevantBindingNames.isEmpty()) {
				result = new SPARQLCrossProductIteration(res, allBindings); // cross
				// product
			} else {
				result = new ServiceJoinConversionIteration(res, allBindings, rowIdxVar); // common
				// join
			}

			if (useFreshConnection) {
				result = new CloseConnectionIteration(result, conn);
			}

			if (service.isSilent()) {
				// A failed SILENT Invocation evaluates to the singleton empty mapping — never to a prefix of
				// its rows. Buffer the complete result before exposing anything, so a mid-stream failure can
				// still discard all partial rows and substitute the pass-through of the input bindings.
				return bufferSilently(result, allBindings);
			}
			return result;

		} catch (RepositoryException e) {
			if (useFreshConnection) {
				closeQuietly(conn);
			}
			if (result != null) {
				result.close();
			}
			if (service.isSilent()) {
				return new CollectionIteration<>(allBindings);
			}
			throw new QueryEvaluationException(
					"Repository for endpoint " + rep.toString() + " could not be initialized.", e);
		} catch (MalformedQueryException e) {
			if (useFreshConnection) {
				closeQuietly(conn);
			}
			// A malformed generated query is a bug in our VALUES rewriting and must not be silenced.
			// Under the explicit ?__rowIdx opt-in the user requested row-correlated vectored evaluation, so
			// the correlated per-binding evaluation is the honest fallback. Otherwise fall back to ONE
			// invocation of the original service pattern joined locally — never to one remote request per
			// input binding, which would partition the logical Invocation.
			logger.debug("Encountered malformed query exception: " + e.getMessage()
					+ ". Falling back to " + (userRequestedRowCorrelation ? "correlated per-binding evaluation."
							: "a single SERVICE invocation with a local join."));
			if (userRequestedRowCorrelation) {
				return perBindingCorrelatedEvaluation(service, allBindings, baseUri);
			}
			return evaluateOnceAndJoinLocally(service, allBindings, baseUri);
		} catch (QueryEvaluationException e) {
			if (useFreshConnection) {
				closeQuietly(conn);
			}
			if (result != null) {
				result.close();
			}
			if (service.isSilent()) {
				return new CollectionIteration<>(allBindings);
			}
			throw e;
		} catch (RuntimeException e) {
			if (useFreshConnection) {
				closeQuietly(conn);
			}
			if (result != null) {
				result.close();
			}
			// suppress special exceptions (e.g. UndeclaredThrowable with wrapped
			// QueryEval) if silent
			if (service.isSilent()) {
				return new CollectionIteration<>(allBindings);
			}
			throw e;
		}
	}

	/**
	 * Evaluate the service expression as ONE logical Invocation of the original service pattern (no VALUES clause, no
	 * per-binding requests) and join the remote solutions with the input bindings locally using compatible-mapping
	 * merging. This is the conforming fallback whenever pushing the bindings down is not possible: partitioning the
	 * Invocation into one remote request per input binding is never authorized by a fallback — separate requests may
	 * observe different remote state, generate distinct fresh values, and fail partially. SILENT handling is inherited
	 * from {@link #select(Service, Set, BindingSet, String)}: a failed silent Invocation yields the singleton empty
	 * mapping, and joining it locally passes the input bindings through unchanged.
	 *
	 * @param service     the SERVICE
	 * @param allBindings all input bindings
	 * @param baseUri     the base URI
	 * @return resulting iteration
	 */
	private CloseableIteration<BindingSet> evaluateOnceAndJoinLocally(Service service,
			List<BindingSet> allBindings, String baseUri) {

		Set<String> projectionVars = new HashSet<>(service.getServiceVars());
		CloseableIteration<BindingSet> remote = select(service, projectionVars, EmptyBindingSet.getInstance(),
				baseUri);
		return new LocalServiceJoinIteration(remote, allBindings);
	}

	/**
	 * Row-correlated per-binding evaluation, available only under the explicit {@code ?__rowIdx} opt-in: one
	 * {@link #select(Service, Set, BindingSet, String)} per input binding, with the binding injected into the remote
	 * query. This is deliberately NOT one logical Invocation — it is the vectored-evaluation extension contract the
	 * user requested.
	 */
	private CloseableIteration<BindingSet> perBindingCorrelatedEvaluation(Service service,
			List<BindingSet> allBindings, String baseUri) {
		Set<String> projectionVars = new HashSet<>(service.getServiceVars());
		Iterator<BindingSet> inputs = allBindings.iterator();
		return new LookAheadIteration<>() {

			private CloseableIteration<BindingSet> current;

			@Override
			protected BindingSet getNextElement() {
				while (true) {
					if (current == null) {
						if (!inputs.hasNext()) {
							return null;
						}
						current = select(service, projectionVars, inputs.next(), baseUri);
					}
					if (current.hasNext()) {
						return current.next();
					}
					current.close();
					current = null;
				}
			}

			@Override
			protected void handleClose() {
				if (current != null) {
					current.close();
				}
			}
		};
	}

	/**
	 * Streams remote SERVICE solutions and merges each with every compatible input binding (SPARQL compatible-mapping
	 * join). Preserves multiplicities on both sides.
	 */
	private static final class LocalServiceJoinIteration extends LookAheadIteration<BindingSet> {

		private final CloseableIteration<BindingSet> remote;
		private final List<BindingSet> inputBindings;
		private BindingSet currentRemote;
		private int inputIndex;

		private LocalServiceJoinIteration(CloseableIteration<BindingSet> remote, List<BindingSet> inputBindings) {
			this.remote = remote;
			this.inputBindings = inputBindings;
		}

		@Override
		protected BindingSet getNextElement() {
			while (true) {
				if (currentRemote == null) {
					if (!remote.hasNext()) {
						return null;
					}
					currentRemote = remote.next();
					inputIndex = 0;
				}
				while (inputIndex < inputBindings.size()) {
					BindingSet input = inputBindings.get(inputIndex++);
					BindingSet merged = merge(input, currentRemote);
					if (merged != null) {
						return merged;
					}
				}
				currentRemote = null;
			}
		}

		private static BindingSet merge(BindingSet input, BindingSet remoteSolution) {
			SPARQLQueryBindingSet merged = new SPARQLQueryBindingSet(input.size() + remoteSolution.size());
			merged.addAll(input);
			for (Binding binding : remoteSolution) {
				Value existing = merged.getValue(binding.getName());
				if (existing == null) {
					merged.addBinding(binding);
				} else if (!existing.equals(binding.getValue())) {
					return null;
				}
			}
			return merged;
		}

		@Override
		protected void handleClose() {
			remote.close();
		}
	}

	/**
	 * Drains the complete result before exposing any row. On success the buffered rows are replayed; on failure all
	 * partial rows are discarded and the input bindings pass through unchanged — a failed SILENT Invocation evaluates
	 * to the singleton empty mapping, never to a prefix of its rows.
	 */
	private CloseableIteration<BindingSet> bufferSilently(CloseableIteration<BindingSet> result,
			List<BindingSet> inputBindings) {
		List<BindingSet> buffered = new ArrayList<>();
		try {
			try (result) {
				while (result.hasNext()) {
					buffered.add(result.next());
				}
			}
		} catch (RuntimeException e) {
			logger.debug("SILENT service invocation failed after {} row(s); substituting the empty mapping: {}",
					buffered.size(), e.getMessage());
			return new CollectionIteration<>(new ArrayList<>(inputBindings));
		}
		return new CollectionIteration<>(buffered);
	}

	/**
	 * Returns a synthetic row-index variable name that cannot collide with any user-visible variable: it is absent from
	 * the service's variables, from every input binding name, and from the serialized service pattern text (which
	 * conservatively also covers string literals mentioning the candidate).
	 */
	private static String freshRowIndexVariable(Service service, List<BindingSet> allBindings) {
		String expressionText = service.getServiceExpressionString();
		String candidate = "__rowIdx";
		int suffix = 0;
		while (collides(candidate, service, allBindings, expressionText)) {
			candidate = "__rowIdx" + (++suffix);
		}
		return candidate;
	}

	private static boolean collides(String candidate, Service service, List<BindingSet> allBindings,
			String expressionText) {
		if (service.getServiceVars().contains(candidate)
				|| (expressionText != null && expressionText.contains(candidate))) {
			return true;
		}
		for (BindingSet binding : allBindings) {
			if (binding.hasBinding(candidate)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Insert the constructed VALUES clause in the beginning of the WHERE block. Also adds the {@link #ROW_IDX_VAR}
	 * projection if it is not already present.
	 *
	 * @param queryString  the SELECT query string from the SERVICE node
	 * @param valuesClause the constructed VALUES clause
	 * @return the final String
	 */
	protected String insertValuesClause(String queryString, String valuesClause) {
		return insertValuesClause(queryString, valuesClause, ROW_IDX_VAR);
	}

	/**
	 * Insert the constructed VALUES clause in the beginning of the WHERE block. Also adds the given row-index
	 * projection if it is not already present.
	 *
	 * @param queryString  the SELECT query string from the SERVICE node
	 * @param valuesClause the constructed VALUES clause
	 * @param rowIdxVar    the synthetic row-index variable name used in the VALUES clause
	 * @return the final String
	 */
	protected String insertValuesClause(String queryString, String valuesClause, String rowIdxVar) {
		StringBuilder sb = new StringBuilder(queryString);
		if (sb.indexOf(rowIdxVar) == -1) {
			// Note: we also explicitly check on "SELECT *", however, this
			// check is heuristics based. If the generated query is invalid
			// after this, the fallback evaluation will jump in
			// This currently does not cover things like "SELECT *"
			if (sb.indexOf("SELECT * ") == -1) {
				sb.insert(sb.indexOf("SELECT") + 6, " ?" + rowIdxVar);
			}
		}
		sb.insert(sb.indexOf("{") + 1, " " + valuesClause);
		return sb.toString();
	}

	@Override
	public void initialize() throws QueryEvaluationException {
		try {
			rep.init();
		} catch (RepositoryException e) {
			throw new QueryEvaluationException(e);
		}
	}

	@Override
	public boolean isInitialized() {
		return rep.isInitialized();
	}

	public int getBoundJoinBlockSize() {
		return boundJoinBlockSize;
	}

	/**
	 * @param boundJoinBlockSize the bound join block size, 0 to evaluate all in a single request
	 */
	public void setBoundJoinBlockSize(int boundJoinBlockSize) {
		this.boundJoinBlockSize = boundJoinBlockSize;
	}

	public boolean isPartitionToleranceDeclared() {
		return partitionToleranceDeclared;
	}

	/**
	 * Declares whether the wrapped endpoint tolerates partitioning one logical SERVICE Invocation into several remote
	 * requests with observationally equivalent outcomes. SPARQL defines a constant-IRI SERVICE as one Invocation
	 * producing one result multiset; block-wise evaluation sends several requests, which is only equivalent when the
	 * endpoint guarantees: stable results for the duration of the query, equivalent blank node and volatile-function
	 * behavior across requests, all-or-nothing failure, and equivalent SILENT handling. This is an affirmative
	 * capability declaration by the operator — a deterministic remote pattern alone does not establish it. Default:
	 * {@code false}, in which case the input bindings are either pushed down in one single request or the service
	 * pattern is evaluated once and joined locally.
	 */
	public void setPartitionToleranceDeclared(boolean partitionToleranceDeclared) {
		this.partitionToleranceDeclared = partitionToleranceDeclared;
	}

	/**
	 * @param flag whether to use a fresh {@link RepositoryConnection} for each individual query
	 */
	public void setUseFreshConnection(boolean flag) {
		this.useFreshConnection = flag;
	}

	@Override
	public void shutdown() throws QueryEvaluationException {
		boolean foundException = false;
		try {
			if (managedConn != null) {
				managedConn.close();
			}
		} catch (RepositoryException e) {
			foundException = true;
			throw new QueryEvaluationException(e);
		} finally {
			try {
				// shutdown only if desired, e.g. do not
				// invoke shutDown for managed repositories
				if (shutDown) {
					rep.shutDown();
				}
			} catch (RepositoryException e) {
				// Try not to clobber the initial exception that may be more useful
				if (!foundException) {
					throw new QueryEvaluationException(e);
				}
			}
		}
	}

	/**
	 * Return a fresh {@link RepositoryConnection} from the configured repository.
	 *
	 * @return connection
	 * @throws RepositoryException
	 */
	private RepositoryConnection freshConnection() throws RepositoryException {
		return rep.getConnection();
	}

	/**
	 * Retrieve a (re-usable) connection. If it is not yet created, open a fresh connection. Note that this connection
	 * is closed automatically when shutting this service.
	 *
	 * @return connection
	 * @throws RepositoryException
	 */
	protected synchronized RepositoryConnection getConnection() throws RepositoryException {
		if (managedConn == null) {
			managedConn = freshConnection();
		}
		return managedConn;
	}

	/**
	 * Compute the relevant binding names using the variables occurring in the service expression and the input
	 * bindings. The idea is find all variables which need to be projected in the subquery, i.e. those that will not be
	 * bound by an input binding.
	 * <p>
	 * If the resulting list is empty, the cross product needs to be formed.
	 *
	 * @param bindings
	 * @param serviceVars
	 * @return the list of relevant bindings (if empty: the cross product needs to be formed)
	 */
	private List<String> getRelevantBindingNames(List<BindingSet> bindings, Set<String> serviceVars) {

		// get the bindings variables
		// TODO CHECK: does the first bindingset give all relevant names

		List<String> relevantBindingNames = new ArrayList<>(5);
		for (String bName : bindings.get(0).getBindingNames()) {
			if (serviceVars.contains(bName)) {
				relevantBindingNames.add(bName);
			}
		}

		return relevantBindingNames;
	}

	/**
	 * Computes the VALUES clause for the set of relevant input bindings. The VALUES clause is attached to a subquery
	 * for block-nested-loop evaluation. Implementation note: we use a special binding to mark the rowIndex of the input
	 * binding.
	 *
	 * @param bindings
	 * @param relevantBindingNames
	 * @return a string with the VALUES clause for the given set of relevant input bindings
	 * @throws QueryEvaluationException
	 */
	private String buildVALUESClause(List<BindingSet> bindings, List<String> relevantBindingNames, String rowIdxVar)
			throws QueryEvaluationException {

		StringBuilder sb = new StringBuilder();
		sb.append(" VALUES (?").append(rowIdxVar); // row index: see comment in evaluateInternal()

		for (String bName : relevantBindingNames) {
			sb.append(" ?").append(bName);
		}

		sb.append(") { ");

		int rowIdx = 0;
		for (BindingSet b : bindings) {
			sb.append(" (");
			sb.append("\"").append(rowIdx++).append("\" "); // identification of
			// the row for post
			// processing
			for (String bName : relevantBindingNames) {
				QueryStringUtil.appendValueAsString(sb, b.getValue(bName)).append(" ");
			}
			sb.append(")");
		}

		sb.append(" }");
		return sb.toString();
	}

	private static void closeQuietly(RepositoryConnection conn) {
		if (conn == null) {
			return;
		}
		try {
			conn.close();
		} catch (Throwable t) {
			logger.warn("Failed to close connection:" + t.getMessage());
			logger.debug("Details: ", t);
		}
	}

	/**
	 * Callback to wrap the runnable prior to passing it to the background Executor. Can be used by specializations to
	 * apply context.
	 *
	 * @param runnable the runnable
	 * @return the runnable
	 */
	protected Runnable wrap(Runnable runnable) {
		return runnable;
	}
}
