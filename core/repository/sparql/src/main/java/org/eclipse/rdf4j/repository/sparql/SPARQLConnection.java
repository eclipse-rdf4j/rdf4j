/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql;

import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Objects;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.ExceptionConvertingIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.http.client.HttpClientDependent;
import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLQueries;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.UnknownTransactionStateException;
import org.eclipse.rdf4j.repository.base.AbstractRepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.query.SPARQLBooleanQuery;
import org.eclipse.rdf4j.repository.sparql.query.SPARQLGraphQuery;
import org.eclipse.rdf4j.repository.sparql.query.SPARQLTupleQuery;
import org.eclipse.rdf4j.repository.sparql.query.SPARQLUpdate;
import org.eclipse.rdf4j.repository.util.RDFLoader;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

/**
 * Provides a {@link RepositoryConnection} interface to any SPARQL endpoint.
 *
 * @author James Leigh
 */
public class SPARQLConnection extends AbstractRepositoryConnection implements HttpClientDependent {

	private static final String EVERYTHING = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }";

	private static final String EVERYTHING_WITH_GRAPH = "SELECT * WHERE {  ?s ?p ?o . OPTIONAL { GRAPH ?ctx { ?s ?p ?o } } }";

	private static final String SOMETHING = "ASK { ?s ?p ?o }";

	private static final String SOMETHING_WITH_GRAPH = "ASK { { GRAPH ?g { ?s ?p ?o } } UNION { ?s ?p ?o } }";

	private static final String NAMEDGRAPHS = "SELECT DISTINCT ?_ WHERE { GRAPH ?_ { ?s ?p ?o } }";

	private static final int DEFAULT_MAX_PENDING_SIZE = 1000000;

	private final SPARQLProtocolSession client;

	private final ModelFactory modelFactory = new DynamicModelFactory();

	private StringBuilder sparqlTransaction;

	private final Object transactionLock = new Object();

	private Model pendingAdds;
	private Model pendingRemoves;

	private final int maxPendingSize = DEFAULT_MAX_PENDING_SIZE;

	private final boolean quadMode;
	private boolean silentClear;

	public SPARQLConnection(SPARQLRepository repository, SPARQLProtocolSession client) {
		this(repository, client, false); // in triple mode by default
	}

	public SPARQLConnection(SPARQLRepository repository, SPARQLProtocolSession client, boolean quadMode) {
		super(repository);
		this.client = client;
		this.quadMode = quadMode;
		this.silentClear = false;
	}

	@Override
	public String toString() {
		return client.getQueryURL();
	}

	/**
	 * Configure the connection to execute {@link #clear(Resource...)} operations silently: the remote endpoint will not
	 * respond with an error if the supplied named graph does not exist on the endpoint.
	 * <p>
	 * By default, the SPARQL connection executes the {@link #clear(Resource...)} API operation by converting it to a
	 * SPARQL `CLEAR GRAPH` update operation. This operation has an optional <code>SILENT</code> modifier, which can be
	 * enabled by setting this flag to <code>true</code>. The behavior of this modifier is speficied as follows in the
	 * SPARQL 1.1 Recommendation:
	 *
	 * <blockquote> If the store records the existence of empty graphs, then the SPARQL 1.1 Update service, by default,
	 * SHOULD return failure if the specified graph does not exist. If SILENT is present, the result of the operation
	 * will always be success.
	 * <p>
	 * Stores that do not record empty graphs will always return success. </blockquote>
	 *
	 * Note that in most SPARQL endpoint implementations not recording empty graphs is the default behavior, and setting
	 * this flag to <code>true</code> will have no effect. Setting this flag will have no effect on any other errors or
	 * other API or SPARQL operations: <strong>only</strong> the behavior of the {@link #clear(Resource...)} API
	 * operation is modified to respond with a success message when removing a non-existent named graph.
	 *
	 * @param silent the value to set this to.
	 * @see https://www.w3.org/TR/sparql11-update/#clear
	 */
	public void setSilentClear(boolean silent) {
		this.silentClear = silent;
	}

	/**
	 * Configure the connection to execute {@link #clear(Resource...)} operations silently: the remote endpoint will not
	 * respond with an error if the supplied named graph does not exist on the endpoint.
	 * <p>
	 * By default, the SPARQL connection executes the {@link #clear(Resource...)} API operation by converting it to a
	 * SPARQL `CLEAR GRAPH` update operation. This operation has an optional <code>SILENT</code> modifier, which can be
	 * enabled by setting this flag to <code>true</code>. The behavior of this modifier is speficied as follows in the
	 * SPARQL 1.1 Recommendation:
	 *
	 * <blockquote> If the store records the existence of empty graphs, then the SPARQL 1.1 Update service, by default,
	 * SHOULD return failure if the specified graph does not exist. If SILENT is present, the result of the operation
	 * will always be success.
	 * <p>
	 * Stores that do not record empty graphs will always return success. </blockquote>
	 *
	 * Note that in most SPARQL endpoint implementations not recording empty graphs is the default behavior, and setting
	 * this flag to <code>true</code> will have no effect. Setting this flag will have no effect on any other errors or
	 * other API or SPARQL operations: <strong>only</strong> the behavior of the {@link #clear(Resource...)} API
	 * operation is modified to respond with a success message when removing a non-existent named graph.
	 *
	 * @param flag the value to set this to.
	 * @see https://www.w3.org/TR/sparql11-update/#clear
	 * @deprecated since 3.6.0 - use {@link #setSilentClear(boolean)} instead.
	 */
	@Deprecated
	public void enableSilentMode(boolean flag) {
		setSilentClear(flag);
	}

	@Override
	public void setParserConfig(ParserConfig parserConfig) {
		client.setParserConfig(parserConfig);
		super.setParserConfig(parserConfig);
	}

	@Override
	public final HttpClient getHttpClient() {
		return client.getHttpClient();
	}

	@Override
	public void setHttpClient(HttpClient httpClient) {
		client.setHttpClient(httpClient);
	}

	@Override
	public void close() throws RepositoryException {
		try {
			super.close();
		} finally {
			client.close();
		}
	}

	@Override
	public void exportStatements(Resource subj, IRI pred, Value obj, boolean includeInferred, RDFHandler handler,
			Resource... contexts) throws RepositoryException, RDFHandlerException {
		try {
			GraphQuery query = prepareGraphQuery(SPARQL, EVERYTHING, "");
			setBindings(query, subj, pred, obj, contexts);
			query.evaluate(handler);
		} catch (MalformedQueryException | QueryEvaluationException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public RepositoryResult<Resource> getContextIDs() throws RepositoryException {
		TupleQueryResult iter = null;
		RepositoryResult<Resource> result = null;

		boolean allGood = false;
		try {
			TupleQuery query = prepareTupleQuery(SPARQL, NAMEDGRAPHS, "");
			iter = query.evaluate();
			result = new RepositoryResult<>(new ExceptionConvertingIteration<Resource, RepositoryException>(
					new ConvertingIteration<BindingSet, Resource, QueryEvaluationException>(iter) {

						@Override
						protected Resource convert(BindingSet bindings) throws QueryEvaluationException {
							return (Resource) bindings.getValue("_");
						}
					}) {

				@Override
				protected RepositoryException convert(Exception e) {
					return new RepositoryException(e);
				}
			});
			allGood = true;
			return result;
		} catch (MalformedQueryException |

				QueryEvaluationException e) {
			throw new RepositoryException(e);
		} finally {
			if (!allGood) {
				try {
					if (result != null) {
						result.close();
					}
				} finally {
					if (iter != null) {
						iter.close();
					}
				}
			}
		}
	}

	@Override
	public String getNamespace(String prefix) throws RepositoryException {
		return null;
	}

	@Override
	public RepositoryResult<Namespace> getNamespaces() throws RepositoryException {
		return new RepositoryResult<>(new EmptyIteration<>());
	}

	@Override
	public boolean isEmpty() throws RepositoryException {
		try {
			BooleanQuery query;
			if (isQuadMode()) {
				query = prepareBooleanQuery(SPARQL, SOMETHING_WITH_GRAPH);
			} else {
				query = prepareBooleanQuery(SPARQL, SOMETHING);
			}
			return !query.evaluate();
		} catch (MalformedQueryException | QueryEvaluationException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public long size(Resource... contexts) throws RepositoryException {
		try (RepositoryResult<Statement> stmts = getStatements(null, null, null, true, contexts)) {
			long i = 0;
			while (stmts.hasNext()) {
				stmts.next();
				i++;
			}
			return i;
		}
	}

	@Override
	public RepositoryResult<Statement> getStatements(Resource subj, IRI pred, Value obj, boolean includeInferred,
			Resource... contexts) throws RepositoryException {
		try {
			if (isQuadMode()) {
				return getStatementsQuadMode(subj, pred, obj, includeInferred, contexts);
			} else if (subj != null && pred != null && obj != null) {
				return getStatementsSingleTriple(subj, pred, obj, includeInferred, contexts);
			} else {
				return getStatementGeneral(subj, pred, obj, includeInferred, contexts);
			}
		} catch (MalformedQueryException | QueryEvaluationException e) {
			throw new RepositoryException(e);
		}
	}

	private RepositoryResult<Statement> getStatementsQuadMode(Resource subj, IRI pred, Value obj,
			boolean includeInferred, Resource... contexts)
			throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		TupleQueryResult qRes = null;
		RepositoryResult<Statement> result = null;

		boolean allGood = false;
		try {
			TupleQuery tupleQuery = prepareTupleQuery(SPARQL, EVERYTHING_WITH_GRAPH);
			setBindings(tupleQuery, subj, pred, obj, contexts);
			tupleQuery.setIncludeInferred(includeInferred);
			qRes = tupleQuery.evaluate();
			result = new RepositoryResult<>(new ExceptionConvertingIteration<>(
					toStatementIteration(qRes, subj, pred, obj)) {

				@Override
				protected RepositoryException convert(Exception e) {
					return new RepositoryException(e);
				}
			});
			allGood = true;
			return result;
		} finally {
			if (!allGood) {
				try {
					if (result != null) {
						result.close();
					}
				} finally {
					if (qRes != null) {
						qRes.close();
					}
				}
			}
		}
	}

	private RepositoryResult<Statement> getStatementsSingleTriple(Resource subj, IRI pred, Value obj,
			boolean includeInferred, Resource... contexts) throws RepositoryException {
		if (hasStatement(subj, pred, obj, includeInferred, contexts)) {
			Statement st = getValueFactory().createStatement(subj, pred, obj);
			CloseableIteration<Statement, RepositoryException> cursor;
			cursor = new SingletonIteration<>(st);
			return new RepositoryResult<>(cursor);
		} else {
			return new RepositoryResult<>(new EmptyIteration<>());
		}
	}

	private RepositoryResult<Statement> getStatementGeneral(Resource subj, IRI pred, Value obj, boolean includeInferred,
			Resource... contexts) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		GraphQueryResult gRes = null;
		RepositoryResult<Statement> result = null;

		boolean allGood = false;
		try {
			GraphQuery query = prepareGraphQuery(SPARQL, EVERYTHING, "");
			query.setIncludeInferred(includeInferred);
			setBindings(query, subj, pred, obj, contexts);
			gRes = query.evaluate();
			result = new RepositoryResult<>(
					new ExceptionConvertingIteration<>(gRes) {
						@Override
						protected RepositoryException convert(Exception e) {
							return new RepositoryException(e);
						}
					});
			allGood = true;
			return result;
		} finally {
			if (!allGood) {
				try {
					if (result != null) {
						result.close();
					}
				} finally {
					if (gRes != null) {
						gRes.close();
					}
				}
			}
		}
	}

	@Override
	public boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		try {
			BooleanQuery query = prepareBooleanQuery(SPARQL, SOMETHING, "");
			setBindings(query, subj, pred, obj, contexts);
			return query.evaluate();
		} catch (MalformedQueryException | QueryEvaluationException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public SPARQLRepository getRepository() {
		return (SPARQLRepository) super.getRepository();
	}

	@Override
	public Query prepareQuery(QueryLanguage ql, String query, String base)
			throws RepositoryException, MalformedQueryException {
		if (SPARQL.equals(ql)) {
			String strippedQuery = QueryParserUtil.removeSPARQLQueryProlog(query).toUpperCase();
			if (strippedQuery.startsWith("SELECT")) {
				return prepareTupleQuery(ql, query, base);
			} else if (strippedQuery.startsWith("ASK")) {
				return prepareBooleanQuery(ql, query, base);
			} else {
				return prepareGraphQuery(ql, query, base);
			}
		}
		throw new UnsupportedOperationException("Unsupported query language " + ql);
	}

	@Override
	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query, String base)
			throws RepositoryException, MalformedQueryException {
		if (SPARQL.equals(ql)) {
			return new SPARQLBooleanQuery(client, base, query);
		}
		throw new UnsupportedQueryLanguageException("Unsupported query language " + ql);
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query, String base)
			throws RepositoryException, MalformedQueryException {
		if (SPARQL.equals(ql)) {
			return new SPARQLGraphQuery(client, base, query);
		}
		throw new UnsupportedQueryLanguageException("Unsupported query language " + ql);
	}

	@Override
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query, String base)
			throws RepositoryException, MalformedQueryException {
		if (SPARQL.equals(ql)) {
			return new SPARQLTupleQuery(client, base, query);
		}
		throw new UnsupportedQueryLanguageException("Unsupported query language " + ql);
	}

	@Override
	public void prepare() throws RepositoryException {
		throw new UnsupportedOperationException("SPARQL protocol has no support for 2-phase commit");
	}

	@Override
	public void commit() throws RepositoryException {
		synchronized (transactionLock) {
			if (isActive()) {
				synchronized (transactionLock) {
					flushPendingAdds();
					flushPendingRemoves();
					// treat commit as a no-op if transaction string is empty
					if (sparqlTransaction.length() > 0) {
						SPARQLUpdate transaction = new SPARQLUpdate(client, null, sparqlTransaction.toString());
						try {
							transaction.execute();
						} catch (UpdateExecutionException e) {
							throw new RepositoryException("error executing transaction", e);
						}
					}

					sparqlTransaction = null;
				}
			} else {
				throw new RepositoryException("no transaction active.");
			}
		}
	}

	@Override
	public void rollback() throws RepositoryException {
		synchronized (transactionLock) {
			if (isActive()) {
				synchronized (transactionLock) {
					sparqlTransaction = null;
					pendingAdds = getModelFactory().createEmptyModel();
					pendingRemoves = getModelFactory().createEmptyModel();
				}
			} else {
				throw new RepositoryException("no transaction active.");
			}
		}
	}

	@Override
	public void begin() throws RepositoryException {
		synchronized (transactionLock) {
			if (!isActive()) {
				synchronized (transactionLock) {
					sparqlTransaction = new StringBuilder();
					this.pendingAdds = getModelFactory().createEmptyModel();
					this.pendingRemoves = getModelFactory().createEmptyModel();
				}
			} else {
				throw new RepositoryException("active transaction already exists");
			}
		}
	}

	@Override
	public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

		// to preserve bnode identity, we need to make sure all statements are
		// processed in a single INSERT DATA command
		StatementCollector collector = new StatementCollector();

		boolean localTransaction = startLocalTransaction();

		try {
			RDFLoader loader = new RDFLoader(getParserConfig(), getValueFactory());
			loader.load(file, baseURI, dataFormat, collector);
			add(collector.getStatements(), contexts);
			conditionalCommit(localTransaction);
		} catch (RDFHandlerException e) {
			conditionalRollback(localTransaction);

			// RDFInserter only throws wrapped RepositoryExceptions
			throw (RepositoryException) e.getCause();
		} catch (RDFParseException e) {
			conditionalRollback(localTransaction);
			throw e;
		} catch (IOException | RuntimeException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	@Override
	public void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

		// to preserve bnode identity, we need to make sure all statements are
		// processed in a single INSERT DATA command
		StatementCollector collector = new StatementCollector();
		boolean localTransaction = startLocalTransaction();

		try {
			RDFLoader loader = new RDFLoader(getParserConfig(), getValueFactory());
			loader.load(url, baseURI, dataFormat, collector);
			add(collector.getStatements(), contexts);
			conditionalCommit(localTransaction);
		} catch (RDFHandlerException e) {
			conditionalRollback(localTransaction);

			// RDFInserter only throws wrapped RepositoryExceptions
			throw (RepositoryException) e.getCause();
		} catch (RDFParseException e) {
			conditionalRollback(localTransaction);
			throw e;
		} catch (IOException | RuntimeException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	@Override
	public void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

		// to preserve bnode identity, we need to make sure all statements are
		// processed in a single INSERT DATA command
		StatementCollector collector = new StatementCollector();

		boolean localTransaction = startLocalTransaction();

		try {
			RDFLoader loader = new RDFLoader(getParserConfig(), getValueFactory());
			loader.load(in, baseURI, dataFormat, collector);
			add(collector.getStatements(), contexts);
			conditionalCommit(localTransaction);
		} catch (RDFHandlerException e) {
			conditionalRollback(localTransaction);

			// RDFInserter only throws wrapped RepositoryExceptions
			throw (RepositoryException) e.getCause();
		} catch (RDFParseException e) {
			conditionalRollback(localTransaction);
			throw e;
		} catch (IOException | RuntimeException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	@Override
	public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

		// to preserve bnode identity, we need to make sure all statements are
		// processed in a single INSERT DATA command
		StatementCollector collector = new StatementCollector();

		boolean localTransaction = startLocalTransaction();

		try {
			RDFLoader loader = new RDFLoader(getParserConfig(), getValueFactory());
			loader.load(reader, baseURI, dataFormat, collector);

			add(collector.getStatements(), contexts);
			conditionalCommit(localTransaction);
		} catch (RDFHandlerException e) {
			conditionalRollback(localTransaction);

			// RDFInserter only throws wrapped RepositoryExceptions
			throw (RepositoryException) e.getCause();
		} catch (RDFParseException e) {
			conditionalRollback(localTransaction);
			throw e;
		} catch (IOException | RuntimeException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	@Override
	public void add(Statement st, Resource... contexts) throws RepositoryException {
		boolean localTransaction = startLocalTransaction();
		addWithoutCommit(st, contexts);
		try {
			conditionalCommit(localTransaction);
		} catch (RepositoryException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	@Override
	public void add(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {
		boolean localTransaction = startLocalTransaction();
		for (Statement st : statements) {
			addWithoutCommit(st, contexts);
		}
		try {
			conditionalCommit(localTransaction);
		} catch (RepositoryException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	@Override
	public void clear(Resource... contexts) throws RepositoryException {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");
		boolean localTransaction = startLocalTransaction();

		String clearMode = "CLEAR";
		if (this.isSilentClear()) {
			clearMode = "CLEAR SILENT";
		}

		if (contexts.length == 0) {
			sparqlTransaction.append(clearMode + " ALL ");
			sparqlTransaction.append("; ");
		} else {
			for (Resource context : contexts) {
				if (context == null) {
					sparqlTransaction.append(clearMode + " DEFAULT ");
					sparqlTransaction.append("; ");
				} else if (context instanceof IRI) {
					sparqlTransaction.append(clearMode + " GRAPH <" + context.stringValue() + "> ");
					sparqlTransaction.append("; ");
				} else {
					throw new RepositoryException("SPARQL does not support named graphs identified by blank nodes.");
				}
			}
		}

		try {
			conditionalCommit(localTransaction);
		} catch (RepositoryException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	@Override
	public void clearNamespaces() throws RepositoryException {
		// silently ignore

		// throw new UnsupportedOperationException();
	}

	@Override
	public void remove(Statement st, Resource... contexts) throws RepositoryException {
		boolean localTransaction = startLocalTransaction();
		removeWithoutCommit(st, contexts);
		try {
			conditionalCommit(localTransaction);
		} catch (RepositoryException e) {
			conditionalRollback(localTransaction);
			throw e;
		}

	}

	@Override
	public void remove(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {
		boolean localTransaction = startLocalTransaction();
		for (Statement st : statements) {
			removeWithoutCommit(st, contexts);
		}

		try {
			conditionalCommit(localTransaction);
		} catch (RepositoryException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	@Override
	public void removeNamespace(String prefix) throws RepositoryException {
		// no-op, ignore silently
		// throw new UnsupportedOperationException();
	}

	@Override
	public void setNamespace(String prefix, String name) throws RepositoryException {
		// no-op, ignore silently
		// throw new UnsupportedOperationException();
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String update, String baseURI)
			throws RepositoryException, MalformedQueryException {
		if (SPARQL.equals(ql)) {
			return new SPARQLUpdate(client, baseURI, update);
		}
		throw new UnsupportedQueryLanguageException("Unsupported query language " + ql);
	}

	/* protected/private methods */

	private void setBindings(Query query, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws RepositoryException {
		if (subj != null) {
			query.setBinding("s", subj);
		}
		if (pred != null) {
			query.setBinding("p", pred);
		}
		if (obj != null) {
			query.setBinding("o", obj);
		}
		if (contexts != null && contexts.length > 0) {
			SimpleDataset dataset = new SimpleDataset();
			for (Resource ctx : contexts) {
				if (ctx == null || ctx instanceof IRI) {
					dataset.addDefaultGraph((IRI) ctx);
				} else {
					throw new RepositoryException("Contexts must be URIs");
				}
			}
			query.setDataset(dataset);
		}
	}

	private String createInsertDataCommand(Iterable<? extends Statement> statements, Resource... contexts) {
		StringBuilder qb = new StringBuilder();
		qb.append("INSERT DATA \n");
		qb.append("{ \n");
		if (contexts.length > 0) {
			for (Resource context : contexts) {
				if (context != null) {
					String namedGraph = context.stringValue();
					if (context instanceof BNode) {
						// SPARQL does not allow blank nodes as named graph
						// identifiers, so we need to skolemize
						// the blank node id.
						namedGraph = "urn:nodeid:" + context.stringValue();
					}
					qb.append("    GRAPH <" + namedGraph + "> { \n");
				}
				createDataBody(qb, statements, true);
				if (context != null) {
					qb.append(" } \n");
				}
			}
		} else {
			createDataBody(qb, statements, false);
		}
		qb.append("}");

		return qb.toString();
	}

	private String createDeleteDataCommand(Iterable<? extends Statement> statements, Resource... contexts) {
		StringBuilder qb = new StringBuilder();
		qb.append("DELETE DATA \n");
		qb.append("{ \n");
		if (contexts.length > 0) {
			for (Resource context : contexts) {
				if (context != null) {
					String namedGraph = context.stringValue();
					if (context instanceof BNode) {
						// SPARQL does not allow blank nodes as named graph
						// identifiers, so we need to skolemize
						// the blank node id.
						namedGraph = "urn:nodeid:" + context.stringValue();
					}
					qb.append("    GRAPH <" + namedGraph + "> { \n");
				}
				createDataBody(qb, statements, true);
				if (context != null) {
					qb.append(" } \n");
				}
			}
		} else {
			createDataBody(qb, statements, false);
		}
		qb.append("}");

		return qb.toString();
	}

	private void createDataBody(StringBuilder qb, Iterable<? extends Statement> statements, boolean ignoreContext) {
		for (Statement st : statements) {
			final Resource context = st.getContext();
			if (!ignoreContext) {
				if (context != null) {
					String namedGraph = context.stringValue();
					if (context instanceof BNode) {
						// SPARQL does not allow blank nodes as named graph
						// identifiers, so we need to skolemize
						// the blank node id.
						namedGraph = "urn:nodeid:" + context.stringValue();
					}
					qb.append("    GRAPH <" + namedGraph + "> { \n");
				}
			}
			if (st.getSubject() instanceof BNode) {
				qb.append("_:" + st.getSubject().stringValue() + " ");
			} else {
				qb.append("<" + st.getSubject().stringValue() + "> ");
			}

			qb.append("<" + st.getPredicate().stringValue() + "> ");

			if (st.getObject() instanceof Literal) {
				Literal lit = (Literal) st.getObject();
				qb.append("\"");
				qb.append(SPARQLQueries.escape(lit.getLabel()));
				qb.append("\"");

				if (Literals.isLanguageLiteral(lit)) {
					qb.append("@");
					qb.append(lit.getLanguage().get());
				} else {
					qb.append("^^<" + lit.getDatatype().stringValue() + ">");
				}
				qb.append(" ");
			} else if (st.getObject() instanceof BNode) {
				qb.append("_:" + st.getObject().stringValue() + " ");
			} else {
				qb.append("<" + st.getObject().stringValue() + "> ");
			}
			qb.append(". \n");

			if (!ignoreContext && context != null) {
				qb.append("    }\n");
			}
		}
	}

	@Override
	public boolean isActive() throws UnknownTransactionStateException, RepositoryException {
		synchronized (transactionLock) {
			return sparqlTransaction != null;
		}
	}

	@Override
	protected void addWithoutCommit(Statement st, Resource... contexts)
			throws RepositoryException {
		flushPendingRemoves();
		if (pendingAdds.size() >= maxPendingSize) {
			flushPendingAdds();
		}
		if (contexts.length == 0) {
			pendingAdds.add(st);
		} else {
			pendingAdds.add(st.getSubject(), st.getPredicate(), st.getObject(), contexts);
		}
	}

	@Override
	protected void addWithoutCommit(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		flushPendingRemoves();
		if (pendingAdds.size() >= maxPendingSize) {
			flushPendingAdds();
		}
		pendingAdds.add(subject, predicate, object, contexts);
	}

	private void flushPendingRemoves() {
		if (!pendingRemoves.isEmpty()) {
			for (Resource context : pendingRemoves.contexts()) {
				String sparqlCommand = createDeleteDataCommand(pendingRemoves.getStatements(null, null, null, context),
						context);
				sparqlTransaction.append(sparqlCommand);
				sparqlTransaction.append("; ");
			}
			pendingRemoves = getModelFactory().createEmptyModel();
		}
	}

	private void flushPendingAdds() {
		if (!pendingAdds.isEmpty()) {
			for (Resource context : pendingAdds.contexts()) {
				String sparqlCommand = createInsertDataCommand(pendingAdds.getStatements(null, null, null, context),
						context);
				sparqlTransaction.append(sparqlCommand);
				sparqlTransaction.append("; ");
			}
			pendingAdds = getModelFactory().createEmptyModel();
		}
	}

	@Override
	protected void removeWithoutCommit(Statement st, Resource... contexts) throws RepositoryException {
		flushPendingAdds();
		if (pendingRemoves.size() >= maxPendingSize) {
			flushPendingRemoves();
		}

		if (contexts.length == 0) {
			pendingRemoves.add(st);
		} else {
			pendingRemoves.add(st.getSubject(), st.getPredicate(), st.getObject(), contexts);
		}
	}

	@Override
	protected void removeWithoutCommit(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		flushPendingAdds();
		if (pendingRemoves.size() >= maxPendingSize) {
			flushPendingRemoves();
		}

		if (subject != null && predicate != null && object != null) {
			pendingRemoves.add(subject, predicate, object, contexts);
		} else {
			flushPendingRemoves();
			String sparqlCommand = createDeletePatternCommand(subject, predicate, object, contexts);
			sparqlTransaction.append(sparqlCommand);
			sparqlTransaction.append("; ");
		}
	}

	private String createDeletePatternCommand(Resource subject, IRI predicate, Value object, Resource[] contexts) {
		StringBuilder qb = new StringBuilder();
		qb.append("DELETE WHERE \n");
		qb.append("{ \n");
		if (contexts.length > 0) {
			for (Resource context : contexts) {
				if (context != null) {
					String namedGraph = context.stringValue();
					if (context instanceof BNode) {
						// SPARQL does not allow blank nodes as named graph
						// identifiers, so we need to skolemize
						// the blank node id.
						namedGraph = "urn:nodeid:" + context.stringValue();
					}
					qb.append("    GRAPH <" + namedGraph + "> { \n");
				}
				createBGP(qb, subject, predicate, object);
				if (context != null && context instanceof IRI) {
					qb.append(" } \n");
				}
			}
		} else {
			createBGP(qb, subject, predicate, object);
		}
		qb.append("}");

		return qb.toString();
	}

	private void createBGP(StringBuilder qb, Resource subject, IRI predicate, Value object) {
		if (subject != null) {
			if (subject instanceof BNode) {
				qb.append("_:" + subject.stringValue() + " ");
			} else {
				qb.append("<" + subject.stringValue() + "> ");
			}
		} else {
			qb.append("?subj");
		}

		if (predicate != null) {
			qb.append("<" + predicate.stringValue() + "> ");
		} else {
			qb.append("?pred");
		}

		if (object != null) {
			if (object instanceof Literal) {
				Literal lit = (Literal) object;
				qb.append("\"");
				qb.append(SPARQLQueries.escape(lit.getLabel()));
				qb.append("\"");

				if (lit.getLanguage().isPresent()) {
					qb.append("@");
					qb.append(lit.getLanguage().get());
				} else {
					qb.append("^^<" + lit.getDatatype().stringValue() + ">");
				}
				qb.append(" ");
			} else if (object instanceof BNode) {
				qb.append("_:" + object.stringValue() + " ");
			} else {
				qb.append("<" + object.stringValue() + "> ");
			}
		} else {
			qb.append("?obj");
		}
		qb.append(". \n");
	}

	/**
	 * Shall graph information also be retrieved, e.g. for
	 * {@link #getStatements(Resource, IRI, Value, boolean, Resource...)}
	 *
	 * @return true if in quad mode
	 */
	protected boolean isQuadMode() {
		return quadMode;
	}

	protected boolean isSilentClear() {
		return silentClear;
	}

	/**
	 * Converts a {@link TupleQueryResult} resulting from the {@link #EVERYTHING_WITH_GRAPH} to a statement by using the
	 * respective values from the {@link BindingSet} or (if provided) the ones from the arguments.
	 *
	 * @param iter the {@link TupleQueryResult}
	 * @param subj the subject {@link Resource} used as input or <code>null</code> if wildcard was used
	 * @param pred the predicate {@link IRI} used as input or <code>null</code> if wildcard was used
	 * @param obj  the object {@link Value} used as input or <code>null</code> if wildcard was used
	 * @return the converted iteration
	 */
	@Deprecated(since = "4.1.0", forRemoval = true)
	protected Iteration<Statement, QueryEvaluationException> toStatementIteration(TupleQueryResult iter,
			final Resource subj, final IRI pred, final Value obj) {

		return new ConvertingIteration<>(iter) {

			@Override
			protected Statement convert(BindingSet b) throws QueryEvaluationException {

				Resource s = subj == null ? (Resource) b.getValue("s") : subj;
				IRI p = pred == null ? (IRI) b.getValue("p") : pred;
				Value o = obj == null ? b.getValue("o") : obj;
				Resource ctx = (Resource) b.getValue("ctx");

				return SimpleValueFactory.getInstance().createStatement(s, p, o, ctx);
			}

		};
	}

	private ModelFactory getModelFactory() {
		return modelFactory;
	}

}
