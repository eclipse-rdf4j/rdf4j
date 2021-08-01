package org.eclipse.rdf4j.spring.dao.exception;

public class UnexpectedResultException extends Rdf4JDaoException {
	public UnexpectedResultException() {
	}

	public UnexpectedResultException(String message) {
		super(message);
	}

	public UnexpectedResultException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnexpectedResultException(Throwable cause) {
		super(cause);
	}

	public UnexpectedResultException(
			String message,
			Throwable cause,
			boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
