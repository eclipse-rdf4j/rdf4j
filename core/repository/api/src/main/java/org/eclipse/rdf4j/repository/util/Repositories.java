/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.UnknownTransactionStateException;
import org.eclipse.rdf4j.rio.RDFHandler;

/**
 * Utility for dealing with {@link Repository} and {@link RepositoryConnection} objects.
 *
 * @author Peter Ansell
 */
public final class Repositories {

	/**
	 * Opens a {@link RepositoryConnection} to the given Repository within a transaction, sends the connection to the
	 * given {@link Consumer}, before either rolling back the transaction if it failed, or committing the transaction if
	 * it was successful.
	 *
	 * @param repository      The {@link Repository} to open a connection to.
	 * @param processFunction A {@link Consumer} that performs an action on the connection.
	 * @throws RepositoryException              If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException If the transaction state was not properly recognised. (Optional specific
	 *                                          exception)
	 */
	public static void consume(Repository repository, Consumer<RepositoryConnection> processFunction)
			throws RepositoryException, UnknownTransactionStateException {
		get(repository, conn -> {
			processFunction.accept(conn);
			return null;
		});
	}

	/**
	 * Opens a {@link RepositoryConnection} to the given Repository without opening a transaction, sends the connection
	 * to the given {@link Consumer}.
	 *
	 * @param repository      The {@link Repository} to open a connection to.
	 * @param processFunction A {@link Consumer} that performs an action on the connection.
	 * @throws RepositoryException              If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException If the transaction state was not properly recognised. (Optional specific
	 *                                          exception)
	 */
	public static void consumeNoTransaction(Repository repository, Consumer<RepositoryConnection> processFunction)
			throws RepositoryException, UnknownTransactionStateException {
		getNoTransaction(repository, conn -> {
			processFunction.accept(conn);
			return null;
		});
	}

	/**
	 * Opens a {@link RepositoryConnection} to the given Repository within a transaction, sends the connection to the
	 * given {@link Consumer}, before either rolling back the transaction if it failed, or committing the transaction if
	 * it was successful.
	 *
	 * @param repository       The {@link Repository} to open a connection to.
	 * @param processFunction  A {@link Consumer} that performs an action on the connection.
	 * @param exceptionHandler A {@link Consumer} that handles an exception if one was generated.
	 * @throws RepositoryException              If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException If the transaction state was not properly recognised. (Optional specific
	 *                                          exception)
	 */
	public static void consume(Repository repository, Consumer<RepositoryConnection> processFunction,
			Consumer<RepositoryException> exceptionHandler)
			throws RepositoryException, UnknownTransactionStateException {
		try {
			consume(repository, processFunction);
		} catch (RepositoryException e) {
			exceptionHandler.accept(e);
		}
	}

	/**
	 * Opens a {@link RepositoryConnection} to the given Repository without opening a transaction, sends the connection
	 * to the given {@link Consumer}.
	 *
	 * @param repository       The {@link Repository} to open a connection to.
	 * @param processFunction  A {@link Consumer} that performs an action on the connection.
	 * @param exceptionHandler A {@link Consumer} that handles an exception if one was generated.
	 * @throws RepositoryException              If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException If the transaction state was not properly recognised. (Optional specific
	 *                                          exception)
	 */
	public static void consumeNoTransaction(Repository repository, Consumer<RepositoryConnection> processFunction,
			Consumer<RepositoryException> exceptionHandler)
			throws RepositoryException, UnknownTransactionStateException {
		try {
			consumeNoTransaction(repository, processFunction);
		} catch (RepositoryException e) {
			exceptionHandler.accept(e);
		}
	}

	/**
	 * Opens a {@link RepositoryConnection} to the given Repository within a transaction, sends the connection to the
	 * given {@link Consumer}, before either rolling back the transaction if it failed, or committing the transaction if
	 * it was successful.
	 *
	 * @param repository      The {@link Repository} to open a connection to.
	 * @param processFunction A {@link Consumer} that performs an action on the connection.
	 */
	public static void consumeSilent(Repository repository, Consumer<RepositoryConnection> processFunction) {
		consume(repository, processFunction, e -> {
		});
	}

	/**
	 * Opens a {@link RepositoryConnection} to the given Repository without opening a transaction, sends the connection
	 * to the given {@link Consumer}.
	 *
	 * @param repository      The {@link Repository} to open a connection to.
	 * @param processFunction A {@link Consumer} that performs an action on the connection.
	 */
	public static void consumeSilentNoTransaction(Repository repository,
			Consumer<RepositoryConnection> processFunction) {
		consumeNoTransaction(repository, processFunction, e -> {
		});
	}

