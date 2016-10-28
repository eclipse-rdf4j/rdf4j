/**
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.http.server.repository.transaction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;

/**
 * A transaction encapsulates a single {@link Thread} and a {@link RepositoryConnection}, to enable executing
 * all operations that are part of the transaction from a single, dedicated thread. This is necessary because
 * {@link RepositoryConnection} is not guaranteed thread-safe and we may run into concurrency issues if we
 * attempt to share it between the various HTTP Request worker threads.
 * 
 * @author Jeen Broekstra
 */
class Transaction {

	private final UUID id;

	private final Repository rep;

	private final RepositoryConnection txnConnection;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final List<Future<?>> futures = new ArrayList<>();

	/**
	 * Create a new Transaction for the given {@link Repository}.
	 * 
	 * @param repository
	 *        the {@link Repository} on which to open a transaction.
	 * @throws InterruptedException
	 *         if the transaction thread is interrupted while opening a connection.
	 * @throws ExecutionException
	 *         if an error occurs while opening the connection.
	 */
	Transaction(Repository repository)
		throws InterruptedException, ExecutionException
	{
		this.id = UUID.randomUUID();
		this.rep = repository;
		this.txnConnection = getTransactionConnection();
	}

	/**
	 * The identifier of this transaction object.
	 * 
	 * @return a {@link UUID} that identifies this Transaction.
	 */
	UUID getID() {
		return id;
	}



	/**
	 * Start the transaction.
	 * 
	 * @param level
	 *        the {@link IsolationLevel} to use for this transction.
	 * @throws InterruptedException
	 *         if the transaction thread is interrupted
	 * @throws ExecutionException
	 *         if an error occurs while starting the transaction.
	 */
	void begin(IsolationLevel level)
		throws InterruptedException, ExecutionException
	{
		Future<Boolean> result = executor.submit(() -> {
			txnConnection.begin(level);
			return true;
		});

		futures.add(result);
		result.get();
	}

	/**
	 * Rolls back all updates in the transaction.
	 * 
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	void rollback()
		throws InterruptedException, ExecutionException
	{
		Future<Boolean> result = executor.submit(() -> {
			txnConnection.rollback();
			return true;
		});
	
		futures.add(result);
		result.get();
	}

	/**
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	void commit()
		throws InterruptedException, ExecutionException
	{
		Future<Boolean> result = executor.submit(() -> {
			txnConnection.commit();
			return true;
		});
	
		futures.add(result);
	
		result.get();
	}

	/**
	 * Prepares a query for evaluation on this transaction.
	 * 
	 * @param ql
	 *        The {@link QueryLanguage query language} in which the query is formulated.
	 * @param query
	 *        The query string.
	 * @param baseURI
	 *        The base URI to resolve any relative URIs that are in the query against, can be <tt>null</tt> if
	 *        the query does not contain any relative URIs.
	 * @return A query ready to be evaluated on this repository.
	 * @throws InterruptedException
	 *         if the transaction thread is interrupted
	 * @throws ExecutionException
	 *         if an error occurs while executing the operation.
	 */
	Query prepareQuery(QueryLanguage queryLn, String queryStr, String baseURI)
		throws InterruptedException, ExecutionException
	{
		Future<Query> result = executor.submit(() -> txnConnection.prepareQuery(queryLn, queryStr, baseURI));

		futures.add(result);
		return result.get();
	}

	/**
	 * Evaluate a TupleQuery in this transaction and return the result.
	 * 
	 * @param tQuery
	 *        a {@link TupleQuery} prepared on this transaction.
	 * @return a {@link TupleQueryResult}
	 * @throws InterruptedException
	 *         if the transaction thread is interrupted
	 * @throws ExecutionException
	 *         if an error occurs while executing the operation.
	 */
	TupleQueryResult evaluate(TupleQuery tQuery)
		throws InterruptedException, ExecutionException
	{
		Future<TupleQueryResult> result = executor.submit(() -> tQuery.evaluate());
		futures.add(result);
		return result.get();
	}

	/**
	 * Evaluate a {@link GraphQuery} in this transaction and return the result.
	 * 
	 * @param gQuery
	 *        a {@link GraphQuery} prepared on this transaction.
	 * @return a {@link GraphQueryResult}
	 * @throws InterruptedException
	 *         if the transaction thread is interrupted
	 * @throws ExecutionException
	 *         if an error occurs while executing the operation.
	 */
	GraphQueryResult evaluate(GraphQuery gQuery)
		throws InterruptedException, ExecutionException
	{
		Future<GraphQueryResult> result = executor.submit(() -> gQuery.evaluate());
		futures.add(result);
		return result.get();
	}

	/**
	 * Evaluate a {@link BooleanQuery} in this transaction and return the result.
	 * 
	 * @param bQuery
	 *        a {@link BooleanQuery} prepared on this transaction.
	 * @return the query result as a boolean
	 * @throws InterruptedException
	 *         if the transaction thread is interrupted
	 * @throws ExecutionException
	 *         if an error occurs while executing the operation.
	 */
	boolean evaluate(BooleanQuery bQuery)
		throws InterruptedException, ExecutionException
	{
		Future<Boolean> result = executor.submit(() -> bQuery.evaluate());
		futures.add(result);
		return result.get();
	}

