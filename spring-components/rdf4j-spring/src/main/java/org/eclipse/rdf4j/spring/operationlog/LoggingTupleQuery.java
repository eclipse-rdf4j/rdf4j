/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.operationlog;

import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.spring.operationlog.log.OperationLog;
import org.eclipse.rdf4j.spring.support.query.DelegatingTupleQuery;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class LoggingTupleQuery extends DelegatingTupleQuery {

	private final OperationLog operationLog;

	public LoggingTupleQuery(TupleQuery delegate, OperationLog operationLog) {
		super(delegate);
		this.operationLog = operationLog;
	}

	@Override
	public TupleQueryResult evaluate() throws QueryEvaluationException {
		return operationLog.runWithLog(getDelegate(), () -> getDelegate().evaluate());
	}

	@Override
	public void evaluate(TupleQueryResultHandler handler)
			throws QueryEvaluationException, TupleQueryResultHandlerException {
		operationLog.runWithLog(getDelegate(), () -> getDelegate().evaluate(handler));
	}
}