	/**
	 * Opens a {@link RepositoryConnection} to the given Repository within a transaction, sends the connection to the
	 * given {@link Function}, before either rolling back the transaction if it failed, or committing the transaction if
	 * it was successful.
	 *
	 * @param <T>             The type of the return value.
	 * @param repository      The {@link Repository} to open a connection to.
	 * @param processFunction A {@link Function} that performs an action on the connection and returns a result.
	 * @return The result of applying the function.
	 * @throws RepositoryException              If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException If the transaction state was not properly recognised. (Optional specific
	 *                                          exception)
	 */
	public static <T> T get(Repository repository, Function<RepositoryConnection, T> processFunction)
			throws RepositoryException, UnknownTransactionStateException {
		RepositoryConnection conn = null;

		try {
			conn = repository.getConnection();
			conn.begin();
			T result = processFunction.apply(conn);
			conn.commit();
			return result;
		} catch (RepositoryException e) {
			if (conn != null && conn.isActive()) {
				conn.rollback();
			}
			throw e;
		} finally {
			if (conn != null && conn.isOpen()) {
				conn.close();
			}
		}
	}

	/**
	 * Opens a {@link RepositoryConnection} to the given Repository without opening a transaction, sends the connection
	 * to the given {@link Function}.
	 *
	 * @param <T>             The type of the return value.
	 * @param repository      The {@link Repository} to open a connection to.
	 * @param processFunction A {@link Function} that performs an action on the connection and returns a result.
	 * @return The result of applying the function.
	 * @throws RepositoryException              If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException If the transaction state was not properly recognised. (Optional specific
	 *                                          exception)
	 */
	public static <T> T getNoTransaction(Repository repository, Function<RepositoryConnection, T> processFunction)
			throws RepositoryException, UnknownTransactionStateException {
		RepositoryConnection conn = null;

		try {
			conn = repository.getConnection();
			T result = processFunction.apply(conn);
			return result;
		} finally {
			if (conn != null && conn.isOpen()) {
				conn.close();
			}
		}
	}

	/**
	 * Opens a {@link RepositoryConnection} to the given Repository within a transaction, sends the connection to the
	 * given {@link Function}, before either rolling back the transaction if it failed, or committing the transaction if
	 * it was successful.
	 *
	 * @param <T>              The type of the return value.
	 * @param repository       The {@link Repository} to open a connection to.
	 * @param processFunction  A {@link Function} that performs an action on the connection and returns a result.
	 * @param exceptionHandler A {@link Consumer} that handles an exception if one was generated.
	 * @return The result of applying the function, or <var>null</var> if an exception occurs and the exception handler
	 *         does not rethrow the exception.
	 * @throws RepositoryException              If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException If the transaction state was not properly recognised. (Optional specific
	 *                                          exception)
	 */
	public static <T> T get(Repository repository, Function<RepositoryConnection, T> processFunction,
			Consumer<RepositoryException> exceptionHandler)
			throws RepositoryException, UnknownTransactionStateException {
		try {
			return get(repository, processFunction);
		} catch (RepositoryException e) {
			exceptionHandler.accept(e);
			return null;
		}
	}

	/**
	 * Opens a {@link RepositoryConnection} to the given Repository without opening a transaction, sends the connection
	 * to the given {@link Function}.
	 *
	 * @param <T>              The type of the return value.
	 * @param repository       The {@link Repository} to open a connection to.
	 * @param processFunction  A {@link Function} that performs an action on the connection and returns a result.
	 * @param exceptionHandler A {@link Consumer} that handles an exception if one was generated.
	 * @return The result of applying the function, or <var>null</var> if an exception occurs and the exception handler
	 *         does not rethrow the exception.
	 * @throws RepositoryException              If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException If the transaction state was not properly recognised. (Optional specific
	 *                                          exception)
	 */
	public static <T> T getNoTransaction(Repository repository, Function<RepositoryConnection, T> processFunction,
			Consumer<RepositoryException> exceptionHandler)
			throws RepositoryException, UnknownTransactionStateException {
		try {
			return getNoTransaction(repository, processFunction);
		} catch (RepositoryException e) {
			exceptionHandler.accept(e);
			return null;
		}
	}

	/**
	 * Opens a {@link RepositoryConnection} to the given Repository within a transaction, sends the connection to the
	 * given {@link Function}, before either rolling back the transaction if it failed, or committing the transaction if
	 * it was successful.
	 *
	 * @param <T>             The type of the return value.
	 * @param repository      The {@link Repository} to open a connection to.
	 * @param processFunction A {@link Function} that performs an action on the connection and returns a result.
	 * @return The result of applying the function, or <var>null</var> if an exception is thrown.
	 */
	public static <T> T getSilent(Repository repository, Function<RepositoryConnection, T> processFunction) {
		return get(repository, processFunction, e -> {
		});
	}

