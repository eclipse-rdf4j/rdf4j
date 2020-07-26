package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree;

public class ShaclUnsupportedException extends UnsupportedOperationException {
	public ShaclUnsupportedException() {
	}

	public ShaclUnsupportedException(String message) {
		super(message);
	}

	public ShaclUnsupportedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ShaclUnsupportedException(Throwable cause) {
		super(cause);
	}

//	@Override
//	public synchronized Throwable fillInStackTrace() {
//		return this;
//	}
}
