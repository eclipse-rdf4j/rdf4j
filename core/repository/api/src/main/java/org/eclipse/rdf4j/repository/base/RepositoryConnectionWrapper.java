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

import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.DelegatingRepositoryConnection;
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

/**
 * Delegates all calls to the delegate RepositoryConnection. Conditionally processes add/remove/read to common base
 * method to make them easier to override.
 *
 * @author James Leigh
 * @see #isDelegatingAdd()
 * @see #isDelegatingRemove()
 * @see #isDelegatingRead()
 */
public class RepositoryConnectionWrapper extends AbstractRepositoryConnection
		implements DelegatingRepositoryConnection {

	private volatile RepositoryConnection delegate;

	public RepositoryConnectionWrapper(Repository repository) {
		super(repository);
	}

	public RepositoryConnectionWrapper(Repository repository, RepositoryConnection delegate) {
		this(repository);
		setDelegate(delegate);
	}

	@Override
	public RepositoryConnection getDelegate() {
		return delegate;
	}

	@Override
	public void setDelegate(RepositoryConnection delegate) {
		this.delegate = delegate;
		setParserConfig(delegate.getParserConfig());
	}

	/**
	 * If false then the following add methods will call {@link #addWithoutCommit(Resource, IRI, Value, Resource[])}.
	 *
	 * @see #add(Iterable, Resource...)
	 * @see #add(Iteration, Resource...)
	 * @see #add(Statement, Resource...)
	 * @see #add(File, String, RDFFormat, Resource...)
	 * @see #add(InputStream, String, RDFFormat, Resource...)
	 * @see #add(Reader, String, RDFFormat, Resource...)
	 * @see #add(Resource, IRI, Value, Resource...)
	 * @see #add(URL, String, RDFFormat, Resource...)
	 * @return <code>true</code> to delegate add methods, <code>false</code> to call
	 *         {@link #addWithoutCommit(Resource, IRI, Value, Resource[])}
	 * @throws RepositoryException
	 */
	protected boolean isDelegatingAdd() throws RepositoryException {
		return true;
	}

	/**
	 * If false then the following has/export/isEmpty methods will call
	 * {@link #getStatements(Resource, IRI, Value, boolean, Resource[])}.
	 *
	 * @see #exportStatements(Resource, IRI, Value, boolean, RDFHandler, Resource...)
	 * @see #hasStatement(Statement, boolean, Resource...)
	 * @see #hasStatement(Resource, IRI, Value, boolean, Resource...)
	 * @see #isEmpty()
	 * @return <code>true</code> to delegate read methods, <code>false</code> to call
	 *         {@link #getStatements(Resource, IRI, Value, boolean, Resource[])}
	 * @throws RepositoryException
	 */
	protected boolean isDelegatingRead() throws RepositoryException {
		return true;
	}

	/**
	 * If false then the following remove methods will call
	 * {@link #removeWithoutCommit(Resource, IRI, Value, Resource[])}.
	 *
	 * @see #clear(Resource...)
	 * @see #remove(Iterable, Resource...)
	 * @see #remove(Iteration, Resource...)
	 * @see #remove(Statement, Resource...)
	 * @see #remove(Resource, IRI, Value, Resource...)
	 * @return <code>true</code> to delegate remove methods, <code>false</code> to call
	 *         {@link #removeWithoutCommit(Resource, IRI, Value, Resource...)}
	 * @throws RepositoryException
	 */
	protected boolean isDelegatingRemove() throws RepositoryException {
		return true;
	}

	@Override
	public void setParserConfig(ParserConfig parserConfig) {
		super.setParserConfig(parserConfig);
		if (getDelegate() != null) {
			getDelegate().setParserConfig(parserConfig);
		}
	}

	@Override
	public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		if (isDelegatingAdd()) {
			getDelegate().add(file, baseURI, dataFormat, contexts);
		} else {
			super.add(file, baseURI, dataFormat, contexts);
		}
	}

	@Override
	public void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		if (isDelegatingAdd()) {
			getDelegate().add(in, baseURI, dataFormat, contexts);
		} else {
			super.add(in, baseURI, dataFormat, contexts);
		}
	}

	@Override
	public void add(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {
		if (isDelegatingAdd()) {
			getDelegate().add(statements, contexts);
		} else {
			super.add(statements, contexts);
		}
	}

	@Override
	public <E extends Exception> void add(Iteration<? extends Statement, E> statementIter, Resource... contexts)
			throws RepositoryException, E {
		if (isDelegatingAdd()) {
			getDelegate().add(statementIter, contexts);
		} else {
			super.add(statementIter, contexts);
		}
	}

	@Override
	public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		if (isDelegatingAdd()) {
			getDelegate().add(reader, baseURI, dataFormat, contexts);
		} else {
			super.add(reader, baseURI, dataFormat, contexts);
		}
	}

	@Override
	public void add(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException {
		if (isDelegatingAdd()) {
			getDelegate().add(subject, predicate, object, contexts);
		} else {
			super.add(subject, predicate, object, contexts);
		}
	}

	@Override
	public void add(Statement st, Resource... contexts) throws RepositoryException {
		if (isDelegatingAdd()) {
			getDelegate().add(st, contexts);
		} else {
			super.add(st, contexts);
		}
	}

	@Override
	public void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		if (isDelegatingAdd()) {
			getDelegate().add(url, baseURI, dataFormat, contexts);
		} else {
			super.add(url, baseURI, dataFormat, contexts);
		}
	}

	@Override
	public void clear(Resource... contexts) throws RepositoryException {
		if (isDelegatingRemove()) {
			getDelegate().clear(contexts);
		} else {
			super.clear(contexts);
		}
	}

	@Override
	public void close() throws RepositoryException {
		try {
			super.close();
		} finally {
			getDelegate().close();
		}
	}

	@Override
	public void prepare() throws RepositoryException {
		getDelegate().prepare();
	}

	@Override
	public void commit() throws RepositoryException {
		getDelegate().commit();
	}

	@Override
	public void exportStatements(Resource subj, IRI pred, Value obj, boolean includeInferred, RDFHandler handler,
			Resource... contexts) throws RepositoryException, RDFHandlerException {
		if (isDelegatingRead()) {
			getDelegate().exportStatements(subj, pred, obj, includeInferred, handler, contexts);
		} else {
			exportStatements(getStatements(subj, pred, obj, includeInferred, contexts), handler);
		}
	}

	@Override
	public RepositoryResult<Resource> getContextIDs() throws RepositoryException {
		return getDelegate().getContextIDs();
	}

	@Override
	public String getNamespace(String prefix) throws RepositoryException {
		return getDelegate().getNamespace(prefix);
	}

	@Override
	public RepositoryResult<Namespace> getNamespaces() throws RepositoryException {
		return getDelegate().getNamespaces();
	}

	@Override
	public RepositoryResult<Statement> getStatements(Resource subj, IRI pred, Value obj, boolean includeInferred,
			Resource... contexts) throws RepositoryException {
		return getDelegate().getStatements(subj, pred, obj, includeInferred, contexts);
	}

	@Override
	public boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		if (isDelegatingRead()) {
			return getDelegate().hasStatement(subj, pred, obj, includeInferred, contexts);
		}
		return super.hasStatement(subj, pred, obj, includeInferred, contexts);
	}

	@Override
	public boolean hasStatement(Statement st, boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		if (isDelegatingRead()) {
			return getDelegate().hasStatement(st, includeInferred, contexts);
		}
		return super.hasStatement(st, includeInferred, contexts);
	}

	/**
	 * @deprecated since 2.0. Use {@link #isActive()} instead.
	 */
	@Override
	@Deprecated
	public boolean isAutoCommit() throws RepositoryException {
		return getDelegate().isAutoCommit();
	}

	@Override
	public boolean isActive() throws UnknownTransactionStateException, RepositoryException {
		return getDelegate().isActive();
	}

	@Override
	public boolean isEmpty() throws RepositoryException {
		if (isDelegatingRead()) {
			return getDelegate().isEmpty();
		}
		return super.isEmpty();
	}

	@Override
	public boolean isOpen() throws RepositoryException {
		return getDelegate().isOpen();
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query, String baseURI)
			throws MalformedQueryException, RepositoryException {
		return getDelegate().prepareGraphQuery(ql, query, baseURI);
	}

	@Override
	public Query prepareQuery(QueryLanguage ql, String query, String baseURI)
			throws MalformedQueryException, RepositoryException {
		return getDelegate().prepareQuery(ql, query, baseURI);
	}

	@Override
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query, String baseURI)
			throws MalformedQueryException, RepositoryException {
		return getDelegate().prepareTupleQuery(ql, query, baseURI);
	}

	@Override
	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query, String baseURI)
			throws MalformedQueryException, RepositoryException {
		return getDelegate().prepareBooleanQuery(ql, query, baseURI);
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String update, String baseURI)
			throws MalformedQueryException, RepositoryException {
		return getDelegate().prepareUpdate(ql, update, baseURI);
	}

	@Override
	public void remove(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {
		if (isDelegatingRemove()) {
			getDelegate().remove(statements, contexts);
		} else {
			super.remove(statements, contexts);
		}
	}

	@Override
	public <E extends Exception> void remove(Iteration<? extends Statement, E> statementIter, Resource... contexts)
			throws RepositoryException, E {
		if (isDelegatingRemove()) {
			getDelegate().remove(statementIter, contexts);
		} else {
			super.remove(statementIter, contexts);
		}
	}

	@Override
	public void remove(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException {
		if (isDelegatingRemove()) {
			getDelegate().remove(subject, predicate, object, contexts);
		} else {
			super.remove(subject, predicate, object, contexts);
		}
	}

	@Override
	public void remove(Statement st, Resource... contexts) throws RepositoryException {
		if (isDelegatingRemove()) {
			getDelegate().remove(st, contexts);
		} else {
			super.remove(st, contexts);
		}
	}

	@Override
	public void removeNamespace(String prefix) throws RepositoryException {
		getDelegate().removeNamespace(prefix);
	}

	@Override
	public void clearNamespaces() throws RepositoryException {
		getDelegate().clearNamespaces();
	}

	@Override
	public void rollback() throws RepositoryException {
		getDelegate().rollback();
	}

	/**
	 * @deprecated use {@link #begin()} instead.
	 */
	@Deprecated
	@Override
	public void setAutoCommit(boolean autoCommit) throws RepositoryException {
		super.setAutoCommit(autoCommit);
		getDelegate().setAutoCommit(autoCommit);
	}

	@Override
	public void setNamespace(String prefix, String name) throws RepositoryException {
		getDelegate().setNamespace(prefix, name);
	}

	@Override
	public long size(Resource... contexts) throws RepositoryException {
		return getDelegate().size(contexts);
	}

	@Override
	protected void addWithoutCommit(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		getDelegate().add(subject, predicate, object, contexts);
	}

	@Override
	protected void removeWithoutCommit(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		getDelegate().remove(subject, predicate, object, contexts);
	}

	/**
	 * Exports all statements contained in the supplied statement iterator and all relevant namespace information to the
	 * supplied RDFHandler.
	 */
	protected void exportStatements(RepositoryResult<Statement> stIter, RDFHandler handler)
			throws RepositoryException, RDFHandlerException {
		try (stIter) {
			handler.startRDF();
			try ( // Export namespace information
					RepositoryResult<Namespace> nsIter = getNamespaces()) {
				while (nsIter.hasNext()) {
					Namespace ns = nsIter.next();
					handler.handleNamespace(ns.getPrefix(), ns.getName());
				}
			}
			// Export statemnts
			while (stIter.hasNext()) {
				handler.handleStatement(stIter.next());
			}
			handler.endRDF();
		}
	}

	@Override
	public void begin() throws RepositoryException {
		getDelegate().begin();
	}

	@Override
	public void begin(IsolationLevel level) throws RepositoryException {
		getDelegate().begin(level);
	}

	@Override
	public void begin(TransactionSetting... settings) {
		getDelegate().begin(settings);
	}

	@Override
	public void setIsolationLevel(IsolationLevel level) throws IllegalStateException {
		getDelegate().setIsolationLevel(level);
	}

	@Override
	public IsolationLevel getIsolationLevel() {
		return getDelegate().getIsolationLevel();
	}
}
