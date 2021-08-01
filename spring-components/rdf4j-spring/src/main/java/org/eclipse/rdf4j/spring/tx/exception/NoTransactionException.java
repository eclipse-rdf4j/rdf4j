package org.eclipse.rdf4j.spring.tx.exception;

public class NoTransactionException extends Rdf4JTransactionException {
	public NoTransactionException() {
	}

	public NoTransactionException(String message) {
		super(message);
	}

	public NoTransactionException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoTransactionException(Throwable cause) {
		super(cause);
	}

	public NoTransactionException(
			String message,
			Throwable cause,
			boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
