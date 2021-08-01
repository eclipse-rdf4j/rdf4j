package org.eclipse.rdf4j.spring.tx.exception;

public class ConnectionClosedException extends Rdf4JTransactionException {
	public ConnectionClosedException() {
	}

	public ConnectionClosedException(String message) {
		super(message);
	}

	public ConnectionClosedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConnectionClosedException(Throwable cause) {
		super(cause);
	}

	public ConnectionClosedException(
			String message,
			Throwable cause,
			boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
