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

import org.eclipse.rdf4j.model.impl.SimpleIRI;
import org.eclipse.rdf4j.sail.lmdb.ValueStoreRevision;

public class LmdbIRI extends SimpleIRI implements LmdbResource {

	private static final long serialVersionUID = -5888138591826143179L;

	/*-----------*
	 * Constants *
	 *-----------*/

	private volatile ValueStoreRevision revision;

	private volatile long internalID;

	private volatile boolean initialized = false;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public LmdbIRI(ValueStoreRevision revision, long internalID) {
		super();
		setInternalID(internalID, revision);
	}

	public LmdbIRI(ValueStoreRevision revision, String uri) {
		this(revision, uri, UNKNOWN_ID);
	}

	public LmdbIRI(ValueStoreRevision revision, String uri, long internalID) {
		super(uri);
		setInternalID(internalID, revision);
		this.initialized = true;
	}

	public LmdbIRI(ValueStoreRevision revision, String namespace, String localname) {
		this(revision, namespace + localname);
	}

	public LmdbIRI(ValueStoreRevision revision, String namespace, String localname, long internalID) {
		this(revision, namespace + localname, internalID);
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
	public void setIRIString(String iriString) {
		super.setIRIString(iriString);
	}

	@Override
	public String getNamespace() {
		init();
		return super.getNamespace();
	}

	@Override
	public String getLocalName() {
		init();
		return super.getLocalName();
	}

	@Override
	public String stringValue() {
		init();
		return super.stringValue();
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

		if (o instanceof LmdbIRI && internalID != UNKNOWN_ID) {
			LmdbIRI otherLmdbURI = (LmdbIRI) o;

			if (otherLmdbURI.internalID != UNKNOWN_ID && revision.equals(otherLmdbURI.revision)) {
				// LmdbURI's from the same revision of the same lmdb store, with
				// both ID's set
				return internalID == otherLmdbURI.internalID;
			}
		}
		return super.equals(o);
	}

	protected Object writeReplace() throws ObjectStreamException {
		init();
		return this;
	}
}
