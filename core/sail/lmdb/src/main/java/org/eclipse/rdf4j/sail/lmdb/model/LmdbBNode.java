/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;

import org.eclipse.rdf4j.model.base.AbstractBNode;
import org.eclipse.rdf4j.sail.lmdb.ValueStoreRevision;

public class LmdbBNode extends AbstractBNode implements LmdbResource {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = 2729080258717960353L;

	/*-----------*
	 * Variables *
	 *-----------*/

	private ValueStoreRevision revision;

	private long internalID = UNKNOWN_ID;

	private boolean initialized = false;

	private transient StringSlot id;

	private transient boolean hashCodeInitialized;

	private transient int hashCode;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public LmdbBNode(ValueStoreRevision revision, long internalID) {
		setInternalID(internalID, revision);
	}

	public LmdbBNode(ValueStoreRevision revision, String nodeID) {
		this(revision, nodeID, UNKNOWN_ID);
	}

	public LmdbBNode(ValueStoreRevision revision, String nodeID, long internalID) {
		setID(nodeID);
		setInternalID(internalID, revision);
		this.initialized = true;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void setInternalID(long internalID, ValueStoreRevision revision) {
		long previousInternalID = this.internalID;
		this.internalID = internalID;
		this.revision = revision;
		if (previousInternalID == UNKNOWN_ID && internalID != UNKNOWN_ID && id != null) {
			id.demote();
		}
	}

	@Override
	public ValueStoreRevision getValueStoreRevision() {
		return revision;
	}

	@Override
	public void setFromInitializedValue(LmdbValue initializedValue) {
		if (initializedValue instanceof LmdbBNode) {
			LmdbBNode lmdbBNode = (LmdbBNode) initializedValue;
			setID(lmdbBNode.getID());
		} else {
			throw new IllegalArgumentException("Initialized value is not of type LmdbBNode");
		}
	}

	@Override
	public long getInternalID() {
		return internalID;
	}

	public void setID(String id) {
		if (this.id == null) {
			this.id = new StringSlot();
		}
		this.id.set(id, keepStrongStrings());
		hashCode = id == null ? 0 : id.hashCode();
		hashCodeInitialized = id != null;
	}

	@Override
	public String getID() {
		init();
		return id == null ? null : id.getIfPresent();
	}

	public void init() {
		if (internalID == UNKNOWN_ID) {
			return;
		}
		if (!initialized || id == null || id.getIfPresent() == null) {
			synchronized (this) {
				if (!initialized || id == null || id.getIfPresent() == null) {
					revision.resolveValue(internalID, this);
					initialized = true;
				}
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof LmdbBNode && internalID != LmdbValue.UNKNOWN_ID) {
			LmdbBNode otherLmdbBNode = (LmdbBNode) o;

			if (otherLmdbBNode.internalID != LmdbValue.UNKNOWN_ID && revision.equals(otherLmdbBNode.revision)) {
				// LmdbBNode's from the same revision of the same lmdb store,
				// with both ID's set
				return internalID == otherLmdbBNode.internalID;
			}
		}

		return super.equals(o);
	}

	@Override
	public int hashCode() {
		if (!hashCodeInitialized) {
			hashCode = getID().hashCode();
			hashCodeInitialized = true;
		}
		return hashCode;
	}

	protected Object writeReplace() throws ObjectStreamException {
		init();
		return this;
	}

	private boolean keepStrongStrings() {
		return internalID == UNKNOWN_ID;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeObject(getID());
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		id = new StringSlot();
		id.setStrong((String) in.readObject());
		hashCode = id.getIfPresent().hashCode();
		hashCodeInitialized = true;
	}
}
