/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.wal;

import java.util.Objects;

/**
 * Representation of a single ValueStore WAL record describing a minted value.
 */
public final class ValueStoreWalRecord {

	private final long lsn;
	private final int id;
	private final ValueStoreWalValueKind valueKind;
	private final String lexical;
	private final String datatype;
	private final String language;
	private final int hash;

	public ValueStoreWalRecord(long lsn, int id, ValueStoreWalValueKind valueKind, String lexical, String datatype,
			String language, int hash) {
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

	public ValueStoreWalValueKind valueKind() {
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