	/**
	 * Opens a {@link RepositoryConnection} to the given Repository without opening a transaction, sends the connection
	 * to the given {@link Function}.
	 *
	 * @param <T>             The type of the return value.
	 * @param repository      The {@link Repository} to open a connection to.
	 * @param processFunction A {@link Function} that performs an action on the connection and returns a result.
	 * @return The result of applying the function, or <var>null</var> if an exception is thrown.
	 */
	public static <T> T getSilentNoTransaction(Repository repository,
			Function<RepositoryConnection, T> processFunction) {
		return getNoTransaction(repository, processFunction, e -> {
		});
	}

	/**
	 * Performs a SPARQL Select query on the given Repository within a transaction and passes the results to the given
	 * {@link Function} with the result from the function returned by the method.
	 *
	 * @param <T>             The type of the return value.
	 * @param repository      The {@link Repository} to open a connection to.
	 * @param query           The SPARQL Select query to execute.
	 * @param processFunction A {@link Function} that performs an action on the results of the query and returns a
	 *                        result.
	 * @return The result of processing the query results.
	 * @throws RepositoryException              If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException If the transaction state was not properly recognised. (Optional specific
	 *                                          exception)
	 * @throws MalformedQueryException          If the supplied query is malformed
	 * @throws QueryEvaluationException         If there was an error evaluating the query
	 */
	public static <T> T tupleQuery(Repository repository, String query, Function<TupleQueryResult, T> processFunction)
			throws RepositoryException, UnknownTransactionStateException, MalformedQueryException,
			QueryEvaluationException {
		return get(repository, conn -> {
			TupleQuery preparedQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
			try (TupleQueryResult queryResult = preparedQuery.evaluate()) {
				return processFunction.apply(queryResult);
			}
		});
	}

	/**
	 * Performs a SPARQL Select query on the given Repository without opening a transaction and passes the results to
	 * the given {@link Function} with the result from the function returned by the method.
	 *
	 * @param <T>             The type of the return value.
	 * @param repository      The {@link Repository} to open a connection to.
	 * @param query           The SPARQL Select query to execute.
	 * @param processFunction A {@link Function} that performs an action on the results of the query and returns a
	 *                        result.
	 * @return The result of processing the query results.
	 * @throws RepositoryException              If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException If the transaction state was not properly recognised. (Optional specific
	 *                                          exception)
	 * @throws MalformedQueryException          If the supplied query is malformed
	 * @throws QueryEvaluationException         If there was an error evaluating the query
	 */
	public static <T> T tupleQueryNoTransaction(Repository repository, String query,
			Function<TupleQueryResult, T> processFunction) throws RepositoryException, UnknownTransactionStateException,
			MalformedQueryException, QueryEvaluationException {
		return getNoTransaction(repository, conn -> {
			TupleQuery preparedQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
			try (TupleQueryResult queryResult = preparedQuery.evaluate()) {
				return processFunction.apply(queryResult);
			}
		});
	}

	/**
	 * Performs a SPARQL Select query on the given Repository within a transaction and passes the results to the given
	 * {@link TupleQueryResultHandler}.
	 *
	 * @param repository The {@link Repository} to open a connection to.
	 * @param query      The SPARQL Select query to execute.
	 * @param handler    A {@link TupleQueryResultHandler} that consumes the results.
	 * @throws RepositoryException              If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException If the transaction state was not properly recognised. (Optional specific
	 *                                          exception)
	 * @throws MalformedQueryException          If the supplied query is malformed
	 * @throws QueryEvaluationException         If there was an error evaluating the query
	 */
	public static void tupleQuery(Repository repository, String query, TupleQueryResultHandler handler)
			throws RepositoryException, UnknownTransactionStateException, MalformedQueryException,
			QueryEvaluationException {
		consume(repository, conn -> {
			TupleQuery preparedQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
			preparedQuery.evaluate(handler);
		});
	}

	/**
	 * Performs a SPARQL Select query on the given Repository without opening a transaction and passes the results to
	 * the given {@link TupleQueryResultHandler}.
	 *
	 * @param repository The {@link Repository} to open a connection to.
	 * @param query      The SPARQL Select query to execute.
	 * @param handler    A {@link TupleQueryResultHandler} that consumes the results.
	 * @throws RepositoryException              If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException If the transaction state was not properly recognised. (Optional specific
	 *                                          exception)
	 * @throws MalformedQueryException          If the supplied query is malformed
	 * @throws QueryEvaluationException         If there was an error evaluating the query
	 */
	public static void tupleQueryNoTransaction(Repository repository, String query, TupleQueryResultHandler handler)
			throws RepositoryException, UnknownTransactionStateException, MalformedQueryException,
			QueryEvaluationException {
		consumeNoTransaction(repository, conn -> {
			TupleQuery preparedQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
			preparedQuery.evaluate(handler);
		});
	}

