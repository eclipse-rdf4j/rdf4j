/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.resultcache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
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
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class CachingRepositoryConnection extends RepositoryConnectionWrapper implements Clearable {
	private final LRUResultCache<ReusableTupleQueryResult> localTupleQueryResultCache;
	private final LRUResultCache<ReusableGraphQueryResult> localGraphQueryResultCache;
	private final LRUResultCache<ReusableTupleQueryResult> globalTupleQueryResultCache;
	private final LRUResultCache<ReusableGraphQueryResult> globalGraphQueryResultCache;
	private final ResultCacheProperties properties;
	private boolean clearGlobalResultCacheOnClose = false;

	public CachingRepositoryConnection(
			RepositoryConnection delegate,
			LRUResultCache<ReusableTupleQueryResult> globalTupleQueryResultCache,
			LRUResultCache<ReusableGraphQueryResult> globalGraphQueryResultCache,
			ResultCacheProperties properties) {
		super(delegate.getRepository(), delegate);
		this.globalGraphQueryResultCache = globalGraphQueryResultCache;
		this.globalTupleQueryResultCache = globalTupleQueryResultCache;
		this.localGraphQueryResultCache = new LRUResultCache<>(properties);
		this.localTupleQueryResultCache = new LRUResultCache<>(properties);
		this.properties = properties;
	}

	private Integer makeCacheKey(QueryLanguage ql, String query, String baseURI) {
		return (ql.toString() + query + baseURI).hashCode();
	}

	public void renewLocalResultCache(ResultCachingTupleQuery resultCachingTupleQuery) {
		resultCachingTupleQuery.renewLocalResultCache(this.localTupleQueryResultCache);
	}

	public void renewLocalResultCache(ResultCachingGraphQuery resultCachingGraphQuery) {
		resultCachingGraphQuery.renewLocalResultCache(this.localGraphQueryResultCache);
	}

	public void renewClearable(ClearableAwareUpdate update) {
		update.renewClearable(this);
	}

	@Override
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String queryString, String baseURI)
			throws MalformedQueryException, RepositoryException {
		return new ResultCachingTupleQuery(
				getDelegate().prepareTupleQuery(ql, queryString, baseURI),
				this.localTupleQueryResultCache,
				this.globalTupleQueryResultCache,
				properties);
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String queryString, String baseURI)
			throws MalformedQueryException, RepositoryException {
		return new ResultCachingGraphQuery(
				getDelegate().prepareGraphQuery(ql, queryString, baseURI),
				this.localGraphQueryResultCache,
				this.globalGraphQueryResultCache,
				this.properties);
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String updateString, String baseURI)
			throws MalformedQueryException, RepositoryException {
		return new ClearableAwareUpdate(
				getDelegate().prepareUpdate(ql, updateString, baseURI), this);
	}

	@Override
	public void close() throws RepositoryException {
		this.localGraphQueryResultCache.markDirty();
		this.localTupleQueryResultCache.markDirty();
		if (this.clearGlobalResultCacheOnClose) {
			this.globalGraphQueryResultCache.markDirty();
			this.globalTupleQueryResultCache.markDirty();
		}
		super.close();
	}

	/** As we are changing the repository's content, we have to reset all caches (even though it */
	@Override
	public void markDirty() {
		this.localGraphQueryResultCache.markDirty();
		this.localTupleQueryResultCache.markDirty();
		this.globalTupleQueryResultCache.bypassForCurrentThread();
		this.globalGraphQueryResultCache.bypassForCurrentThread();
		this.clearGlobalResultCacheOnClose = true;
	}

	@Override
	public void clearCachedResults() {
		this.localGraphQueryResultCache.clearCachedResults();
		this.localTupleQueryResultCache.clearCachedResults();
	}

	@Override
	public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		super.add(file, baseURI, dataFormat, contexts);
		markDirty();
	}

	@Override
	public void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		super.add(in, baseURI, dataFormat, contexts);
		markDirty();
	}

	@Override
	public void add(Iterable<? extends Statement> statements, Resource... contexts)
			throws RepositoryException {
		super.add(statements, contexts);
		markDirty();
	}

	@Override
	public <E extends Exception> void add(
			Iteration<? extends Statement, E> statementIter, Resource... contexts)
			throws RepositoryException, E {
		super.add(statementIter, contexts);
		markDirty();
	}

	@Override
	public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		super.add(reader, baseURI, dataFormat, contexts);
		markDirty();
	}

	@Override
	public void add(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		super.add(subject, predicate, object, contexts);
		markDirty();
	}

	@Override
	public void add(Statement st, Resource... contexts) throws RepositoryException {
		super.add(st, contexts);
		markDirty();
	}

	@Override
	public void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		super.add(url, baseURI, dataFormat, contexts);
		markDirty();
	}

	@Override
	public void clear(Resource... contexts) throws RepositoryException {
		super.clear(contexts);
		markDirty();
	}

	@Override
	public void remove(Iterable<? extends Statement> statements, Resource... contexts)
			throws RepositoryException {
		super.remove(statements, contexts);
		markDirty();
	}

	@Override
	public <E extends Exception> void remove(
			Iteration<? extends Statement, E> statementIter, Resource... contexts)
			throws RepositoryException, E {
		super.remove(statementIter, contexts);
		markDirty();
	}

	@Override
	public void remove(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		super.remove(subject, predicate, object, contexts);
		markDirty();
	}

	@Override
	public void remove(Statement st, Resource... contexts) throws RepositoryException {
		super.remove(st, contexts);
		markDirty();
	}

	@Override
	public void removeNamespace(String prefix) throws RepositoryException {
		super.removeNamespace(prefix);
		markDirty();
	}
}
