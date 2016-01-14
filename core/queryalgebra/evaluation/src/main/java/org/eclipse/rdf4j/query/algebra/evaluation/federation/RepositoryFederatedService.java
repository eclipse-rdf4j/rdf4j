/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.federation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
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
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.CollectionIteration;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.CrossProductIteration;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.SilentIteration;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.query.InsertBindingSetCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Federated Service wrapping the {@link Repository} to communicate with a
 * SPARQL endpoint.
 * 
 * @author Andreas Schwarte
 */
public class RepositoryFederatedService implements FederatedService {

	final static Logger logger = LoggerFactory.getLogger(RepositoryFederatedService.class);

	/**
	 * A convenience iteration for SERVICE expression which evaluates
	 * intermediate results in batches and manages all results. Uses
	 * {@link JoinExecutorBase} facilities to guarantee correct access to the
	 * final results
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
				int blockSize, Service service)
			throws QueryEvaluationException
		{
			super(inputBindings, null, EmptyBindingSet.getInstance());
			this.blockSize = blockSize;
			this.service = service;
			run();
		}

		@Override
		protected void handleBindings()
			throws Exception
		{
			while (!closed && leftIter.hasNext()) {

				ArrayList<BindingSet> blockBindings = new ArrayList<BindingSet>(blockSize);
				for (int i = 0; i < blockSize; i++) {
					if (!leftIter.hasNext())
						break;
					blockBindings.add(leftIter.next());
				}
				CloseableIteration<BindingSet, QueryEvaluationException> materializedIter = new CollectionIteration<BindingSet, QueryEvaluationException>(
						blockBindings);
				addResult(evaluateInternal(service, materializedIter, service.getBaseURI()));
			}
		}
	}

	protected final Repository rep;
	
	// flag indicating whether the repository shall be closed in #shutdown()
	protected boolean shutDown = true;

	protected RepositoryConnection conn = null;

	/**
	 * @param repo
	 * 			the repository to be used
	 */
	public RepositoryFederatedService(Repository repo) {
		this(repo, true);
	}
	
	/**
	 * @param repo
	 * 			the repository to be used
	 * @param shutDown
	 * 			a flag indicating whether the repository shall be closed in {@link #shutdown()}
	 */
	public RepositoryFederatedService(Repository repo, boolean shutDown) {
		super();
		this.rep = repo;
		this.shutDown = shutDown;
	}

