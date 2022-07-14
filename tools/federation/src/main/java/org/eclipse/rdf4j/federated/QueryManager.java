/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.FederationEvaluationStatistics;
import org.eclipse.rdf4j.federated.exception.FedXException;
import org.eclipse.rdf4j.federated.exception.FedXRuntimeException;
import org.eclipse.rdf4j.federated.repository.FedXRepository;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.structures.QueryType;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.parser.ParsedOperation;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * QueryManager to manage queries.
 *
 * a) Management of running queries (abort, finish) b) Factory to create queries
 *
 * @author Andreas Schwarte
 */
public class QueryManager {

	private static final Logger log = LoggerFactory.getLogger(QueryManager.class);

	private final AtomicBigInteger nextQueryID;
	private final Set<QueryInfo> runningQueries = new ConcurrentSkipListSet<>();
	private final Map<String, String> prefixDeclarations = new HashMap<>();

	private FedXRepository repo;
	private FederationContext federationContext;

	/**
	 * The global {@link RepositoryConnection} used by the query manager.
	 * <p>
	 * Always access using {@link #getOrCreateConn()}
	 * </p>
	 */
	private transient RepositoryConnection conn;

	public QueryManager() {

		BigInteger lastQueryId = new BigInteger("0");
		this.nextQueryID = new AtomicBigInteger(lastQueryId);
	}

	public void init(FedXRepository repo, FederationContext federationContext) {

		this.federationContext = federationContext;
		this.repo = repo;

		// initialize prefix declarations, if any
		String prefixFile = federationContext.getConfig().getPrefixDeclarations();
		if (prefixFile != null) {
			Properties props = new Properties();
			try (FileInputStream fin = new FileInputStream(new File(prefixFile))) {
				props.load(fin);
			} catch (IOException e) {
				throw new FedXRuntimeException("Error loading prefix properties: " + e.getMessage());
			}

			for (String ns : props.stringPropertyNames()) {
				addPrefixDeclaration(ns, props.getProperty(ns)); // register namespace/prefix pair
			}
		}
	}

	private synchronized RepositoryConnection getOrCreateConn() {
		if (this.conn == null) {
			this.conn = repo.getConnection();
		}
		return this.conn;
	}

	public void shutdown() {
		if (conn != null) {
			try {
				conn.close();
			} catch (RepositoryException e) {
				throw new FedXRuntimeException(e);
			}
		}
	}

	/**
	 * Add the query to the set of running queries, queries are identified via a unique id
	 *
	 * @param queryInfo
	 */
	public void registerQuery(QueryInfo queryInfo) {
		assert runningQueries.contains(queryInfo) : "Duplicate query: query " + queryInfo.getQueryID()
				+ " is already registered.";
		runningQueries.add(queryInfo);
	}

	public Set<QueryInfo> getRunningQueries() {
		return new HashSet<>(runningQueries);
	}

	public int getNumberOfRunningQueries() {
		return runningQueries.size();
	}

	public void abortQuery(QueryInfo queryInfo) {
		synchronized (queryInfo) {
			if (!runningQueries.contains(queryInfo)) {
				return;
			}
			log.info("Aborting query " + queryInfo.getQueryID());
			queryInfo.abort();
			runningQueries.remove(queryInfo);
		}
	}

	public void finishQuery(QueryInfo queryInfo) {
		runningQueries.remove(queryInfo);
	}

	public boolean isRunning(QueryInfo queryInfo) {
		return runningQueries.contains(queryInfo);
	}

	/**
	 * Register a prefix declaration to be used during query evaluation. If a known prefix is used in a query, it is
	 * substituted in the parsing step.
	 *
	 * If namespace is null, the corresponding entry is removed.
	 *
	 * @param prefix    a common prefix, e.g. rdf
	 * @param namespace the corresponding namespace, e.g. "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	 */
	public void addPrefixDeclaration(String prefix, String namespace) {
		if (namespace == null) {
			prefixDeclarations.remove(prefix);
			return;
		}

		prefixDeclarations.put(prefix, namespace);
	}

