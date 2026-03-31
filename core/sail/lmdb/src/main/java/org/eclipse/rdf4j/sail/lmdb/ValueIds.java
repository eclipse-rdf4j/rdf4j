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
	 * An inlined double value. The least significant bit of the value is set to 1 to distinguish it from other inlined
	 * values and references.
	 */
	public static final int T_DOUBLE = -1;

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
		if (isDouble(id)) {
			return T_DOUBLE;
		}
		return (int) ((id >> 1) & 0x3F);
	}

	/**
	 * Returns the value section of the given id.
	 *
	 * @param id The id of which the value should be extracted.
	 * @return The id's value.
	 */
	public static long getValue(long id) {
		return id >> 7;
	}

	/**
	 * Combines an id type and a value into a single long id.
	 *
	 * @param idType The id's type.
	 * @param value  The id's value.
	 * @return A composite id.
	 */
	public static long createId(int idType, long value) {
		return value << 7 | (long) idType << 1;
	}

	/**
	 * Tests if the given id is an inlined value or a reference.
	 *
	 * @param id The id to test
	 * @return <code>true</code> if the value is inlined, else <code>false</code>
	 */
	public static boolean isInlined(long id) {
		return isDouble(id) || getIdType(id) >= T_INTEGER;
	}

	/**
	 * Tests if the given id is an inlined double value, which is identified by the least significant bit being set to
	 * 1.
	 *
	 * @param value The id's value
	 * @return <code>true</code> if the value is an inlined double, else <code>false</code>
	 */
	public static boolean isDouble(long value) {
		return (value & 1L) != 0;
	}
}
