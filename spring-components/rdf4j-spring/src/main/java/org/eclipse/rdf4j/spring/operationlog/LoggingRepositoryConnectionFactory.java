package org.eclipse.rdf4j.spring.operationlog;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.spring.operationlog.log.OperationLog;
import org.eclipse.rdf4j.spring.support.connectionfactory.DelegatingRepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.support.connectionfactory.RepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.util.RepositoryConnectionWrappingUtils;

public class LoggingRepositoryConnectionFactory extends DelegatingRepositoryConnectionFactory {

	private OperationLog operationLog;

	public LoggingRepositoryConnectionFactory(
			RepositoryConnectionFactory delegate, OperationLog operationLog) {
		super(delegate);
		this.operationLog = operationLog;
	}

	@Override
	public RepositoryConnection getConnection() {
		return RepositoryConnectionWrappingUtils.wrapOnce(
				getDelegate().getConnection(),
				con -> new LoggingRepositoryConnection(con, operationLog),
				LoggingRepositoryConnection.class);
	}
}
