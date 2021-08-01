package org.eclipse.rdf4j.spring.resultcache;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.spring.support.connectionfactory.DelegatingRepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.support.connectionfactory.RepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.util.RepositoryConnectionWrappingUtils;

public class CachingRepositoryConnectionFactory extends DelegatingRepositoryConnectionFactory {
	public CachingRepositoryConnectionFactory(
			RepositoryConnectionFactory delegate, ResultCacheProperties properties) {
		super(delegate);
		this.properties = properties;
		this.globalGraphQueryResultCache = new LRUResultCache<>(properties);
		this.globalTupleQueryResultCache = new LRUResultCache<>(properties);
	}

	private LRUResultCache<ReusableTupleQueryResult> globalTupleQueryResultCache;
	private LRUResultCache<ReusableGraphQueryResult> globalGraphQueryResultCache;

	private ResultCacheProperties properties;

	@Override
	public RepositoryConnection getConnection() {
		return RepositoryConnectionWrappingUtils.wrapOnce(
				getDelegate().getConnection(),
				con -> new CachingRepositoryConnection(
						con,
						globalTupleQueryResultCache,
						globalGraphQueryResultCache,
						properties),
				CachingRepositoryConnection.class);
	}
}
