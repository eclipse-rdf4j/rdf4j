package org.eclipse.rdf4j.spring.operationlog;

import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.spring.operationlog.log.OperationLog;
import org.eclipse.rdf4j.spring.support.query.DelegatingGraphQuery;

public class LoggingGraphQuery extends DelegatingGraphQuery {

	private OperationLog operationLog;

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
