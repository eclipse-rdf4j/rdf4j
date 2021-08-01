package org.eclipse.rdf4j.spring.tx.exception;

public class Rdf4JTransactionException extends RuntimeException {
	public Rdf4JTransactionException() {
	}

	public Rdf4JTransactionException(String message) {
		super(message);
	}

	public Rdf4JTransactionException(String message, Throwable cause) {
		super(message, cause);
	}

	public Rdf4JTransactionException(Throwable cause) {
		super(cause);
	}

	public Rdf4JTransactionException(
			String message,
			Throwable cause,
			boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
