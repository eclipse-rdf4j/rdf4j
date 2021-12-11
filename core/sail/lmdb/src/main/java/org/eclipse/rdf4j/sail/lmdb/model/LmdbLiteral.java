/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.model;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleLiteral;
import org.eclipse.rdf4j.sail.lmdb.ValueStoreRevision;

public class LmdbLiteral extends SimpleLiteral implements LmdbValue {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = 5198968663650168819L;

	/*----------*
	 * Variable *
	 *----------*/

	private volatile ValueStoreRevision revision;

	private volatile long internalID;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected LmdbLiteral(ValueStoreRevision revision, long internalID) {
		super();
		setInternalID(internalID, revision);
	}

	public LmdbLiteral(ValueStoreRevision revision, String label) {
		this(revision, label, UNKNOWN_ID);
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, long internalID) {
		super(label);
		setInternalID(internalID, revision);
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, String lang) {
		this(revision, label, lang, UNKNOWN_ID);
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, String lang, long internalID) {
		super(label, lang);
		setInternalID(internalID, revision);
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, IRI datatype) {
		this(revision, label, datatype, UNKNOWN_ID);
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, IRI datatype, long internalID) {
		super(label, datatype);
		setInternalID(internalID, revision);
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
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof LmdbLiteral && internalID != UNKNOWN_ID) {
			LmdbLiteral otherLmdbLiteral = (LmdbLiteral) o;

			if (otherLmdbLiteral.internalID != UNKNOWN_ID
					&& revision.equals(otherLmdbLiteral.revision)) {
				// LmdbLiteral's from the same revision of the same lmdb store,
				// with both ID's set
				return internalID == otherLmdbLiteral.internalID;
			}
		}

		return super.equals(o);
	}
}
