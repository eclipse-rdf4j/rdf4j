/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.base;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.UnknownTransactionStateException;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.repository.util.RDFLoader;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class implementing most 'convenience' methods in the {@link RepositoryConnection} interface by transforming
 * parameters and mapping the methods to the basic (abstractly declared) methods.
 * <p>
 * Open connections are automatically closed when being garbage collected. A warning message will be logged when the
 * system property <var>org.eclipse.rdf4j.repository.debug</var> has been set to a non-<var>null</var> value.
 *
 * @author Jeen Broekstra
 * @author Arjohn Kampman
 */
public abstract class AbstractRepositoryConnection implements RepositoryConnection {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final Repository repository;

	private volatile ParserConfig parserConfig = new ParserConfig();

	private final AtomicBoolean isOpen = new AtomicBoolean(true);

	private volatile IsolationLevel isolationLevel;

	// private volatile boolean active;

	protected AbstractRepositoryConnection(Repository repository) {
		this.repository = repository;
	}

	@Override
	public void setParserConfig(ParserConfig parserConfig) {
		this.parserConfig = parserConfig;
	}

	@Override
	public ParserConfig getParserConfig() {
		return parserConfig;
	}

	@Override
	public Repository getRepository() {
		return repository;
	}

	@Override
	public ValueFactory getValueFactory() {
		return getRepository().getValueFactory();
	}

	@Override
	public void begin(IsolationLevel level) throws RepositoryException {
		setIsolationLevel(level);
		begin();
	}

	@Override
	public void setIsolationLevel(IsolationLevel level) throws IllegalStateException {
		try {
			if (isActive()) {
				throw new IllegalStateException(
						"Transaction isolation level can not be modified while transaction is active");
			}
			this.isolationLevel = level;
		} catch (UnknownTransactionStateException e) {
			throw new IllegalStateException(
					"Transaction isolation level can not be modified while transaction state is unknown", e);

		} catch (RepositoryException e) {
			throw new IllegalStateException("Transaction isolation level can not be modified due to repository error",
					e);
		}
	}

	@Override
	public IsolationLevel getIsolationLevel() {
		return this.isolationLevel;
	}

	@Override
	public boolean isOpen() throws RepositoryException {
		return isOpen.get();
	}

	@Override
	public void close() throws RepositoryException {
		isOpen.set(false);
	}

	@Override
	public Query prepareQuery(QueryLanguage ql, String query) throws MalformedQueryException, RepositoryException {
		return prepareQuery(ql, query, null);
	}

