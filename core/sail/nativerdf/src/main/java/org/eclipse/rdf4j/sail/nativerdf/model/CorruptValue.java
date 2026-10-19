/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.nativerdf.model;

import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.nativerdf.ValueStoreRevision;

/**
 * CorruptValue is used when a NativeValue cannot be read from the ValueStore and if soft failure is enabled
 *
 * @see NativeStore#SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES .
 *      <p>
 *      There is no method isCorruptValue() as it would exist for a "regular" implementation of NativeValue. Since
 *      CorruptValue is only to be used in exceptional situations, the recommended way of checking for it is using
 *      "instanceof".
 *
 * @author Hannes Ebner
 */
public class CorruptValue implements NativeValue {

	private static final long serialVersionUID = 8829067881854394802L;

	private final byte[] data;
	private volatile ValueStoreRevision revision;
	private volatile int internalID;
	private transient NativeValue recovered; // optional recovered value constructed from WAL

	public CorruptValue(ValueStoreRevision revision, int internalID, byte[] data) {
		setInternalID(internalID, revision);
		this.data = data;
	}

	@Override
	public void setInternalID(int internalID, ValueStoreRevision revision) {
		this.internalID = internalID;
		this.revision = revision;
	}

	@Override
	public ValueStoreRevision getValueStoreRevision() {
		return revision;
	}

	@Override
	public int getInternalID() {
		return internalID;
	}

	public String stringValue() {
		return "CorruptValue_with_ID_" + internalID;
	}

	/**
	 * Returns the bytes that were read from the ValueStore for this value's internalID. Since the value is corrupt the
	 * data may be null or an empty array.
	 *
	 * @return null, empty array or corrupt data
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Set a recovered value corresponding to this corrupt entry. The recovered value should be a NativeValue with its
	 * internal ID set to the same ID as this corrupt value.
	 */
	public void setRecovered(NativeValue recovered) {
		this.recovered = recovered;
	}

	/**
	 * Returns a recovered value if one was attached; may be null if recovery failed.
	 */
	public NativeValue getRecovered() {
		return recovered;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof CorruptValue && internalID != NativeValue.UNKNOWN_ID) {
			CorruptValue otherCorruptValue = (CorruptValue) o;

			if (otherCorruptValue.internalID != NativeValue.UNKNOWN_ID && revision.equals(otherCorruptValue.revision)) {
				// CorruptValue is from the same revision of the same native store with both IDs set
				return internalID == otherCorruptValue.internalID;
			}
		}

		return super.equals(o);
	}

	static byte[] truncateData(byte[] data) {
		int offset = data.length - 1;
		int limit = data.length;
		// Only consider 0x00 0x00 0x00 AFTER a non-zero byte has been seen
		for (int j = 0; j < data.length; j++) {
			if (data[j] != 0) {
				offset = j;
				break;
			}
		}

		for (int j = offset; j + 2 < data.length; j++) {
			if (data[j] == 0x00 && data[j + 1] == 0x00 && data[j + 2] == 0x00) {
				limit = j;
				break;
			}
		}

		byte[] truncated = new byte[limit - offset];
		System.arraycopy(data, offset, truncated, 0, limit - offset);
		data = truncated;

		// truncate data to first 2048 bytes
		if (data.length > 2048) {
			truncated = new byte[2048];
			System.arraycopy(data, 0, truncated, 0, 2048);
			data = truncated;
		}
		return data;
	}

}
