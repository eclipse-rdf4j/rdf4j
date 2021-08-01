package org.eclipse.rdf4j.spring.dao.exception;

public class UnsupportedDataTypeException extends UnexpectedResultException {
	public UnsupportedDataTypeException() {
	}

	public UnsupportedDataTypeException(String message) {
		super(message);
	}

	public UnsupportedDataTypeException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnsupportedDataTypeException(Throwable cause) {
		super(cause);
	}

	public UnsupportedDataTypeException(
			String message,
			Throwable cause,
			boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
