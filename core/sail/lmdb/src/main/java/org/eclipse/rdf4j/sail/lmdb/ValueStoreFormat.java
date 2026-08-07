/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

/**
 * The value-ID and value-record format used by an LMDB store.
 * <p>
 * A store must use one format consistently for its value dictionary and every statement index. Legacy IDs cannot be
 * translated lazily because the raw IDs are embedded directly in each SPOC/POSC/etc. key.
 */
enum ValueStoreFormat {
	CURRENT((byte) 4, (byte) 5, (byte) 6, (byte) 7, true, true, true) {
		@Override
		long createId(byte valueType, long value) {
			int idType = switch (valueType) {
			case URI_VALUE -> ValueIds.T_URI;
			case LITERAL_VALUE -> ValueIds.T_LITERAL;
			case BNODE_VALUE -> ValueIds.T_BNODE;
			case TRIPLE_VALUE -> ValueIds.T_TRIPLE;
			case CURRENT_NAMESPACE_VALUE -> ValueIds.T_PTR;
			default -> throw new IllegalArgumentException("Unexpected value type: " + valueType);
			};
			return ValueIds.createId(idType, value);
		}

		@Override
		long getValue(long id) {
			return ValueIds.getValue(id);
		}

		@Override
		IdKind getKind(long id) {
			int idType = ValueIds.getIdType(id);
			if (idType == ValueIds.T_DOUBLE || idType >= ValueIds.T_INTEGER) {
				return IdKind.LITERAL;
			}
			return switch (idType) {
			case ValueIds.T_URI -> IdKind.IRI;
			case ValueIds.T_LITERAL -> IdKind.LITERAL;
			case ValueIds.T_BNODE -> IdKind.BNODE;
			case ValueIds.T_TRIPLE -> IdKind.TRIPLE;
			default -> IdKind.INTERNAL;
			};
		}

		@Override
		boolean isInlined(long id) {
			return ValueIds.isInlined(id);
		}
	},

	/** RDF4J 5.3.x: {@code id = sequence << 2 | type}. */
	LEGACY_5_3((byte) 3, (byte) 4, (byte) 5, (byte) 6, false, false, false) {
		@Override
		long createId(byte valueType, long value) {
			if (valueType < URI_VALUE || valueType > LEGACY_NAMESPACE_VALUE) {
				throw new IllegalArgumentException("Unexpected legacy value type: " + valueType);
			}
			return value << 2 | (valueType & 0x3L);
		}

		@Override
		long getValue(long id) {
			return id >>> 2;
		}

		@Override
		IdKind getKind(long id) {
			return switch ((int) (id & 0x3L)) {
			case URI_VALUE -> IdKind.IRI;
			case LITERAL_VALUE -> IdKind.LITERAL;
			case BNODE_VALUE -> IdKind.BNODE;
			default -> IdKind.INTERNAL; // namespace IDs are internal values
			};
		}

		@Override
		boolean isInlined(long id) {
			return false;
		}
	};

	private static final byte URI_VALUE = 0;
	private static final byte LITERAL_VALUE = 1;
	private static final byte BNODE_VALUE = 2;
	private static final byte TRIPLE_VALUE = 3;
	private static final byte LEGACY_NAMESPACE_VALUE = 3;
	private static final byte CURRENT_NAMESPACE_VALUE = 4;

	enum IdKind {
		IRI,
		LITERAL,
		BNODE,
		TRIPLE,
		INTERNAL
	}

	private final byte namespaceValue;
	private final byte idKey;
	private final byte hashKey;
	private final byte hashIdKey;
	private final boolean inlineLiterals;
	private final boolean tripleTerms;
	private final boolean baseDirection;

	ValueStoreFormat(byte namespaceValue, byte idKey, byte hashKey, byte hashIdKey, boolean inlineLiterals,
			boolean tripleTerms, boolean baseDirection) {
		this.namespaceValue = namespaceValue;
		this.idKey = idKey;
		this.hashKey = hashKey;
		this.hashIdKey = hashIdKey;
		this.inlineLiterals = inlineLiterals;
		this.tripleTerms = tripleTerms;
		this.baseDirection = baseDirection;
	}

	abstract long createId(byte valueType, long value);

	abstract long getValue(long id);

	abstract IdKind getKind(long id);

	abstract boolean isInlined(long id);

	byte namespaceValue() {
		return namespaceValue;
	}

	byte idKey() {
		return idKey;
	}

	byte hashKey() {
		return hashKey;
	}

	byte hashIdKey() {
		return hashIdKey;
	}

	boolean supportsInlineLiterals() {
		return inlineLiterals;
	}

	boolean supportsTripleTerms() {
		return tripleTerms;
	}

	boolean supportsBaseDirection() {
		return baseDirection;
	}

	boolean isLiteral(long id) {
		return getKind(id) == IdKind.LITERAL;
	}

	boolean isTripleTerm(long id) {
		return getKind(id) == IdKind.TRIPLE;
	}

	boolean isLegacy() {
		return this == LEGACY_5_3;
	}
}