	/**
	 * Prepare a tuple query which uses the underlying federation to evaluate the query.
	 * <p>
	 *
	 * The queryString is modified to use the declared PREFIX declarations, see
	 * {@link FedXConfig#getPrefixDeclarations()} for details.
	 *
	 * @param queryString
	 * @return the prepared tuple query
	 * @throws MalformedQueryException
	 */
	public TupleQuery prepareTupleQuery(String queryString) throws MalformedQueryException {

		Query q = prepareQuery(queryString);
		if (!(q instanceof TupleQuery)) {
			throw new FedXRuntimeException("Query is not a tuple query: " + q.getClass());
		}
		return (TupleQuery) q;
	}

	/**
	 * Prepare a tuple query which uses the underlying federation to evaluate the query.
	 * <p>
	 *
	 * The queryString is modified to use the declared PREFIX declarations, see
	 * {@link FedXConfig#getPrefixDeclarations()} for details.
	 *
	 * @param queryString
	 * @return the prepared graph query
	 * @throws MalformedQueryException
	 */
	public GraphQuery prepareGraphQuery(String queryString) throws MalformedQueryException {

		Query q = prepareQuery(queryString);
		if (!(q instanceof GraphQuery)) {
			throw new FedXRuntimeException("Query is not a graph query: " + q.getClass());
		}
		return (GraphQuery) q;
	}

	/**
	 * Prepare a boolean query which uses the underlying federation to evaluate the query.
	 * <p>
	 *
	 * The queryString is modified to use the declared PREFIX declarations, see
	 * {@link FedXConfig#getPrefixDeclarations()} for details.
	 *
	 * @param queryString
	 * @return the prepared {@link BooleanQuery}
	 * @throws MalformedQueryException
	 */
	public BooleanQuery prepareBooleanQuery(String queryString) throws MalformedQueryException {

		Query q = prepareQuery(queryString);
		if (!(q instanceof BooleanQuery)) {
			throw new FedXRuntimeException("Unexpected query type: " + q.getClass());
		}
		return (BooleanQuery) q;
	}

