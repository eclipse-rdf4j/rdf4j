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

import java.io.ObjectStreamException;

import org.eclipse.rdf4j.model.impl.SimpleBNode;
import org.eclipse.rdf4j.sail.lmdb.ValueStoreRevision;

public class LmdbBNode extends SimpleBNode implements LmdbResource {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = 2729080258717960353L;

	/*-----------*
	 * Variables *
	 *-----------*/

	private volatile ValueStoreRevision revision;

	private volatile long internalID;

	private volatile boolean initialized = false;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public LmdbBNode(ValueStoreRevision revision, long internalID) {
		super();
		setInternalID(internalID, revision);
	}

	public LmdbBNode(ValueStoreRevision revision, String nodeID) {
		this(revision, nodeID, UNKNOWN_ID);
	}

	public LmdbBNode(ValueStoreRevision revision, String nodeID, long internalID) {
		super(nodeID);
		setInternalID(internalID, revision);
		this.initialized = true;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void setInternalID(long internalID, ValueStoreRevision revision) {
		this.internalID = internalID;
		this.revision = revision;
	}

	@Override
	public ValueStoreRevision getValueStoreRevision() {
		return revision;
	}

	@Override
	public long getInternalID() {
		return internalID;
	}

	@Override
	public void setID(String id) {
		super.setID(id);
	}

	@Override
	public String getID() {
		init();
		return super.getID();
	}

	protected void init() {
		if (!initialized) {
			synchronized (this) {
				if (!initialized) {
					revision.resolveValue(internalID, this);
				}
				initialized = true;
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

	protected Object writeReplace() throws ObjectStreamException {
		init();
		return this;
	}
}
