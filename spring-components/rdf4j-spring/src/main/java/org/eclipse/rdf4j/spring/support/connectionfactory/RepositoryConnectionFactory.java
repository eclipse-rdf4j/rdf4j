package org.eclipse.rdf4j.spring.support.connectionfactory;

import org.eclipse.rdf4j.repository.RepositoryConnection;

public interface RepositoryConnectionFactory {
	RepositoryConnection getConnection();
}
