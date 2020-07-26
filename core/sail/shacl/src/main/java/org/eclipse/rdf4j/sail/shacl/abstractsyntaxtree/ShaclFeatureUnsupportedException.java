package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree;

public class ShaclFeatureUnsupportedException extends UnsupportedOperationException {
	public ShaclFeatureUnsupportedException() {
	}

	public ShaclFeatureUnsupportedException(String message) {
		super(message);
	}

	public ShaclFeatureUnsupportedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ShaclFeatureUnsupportedException(Throwable cause) {
		super(cause);
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}
}