	/**
	 * Performs a SPARQL Construct or Describe query on the given Repository within a transaction and passes the results
	 * to the given {@link Function} with the result from the function returned by the method.
	 *
	 * @param <T>             The type of the return value.
	 * @param repository      The {@link Repository} to open a connection to.
	 * @param query           The SPARQL Construct or Describe query to execute.
	 * @param processFunction A {@link Function} that performs an action on the results of the query and returns a
	 *                        result.
	 * @return The result of processing the query results.
	 * @throws RepositoryException              If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException If the transaction state was not properly recognised. (Optional specific
	 *                                          exception)
	 * @throws MalformedQueryException          If the supplied query is malformed
	 * @throws QueryEvaluationException         If there was an error evaluating the query
	 */
	public static <T> T graphQuery(Repository repository, String query, Function<GraphQueryResult, T> processFunction)
			throws RepositoryException, UnknownTransactionStateException, MalformedQueryException,
			QueryEvaluationException {
		return get(repository, conn -> {
			GraphQuery preparedQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);
			try (GraphQueryResult queryResult = preparedQuery.evaluate()) {
				return processFunction.apply(queryResult);
			}
		});
	}

	/**
	 * Performs a SPARQL Construct or Describe query on the given Repository without opening a transaction and passes
	 * the results to the given {@link Function} with the result from the function returned by the method.
	 *
	 * @param <T>             The type of the return value.
	 * @param repository      The {@link Repository} to open a connection to.
	 * @param query           The SPARQL Construct or Describe query to execute.
	 * @param processFunction A {@link Function} that performs an action on the results of the query and returns a
	 *                        result.
	 * @return The result of processing the query results.
	 * @throws RepositoryException              If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException If the transaction state was not properly recognised. (Optional specific
	 *                                          exception)
	 * @throws MalformedQueryException          If the supplied query is malformed
	 * @throws QueryEvaluationException         If there was an error evaluating the query
	 */
	public static <T> T graphQueryNoTransaction(Repository repository, String query,
			Function<GraphQueryResult, T> processFunction) throws RepositoryException, UnknownTransactionStateException,
			MalformedQueryException, QueryEvaluationException {
		return getNoTransaction(repository, conn -> {
			GraphQuery preparedQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);
			try (GraphQueryResult queryResult = preparedQuery.evaluate()) {
				return processFunction.apply(queryResult);
			}
		});
	}

	/**
	 * Performs a SPARQL Construct or Describe query on the given Repository within a transaction and passes the results
	 * to the given {@link RDFHandler}.
	 *
	 * @param repository The {@link Repository} to open a connection to.
	 * @param query      The SPARQL Construct or Describe query to execute.
	 * @param handler    An {@link RDFHandler} that consumes the results.
	 * @throws RepositoryException              If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException If the transaction state was not properly recognised. (Optional specific
	 *                                          exception)
	 * @throws MalformedQueryException          If the supplied query is malformed
	 * @throws QueryEvaluationException         If there was an error evaluating the query
	 */
	public static void graphQuery(Repository repository, String query, RDFHandler handler) throws RepositoryException,
			UnknownTransactionStateException, MalformedQueryException, QueryEvaluationException {
		consume(repository, conn -> {
			GraphQuery preparedQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);
			preparedQuery.evaluate(handler);
		});
	}

	/**
	 * Performs a SPARQL Construct or Describe query on the given Repository without opening a transaction and passes
	 * the results to the given {@link RDFHandler}.
	 *
	 * @param repository The {@link Repository} to open a connection to.
	 * @param query      The SPARQL Construct or Describe query to execute.
	 * @param handler    An {@link RDFHandler} that consumes the results.
	 * @throws RepositoryException              If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException If the transaction state was not properly recognised. (Optional specific
	 *                                          exception)
	 * @throws MalformedQueryException          If the supplied query is malformed
	 * @throws QueryEvaluationException         If there was an error evaluating the query
	 */
	public static void graphQueryNoTransaction(Repository repository, String query, RDFHandler handler)
			throws RepositoryException, UnknownTransactionStateException, MalformedQueryException,
			QueryEvaluationException {
		consumeNoTransaction(repository, conn -> {
			GraphQuery preparedQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);
			preparedQuery.evaluate(handler);
		});
	}

	/**
	 * Creates a {@link Supplier} of {@link RepositoryException} objects that be passed to
	 * {@link Optional#orElseThrow(Supplier)} to generate exceptions as necessary.
	 *
	 * @param message The message to be used for the exception
	 * @return A {@link Supplier} that will create {@link RepositoryException} objects with the given message.
	 */
	public static Supplier<RepositoryException> repositoryException(String message) {
		return () -> new RepositoryException(message);
	}

	/**
	 * Private constructor to prevent instantiation, this is a static helper class.
	 */
	private Repositories() {
	}

}