	@Override
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query)
			throws MalformedQueryException, RepositoryException {
		return prepareTupleQuery(ql, query, null);
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query)
			throws MalformedQueryException, RepositoryException {
		return prepareGraphQuery(ql, query, null);
	}

	@Override
	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query)
			throws MalformedQueryException, RepositoryException {
		return prepareBooleanQuery(ql, query, null);
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String update) throws MalformedQueryException, RepositoryException {
		return prepareUpdate(ql, update, null);
	}

	@Override
	public boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		try (RepositoryResult<Statement> stIter = getStatements(subj, pred, obj, includeInferred, contexts)) {
			return stIter.hasNext();
		}
	}

	@Override
	public boolean hasStatement(Statement st, boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		return hasStatement(st.getSubject(), st.getPredicate(), st.getObject(), includeInferred, contexts);
	}

	@Override
	public boolean isEmpty() throws RepositoryException {
		return size() == 0;
	}

	@Override
	public void export(RDFHandler handler, Resource... contexts) throws RepositoryException, RDFHandlerException {
		exportStatements(null, null, null, false, handler, contexts);
	}

	/**
	 * @deprecated since 2.0. Use {@link #begin()} instead.
	 */
	@Deprecated
	@Override
	public void setAutoCommit(boolean autoCommit) throws RepositoryException {
		if (isActive()) {
			if (autoCommit) {
				// we are switching to autocommit mode from an active transaction.
				commit();
			}
		} else if (!autoCommit) {
			// begin a transaction
			begin();
		}
	}

	/**
	 * @deprecated since 2.0. Use {@link #isActive()} instead.
	 */
	@Deprecated
	@Override
	public boolean isAutoCommit() throws RepositoryException {
		return !isActive();
	}

	@Override
	public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

		RDFInserter rdfInserter = new RDFInserter(this);
		rdfInserter.enforceContext(contexts);

		boolean localTransaction = startLocalTransaction();

		try {
			RDFLoader loader = new RDFLoader(getParserConfig(), getValueFactory());
			loader.load(file, baseURI, dataFormat, rdfInserter);

			conditionalCommit(localTransaction);
		} catch (RDFHandlerException e) {
			conditionalRollback(localTransaction);

			// RDFInserter only throws wrapped RepositoryExceptions
			throw (RepositoryException) e.getCause();
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

		RDFInserter rdfInserter = new RDFInserter(this);
		rdfInserter.enforceContext(contexts);

		boolean localTransaction = startLocalTransaction();

		try {
			RDFLoader loader = new RDFLoader(getParserConfig(), getValueFactory());
			loader.load(url, baseURI, dataFormat, rdfInserter);

			conditionalCommit(localTransaction);
		} catch (RDFHandlerException e) {
			conditionalRollback(localTransaction);

			// RDFInserter only throws wrapped RepositoryExceptions
			throw (RepositoryException) e.getCause();
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

		RDFInserter rdfInserter = new RDFInserter(this);
		rdfInserter.enforceContext(contexts);

		boolean localTransaction = startLocalTransaction();

		try {
			RDFLoader loader = new RDFLoader(getParserConfig(), getValueFactory());
			loader.load(in, baseURI, dataFormat, rdfInserter);

			conditionalCommit(localTransaction);
		} catch (RDFHandlerException e) {
			conditionalRollback(localTransaction);

			// RDFInserter only throws wrapped RepositoryExceptions
			throw (RepositoryException) e.getCause();
		} catch (IOException | RuntimeException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	/**
	 * Starts a new transaction if one is not already active.
	 *
	 * @return <code>true</code> if a new transaction was started, <code>false</code> if a transaction was already
	 *         active.
	 * @throws RepositoryException
	 */
	protected final boolean startLocalTransaction() throws RepositoryException {
		if (!isActive()) {
			begin();
			return true;
		}
		return false;
	}

	/**
	 * Invokes {@link #commit()} if supplied boolean condition is <code>true</code>.
	 *
	 * @param condition a boolean condition.
	 * @throws RepositoryException
	 */
	protected final void conditionalCommit(boolean condition) throws RepositoryException {
		if (condition) {
			commit();
		}
	}

	/**
	 * Invokes {@link #rollback()} if supplied boolean condition is <code>true</code>.
	 *
	 * @param condition a boolean condition.
	 * @throws RepositoryException
	 */
	protected final void conditionalRollback(boolean condition) throws RepositoryException {
		if (condition) {
			rollback();
		}
	}

	@Override
	public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

		RDFInserter rdfInserter = new RDFInserter(this);
		rdfInserter.enforceContext(contexts);

		boolean localTransaction = startLocalTransaction();

		try {
			RDFLoader loader = new RDFLoader(getParserConfig(), getValueFactory());
			loader.load(reader, baseURI, dataFormat, rdfInserter);

			conditionalCommit(localTransaction);
		} catch (RDFHandlerException e) {
			conditionalRollback(localTransaction);

			// RDFInserter only throws wrapped RepositoryExceptions
			throw (RepositoryException) e.getCause();
		} catch (IOException | RuntimeException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	@Override
	public void add(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

		boolean localTransaction = startLocalTransaction();
		try {
			for (Statement st : statements) {
				addWithoutCommit(st, contexts);
			}
			conditionalCommit(localTransaction);
		} catch (RuntimeException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	@Override
	public <E extends Exception> void add(Iteration<? extends Statement, E> statements, Resource... contexts)
			throws RepositoryException, E {
		try {
			Objects.requireNonNull(contexts,
					"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

			boolean localTransaction = startLocalTransaction();

			try {
				while (statements.hasNext()) {
					addWithoutCommit(statements.next(), contexts);
				}

				conditionalCommit(localTransaction);
			} catch (RuntimeException e) {
				conditionalRollback(localTransaction);
				throw e;
			}
		} finally {
			Iterations.closeCloseable(statements);
		}
	}

	@Override
	public void add(Statement st, Resource... contexts) throws RepositoryException {
		boolean localTransaction = startLocalTransaction();

		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");
		addWithoutCommit(st, contexts);

		conditionalCommit(localTransaction);
	}

	@Override
	public void add(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException {
		boolean localTransaction = startLocalTransaction();

		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");
		addWithoutCommit(subject, predicate, object, contexts);

		conditionalCommit(localTransaction);
	}

	@Override
	public void remove(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

		boolean localTransaction = startLocalTransaction();

		try {
			for (Statement st : statements) {
				remove(st, contexts);
			}

			conditionalCommit(localTransaction);
		} catch (RuntimeException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	@Override
	public <E extends Exception> void remove(Iteration<? extends Statement, E> statements, Resource... contexts)
			throws RepositoryException, E {
		try {
			boolean localTransaction = startLocalTransaction();

			try {
				while (statements.hasNext()) {
					remove(statements.next(), contexts);
				}

				conditionalCommit(localTransaction);
			} catch (RuntimeException e) {
				conditionalRollback(localTransaction);
				throw e;
			}
		} finally {
			Iterations.closeCloseable(statements);
		}
	}

	@Override
	public void remove(Statement st, Resource... contexts) throws RepositoryException {
		boolean localTransaction = startLocalTransaction();

		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");
		removeWithoutCommit(st, contexts);

		conditionalCommit(localTransaction);
	}

	@Override
	public void remove(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException {
		boolean localTransaction = startLocalTransaction();

		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");
		removeWithoutCommit(subject, predicate, object, contexts);

		conditionalCommit(localTransaction);
	}

	@Override
	public void clear(Resource... contexts) throws RepositoryException {
		remove(null, null, null, contexts);
	}

	protected void addWithoutCommit(Statement st, Resource... contexts) throws RepositoryException {
		if (contexts.length == 0 && st.getContext() != null) {
			contexts = new Resource[] { st.getContext() };
		}

		addWithoutCommit(st.getSubject(), st.getPredicate(), st.getObject(), contexts);
	}

	protected abstract void addWithoutCommit(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException;

	protected void removeWithoutCommit(Statement st, Resource... contexts) throws RepositoryException {
		if (contexts.length == 0 && st.getContext() != null) {
			contexts = new Resource[] { st.getContext() };
		}

		removeWithoutCommit(st.getSubject(), st.getPredicate(), st.getObject(), contexts);
	}

	protected abstract void removeWithoutCommit(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException;
}