	/**
	 * Evaluate the provided sparqlQueryString at the initialized
	 * {@link Repository} of this {@link FederatedService}. Insert bindings
	 * into SELECT query and evaluate
	 */
	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> select(Service service,
			Set<String> projectionVars, BindingSet bindings, String baseUri)
		throws QueryEvaluationException
	{

		try {
			String sparqlQueryString = service.getSelectQueryString(projectionVars);
			TupleQuery query = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, sparqlQueryString,
					baseUri);

			Iterator<Binding> bIter = bindings.iterator();
			while (bIter.hasNext()) {
				Binding b = bIter.next();
				if (service.getServiceVars().contains(b.getName()))
					query.setBinding(b.getName(), b.getValue());
			}

			TupleQueryResult res = query.evaluate();

			// insert original bindings again
			return new InsertBindingSetCursor(res, bindings);
		}
		catch (MalformedQueryException e) {
			throw new QueryEvaluationException(e);
		}
		catch (RepositoryException e) {
			throw new QueryEvaluationException("Repository for endpoint " + rep.toString()
					+ " could not be initialized.", e);
		}
	}

	/**
	 * Evaluate the provided sparqlQueryString at the initialized
	 * {@link Repository} of this {@link FederatedService}. Insert
	 * bindings, send ask query and return final result
	 */
	@Override
	public boolean ask(Service service, BindingSet bindings, String baseUri)
		throws QueryEvaluationException
	{

		try {
			String sparqlQueryString = service.getAskQueryString();
			BooleanQuery query = getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, sparqlQueryString,
					baseUri);

			Iterator<Binding> bIter = bindings.iterator();
			while (bIter.hasNext()) {
				Binding b = bIter.next();
				if (service.getServiceVars().contains(b.getName()))
					query.setBinding(b.getName(), b.getValue());
			}

			return query.evaluate();
		}
		catch (MalformedQueryException e) {
			throw new QueryEvaluationException(e);
		}
		catch (RepositoryException e) {
			throw new QueryEvaluationException("Repository for endpoint " + rep.toString()
					+ " could not be initialized.", e);
		}
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Service service,
			CloseableIteration<BindingSet, QueryEvaluationException> bindings, String baseUri)
		throws QueryEvaluationException
	{

		// the number of bindings sent in a single subquery.
		// if blockSize is set to 0, the entire input stream is used as block
		// input
		// the block size effectively determines the number of remote requests
		int blockSize = 15; // TODO configurable block size

		if (blockSize > 0) {
			return new BatchingServiceIteration(bindings, blockSize, service);
		}
		else {
			// if blocksize is 0 (i.e. disabled) the entire iteration is used as
			// block
			return evaluateInternal(service, bindings, service.getBaseURI());
		}
	}

	/**
	 * Evaluate the SPARQL query that can be constructed from the SERVICE node at
	 * the initialized {@link Repository} of this {@link FederatedService}.
	 * Use specified bindings as constraints to the query. Try to evaluate using
	 * BINDINGS clause, if this yields an exception fall back to the naive
	 * implementation. This method deals with SILENT SERVICEs.
	 */
	protected CloseableIteration<BindingSet, QueryEvaluationException> evaluateInternal(Service service,
			CloseableIteration<BindingSet, QueryEvaluationException> bindings, String baseUri)
		throws QueryEvaluationException
	{

		// materialize all bindings (to allow for fallback in case of errors)
		// note that this may be blocking depending on the underlying iterator
		List<BindingSet> allBindings = new LinkedList<BindingSet>();
		while (bindings.hasNext()) {
			allBindings.add(bindings.next());
		}

		if (allBindings.size() == 0) {
			return new EmptyIteration<BindingSet, QueryEvaluationException>();
		}

		// projection vars
		Set<String> projectionVars = new HashSet<String>(service.getServiceVars());
		projectionVars.removeAll(allBindings.get(0).getBindingNames());

		// below we need to take care for SILENT services
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
			// projection variable, which is also passed in the BINDINGS clause
			// with the value of the actual row. The value corresponds to the index
			// of the binding in the index list
			projectionVars.add("__rowIdx");

			String queryString = service.getSelectQueryString(projectionVars);

			List<String> relevantBindingNames = getRelevantBindingNames(allBindings, service.getServiceVars());

			if (relevantBindingNames.size() != 0) {
				// append the VALUES clause to the query
				queryString += buildVALUESClause(allBindings, relevantBindingNames);
			}

			TupleQuery query = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, queryString, baseUri);
			TupleQueryResult res = null;
			try {
				query.setMaxQueryTime(60); // TODO how to retrieve max query value
													// from actual setting?
				res = query.evaluate();
			}
			catch (QueryEvaluationException q) {

				closeQuietly(res);

				// use fallback: endpoint might not support BINDINGS clause
				result = new ServiceFallbackIteration(service, projectionVars, allBindings, this);
				result = service.isSilent() ? new SilentIteration(result) : result;
				return result;
			}

			if (relevantBindingNames.size() == 0)
				result = new CrossProductIteration(res, allBindings); // cross
																									// product
			else
				result = new ServiceJoinConversionIteration(res, allBindings); // common
																									// join

			result = service.isSilent() ? new SilentIteration(result) : result;
			return result;

		}
		catch (RepositoryException e) {
			Iterations.closeCloseable(result);
			if (service.isSilent())
				return new CollectionIteration<BindingSet, QueryEvaluationException>(allBindings);
			throw new QueryEvaluationException("Repository for endpoint " + rep.toString()
					+ " could not be initialized.", e);
		}
		catch (MalformedQueryException e) {
			// this exception must not be silenced, bug in our code
			throw new QueryEvaluationException(e);
		}
		catch (QueryEvaluationException e) {
			Iterations.closeCloseable(result);
			if (service.isSilent())
				return new CollectionIteration<BindingSet, QueryEvaluationException>(allBindings);
			throw e;
		}
		catch (RuntimeException e) {
			Iterations.closeCloseable(result);
			// suppress special exceptions (e.g. UndeclaredThrowable with wrapped
			// QueryEval) if silent
			if (service.isSilent())
				return new CollectionIteration<BindingSet, QueryEvaluationException>(allBindings);
			throw e;
		}
	}

	@Override
	public void initialize()
		throws QueryEvaluationException
	{
		try {
			rep.initialize();
		}
		catch (RepositoryException e) {
			throw new QueryEvaluationException(e);
		}
	}

	@Override
	public boolean isInitialized() {
		return rep.isInitialized();
	}

	private void closeQuietly(TupleQueryResult res) {
		try {
			if (res != null)
				res.close();
		}
		catch (Exception e) {
			logger.debug("Could not close connection properly: " + e.getMessage(), e);
		}
	}

	@Override
	public void shutdown()
		throws QueryEvaluationException
	{
		boolean foundException = false;
		try {
			if (conn != null) {
				conn.close();
			}
		}
		catch (RepositoryException e) {
			foundException = true;
			throw new QueryEvaluationException(e);
		}
		finally {
			try {
				// shutdown only if desired, e.g. do not 
				// invoke shutDown for managed repositories
				if (shutDown) {
					rep.shutDown();
				}
			}
			catch (RepositoryException e) {
				// Try not to clobber the initial exception that may be more useful
				if (!foundException) {
					throw new QueryEvaluationException(e);
				}
			}
		}
	}

	protected RepositoryConnection getConnection()
		throws RepositoryException
	{
		// use a cache connection if possible
		// (TODO add mechanism to unset/close connection)
		if (conn == null) {
			conn = rep.getConnection();
		}
		return conn;
	}

	/**
	 * Compute the relevant binding names using the variables occuring in the
	 * service expression and the input bindings. The idea is find all variables
	 * which need to be projected in the subquery, i.e. those that will not be
	 * bound by an input binding.
	 * <p>
	 * If the resulting list is empty, the cross product needs to be formed.
	 * 
	 * @param bindings
	 * @param serviceVars
	 * @return the list of relevant bindings (if empty: the cross product needs
	 *         to be formed)
	 */
	private List<String> getRelevantBindingNames(List<BindingSet> bindings, Set<String> serviceVars) {

		// get the bindings variables
		// TODO CHECK: does the first bindingset give all relevant names

		List<String> relevantBindingNames = new ArrayList<String>(5);
		for (String bName : bindings.get(0).getBindingNames()) {
			if (serviceVars.contains(bName))
				relevantBindingNames.add(bName);
		}

		return relevantBindingNames;
	}

	/**
	 * Computes the VALUES clause for the set of relevant input bindings. The
	 * VALUES clause is attached to a subquery for block-nested-loop evaluation.
	 * Implementation note: we use a special binding to mark the rowIndex of the
	 * input binding.
	 * 
	 * @param bindings
	 * @param relevantBindingNames
	 * @return a string with the VALUES clause for the given set of relevant
	 *         input bindings
	 * @throws QueryEvaluationException
	 */
	private String buildVALUESClause(List<BindingSet> bindings, List<String> relevantBindingNames)
		throws QueryEvaluationException
	{

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
				appendValueAsString(sb, b.getValue(bName)).append(" ");
			}
			sb.append(")");
		}

		sb.append(" }");
		return sb.toString();
	}

	protected StringBuilder appendValueAsString(StringBuilder sb, Value value) {

		// TODO check if there is some convenient method in Sesame!

		if (value == null)
			return sb.append("UNDEF"); // see grammar for BINDINGs def

		else if (value instanceof URI)
			return appendURI(sb, (URI)value);

		else if (value instanceof Literal)
			return appendLiteral(sb, (Literal)value);

		// XXX check for other types ? BNode ?
		throw new RuntimeException("Type not supported: " + value.getClass().getCanonicalName());
	}

	/**
	 * Append the uri to the stringbuilder, i.e. <uri.stringValue>.
	 * 
	 * @param sb
	 * @param uri
	 * @return the StringBuilder, for convenience
	 */
	protected static StringBuilder appendURI(StringBuilder sb, URI uri) {
		sb.append("<").append(uri.stringValue()).append(">");
		return sb;
	}

	/**
	 * Append the literal to the stringbuilder: "myLiteral"^^<dataType>
	 * 
	 * @param sb
	 * @param lit
	 * @return the StringBuilder, for convenience
	 */
	protected static StringBuilder appendLiteral(StringBuilder sb, Literal lit) {
		sb.append('"');
		sb.append(lit.getLabel().replace("\"", "\\\""));
		sb.append('"');

		if (Literals.isLanguageLiteral(lit)) {
			sb.append('@');
			sb.append(lit.getLanguage());
		}
		else {
			sb.append("^^<");
			sb.append(lit.getDatatype().stringValue());
			sb.append('>');
		}
		return sb;
	}
}
