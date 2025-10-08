package org.eclipse.rdf4j.sail.nativerdf.wal;

import java.util.Objects;

/**
 * Representation of a single WAL record describing a minted value.
 */
public final class WalRecord {

	private final long lsn;
	private final int id;
	private final ValueKind valueKind;
	private final String lexical;
	private final String datatype;
	private final String language;
	private final int hash;

	public WalRecord(long lsn, int id, ValueKind valueKind, String lexical, String datatype, String language,
			int hash) {
		this.lsn = lsn;
		this.id = id;
		this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
		this.lexical = lexical == null ? "" : lexical;
		this.datatype = datatype == null ? "" : datatype;
		this.language = language == null ? "" : language;
		this.hash = hash;
	}

	public long lsn() {
		return lsn;
	}

	public int id() {
		return id;
	}

	public ValueKind valueKind() {
		return valueKind;
	}

	public String lexical() {
		return lexical;
	}

	public String datatype() {
		return datatype;
	}

	public String language() {
		return language;
	}

	public int hash() {
		return hash;
	}
}
