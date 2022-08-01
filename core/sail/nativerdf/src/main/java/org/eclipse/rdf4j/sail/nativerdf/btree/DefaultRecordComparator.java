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
 * A RecordComparator that compares values with eachother by comparing all of their bytes.
 *
 * @author Arjohn Kampman
 */
public class DefaultRecordComparator implements RecordComparator {

	// implements RecordComparator.compareBTreeValues()
	@Override
	public int compareBTreeValues(byte[] key, byte[] data, int offset, int length) {
		int result = 0;
		for (int i = 0; result == 0 && i < length; i++) {
			result = (key[i] & 0xff) - (data[offset + i] & 0xff);
		}
		return result;
	}
}
