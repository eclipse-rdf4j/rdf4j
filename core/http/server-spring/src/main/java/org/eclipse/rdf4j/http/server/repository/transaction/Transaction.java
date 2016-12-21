/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository.transaction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * A transaction encapsulates a single {@link Thread} and a {@link RepositoryConnection}, to enable executing
 * all operations that are part of the transaction from a single, dedicated thread. This is necessary because
 * {@link RepositoryConnection} is not guaranteed thread-safe and we may run into concurrency issues if we
 * attempt to share it between the various HTTP Request worker threads.
 * 
 * @author Jeen Broekstra
 */
class Transaction implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(Transaction.class);

	/**
	 * The period during the {@link #close()} method between waiting for
	 * {@link ExecutorService#awaitTermination(long, TimeUnit)}, logging that the transaction has not
	 * completed, and trying again up to {@link #MAX_CLOSE_WAIT_PERIODS} attempts.
	 */
	private static final int EXECUTOR_SHUTDOWN_WAIT_PERIOD = 10000;

	/**
	 * The maximum number of times during the {@link #close()} method that
	 * {@link ExecutorService#awaitTermination(long, TimeUnit)} will be called before giving up and calling
	 * {@link ExecutorService#shutdownNow()}.
	 */
	private static final int MAX_CLOSE_WAIT_PERIODS = 3;

	/**
	 * Set to true when entering the {@link #close()} method for the first time, to ensure that only a single
	 * thread executes the close operations.
	 */
	private final AtomicBoolean isClosed = new AtomicBoolean(false);

	/**
	 * Set to true when the {@link #close()} method is about to complete for the first invocation.
	 */
	private final AtomicBoolean closeCompleted = new AtomicBoolean(false);

	private final UUID id;

	private final Repository rep;

	private final RepositoryConnection txnConnection;

	/**
	 * The {@link ExecutorService} that performs all of the operations related to this Transaction.
	 */
	private final ExecutorService executor = Executors.newSingleThreadExecutor(
			new ThreadFactoryBuilder().setNameFormat("rdf4j-transaction-%d").build());

	/**
	 * A {@link List} that tracks the state of the futures involved in this Transaction. The front of the list
	 * is cleaned of done operations during {@link #hasActiveOperations()} to release references to results.
	 */
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
		Future<Boolean> result = submit(() -> {
			txnConnection.begin(level);
			return true;
		});
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
		Future<Boolean> result = submit(() -> {
			txnConnection.rollback();
			return true;
		});
		result.get();
	}

	/**
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	void commit()
		throws InterruptedException, ExecutionException
	{
		Future<Boolean> result = submit(() -> {
			txnConnection.commit();
			return true;
		});
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
		Future<Query> result = submit(() -> txnConnection.prepareQuery(queryLn, queryStr, baseURI));
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
		Future<TupleQueryResult> result = submit(() -> tQuery.evaluate());
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
		Future<GraphQueryResult> result = submit(() -> gQuery.evaluate());
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
		Future<Boolean> result = submit(() -> bQuery.evaluate());
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
	void exportStatements(Resource subj, IRI pred, Value obj, boolean useInferencing, RDFWriter rdfWriter,
			Resource... contexts)
		throws InterruptedException, ExecutionException
	{
		Future<Boolean> result = submit(() -> {
			txnConnection.exportStatements(subj, pred, obj, useInferencing, rdfWriter, contexts);
			return true;
		});
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
		Future<Long> result = submit(() -> txnConnection.size(contexts));
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
		Future<Boolean> result = submit(() -> {
			logger.debug("executing add operation");
			try {
				if (preserveBNodes) {
					// create a reconfigured parser + inserter instead of
					// relying on standard
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
		Future<Boolean> result = submit(() -> {
			logger.debug("executing delete operation");
			RDFParser parser = Rio.createParser(contentType, txnConnection.getValueFactory());

			parser.setRDFHandler(new WildcardRDFRemover(txnConnection));
			parser.getParserConfig().set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
			try {
				parser.parse(inputStream, baseURI);
				return true;
			}
			catch (IOException e) {
				logger.error("error during txn delete operation", e);
				throw new RuntimeException(e);
			}
		});
		result.get();
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
		Future<Boolean> result = submit(() -> {
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
		result.get();
	}

	/**
	 * Checks if the user has any scheduled tasks for this transaction that have not yet completed.
	 * 
	 * @return True if there are currently no active tasks being executed for this transaction and false
	 *         otherwise.
	 */
	boolean hasActiveOperations() {
		// Synchronize on the futures set to ensure that new executions are blocked from submitting once we get this monitor
		synchronized (futures) {
			Iterator<Future<?>> iterator = futures.iterator();
			while (iterator.hasNext()) {
				Future<?> future = iterator.next();
				if (!future.isDone()) {
					// Execution is in serial, so everything after this item is assumed not to be able to be cleaned up
					return true;
				}
				else {
					// Clean up this item to remove the reference to the item, while we have a lock on futures
					iterator.remove();
				}
			}
			return false;
		}
	}

	/**
	 * Checks if close has been called for this transaction.
	 * 
	 * @return True if the close method has been called for this transaction.
	 */
	boolean isClosed() {
		return isClosed.get();
	}

	/**
	 * Checks if close has been completed for this transaction.
	 * 
	 * @return True if the close operations have been completed.
	 */
	boolean isComplete() {
		return closeCompleted.get();
	}

	/**
	 * Close this transaction.
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@Override
	public void close()
		throws InterruptedException, ExecutionException
	{
		if (isClosed.compareAndSet(false, true)) {
			try {
				Future<Boolean> result = submit(() -> {
					txnConnection.close();
					return true;
				});
				// Stop new tasks being submitted to the executor from now
				executor.shutdown();
				// We want to allow time for at least the close to complete, plus awaitTermination below if necessary for other tasks
				result.get();
			}
			finally {
				try {
					int retryCount = 1;
					while (!executor.awaitTermination(EXECUTOR_SHUTDOWN_WAIT_PERIOD, TimeUnit.MILLISECONDS)
							|| retryCount > MAX_CLOSE_WAIT_PERIODS)
					{
						logger.warn("Transaction was not complete in {} seconds after close called",
								TimeUnit.SECONDS.convert(retryCount * EXECUTOR_SHUTDOWN_WAIT_PERIOD,
										TimeUnit.MILLISECONDS));
						retryCount++;
					}
				}
				finally {
					try {
						if (!executor.isTerminated()) {
							executor.shutdownNow();
						}
					}
					finally {
						closeCompleted.set(true);
					}
				}
			}
		}
	}

	/**
	 * Obtains a {@link RepositoryConnection} through the {@link ExecutorService}.
	 * 
	 * @return A new {@link RepositoryConnection} to use for this Transaction.
	 * @throws InterruptedException
	 *         If the execution of the task was interrupted.
	 * @throws ExecutionException
	 *         If the execution of the task failed for any reason.
	 */
	private RepositoryConnection getTransactionConnection()
		throws InterruptedException, ExecutionException
	{
		// create a new RepositoryConnection with correct parser settings
		Future<RepositoryConnection> future = submit(() -> {
			RepositoryConnection conn = rep.getConnection();
			ParserConfig config = conn.getParserConfig();
			config.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
			config.addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
			config.addNonFatalError(BasicParserSettings.VERIFY_LANGUAGE_TAGS);

			return conn;
		});
		return future.get();
	}

	/**
	 * Atomically submit the task to the executor and add to our local list used to track whether there are
	 * outstanding operations for the executor.
	 * 
	 * @param callable
	 *        The task to submit
	 * @return A {@link Future} that can be used to track whether the operation has succeeded and get the
	 *         result.
	 */
	private <T> Future<T> submit(final Callable<T> callable) {
		// Synchronize around futures before submitting task so hasActiveOperations gets an instantaneous snapshot of the executor from futures
		synchronized (futures) {
			final Future<T> result = executor.submit(callable);
			futures.add(result);
			return result;
		}
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

			// use the RepositoryConnection.clear operation if we're removing
			// all statements
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
