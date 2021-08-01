package org.eclipse.rdf4j.spring.support.connectionfactory;

import org.eclipse.rdf4j.repository.RepositoryConnection;

public abstract class DelegatingRepositoryConnectionFactory implements RepositoryConnectionFactory {
	private RepositoryConnectionFactory delegate;

	public DelegatingRepositoryConnectionFactory(RepositoryConnectionFactory delegate) {
		this.delegate = delegate;
	}

	@Override
	public RepositoryConnection getConnection() {
		return delegate.getConnection();
	}

	protected RepositoryConnectionFactory getDelegate() {
		return delegate;
	}
}
