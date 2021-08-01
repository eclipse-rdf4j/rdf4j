package org.eclipse.rdf4j.spring.dao.exception;

public class IncorrectResultSetSizeException extends Rdf4JDaoException {
	int expectedSize;
	int actualSize;

	public IncorrectResultSetSizeException(int expectedSize, int actualSize) {
		super(makeMessage(expectedSize, actualSize));
		this.expectedSize = expectedSize;
		this.actualSize = actualSize;
	}

	private static String makeMessage(int expectedSize, int actualSize) {
		return String.format("Expected %d results but got %d", expectedSize, actualSize);
	}

	public IncorrectResultSetSizeException(String message, int expectedSize, int actualSize) {
		super(message);
		this.expectedSize = expectedSize;
		this.actualSize = actualSize;
	}

	public IncorrectResultSetSizeException(
			String message, Throwable cause, int expectedSize, int actualSize) {
		super(message, cause);
		this.expectedSize = expectedSize;
		this.actualSize = actualSize;
	}

	public IncorrectResultSetSizeException(Throwable cause, int expectedSize, int actualSize) {
		super(makeMessage(expectedSize, actualSize), cause);
		this.expectedSize = expectedSize;
		this.actualSize = actualSize;
	}

	public IncorrectResultSetSizeException(
			String message,
			Throwable cause,
			boolean enableSuppression,
			boolean writableStackTrace,
			int expectedSize,
			int actualSize) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.expectedSize = expectedSize;
		this.actualSize = actualSize;
	}

	public int getExpectedSize() {
		return expectedSize;
	}

	public int getActualSize() {
		return actualSize;
	}
}
