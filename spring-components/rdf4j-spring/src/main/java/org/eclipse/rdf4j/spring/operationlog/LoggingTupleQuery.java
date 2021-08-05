/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.spring.operationlog;

import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.spring.operationlog.log.OperationLog;
import org.eclipse.rdf4j.spring.support.query.DelegatingTupleQuery;

public class LoggingTupleQuery extends DelegatingTupleQuery {

	private OperationLog operationLog;

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
