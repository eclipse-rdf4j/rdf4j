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

/**
 * Enumeration of value kinds that may be persisted in the value store WAL.
 */
public enum ValueStoreWalValueKind {

	IRI('I'),
	BNODE('B'),
	LITERAL('L'),
	NAMESPACE('N');

	private final char code;

	ValueStoreWalValueKind(char code) {
		this.code = code;
	}

	public char code() {
		return code;
	}

	public static ValueStoreWalValueKind fromCode(String code) {
		if (code == null || code.isEmpty()) {
			throw new IllegalArgumentException("Missing value kind code");
		}
		char c = code.charAt(0);
		for (ValueStoreWalValueKind kind : values()) {
			if (kind.code == c) {
				return kind;
			}
		}
		throw new IllegalArgumentException("Unknown value kind code: " + code);
	}
}
