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
