package org.eclipse.rdf4j.spring.dao.exception;

public class Rdf4JDaoException extends Rdf4JSpringException {
	public Rdf4JDaoException() {
	}

	public Rdf4JDaoException(String message) {
		super(message);
	}

	public Rdf4JDaoException(String message, Throwable cause) {
		super(message, cause);
	}

	public Rdf4JDaoException(Throwable cause) {
		super(cause);
	}

	public Rdf4JDaoException(
			String message,
			Throwable cause,
			boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
