/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.operationlog;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.spring.operationlog.log.OperationLog;
import org.eclipse.rdf4j.spring.support.connectionfactory.DelegatingRepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.support.connectionfactory.RepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.util.RepositoryConnectionWrappingUtils;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class LoggingRepositoryConnectionFactory extends DelegatingRepositoryConnectionFactory {

	private final OperationLog operationLog;

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
