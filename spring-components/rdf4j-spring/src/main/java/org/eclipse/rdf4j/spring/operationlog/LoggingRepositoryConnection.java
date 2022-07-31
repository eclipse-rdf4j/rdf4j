/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.operationlog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.net.URL;

import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.spring.dao.exception.RDF4JSpringException;
import org.eclipse.rdf4j.spring.operationlog.log.OperationLog;
import org.eclipse.rdf4j.spring.operationlog.log.PseudoOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class LoggingRepositoryConnection extends RepositoryConnectionWrapper {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	OperationLog operationLog;

	public LoggingRepositoryConnection(RepositoryConnection delegate, OperationLog operationLog) {
		super(delegate.getRepository(), delegate);
		this.operationLog = operationLog;
	}

	@Override
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String queryString, String baseURI)
			throws MalformedQueryException, RepositoryException {
		logWarning();
		return new LoggingTupleQuery(
				getDelegate().prepareTupleQuery(ql, queryString, baseURI), operationLog);
	}

	@Override
	public TupleQuery prepareTupleQuery(String query)
			throws RepositoryException, MalformedQueryException {
		logWarning();
		return new LoggingTupleQuery(getDelegate().prepareTupleQuery(query), operationLog);
	}

	@Override
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException {
		logWarning();
		return new LoggingTupleQuery(getDelegate().prepareTupleQuery(ql, query), operationLog);
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String queryString, String baseURI)
			throws MalformedQueryException, RepositoryException {
		logWarning();
		return new LoggingGraphQuery(
				getDelegate().prepareGraphQuery(ql, queryString, baseURI), operationLog);
	}

	@Override
	public GraphQuery prepareGraphQuery(String query)
			throws RepositoryException, MalformedQueryException {
		logWarning();
		return new LoggingGraphQuery(getDelegate().prepareGraphQuery(query), operationLog);
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException {
		logWarning();
		return new LoggingGraphQuery(getDelegate().prepareGraphQuery(ql, query), operationLog);
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String updateString, String baseURI)
			throws MalformedQueryException, RepositoryException {
		logWarning();
		return new LoggingUpdate(
				getDelegate().prepareUpdate(ql, updateString, baseURI), operationLog);
	}

	@Override
	public RepositoryResult<Statement> getStatements(
			Resource subj, IRI pred, Value obj, Resource... contexts) throws RepositoryException {
		logWarning();
		return operationLog.runWithLog(
				PseudoOperation.forGetSatements(subj, pred, obj, contexts),
				() -> getDelegate().getStatements(subj, pred, obj, contexts));
	}

	@Override
	public void add(RepositoryResult<Statement> statements, Resource... contexts)
			throws RepositoryException {
		operationLog.runWithLog(
				PseudoOperation.forAdd(statements, contexts),
				() -> getDelegate().add(statements, contexts));
	}

	@Override
	public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		operationLog.runWithLog(
				PseudoOperation.forAdd(file, baseURI, dataFormat, contexts),
				wrapInRuntimeException(
						() -> getDelegate().add(file, baseURI, dataFormat, contexts)));
	}

	@Override
	public void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		operationLog.runWithLog(
				PseudoOperation.forAdd(in, baseURI, dataFormat, contexts),
				wrapInRuntimeException(() -> getDelegate().add(in, baseURI, dataFormat, contexts)));
	}

	@Override
	public void add(Iterable<? extends Statement> statements, Resource... contexts)
			throws RepositoryException {
		operationLog.runWithLog(
				PseudoOperation.forAdd(statements, contexts),
				() -> getDelegate().add(statements, contexts));
	}

	@Override
	public <E extends Exception> void add(
			Iteration<? extends Statement, E> statementIter, Resource... contexts)
			throws RepositoryException, E {
		operationLog.runWithLog(
				PseudoOperation.forAdd(statementIter, contexts),
				wrapInRuntimeException(() -> getDelegate().add(statementIter, contexts)));
	}

	@Override
	public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		operationLog.runWithLog(
				PseudoOperation.forAdd(reader, baseURI, dataFormat, contexts),
				wrapInRuntimeException(
						() -> getDelegate().add(reader, baseURI, dataFormat, contexts)));
	}

	@Override
	public void add(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		operationLog.runWithLog(
				PseudoOperation.forAdd(subject, predicate, object, contexts),
				() -> getDelegate().add(subject, predicate, object, contexts));
	}

	@Override
	public void add(Statement st, Resource... contexts) throws RepositoryException {
		operationLog.runWithLog(
				PseudoOperation.forAdd(st, contexts), () -> getDelegate().add(st, contexts));
	}

	@Override
	public void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		operationLog.runWithLog(
				PseudoOperation.forAdd(url, baseURI, dataFormat, contexts),
				wrapInRuntimeException(
						() -> getDelegate().add(url, baseURI, dataFormat, contexts)));
	}

	@Override
	public void remove(RepositoryResult<Statement> statements, Resource... contexts)
			throws RepositoryException {
		operationLog.runWithLog(
				PseudoOperation.forRemove(statements, contexts),
				wrapInRuntimeException(() -> getDelegate().remove(statements, contexts)));
	}

	@Override
	public void remove(Iterable<? extends Statement> statements, Resource... contexts)
			throws RepositoryException {
		operationLog.runWithLog(
				PseudoOperation.forRemove(statements, contexts),
				() -> getDelegate().remove(statements, contexts));
	}

	@Override
	public <E extends Exception> void remove(
			Iteration<? extends Statement, E> statementIter, Resource... contexts)
			throws RepositoryException, E {
		operationLog.runWithLog(
				PseudoOperation.forRemove(statementIter, contexts),
				wrapInRuntimeException(() -> getDelegate().remove(statementIter, contexts)));
	}

	@Override
	public void remove(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		operationLog.runWithLog(
				PseudoOperation.forRemove(subject, predicate, object, contexts),
				() -> getDelegate().remove(subject, predicate, object, contexts));
	}

	@Override
	public void remove(Statement st, Resource... contexts) throws RepositoryException {
		operationLog.runWithLog(
				PseudoOperation.forRemove(st, contexts), () -> getDelegate().remove(st, contexts));
	}

	@Override
	public void clear(Resource... contexts) throws RepositoryException {
		operationLog.runWithLog(
				PseudoOperation.forClear(contexts), () -> getDelegate().clear((contexts)));
	}

	@Override
	public RepositoryResult<Statement> getStatements(
			Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		return operationLog.runWithLog(
				PseudoOperation.forGetSatements(subj, pred, obj, includeInferred, contexts),
				() -> getDelegate().getStatements(subj, pred, obj, includeInferred, contexts));
	}

	@Override
	public boolean hasStatement(
			Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		return operationLog.runWithLog(
				PseudoOperation.forHasStatement(subj, pred, obj, includeInferred, contexts),
				() -> getDelegate().hasStatement(subj, pred, obj, includeInferred, contexts));
	}

	@Override
	public boolean hasStatement(Statement st, boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		return operationLog.runWithLog(
				PseudoOperation.forHasStatement(st, includeInferred, contexts),
				() -> getDelegate().hasStatement(st, includeInferred, contexts));
	}

	@Override
	public long size(Resource... contexts) throws RepositoryException {
		return operationLog.runWithLog(
				PseudoOperation.forSize(contexts), () -> getDelegate().size(contexts));
	}

	@Override
	public void removeNamespace(String prefix) throws RepositoryException {
		super.removeNamespace(prefix);
	}

	private Runnable wrapInRuntimeException(ExceptionThrowingRunnable task) {
		return () -> {
			try {
				task.run();
			} catch (Exception e) {
				throw new RDF4JSpringException(e);
			}
		};
	}

	private interface ExceptionThrowingRunnable {
		void run() throws Exception;
	}

	private void logWarning() {
		logger.warn(
				"rdf4j operations (queries and updates) are being timed and logged. "
						+ "Don't do this in production as the log is not limited in size! "
						+ "You can disable this feature by setting the configuration property "
						+ "'org.eclipse.rdf4j.spring.operationlog.enabled' to 'false'");
	}
}
