package org.eclipse.rdf4j.spring.tx.exception;

public class RepositoryConnectionPoolException extends RuntimeException {
	public RepositoryConnectionPoolException() {
	}

	public RepositoryConnectionPoolException(String message) {
		super(message);
	}

	public RepositoryConnectionPoolException(String message, Throwable cause) {
		super(message, cause);
	}

	public RepositoryConnectionPoolException(Throwable cause) {
		super(cause);
	}

	public RepositoryConnectionPoolException(
			String message,
			Throwable cause,
			boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
