/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.sparql;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
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
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.testsuite.sparql.vocabulary.EX;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for tests included in the {@link RepositorySPARQLComplianceTestSuite}.
 *
 * @author Jeen Broekstra
 */
public abstract class AbstractComplianceTest {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	protected Repository repo;
	protected RepositoryConnection conn;

	@Before
	public void setUp() throws Exception {
		repo = RepositorySPARQLComplianceTestSuite.getEmptyInitializedRepository(this.getClass());
		conn = new RepositoryConnectionWrapper(repo.getConnection());
	}

	@After
	public void tearDown() throws Exception {
		try {
			conn.close();
		} finally {
			repo.shutDown();
		}
	}

	protected void loadTestData(String dataFile, Resource... contexts)
			throws RDFParseException, RepositoryException, IOException {
		logger.debug("loading dataset {}", dataFile);
		try (InputStream dataset = this.getClass().getResourceAsStream(dataFile)) {
			conn.add(dataset, "", Rio.getParserFormatForFileName(dataFile).orElseThrow(Rio.unsupportedFormat(dataFile)),
					contexts);
		}
		logger.debug("dataset loaded.");
	}

	/**
	 * Get a set of useful namespace prefix declarations.
	 *
	 * @return namespace prefix declarations for dc, foaf and ex.
	 */
	protected String getNamespaceDeclarations() {
		return "PREFIX dc: <" + DCTERMS.NAMESPACE + "> \n" +
				"PREFIX foaf: <" + FOAF.NAMESPACE + "> \n" +
				"PREFIX ex: <" + EX.NAMESPACE + "> \n" +
				"\n";
	}

	private static class RepositoryConnectionWrapper implements RepositoryConnection {

		private final RepositoryConnection delegate;

		public RepositoryConnectionWrapper(RepositoryConnection delegate) {
			this.delegate = delegate;
		}

		@Override
		public Repository getRepository() {
			return delegate.getRepository();
		}

		@Override
		public void setParserConfig(ParserConfig config) {
			delegate.setParserConfig(config);
		}

		@Override
		public ParserConfig getParserConfig() {
			return delegate.getParserConfig();
		}

		@Override
		public ValueFactory getValueFactory() {
			return delegate.getValueFactory();
		}

		@Override
		public boolean isOpen() throws RepositoryException {
			return delegate.isOpen();
		}

		@Override
		public void close() throws RepositoryException {
			delegate.close();
		}

		private <T> T checkThatHashCodeWorks(T prepareQuery) {
			assert prepareQuery.hashCode() == prepareQuery.hashCode();
			assert prepareQuery.hashCode() != System.identityHashCode(prepareQuery);

			return prepareQuery;
		}

		@Override
		public Query prepareQuery(String query) throws RepositoryException, MalformedQueryException {
			return checkThatHashCodeWorks(delegate.prepareQuery(query));
		}

		@Override
		public Query prepareQuery(QueryLanguage ql, String query) throws RepositoryException, MalformedQueryException {
			return checkThatHashCodeWorks(delegate.prepareQuery(ql, query));
		}

		@Override
		public Query prepareQuery(QueryLanguage ql, String query, String baseURI)
				throws RepositoryException, MalformedQueryException {
			return checkThatHashCodeWorks(delegate.prepareQuery(ql, query, baseURI));
		}

		@Override
		public TupleQuery prepareTupleQuery(String query) throws RepositoryException, MalformedQueryException {
			return checkThatHashCodeWorks(delegate.prepareTupleQuery(query));
		}

		@Override
		public TupleQuery prepareTupleQuery(QueryLanguage ql, String query)
				throws RepositoryException, MalformedQueryException {
			return checkThatHashCodeWorks(delegate.prepareTupleQuery(ql, query));
		}

		@Override
		public TupleQuery prepareTupleQuery(QueryLanguage ql, String query, String baseURI)
				throws RepositoryException, MalformedQueryException {
			return checkThatHashCodeWorks(delegate.prepareTupleQuery(ql, query, baseURI));
		}

		@Override
		public GraphQuery prepareGraphQuery(String query) throws RepositoryException, MalformedQueryException {
			return checkThatHashCodeWorks(delegate.prepareGraphQuery(query));
		}

		@Override
		public GraphQuery prepareGraphQuery(QueryLanguage ql, String query)
				throws RepositoryException, MalformedQueryException {
			return checkThatHashCodeWorks(delegate.prepareGraphQuery(ql, query));
		}