	static Pattern prefixCheck = Pattern.compile(".*PREFIX .*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	static Pattern prefixPattern = Pattern.compile("PREFIX[ ]*(\\w*):[ ]*<(\\S*)>",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	/**
	 * Prepare a {@link Query} which uses the underlying federation to evaluate the SPARQL query.
	 * <p>
	 *
	 * The queryString is modified to use the declared PREFIX declarations, see
	 * {@link FedXConfig#getPrefixDeclarations()} for details.
	 *
	 * @param queryString
	 * @return the prepared {@link Query}
	 * @throws MalformedQueryException
	 */
	public Query prepareQuery(String queryString) throws MalformedQueryException {

		if (prefixDeclarations.size() > 0) {

			/*
			 * we have to check for prefixes in the query to not add duplicate entries. In case duplicates are present
			 * RDF4J throws a MalformedQueryException
			 */
			if (prefixCheck.matcher(queryString).matches()) {
				queryString = getPrefixDeclarationsCheck(queryString) + queryString;
			} else {
				queryString = getPrefixDeclarations() + queryString;
			}
		}

		Query q;
		try {
			q = getOrCreateConn().prepareQuery(QueryLanguage.SPARQL, queryString);
		} catch (RepositoryException e) {
			throw new FedXRuntimeException(e); // cannot occur
		}

		// TODO set query time

		return q;
	}

	/**
	 * Retrieve the query plan for the given query string.
	 *
	 * @param queryString
	 * @return the query plan
	 * @throws MalformedQueryException
	 * @throws FedXException
	 */
	public String getQueryPlan(String queryString) throws MalformedQueryException, FedXException {

		if (prefixDeclarations.size() > 0) {

			/*
			 * we have to check for prefixes in the query to not add duplicate entries. In case duplicates are present
			 * RDF4J throws a MalformedQueryException
			 */
			if (prefixCheck.matcher(queryString).matches()) {
				queryString = getPrefixDeclarationsCheck(queryString) + queryString;
			} else {
				queryString = getPrefixDeclarations() + queryString;
			}
		}

		ParsedOperation query = QueryParserUtil.parseOperation(QueryLanguage.SPARQL, queryString, null);
		if (!(query instanceof ParsedQuery)) {
			throw new MalformedQueryException("Not a ParsedQuery: " + query.getClass());
		}
		Dataset dataset = ((ParsedQuery) query).getDataset();
		FederationEvalStrategy strategy = federationContext.createStrategy(dataset);
		// we use a dummy query info object here
		QueryInfo qInfo = new QueryInfo(queryString, null, QueryType.SELECT,
				federationContext.getConfig().getEnforceMaxQueryTime(),
				federationContext.getConfig().getIncludeInferredDefault(), federationContext, strategy,
				dataset);
		TupleExpr tupleExpr = ((ParsedQuery) query).getTupleExpr();
		try {
			FederationEvaluationStatistics evaluationStatistics = new FederationEvaluationStatistics(qInfo,
					new SimpleDataset());
			tupleExpr = strategy.optimize(tupleExpr, evaluationStatistics, EmptyBindingSet.getInstance());
			return tupleExpr.toString();
		} catch (SailException e) {
			throw new FedXException("Unable to retrieve query plan: " + e.getMessage());
		}
	}

	/**
	 * Computes the (incremental) next query identifier. Implementation is thread safe and synchronized.
	 *
	 * @return the next query identifier
	 */
	public BigInteger getNextQueryId() {
		return nextQueryID.incrementAndGet();
	}

	/**
	 * Get the prefix declarations that have to be prepended to the query.
	 *
	 * @return the prefix declarations
	 */
	protected String getPrefixDeclarations() {
		StringBuilder sb = new StringBuilder();
		for (String namespace : prefixDeclarations.keySet()) {
			sb.append("PREFIX ")
					.append(namespace)
					.append(": <")
					.append(prefixDeclarations.get(namespace))
					.append(">\r\n");
		}
		return sb.toString();
	}

	/**
	 * Get the prefix declarations that have to be added while considering prefixes that are already declared in the
	 * query. The issue here is that duplicate declaration causes exceptions in Sesame
	 *
	 * @param queryString
	 * @return the prefix declarations
	 */
	protected String getPrefixDeclarationsCheck(String queryString) {

		Set<String> queryPrefixes = findQueryPrefixes(queryString);

		StringBuilder sb = new StringBuilder();
		for (String prefix : prefixDeclarations.keySet()) {
			if (queryPrefixes.contains(prefix)) {
				continue; // already there, do not add
			}
			sb.append("PREFIX ")
					.append(prefix)
					.append(": <")
					.append(prefixDeclarations.get(prefix))
					.append(">\r\n");
		}
		return sb.toString();
	}

	/**
	 * Find all prefixes declared in the query
	 *
	 * @param queryString
	 * @return the prefixes
	 */
	protected static Set<String> findQueryPrefixes(String queryString) {

		HashSet<String> res = new HashSet<>();

		Scanner sc = new Scanner(queryString);
		while (true) {
			while (sc.findInLine(prefixPattern) != null) {
				MatchResult m = sc.match();
				res.add(m.group(1));
			}
			if (!sc.hasNextLine()) {
				break;
			}
			sc.nextLine();
		}
		sc.close();
		return res;
	}

	static class AtomicBigInteger {

		private final AtomicReference<BigInteger> valueHolder = new AtomicReference<>();

		public AtomicBigInteger(BigInteger bigInteger) {
			valueHolder.set(bigInteger);
		}

		public BigInteger incrementAndGet() {
			for (;;) {
				BigInteger current = valueHolder.get();
				BigInteger next = current.add(BigInteger.ONE);
				if (valueHolder.compareAndSet(current, next)) {
					return next;
				}
			}
		}
	}
}