	/**
	 * @param subj
	 * @param pred
	 * @param obj
	 * @param useInferencing
	 * @param rdfWriter
	 * @param contexts
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	void exportStatements(Resource subj, IRI pred, Value obj, boolean useInferencing,
			RDFWriter rdfWriter, Resource... contexts)
		throws InterruptedException, ExecutionException
	{
		Future<Boolean> result = executor.submit(() -> {
			txnConnection.exportStatements(subj, pred, obj, useInferencing, rdfWriter, contexts);
			return true;
		});
	
		futures.add(result);
		result.get();
	}

	/**
	 * Returns the number of (explicit) statements that are in the specified contexts in this transaction.
	 * 
	 * @param contexts
	 *        The context(s) to get the data from. Note that this parameter is a vararg and as such is
	 *        optional. If no contexts are supplied the method operates on the entire repository.
	 * @return The number of explicit statements from the specified contexts in this transaction.
	 */
	long getSize(Resource[] contexts)
		throws InterruptedException, ExecutionException
	{
		Future<Long> result = executor.submit(() -> txnConnection.size(contexts));
		futures.add(result);
		return result.get();
	}

	/**
	 * Adds RDF data from an {@link InputStream} to the transaction.
	 * 
	 * @param inputStream
	 * @param baseURI
	 * @param format
	 * @param contexts
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	void add(InputStream inputStream, String baseURI, RDFFormat format, boolean preserveBNodes,
			Resource... contexts)
		throws InterruptedException, ExecutionException
	{
		Future<Boolean> result = executor.submit(() -> {
			try {
				if (preserveBNodes) {
					// create a reconfigured parser + inserter instead of relying on standard
					// repositoryconn add method.
					RDFParser parser = Rio.createParser(format);
					parser.getParserConfig().set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
					RDFInserter inserter = new RDFInserter(txnConnection);
					inserter.setPreserveBNodeIDs(true);
					if (contexts.length > 0) {
						inserter.enforceContext(contexts);
					}
					parser.setRDFHandler(inserter);
					parser.parse(inputStream, baseURI);
				}
				else {
					txnConnection.add(inputStream, baseURI, format, contexts);
				}
				return true;
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		futures.add(result);
		result.get();
	}

	/**
	 * @param contentType
	 * @param inputStream
	 * @param baseURI
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	void delete(RDFFormat contentType, InputStream inputStream, String baseURI)
		throws InterruptedException, ExecutionException
	{
		Future<Boolean> exception = executor.submit(() -> {
			RDFParser parser = Rio.createParser(contentType, txnConnection.getValueFactory());

			parser.setRDFHandler(new WildcardRDFRemover(txnConnection));
			parser.getParserConfig().set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
			try {
				parser.parse(inputStream, baseURI);
				return true;
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		futures.add(exception);

		exception.get();
	}

	/**
	 * @param queryLn
	 * @param sparqlUpdateString
	 * @param baseURI
	 * @param includeInferred
	 * @param dataset
	 * @param bindings
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	void executeUpdate(QueryLanguage queryLn, String sparqlUpdateString, String baseURI,
			boolean includeInferred, Dataset dataset, Map<String, Value> bindings)
		throws InterruptedException, ExecutionException
	{
		Future<Boolean> result = executor.submit(() -> {
			Update update = txnConnection.prepareUpdate(queryLn, sparqlUpdateString);
			update.setIncludeInferred(includeInferred);
			if (dataset != null) {
				update.setDataset(dataset);
			}
			for (String bindingName : bindings.keySet()) {
				update.setBinding(bindingName, bindings.get(bindingName));
			}

			update.execute();
			return true;
		});

		futures.add(result);
		result.get();
	}

	boolean hasActiveOperations() {
		for (Future future : futures) {
			if (!future.isDone()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Close this transaction.
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	void close()
		throws InterruptedException, ExecutionException
	{
		try {
			Future<Boolean> result = executor.submit(() -> {
				txnConnection.close();
				return true;
			});
	
			futures.add(result);
			result.get();
		}
		finally {
			executor.shutdown();
		}
	}

	private RepositoryConnection getTransactionConnection()
			throws InterruptedException, ExecutionException
		{
			// create a new RepositoryConnection with correct parser settings
			Future<RepositoryConnection> future = executor.submit(() -> {
				RepositoryConnection conn = rep.getConnection();
				ParserConfig config = conn.getParserConfig();
				config.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
				config.addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
				config.addNonFatalError(BasicParserSettings.VERIFY_LANGUAGE_TAGS);

				return conn;
			});

			futures.add(future);
			return future.get();
		}

	private static class WildcardRDFRemover extends AbstractRDFHandler {
	
		private final RepositoryConnection conn;
	
		public WildcardRDFRemover(RepositoryConnection conn) {
			super();
			this.conn = conn;
		}
	
		@Override
		public void handleStatement(Statement st)
			throws RDFHandlerException
		{
			Resource subject = SESAME.WILDCARD.equals(st.getSubject()) ? null : st.getSubject();
			IRI predicate = SESAME.WILDCARD.equals(st.getPredicate()) ? null : st.getPredicate();
			Value object = SESAME.WILDCARD.equals(st.getObject()) ? null : st.getObject();
	
			// use the RepositoryConnection.clear operation if we're removing all statements
			final boolean clearAllTriples = subject == null && predicate == null && object == null;
	
			try {
				Resource context = st.getContext();
				if (context != null) {
					if (clearAllTriples) {
						conn.clear(context);
					}
					else {
						conn.remove(subject, predicate, object, context);
					}
				}
				else {
					if (clearAllTriples) {
						conn.clear();
					}
					else {
						conn.remove(subject, predicate, object);
					}
				}
			}
			catch (RepositoryException e) {
				throw new RDFHandlerException(e);
			}
		}
	
	}
}
