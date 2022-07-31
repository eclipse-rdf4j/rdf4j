/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.contextaware;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;

/**
 * Allows contexts to be specified at the connection level or the method level.
 *
 * @author James Leigh
 */
public class ContextAwareConnection extends RepositoryConnectionWrapper {

	private static final IRI[] ALL_CONTEXTS = new IRI[0];

	private final ContextAwareConnection next;

	private boolean includeInferred = true;

	private int maxQueryTime;

	private QueryLanguage ql = QueryLanguage.SPARQL;

	private String baseURI;

	private IRI[] readContexts = ALL_CONTEXTS;

	private IRI[] addContexts = ALL_CONTEXTS;

	private IRI[] removeContexts = ALL_CONTEXTS;

	private IRI[] archiveContexts = ALL_CONTEXTS;

	private IRI insertContext = null;

	public ContextAwareConnection(Repository repository) throws RepositoryException {
		this(repository, repository.getConnection());
	}

	public ContextAwareConnection(RepositoryConnection connection) throws RepositoryException {
		this(connection.getRepository(), connection);
	}

	public ContextAwareConnection(Repository repository, RepositoryConnection connection) throws RepositoryException {
		super(repository, connection);
		ContextAwareConnection next = null;
		RepositoryConnection up = connection;
		while (up instanceof RepositoryConnectionWrapper) {
			if (up instanceof ContextAwareConnection) {
				next = (ContextAwareConnection) up;
				break;
			} else {
				up = ((RepositoryConnectionWrapper) up).getDelegate();
			}
		}
		this.next = next;
	}

	@Override
	protected boolean isDelegatingRemove() throws RepositoryException {
		return getArchiveContexts().length == 0 && getRemoveContexts().length < 2;
	}

	/**
	 * if false, no inferred statements are considered; if true, inferred statements are considered if available
	 */
	public boolean isIncludeInferred() {
		return includeInferred;
	}

	/**
	 * if false, no inferred statements are considered; if true, inferred statements are considered if available
	 */
	public void setIncludeInferred(boolean includeInferred) {
		this.includeInferred = includeInferred;
		if (next != null) {
			next.setIncludeInferred(includeInferred);
		}
	}

	public int getMaxQueryTime() {
		return maxQueryTime;
	}

	public void setMaxQueryTime(int maxQueryTime) {
		this.maxQueryTime = maxQueryTime;
		if (next != null) {
			next.setMaxQueryTime(maxQueryTime);
		}
	}

	public QueryLanguage getQueryLanguage() {
		return ql;
	}

	public void setQueryLanguage(QueryLanguage ql) {
		this.ql = ql;
		if (next != null) {
			next.setQueryLanguage(ql);
		}
	}

	/**
	 * @return Returns the default baseURI.
	 */
	public String getBaseURI() {
		return baseURI;
	}

