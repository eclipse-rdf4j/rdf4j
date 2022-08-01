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
package org.eclipse.rdf4j.sail.nativerdf.model;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.nativerdf.ValueStoreRevision;

public interface NativeValue extends Value {

	int UNKNOWN_ID = -1;

	/**
	 * Sets the ID that is used for this value in a specific revision of the value store.
	 */
	void setInternalID(int id, ValueStoreRevision revision);

	/**
	 * Gets the ID that is used in the native store for this Value.
	 *
	 * @return The value's ID, or {@link #UNKNOWN_ID} if not yet set.
	 */
	int getInternalID();

	/**
	 * Gets the revision of the value store that created this value. The value's internal ID is only valid when it's
	 * value store revision is equal to the value store's current revision.
	 *
	 * @return The revision of the value store that created this value at the time it last set the value's internal ID.
	 */
	ValueStoreRevision getValueStoreRevision();
}
