/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
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
import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.AbstractTripleTerm;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.ValueStoreRevision;

public class LmdbTripleTerm extends AbstractTripleTerm implements LmdbValue {

	private static final long serialVersionUID = 138753260239097856L;

	private ValueStoreRevision revision;
	private long internalID;

	private Resource subject;
	private IRI predicate;
	private Value object;
	private boolean initialized = false;

	public LmdbTripleTerm(ValueStoreRevision revision, long internalID) {
		setInternalID(internalID, revision);
	}

	public LmdbTripleTerm(ValueStoreRevision revision, Resource subject, IRI predicate, Value object) {
		this(revision, subject, predicate, object, UNKNOWN_ID);
	}

	public LmdbTripleTerm(ValueStoreRevision revision, Resource subject, IRI predicate, Value object, long internalID) {
		setInternalID(internalID, revision);
		this.subject = Objects.requireNonNull(subject, "subject must not be null");
		this.predicate = Objects.requireNonNull(predicate, "predicate must not be null");
		this.object = Objects.requireNonNull(object, "object must not be null");
		this.initialized = true;
	}

	@Override
	public Resource getSubject() {
		if (subject == null) {
			init();
		}
		return subject;
	}

	@Override
	public IRI getPredicate() {
		if (predicate == null) {
			init();
		}
		return predicate;
	}

	@Override
	public Value getObject() {
		if (object == null) {
			init();
		}
		return object;
	}

	@Override
	public void setInternalID(long id, ValueStoreRevision revision) {
		this.internalID = id;
		this.revision = revision;
	}

	@Override
	public long getInternalID() {
		return internalID;
	}

	@Override
	public void init() {
		if (subject == null && !initialized) {
			synchronized (this) {
				if (!initialized) {
					initialized = revision.resolveValue(internalID, this);
				}
			}
		}
	}

	@Override
	public ValueStoreRevision getValueStoreRevision() {
		return revision;
	}

	@Override
	public void setFromInitializedValue(LmdbValue initializedValue) {
		if (initializedValue instanceof LmdbTripleTerm lmdbTripleTerm) {
			this.subject = lmdbTripleTerm.subject;
			this.predicate = lmdbTripleTerm.predicate;
			this.object = lmdbTripleTerm.object;
			this.initialized = true;
		} else {
			throw new SailException("Trying to initialize LmdbTripleTerm from non-triple-term value");
		}
	}

	protected Object writeReplace() throws ObjectStreamException {
		init();
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof LmdbTripleTerm otherTerm && internalID != UNKNOWN_ID) {
			if (otherTerm.internalID != UNKNOWN_ID
					&& revision.equals(otherTerm.revision)) {
				// LmdbTripleTerm's from the same revision of the same lmdb store,
				// with both ID's set
				return internalID == otherTerm.internalID;
			}
		}

		init();
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		if (internalID != UNKNOWN_ID) {
			int cachedHash = revision.getStoredHash(internalID);
			if (cachedHash != 0) {
				return cachedHash;
			}
		}

		init();
		int hash = super.hashCode();
		if (internalID != UNKNOWN_ID) {
			revision.storeHash(internalID, hash);
		}
		return hash;
	}
}
