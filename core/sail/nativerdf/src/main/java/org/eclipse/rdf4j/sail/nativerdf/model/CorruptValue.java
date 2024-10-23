/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.model;

import org.eclipse.rdf4j.sail.nativerdf.ValueStoreRevision;

/**
 * CorruptValue is used when a NativeValue cannot be read from the ValueStore and if soft failure is enabled (see
 * ValueStore#softFailOnCorruptData).
 *
 * There is no method isCorruptValue() as it would exist for a "regular" implementation of NativeValue. Since
 * CorruptValue is only to be used in exceptional situations, the recommended way of checking for it is using
 * "instanceof".
 *
 * @author Hannes Ebner
 */
public class CorruptValue implements NativeValue {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = 8829067881854394802L;

	/*----------*
	 * Variables *
	 *----------*/

	private volatile ValueStoreRevision revision;

	private volatile int internalID;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public CorruptValue(ValueStoreRevision revision, int internalID) {
		setInternalID(internalID, revision);
	}

	/*---------*
	 * Methods *
	 *---------*/

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
		return Integer.toString(internalID);
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

}