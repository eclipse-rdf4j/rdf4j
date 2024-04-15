/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
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
 * Constants and functions for working with ids encoded into long values.
 */
public class ValueIds {
	/**
	 * Pointer to an arbitrary value in the value store. This is not used as RDF value.
	 */
	public static final int T_PTR = 0;

	/** Reference to a URI */
	public static final int T_URI = 1;
	/** Reference to a literal */
	public static final int T_LITERAL = 2;
	/** Reference to a blank node */
	public static final int T_BNODE = 3;
	/** Reference to a triple */
	public static final int T_TRIPLE = 4;

	// inlined values
	public static final int T_INTEGER = 16;
	public static final int T_DECIMAL = 17;
	public static final int T_FLOAT = 18;
	public static final int T_DATETIME = 19;
	public static final int T_DATETIMESTAMP = 20;
	public static final int T_DATE = 21;
	public static final int T_BOOLEAN = 22;
	public static final int T_SHORTSTRING = 23;
	public static final int T_POSITIVE_INTEGER = 24;
	public static final int T_NEGATIVE_INTEGER = 25;
	public static final int T_NON_NEGATIVE_INTEGER = 26;
	public static final int T_NON_POSITIVE_INTEGER = 27;
	public static final int T_LONG = 28;
	public static final int T_INT = 29;
	public static final int T_SHORT = 30;
	public static final int T_BYTE = 31;
	public static final int T_UNSIGNEDLONG = 32;
	public static final int T_UNSIGNEDINT = 33;
	public static final int T_UNSIGNEDSHORT = 34;
	public static final int T_UNSIGNEDBYTE = 35;

	/**
	 * Returns the type section of the given id.
	 *
	 * @param id The id of which the type should be extracted.
	 * @return The id's type.
	 */
	public static int getIdType(long id) {
		return (int) ((id >> 1) & 0x7F);
	}

	/**
	 * Returns the value section of the given id.
	 *
	 * @param id The id of which the value should be extracted.
	 * @return The id's value.
	 */
	public static long getValue(long id) {
		return id >> 8;
	}

	/**
	 * Combines an id type and a value into a single long id.
	 *
	 * @param idType The id's type.
	 * @param value  The id's value.
	 * @return A composite id.
	 */
	public static long createId(int idType, long value) {
		return value << 8 | idType << 1;
	}
}
