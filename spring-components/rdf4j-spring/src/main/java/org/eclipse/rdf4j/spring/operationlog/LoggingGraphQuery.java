/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.operationlog;

import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.spring.operationlog.log.OperationLog;
import org.eclipse.rdf4j.spring.support.query.DelegatingGraphQuery;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class LoggingGraphQuery extends DelegatingGraphQuery {

	private final OperationLog operationLog;

	public LoggingGraphQuery(GraphQuery delegate, OperationLog operationLog) {
		super(delegate);
		this.operationLog = operationLog;
	}

	@Override
	public GraphQueryResult evaluate() throws QueryEvaluationException {
		return operationLog.runWithLog(getDelegate(), () -> getDelegate().evaluate());
	}

	@Override
	public void evaluate(RDFHandler handler) throws QueryEvaluationException, RDFHandlerException {
		operationLog.runWithLog(getDelegate(), () -> getDelegate().evaluate(handler));
	}
}