		@Override
		public GraphQuery prepareGraphQuery(QueryLanguage ql, String query, String baseURI)
				throws RepositoryException, MalformedQueryException {
			return checkThatHashCodeWorks(delegate.prepareGraphQuery(ql, query, baseURI));
		}

		@Override
		public BooleanQuery prepareBooleanQuery(String query) throws RepositoryException, MalformedQueryException {
			return checkThatHashCodeWorks(delegate.prepareBooleanQuery(query));
		}

		@Override
		public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query)
				throws RepositoryException, MalformedQueryException {
			return checkThatHashCodeWorks(delegate.prepareBooleanQuery(ql, query));
		}

		@Override
		public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query, String baseURI)
				throws RepositoryException, MalformedQueryException {
			return checkThatHashCodeWorks(delegate.prepareBooleanQuery(ql, query, baseURI));
		}

		@Override
		public Update prepareUpdate(String update) throws RepositoryException, MalformedQueryException {
			return checkThatHashCodeWorks(delegate.prepareUpdate(update));
		}

		@Override
		public Update prepareUpdate(QueryLanguage ql, String update)
				throws RepositoryException, MalformedQueryException {
			return checkThatHashCodeWorks(delegate.prepareUpdate(ql, update));
		}

		@Override
		public Update prepareUpdate(QueryLanguage ql, String update, String baseURI)
				throws RepositoryException, MalformedQueryException {
			return checkThatHashCodeWorks(delegate.prepareUpdate(ql, update, baseURI));
		}

		@Override
		public RepositoryResult<Resource> getContextIDs() throws RepositoryException {
			return delegate.getContextIDs();
		}

		@Override
		public RepositoryResult<Statement> getStatements(Resource subj, IRI pred, Value obj, Resource... contexts)
				throws RepositoryException {
			return delegate.getStatements(subj, pred, obj, contexts);
		}

		@Override
		public RepositoryResult<Statement> getStatements(Resource subj, IRI pred, Value obj, boolean includeInferred,
				Resource... contexts) throws RepositoryException {
			return delegate.getStatements(subj, pred, obj, includeInferred, contexts);
		}

		@Override
		public boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
				throws RepositoryException {
			return delegate.hasStatement(subj, pred, obj, includeInferred, contexts);
		}

		@Override
		public boolean hasStatement(Statement st, boolean includeInferred, Resource... contexts)
				throws RepositoryException {
			return delegate.hasStatement(st, includeInferred, contexts);
		}

		@Override
		public void exportStatements(Resource subj, IRI pred, Value obj, boolean includeInferred, RDFHandler handler,
				Resource... contexts) throws RepositoryException, RDFHandlerException {
			delegate.exportStatements(subj, pred, obj, includeInferred, handler, contexts);
		}

		@Override
		public void export(RDFHandler handler, Resource... contexts) throws RepositoryException, RDFHandlerException {
			delegate.export(handler, contexts);
		}

		@Override
		public long size(Resource... contexts) throws RepositoryException {
			return delegate.size(contexts);
		}

		@Override
		public boolean isEmpty() throws RepositoryException {
			return delegate.isEmpty();
		}

		@Override
		@Deprecated
		public void setAutoCommit(boolean autoCommit) throws RepositoryException {
			delegate.setAutoCommit(autoCommit);
		}

		@Override
		@Deprecated
		public boolean isAutoCommit() throws RepositoryException {
			return delegate.isAutoCommit();
		}

		@Override
		public boolean isActive() throws UnknownTransactionStateException, RepositoryException {
			return delegate.isActive();
		}

		@Override
		public void setIsolationLevel(IsolationLevel level) throws IllegalStateException {
			delegate.setIsolationLevel(level);
		}

		@Override
		public IsolationLevel getIsolationLevel() {
			return delegate.getIsolationLevel();
		}

		@Override
		public void begin() throws RepositoryException {
			delegate.begin();
		}

		@Override
		public void begin(IsolationLevel level) throws RepositoryException {
			delegate.begin(level);
		}

		@Override
		public void begin(TransactionSetting... settings) {
			delegate.begin(settings);
		}

		@Override
		public void prepare() throws RepositoryException {
			delegate.prepare();
		}

		@Override
		public void commit() throws RepositoryException {
			delegate.commit();
		}

		@Override
		public void rollback() throws RepositoryException {
			delegate.rollback();
		}

		@Override
		public void add(InputStream in, RDFFormat dataFormat, Resource... contexts)
				throws IOException, RDFParseException, RepositoryException {
			delegate.add(in, dataFormat, contexts);
		}

		@Override
		public void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
				throws IOException, RDFParseException, RepositoryException {
			delegate.add(in, baseURI, dataFormat, contexts);
		}

		@Override
		public void add(Reader reader, RDFFormat dataFormat, Resource... contexts)
				throws IOException, RDFParseException, RepositoryException {
			delegate.add(reader, dataFormat, contexts);
		}

		@Override
		public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
				throws IOException, RDFParseException, RepositoryException {
			delegate.add(reader, baseURI, dataFormat, contexts);
		}

		@Override
		public void add(URL url, Resource... contexts) throws IOException, RDFParseException, RepositoryException {
			delegate.add(url, contexts);
		}

		@Override
		public void add(URL url, RDFFormat dataFormat, Resource... contexts)
				throws IOException, RDFParseException, RepositoryException {
			delegate.add(url, dataFormat, contexts);
		}

		@Override
		public void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts)
				throws IOException, RDFParseException, RepositoryException {
			delegate.add(url, baseURI, dataFormat, contexts);
		}

		@Override
		public void add(File file, Resource... contexts) throws IOException, RDFParseException, RepositoryException {
			delegate.add(file, contexts);
		}

		@Override
		public void add(File file, RDFFormat dataFormat, Resource... contexts)
				throws IOException, RDFParseException, RepositoryException {
			delegate.add(file, dataFormat, contexts);
		}

		@Override
		public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts)
				throws IOException, RDFParseException, RepositoryException {
			delegate.add(file, baseURI, dataFormat, contexts);
		}

		@Override
		public void add(Resource subject, IRI predicate, Value object, Resource... contexts)
				throws RepositoryException {
			delegate.add(subject, predicate, object, contexts);
		}

		@Override
		public void add(Statement st, Resource... contexts) throws RepositoryException {
			delegate.add(st, contexts);
		}

		@Override
		public void add(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {
			delegate.add(statements, contexts);
		}

		@Override
		@Deprecated(since = "4.1.0", forRemoval = true)
		public <E extends Exception> void add(Iteration<? extends Statement, E> statements, Resource... contexts)
				throws RepositoryException, E {
			delegate.add(statements, contexts);
		}

		@Override
		public <E extends Exception> void add(CloseableIteration<? extends Statement, E> statements,
				Resource... contexts) throws RepositoryException, E {
			delegate.add(statements, contexts);
		}

		@Override
		public void add(RepositoryResult<Statement> statements, Resource... contexts) throws RepositoryException {
			delegate.add(statements, contexts);
		}

		@Override
		public void remove(Resource subject, IRI predicate, Value object, Resource... contexts)
				throws RepositoryException {
			delegate.remove(subject, predicate, object, contexts);
		}

		@Override
		public void remove(Statement st, Resource... contexts) throws RepositoryException {
			delegate.remove(st, contexts);
		}

		@Override
		public void remove(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {
			delegate.remove(statements, contexts);
		}

		@Override
		@Deprecated(since = "4.1.0", forRemoval = true)
		public <E extends Exception> void remove(Iteration<? extends Statement, E> statements, Resource... contexts)
				throws RepositoryException, E {
			delegate.remove(statements, contexts);
		}

		@Override
		public <E extends Exception> void remove(CloseableIteration<? extends Statement, E> statements,
				Resource... contexts) throws RepositoryException, E {
			delegate.remove(statements, contexts);
		}

		@Override
		public void remove(RepositoryResult<Statement> statements, Resource... contexts) throws RepositoryException {
			delegate.remove(statements, contexts);
		}

		@Override
		public void clear(Resource... contexts) throws RepositoryException {
			delegate.clear(contexts);
		}

		@Override
		public RepositoryResult<Namespace> getNamespaces() throws RepositoryException {
			return delegate.getNamespaces();
		}

		@Override
		public String getNamespace(String prefix) throws RepositoryException {
			return delegate.getNamespace(prefix);
		}

		@Override
		public void setNamespace(String prefix, String name) throws RepositoryException {
			delegate.setNamespace(prefix, name);
		}

		@Override
		public void removeNamespace(String prefix) throws RepositoryException {
			delegate.removeNamespace(prefix);
		}

		@Override
		public void clearNamespaces() throws RepositoryException {
			delegate.clearNamespaces();
		}
	}
}
