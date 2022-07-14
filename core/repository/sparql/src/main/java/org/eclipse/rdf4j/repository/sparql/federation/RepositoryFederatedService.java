/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql.federation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.iteration.SilentIteration;
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
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.query.InsertBindingSetCursor;
import org.eclipse.rdf4j.repository.sparql.query.QueryStringUtil;
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

		/**
		 * @param inputBindings
		 * @throws QueryEvaluationException
		 */
		public BatchingServiceIteration(CloseableIteration<BindingSet, QueryEvaluationException> inputBindings,
				int blockSize, Service service) throws QueryEvaluationException {
			super(inputBindings, null, EmptyBindingSet.getInstance());
			this.blockSize = blockSize;
			this.service = service;
			run();
		}

		@Override
		protected void handleBindings() throws Exception {
			while (!isClosed() && leftIter.hasNext()) {

				ArrayList<BindingSet> blockBindings = new ArrayList<>(blockSize);
				for (int i = 0; i < blockSize; i++) {
					if (!leftIter.hasNext()) {
						break;
					}
					blockBindings.add(leftIter.next());
				}
				CloseableIteration<BindingSet, QueryEvaluationException> materializedIter = new CollectionIteration<>(
						blockBindings);
				addResult(evaluateInternal(service, materializedIter, service.getBaseURI()));
			}
		}
	}

	/**
	 * Helper iteration to evaluate a block of {@link BindingSet}s using the simple
	 * {@link RepositoryFederatedService#select(Service, Set, BindingSet, String)} routine.
	 *
	 * @author Andreas Schwarte
	 *
	 */
	private class FallbackServiceIteration extends JoinExecutorBase<BindingSet> {

		private final Service service;
		private final List<BindingSet> allBindings;
		private final String baseUri;

		public FallbackServiceIteration(Service service,
				List<BindingSet> allBindings, String baseUri) {
			super(null, null, null);
			this.service = service;
			this.allBindings = allBindings;
			this.baseUri = baseUri;
			run();
		}

		@Override
		protected void handleBindings() throws Exception {
			Set<String> projectionVars = new HashSet<>(service.getServiceVars());
			for (BindingSet b : allBindings) {
				addResult(select(service, projectionVars, b, baseUri));
			}
		}
	}

	/**
	 * Wrapper iteration which closes a {@link RepositoryConnection} upon {@link #close()}
	 *
	 * @author Andreas Schwarte
	 *
	 */
	private static class CloseConnectionIteration implements CloseableIteration<BindingSet, QueryEvaluationException> {

		private final CloseableIteration<BindingSet, QueryEvaluationException> delegate;
		private final RepositoryConnection connection;

		private CloseConnectionIteration(CloseableIteration<BindingSet, QueryEvaluationException> delegate,
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
	public CloseableIteration<BindingSet, QueryEvaluationException> select(Service service, Set<String> projectionVars,
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
			CloseableIteration<BindingSet, QueryEvaluationException> result = new InsertBindingSetCursor(res, bindings);

			if (useFreshConnection) {
				result = new CloseConnectionIteration(result, conn);
			}

			if (service.isSilent()) {
				return new SilentIteration<>(result);
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
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Service service,
			CloseableIteration<BindingSet, QueryEvaluationException> bindings, String baseUri)
			throws QueryEvaluationException {

		if (boundJoinBlockSize > 0) {
			return new BatchingServiceIteration(bindings, boundJoinBlockSize, service);
		} else {
			// if blocksize is 0 (i.e. disabled) the entire iteration is used as
			// block
			return evaluateInternal(service, bindings, service.getBaseURI());
		}
	}

	/**
	 * Evaluate the SPARQL query that can be constructed from the SERVICE node at the initialized {@link Repository} of
	 * this {@link FederatedService}. Use specified bindings as constraints to the query. Try to evaluate using VALUES
	 * clause, if this yields an exception fall back to the naive implementation. This method deals with SILENT
	 * SERVICEs.
	 */
	protected CloseableIteration<BindingSet, QueryEvaluationException> evaluateInternal(Service service,
			CloseableIteration<BindingSet, QueryEvaluationException> bindings, String baseUri)
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

		// below we need to take care for SILENT services
		RepositoryConnection conn = null;
		CloseableIteration<BindingSet, QueryEvaluationException> result = null;
		try {
			// fallback to simple evaluation (just a single binding)
			if (allBindings.size() == 1) {
				result = select(service, projectionVars, allBindings.get(0), baseUri);
				result = service.isSilent() ? new SilentIteration(result) : result;
				return result;
			}

			// To be able to insert the input bindings again later on, we need some
			// means to identify the row of each binding. hence, we use an
			// additional
			// projection variable, which is also passed in the VALUES clause
			// with the value of the actual row. The value corresponds to the index
			// of the binding in the index list
			projectionVars.add(ROW_IDX_VAR);

			String queryString = service.getSelectQueryString(projectionVars);

			List<String> relevantBindingNames = getRelevantBindingNames(allBindings, service.getServiceVars());

			if (!relevantBindingNames.isEmpty()) {
				// insert VALUES clause into the query
				queryString = insertValuesClause(queryString, buildVALUESClause(allBindings, relevantBindingNames));
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
				result = new ServiceJoinConversionIteration(res, allBindings); // common
				// join
			}

			if (useFreshConnection) {
				result = new CloseConnectionIteration(result, conn);
			}

			result = service.isSilent() ? new SilentIteration(result) : result;
			return result;

		} catch (RepositoryException e) {
			if (useFreshConnection) {
				closeQuietly(conn);
			}
			Iterations.closeCloseable(result);
			if (service.isSilent()) {
				return new CollectionIteration<>(allBindings);
			}
			throw new QueryEvaluationException(
					"Repository for endpoint " + rep.toString() + " could not be initialized.", e);
		} catch (MalformedQueryException e) {
			if (useFreshConnection) {
				closeQuietly(conn);
			}
			// this exception must not be silenced, bug in our code
			// => try a fallback to the simple evaluation
			logger.debug("Encounted malformed query exception: " + e.getMessage()
					+ ". Falling back to simple SERVICE evaluation.");
			return evaluateInternalFallback(service, allBindings, baseUri);
		} catch (QueryEvaluationException e) {
			if (useFreshConnection) {
				closeQuietly(conn);
			}
			Iterations.closeCloseable(result);
			if (service.isSilent()) {
				return new CollectionIteration<>(allBindings);
			}
			throw e;
		} catch (RuntimeException e) {
			if (useFreshConnection) {
				closeQuietly(conn);
			}
			Iterations.closeCloseable(result);
			// suppress special exceptions (e.g. UndeclaredThrowable with wrapped
			// QueryEval) if silent
			if (service.isSilent()) {
				return new CollectionIteration<>(allBindings);
			}
			throw e;
		}
	}

	/**
	 * Evaluate the service expression for the given lists of bindings using {@link FallbackServiceIteration}, i.e.
	 * basically as a simple join without VALUES clause.
	 *
	 * @param service     the SERVICE
	 * @param allBindings all bindings to be processed
	 * @param baseUri     the base URI
	 * @return resulting iteration
	 */
	private CloseableIteration<BindingSet, QueryEvaluationException> evaluateInternalFallback(Service service,
			List<BindingSet> allBindings, String baseUri) {

		CloseableIteration<BindingSet, QueryEvaluationException> res = new FallbackServiceIteration(service,
				allBindings, baseUri);

		if (service.isSilent()) {
			res = new SilentIteration(res);
		}
		return res;

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
		StringBuilder sb = new StringBuilder(queryString);
		if (sb.indexOf(ROW_IDX_VAR) == -1) {
			// Note: we also explicitly check on "SELECT *", however, this
			// check is heuristics based. If the generated query is invalid
			// after this, the fallback evaluation will jump in
			// This currently does not cover things like "SELECT *"
			if (sb.indexOf("SELECT * ") == -1) {
				sb.insert(sb.indexOf("SELECT") + 6, " ?" + ROW_IDX_VAR);
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
	 *
	 * @param boundJoinBlockSize the bound join block size, 0 to evaluate all in a single request
	 */
	public void setBoundJoinBlockSize(int boundJoinBlockSize) {
		this.boundJoinBlockSize = boundJoinBlockSize;
	}

	/**
	 *
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
	private String buildVALUESClause(List<BindingSet> bindings, List<String> relevantBindingNames)
			throws QueryEvaluationException {

		StringBuilder sb = new StringBuilder();
		sb.append(" VALUES (?__rowIdx"); // __rowIdx: see comment in evaluate()

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
}
