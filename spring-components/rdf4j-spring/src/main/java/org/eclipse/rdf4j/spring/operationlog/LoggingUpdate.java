package org.eclipse.rdf4j.spring.operationlog;

import java.lang.invoke.MethodHandles;

import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.spring.operationlog.log.OperationLog;
import org.eclipse.rdf4j.spring.support.query.DelegatingUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingUpdate extends DelegatingUpdate {

	private OperationLog operationLog;

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public LoggingUpdate(Update delegate, OperationLog operationLog) {
		super(delegate);
		this.operationLog = operationLog;
	}

	@Override
	public void execute() throws UpdateExecutionException {
		operationLog.runWithLog(getDelegate(), () -> getDelegate().execute());
	}
}
