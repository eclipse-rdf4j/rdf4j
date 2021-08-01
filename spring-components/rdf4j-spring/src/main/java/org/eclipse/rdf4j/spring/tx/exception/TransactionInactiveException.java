package org.eclipse.rdf4j.spring.tx.exception;

public class TransactionInactiveException extends Rdf4JTransactionException {
	public TransactionInactiveException() {
	}

	public TransactionInactiveException(String message) {
		super(message);
	}

	public TransactionInactiveException(String message, Throwable cause) {
		super(message, cause);
	}

	public TransactionInactiveException(Throwable cause) {
		super(cause);
	}

	public TransactionInactiveException(
			String message,
			Throwable cause,
			boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