	/**
	 * @param baseURI The default baseURI to set.
	 */
	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
		if (next != null) {
			next.setBaseURI(baseURI);
		}
	}

	/**
	 * The default context(s) to get the data from. Note that this parameter is a vararg and as such is optional. If no
	 * contexts are supplied the method operates on the entire repository.
	 */
	public IRI[] getReadContexts() {
		return readContexts;
	}

	/**
	 * The default context(s) to get the data from. Note that this parameter is a vararg and as such is optional. If no
	 * contexts are supplied the method operates on the entire repository.
	 */
	public void setReadContexts(IRI... readContexts) {
		this.readContexts = readContexts;
		if (next != null) {
			next.setReadContexts(readContexts);
		}
	}

	/**
	 * The contexts to add the statements to. Note that this parameter is a vararg and as such is optional. If no
	 * contexts are specified, each statement is added to any context specified in the statement, or if the statement
	 * contains no context, it is added without a context. If one or more contexts are specified each statement is added
	 * to these contexts, ignoring any context information in the statement itself.
	 */
	@Deprecated
	public IRI[] getAddContexts() {
		if (isNilContext(addContexts)) {
			return new IRI[] { getInsertContext() };
		}
		return addContexts;
	}

	/**
	 * The contexts to add the statements to. Note that this parameter is a vararg and as such is optional. If no
	 * contexts are specified, each statement is added to any context specified in the statement, or if the statement
	 * contains no context, it is added without a context. If one or more contexts are specified each statement is added
	 * to these contexts, ignoring any context information in the statement itself.
	 */
	@Deprecated
	public void setAddContexts(IRI... addContexts) {
		this.addContexts = addContexts;
		if (isNilContext(addContexts)) {
			this.insertContext = null;
		} else if (addContexts.length == 1) {
			this.insertContext = addContexts[0];
		}
		if (next != null) {
			next.setAddContexts(addContexts);
		}
	}

	/**
	 * The context(s) to remove the data from. Note that this parameter is a vararg and as such is optional. If no
	 * contexts are supplied the method operates on the contexts associated with the statement itself, and if no context
	 * is associated with the statement, on the entire repository.
	 */
	public IRI[] getRemoveContexts() {
		return removeContexts;
	}

	/**
	 * The context(s) to remove the data from. Note that this parameter is a vararg and as such is optional. If no
	 * contexts are supplied the method operates on the contexts associated with the statement itself, and if no context
	 * is associated with the statement, on the entire repository.
	 */
	public void setRemoveContexts(IRI... removeContexts) {
		this.removeContexts = removeContexts;
		if (next != null) {
			next.setRemoveContexts(removeContexts);
		}
	}

	/**
	 * Before Statements are removed, they are first copied to these contexts.
	 */
	@Deprecated
	public IRI[] getArchiveContexts() {
		return archiveContexts;
	}

	/**
	 * Before Statements are removed, they are first copied to these contexts.
	 */
	@Deprecated
	public void setArchiveContexts(IRI... archiveContexts) {
		this.archiveContexts = archiveContexts;
		if (next != null) {
			next.setArchiveContexts(archiveContexts);
		}
	}

	/**
	 * The default context to add the statements to. For INSERT/add operations Each statement is added to any context
	 * specified in the statement, or if the statement contains no context, it is added with the context specified here.
	 */
	public IRI getInsertContext() {
		return insertContext;
	}

	/**
	 * The default context to add the statements to. For INSERT/add operations Each statement is added to any context
	 * specified in the statement, or if the statement contains no context, it is added with the context specified here.
	 */
	public void setInsertContext(IRI insertContext) {
		this.insertContext = insertContext;
		this.addContexts = new IRI[] { insertContext };
		if (next != null) {
			next.setInsertContext(insertContext);
		}
	}

	public void add(File file, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		if (isNilContext(contexts) && !dataFormat.supportsContexts()) {
			super.add(file, getBaseURI(), dataFormat, getAddContexts());
		} else {
			super.add(file, getBaseURI(), dataFormat, contexts);
		}
	}

	@Override
	public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		if (baseURI == null) {
			baseURI = getBaseURI();
		}
		if (isNilContext(contexts) && !dataFormat.supportsContexts()) {
			super.add(file, baseURI, dataFormat, getAddContexts());
		} else {
			super.add(file, baseURI, dataFormat, contexts);
		}
	}

	public void add(InputStream in, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		if (isNilContext(contexts) && !dataFormat.supportsContexts()) {
			super.add(in, getBaseURI(), dataFormat, getAddContexts());
		} else {
			super.add(in, getBaseURI(), dataFormat, contexts);
		}
	}

	@Override
	public void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		if (baseURI == null) {
			baseURI = getBaseURI();
		}
		if (isNilContext(contexts) && !dataFormat.supportsContexts()) {
			super.add(in, baseURI, dataFormat, getAddContexts());
		} else {
			super.add(in, baseURI, dataFormat, contexts);
		}
	}

	@Override
	public void add(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {
		if (isNilContext(contexts)) {
			add(new CloseableIteratorIteration<>(statements.iterator()));
		} else {
			super.add(statements, contexts);
		}
	}

	@Override
	public <E extends Exception> void add(Iteration<? extends Statement, E> statementIter, Resource... contexts)
			throws RepositoryException, E {
		final IRI insertContext = getInsertContext();
		if (isNilContext(contexts)) {
			super.add(new ConvertingIteration<Statement, Statement, E>(statementIter) {

				@Override
				protected Statement convert(Statement st) {
					if (st.getContext() == null) {
						return getValueFactory().createStatement(st.getSubject(), st.getPredicate(), st.getObject(),
								insertContext);
					}
					return st;
				}
			});
		} else {
			super.add(statementIter, contexts);
		}
	}

	public void add(Reader reader, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		if (isNilContext(contexts) && !dataFormat.supportsContexts()) {
			super.add(reader, getBaseURI(), dataFormat, getAddContexts());
		} else {
			super.add(reader, getBaseURI(), dataFormat, contexts);
		}
	}

	@Override
	public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		if (baseURI == null) {
			baseURI = getBaseURI();
		}
		if (isNilContext(contexts) && !dataFormat.supportsContexts()) {
			super.add(reader, baseURI, dataFormat, getAddContexts());
		} else {
			super.add(reader, baseURI, dataFormat, contexts);
		}
	}

	@Override
	public void add(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException {
		if (isNilContext(contexts)) {
			super.add(subject, predicate, object, getAddContexts());
		} else {
			super.add(subject, predicate, object, contexts);
		}
	}

	@Override
	public void add(Statement st, Resource... contexts) throws RepositoryException {
		if (isNilContext(contexts) && st.getContext() == null) {
			super.add(st, getAddContexts());
		} else {
			super.add(st, contexts);
		}
	}

	public void add(URL url, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		if (isNilContext(contexts) && !dataFormat.supportsContexts()) {
			super.add(url, getBaseURI(), dataFormat, getAddContexts());
		} else {
			super.add(url, getBaseURI(), dataFormat, contexts);
		}
	}

	@Override
	public void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		if (baseURI == null) {
			baseURI = getBaseURI();
		}
		if (isNilContext(contexts) && !dataFormat.supportsContexts()) {
			super.add(url, baseURI, dataFormat, getAddContexts());
		} else {
			super.add(url, baseURI, dataFormat, contexts);
		}
	}

	@Override
	public void clear(Resource... contexts) throws RepositoryException {
		if (isAllContext(contexts)) {
			super.clear(getRemoveContexts());
		} else {
			super.clear(contexts);
		}
	}

	@Override
	public void export(RDFHandler handler, Resource... contexts) throws RepositoryException, RDFHandlerException {
		if (isAllContext(contexts)) {
			super.export(handler, getReadContexts());
		} else {
			super.export(handler, contexts);
		}
	}

	/**
	 * Exports all statements with a specific subject, predicate and/or object from the repository, optionally from the
	 * specified contexts.
	 *
	 * @param subj    The subject, or null if the subject doesn't matter.
	 * @param pred    The predicate, or null if the predicate doesn't matter.
	 * @param obj     The object, or null if the object doesn't matter.
	 * @param handler The handler that will handle the RDF data.
	 * @throws RDFHandlerException If the handler encounters an unrecoverable error.
	 * @see #getReadContexts()
	 * @see #isIncludeInferred()
	 */
	public void exportStatements(Resource subj, IRI pred, Value obj, RDFHandler handler, Resource... contexts)
			throws RepositoryException, RDFHandlerException {
		if (isAllContext(contexts)) {
			super.exportStatements(subj, pred, obj, isIncludeInferred(), handler, getReadContexts());
		} else {
			super.exportStatements(subj, pred, obj, isIncludeInferred(), handler, contexts);
		}
	}

	@Override
	public void exportStatements(Resource subj, IRI pred, Value obj, boolean includeInferred, RDFHandler handler,
			Resource... contexts) throws RepositoryException, RDFHandlerException {
		if (isAllContext(contexts)) {
			super.exportStatements(subj, pred, obj, includeInferred, handler, getReadContexts());
		} else {
			super.exportStatements(subj, pred, obj, includeInferred, handler, contexts);
		}
	}

	/**
	 * Gets all statements with a specific subject, predicate and/or object from the repository. The result is
	 * optionally restricted to the specified set of named contexts.
	 *
	 * @param subj A Resource specifying the subject, or <var>null</var> for a wildcard.
	 * @param pred A URI specifying the predicate, or <var>null</var> for a wildcard.
	 * @param obj  A Value specifying the object, or <var>null</var> for a wildcard.
	 * @return The statements matching the specified pattern. The result object is a {@link RepositoryResult} object, a
	 *         lazy Iterator-like object containing {@link Statement}s and optionally throwing a
	 *         {@link RepositoryException} when an error when a problem occurs during retrieval.
	 * @see #getReadContexts()
	 * @see #isIncludeInferred()
	 */
	@Override
	public RepositoryResult<Statement> getStatements(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws RepositoryException {
		if (isAllContext(contexts)) {
			return super.getStatements(subj, pred, obj, isIncludeInferred(), getReadContexts());
		} else {
			return super.getStatements(subj, pred, obj, isIncludeInferred(), contexts);
		}
	}

	@Override
	public RepositoryResult<Statement> getStatements(Resource subj, IRI pred, Value obj, boolean includeInferred,
			Resource... contexts) throws RepositoryException {
		if (isAllContext(contexts)) {
			return super.getStatements(subj, pred, obj, includeInferred, getReadContexts());
		} else {
			return super.getStatements(subj, pred, obj, includeInferred, contexts);
		}
	}

	@Override
	public boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		if (isAllContext(contexts)) {
			return super.hasStatement(subj, pred, obj, includeInferred, getReadContexts());
		} else {
			return super.hasStatement(subj, pred, obj, includeInferred, contexts);
		}
	}

	@Override
	public boolean hasStatement(Statement st, boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		if (isAllContext(contexts) && st.getContext() == null) {
			return super.hasStatement(st, includeInferred, getReadContexts());
		} else {
			return super.hasStatement(st, includeInferred, contexts);
		}
	}

	/**
	 * Checks whether the repository contains statements with a specific subject, predicate and/or object, optionally in
	 * the specified contexts.
	 *
	 * @param subj A Resource specifying the subject, or <var>null</var> for a wildcard.
	 * @param pred A URI specifying the predicate, or <var>null</var> for a wildcard.
	 * @param obj  A Value specifying the object, or <var>null</var> for a wildcard.
	 * @return true If a matching statement is in the repository in the specified context, false otherwise.
	 * @see #getReadContexts()
	 * @see #isIncludeInferred()
	 */
	public boolean hasStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws RepositoryException {
		if (isAllContext(contexts)) {
			return super.hasStatement(subj, pred, obj, isIncludeInferred(), getReadContexts());
		} else {
			return super.hasStatement(subj, pred, obj, isIncludeInferred(), contexts);
		}
	}

	/**
	 * Checks whether the repository contains the specified statement, optionally in the specified contexts.
	 *
	 * @param st The statement to look for. Context information in the statement is ignored.
	 * @return true If the repository contains the specified statement, false otherwise.
	 * @see #getReadContexts()
	 * @see #isIncludeInferred()
	 */
	public boolean hasStatement(Statement st, Resource... contexts) throws RepositoryException {
		if (isAllContext(contexts) && st.getContext() == null) {
			return super.hasStatement(st, isIncludeInferred(), getReadContexts());
		} else {
			return super.hasStatement(st, isIncludeInferred(), contexts);
		}
	}

	@Override
	public GraphQuery prepareGraphQuery(String query) throws MalformedQueryException, RepositoryException {
		return prepareGraphQuery(getQueryLanguage(), query);
	}

	@Override
	public Query prepareQuery(String query) throws MalformedQueryException, RepositoryException {
		return prepareQuery(getQueryLanguage(), query);
	}

	@Override
	public TupleQuery prepareTupleQuery(String query) throws MalformedQueryException, RepositoryException {
		return prepareTupleQuery(getQueryLanguage(), query);
	}

	@Override
	public Update prepareUpdate(String query) throws MalformedQueryException, RepositoryException {
		return prepareUpdate(getQueryLanguage(), query);
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query)
			throws MalformedQueryException, RepositoryException {
		return prepareGraphQuery(ql, query, getBaseURI());
	}

	@Override
	public Query prepareQuery(QueryLanguage ql, String query) throws MalformedQueryException, RepositoryException {
		return prepareQuery(ql, query, getBaseURI());
	}

	@Override
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query)
			throws MalformedQueryException, RepositoryException {
		return prepareTupleQuery(ql, query, getBaseURI());
	}

	@Override
	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query)
			throws MalformedQueryException, RepositoryException {
		return prepareBooleanQuery(ql, query, getBaseURI());
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String query) throws MalformedQueryException, RepositoryException {
		return prepareUpdate(ql, query, getBaseURI());
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query, String baseURI)
			throws MalformedQueryException, RepositoryException {
		if (baseURI == null) {
			baseURI = getBaseURI();
		}
		return initQuery(super.prepareGraphQuery(ql, query, baseURI));
	}

	@Override
	public Query prepareQuery(QueryLanguage ql, String query, String baseURI)
			throws MalformedQueryException, RepositoryException {
		if (baseURI == null) {
			baseURI = getBaseURI();
		}
		return initQuery(super.prepareQuery(ql, query, baseURI));
	}

	@Override
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query, String baseURI)
			throws MalformedQueryException, RepositoryException {
		if (baseURI == null) {
			baseURI = getBaseURI();
		}
		return initQuery(super.prepareTupleQuery(ql, query, baseURI));
	}

	@Override
	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query, String baseURI)
			throws MalformedQueryException, RepositoryException {
		if (baseURI == null) {
			baseURI = getBaseURI();
		}
		return initQuery(super.prepareBooleanQuery(ql, query, baseURI));
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String update, String baseURI)
			throws MalformedQueryException, RepositoryException {
		if (baseURI == null) {
			baseURI = getBaseURI();
		}
		return initOperation(super.prepareUpdate(ql, update, baseURI));
	}

	@Override
	public void remove(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {
		if (isAllContext(contexts)) {
			remove(new CloseableIteratorIteration<>(statements.iterator()));
		} else {
			super.remove(statements, contexts);
		}
	}

	/**
	 * Removes the supplied statements from a specific context in this repository, ignoring any context information
	 * carried by the statements themselves.
	 *
	 * @param statementIter The statements to remove. In case the iterator is a {@link CloseableIteration}, it will be
	 *                      closed before this method returns.
	 * @throws RepositoryException If the statements could not be removed from the repository, for example because the
	 *                             repository is not writable.
	 * @see #getRemoveContexts()
	 */
	@Override
	public <E extends Exception> void remove(Iteration<? extends Statement, E> statementIter, Resource... contexts)
			throws RepositoryException, E {
		final IRI[] removeContexts = getRemoveContexts();
		if (isAllContext(contexts) && removeContexts.length == 1) {
			super.remove(new ConvertingIteration<Statement, Statement, E>(statementIter) {

				@Override
				protected Statement convert(Statement st) {
					if (st.getContext() == null) {
						return getValueFactory().createStatement(st.getSubject(), st.getPredicate(), st.getObject(),
								removeContexts[0]);
					}
					return st;
				}
			});
		} else {
			super.remove(statementIter, contexts);
		}
	}

	/**
	 * Removes the statement with the specified subject, predicate and object from the repository, optionally restricted
	 * to the specified contexts.
	 *
	 * @param subject   The statement's subject.
	 * @param predicate The statement's predicate.
	 * @param object    The statement's object.
	 * @throws RepositoryException If the statement could not be removed from the repository, for example because the
	 *                             repository is not writable.
	 * @see #getRemoveContexts()
	 */
	@Override
	public void remove(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException {
		if (isAllContext(contexts)) {
			super.remove(subject, predicate, object, getRemoveContexts());
		} else {
			super.remove(subject, predicate, object, contexts);
		}
	}

	/**
	 * Removes the supplied statement from the specified contexts in the repository.
	 *
	 * @param st The statement to remove.
	 * @throws RepositoryException If the statement could not be removed from the repository, for example because the
	 *                             repository is not writable.
	 * @see #getRemoveContexts()
	 */
	@Override
	public void remove(Statement st, Resource... contexts) throws RepositoryException {
		if (isAllContext(contexts) && st.getContext() == null) {
			super.remove(st, getRemoveContexts());
		} else {
			super.remove(st, contexts);
		}
	}

	/**
	 * Returns the number of (explicit) statements that are in the specified contexts in this repository.
	 *
	 * @return The number of explicit statements from the specified contexts in this repository.
	 * @see #getReadContexts()
	 */
	@Override
	public long size(Resource... contexts) throws RepositoryException {
		if (isAllContext(contexts)) {
			return super.size(getReadContexts());
		} else {
			return super.size(contexts);
		}
	}

	@Override
	protected void removeWithoutCommit(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		IRI[] archiveContexts = getArchiveContexts();
		if (archiveContexts.length > 0) {
			RDFHandler handler = new RDFInserter(getDelegate());
			try {
				getDelegate().exportStatements(subject, predicate, object, true, handler, archiveContexts);
			} catch (RDFHandlerException e) {
				if (e.getCause() instanceof RepositoryException) {
					throw (RepositoryException) e.getCause();
				}
				throw new AssertionError(e);
			}
		}
		if (isAllContext(contexts)) {
			getDelegate().remove(subject, predicate, object, getRemoveContexts());
		} else {
			getDelegate().remove(subject, predicate, object, contexts);
		}
	}

	private <O extends Query> O initQuery(O query) {
		initOperation(query);
		query.setMaxQueryTime(getMaxQueryTime());
		return query;
	}

	private <O extends Operation> O initOperation(O op) {
		IRI[] readContexts = getReadContexts();
		IRI[] removeContexts = getRemoveContexts();
		IRI insertContext = getInsertContext();
		if (readContexts.length > 0 || removeContexts.length > 0 || insertContext != null) {
			SimpleDataset ds = new SimpleDataset();
			for (IRI graph : readContexts) {
				ds.addDefaultGraph(graph);
			}
			for (IRI graph : removeContexts) {
				ds.addDefaultRemoveGraph(graph);
			}
			ds.setDefaultInsertGraph(insertContext);
			op.setDataset(ds);
		}

		op.setIncludeInferred(isIncludeInferred());

		return op;
	}

	private boolean isNilContext(Resource[] contexts) {
		return isAllContext(contexts) || contexts.length == 1 && contexts[0] == null;
	}

	private boolean isAllContext(Resource[] contexts) {
		return contexts == null || contexts.length == 0;
	}

}
