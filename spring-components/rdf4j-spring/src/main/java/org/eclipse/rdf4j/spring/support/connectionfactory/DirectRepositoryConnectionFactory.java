package org.eclipse.rdf4j.spring.support.connectionfactory;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class DirectRepositoryConnectionFactory implements RepositoryConnectionFactory {
	private Repository repository;

	public DirectRepositoryConnectionFactory(Repository repository) {
		this.repository = repository;
	}

	@Override
	public RepositoryConnection getConnection() {
		return repository.getConnection();
	}
}
