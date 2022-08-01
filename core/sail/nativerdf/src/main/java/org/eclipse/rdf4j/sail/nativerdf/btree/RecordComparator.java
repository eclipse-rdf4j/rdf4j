/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.btree;

/**
 * @author Arjohn Kampman
 */
public interface RecordComparator {

	/**
	 * Compares the supplied <var>key</var> to the value of length <var>length</var>, starting at offset
	 * <var>offset</var> in the supplied <var>data</var> array.
	 *
	 * @param key    A byte array representing the search key.
	 * @param data   A byte array containing the value to compare the key to.
	 * @param offset The offset (0-based) of the value in <var>data</var>.
	 * @param length The length of the value.
	 * @return A negative integer when the key is smaller than the value, a positive integer when the key is larger than
	 *         the value, or <var>0</var> when the key is equal to the value.
	 */
	int compareBTreeValues(byte[] key, byte[] data, int offset, int length);
}
