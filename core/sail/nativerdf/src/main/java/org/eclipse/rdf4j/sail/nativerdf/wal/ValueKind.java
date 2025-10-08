package org.eclipse.rdf4j.sail.nativerdf.wal;

/**
 * Enumeration of value kinds that may be persisted in the value store WAL.
 */
public enum ValueKind {

	IRI('I'),
	BNODE('B'),
	LITERAL('L'),
	NAMESPACE('N');

	private final char code;

	ValueKind(char code) {
		this.code = code;
	}

	public char code() {
		return code;
	}

	public static ValueKind fromCode(String code) {
		if (code == null || code.isEmpty()) {
			throw new IllegalArgumentException("Missing value kind code");
		}
		char c = code.charAt(0);
		for (ValueKind kind : values()) {
			if (kind.code == c) {
				return kind;
			}
		}
		throw new IllegalArgumentException("Unknown value kind code: " + code);
	}
}
