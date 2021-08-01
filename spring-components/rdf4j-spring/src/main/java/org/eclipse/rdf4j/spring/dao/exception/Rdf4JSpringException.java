package org.eclipse.rdf4j.spring.dao.exception;

public class Rdf4JSpringException extends RuntimeException {
	public Rdf4JSpringException() {
	}

	public Rdf4JSpringException(String message) {
		super(message);
	}

	public Rdf4JSpringException(String message, Throwable cause) {
		super(message, cause);
	}

	public Rdf4JSpringException(Throwable cause) {
		super(cause);
	}

	public Rdf4JSpringException(
			String message,
			Throwable cause,
			boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
