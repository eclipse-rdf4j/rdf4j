package org.eclipse.rdf4j.spring.tx.exception;

public class RollbackException extends Rdf4JTransactionException {
	public RollbackException() {
	}

	public RollbackException(String message) {
		super(message);
	}

	public RollbackException(String message, Throwable cause) {
		super(message, cause);
	}

	public RollbackException(Throwable cause) {
		super(cause);
	}

	public RollbackException(
			String message,
			Throwable cause,
			boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
