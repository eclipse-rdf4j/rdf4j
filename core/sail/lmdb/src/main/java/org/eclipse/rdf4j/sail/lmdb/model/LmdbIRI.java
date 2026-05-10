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
import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.URIUtil;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.ValueStoreRevision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LmdbIRI implements LmdbResource, IRI {

	private static final long serialVersionUID = -5888138591826143179L;
	private static final Logger log = LoggerFactory.getLogger(LmdbIRI.class);

	/*-----------*
	 * Constants *
	 *-----------*/

	private ValueStoreRevision revision;

	private long internalID;

	private boolean initialized = false;
	/**
	 * The IRI string.
	 */
	private String iriString;

	/**
	 * An index indicating the first character of the local name in the IRI string, -1 if not yet set.
	 */
	private int localNameIdx;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public LmdbIRI(ValueStoreRevision revision, long internalID) {
		setInternalID(internalID, revision);
	}

	public LmdbIRI(ValueStoreRevision revision, String uri) {
		this(revision, uri, UNKNOWN_ID);
	}

	public LmdbIRI(ValueStoreRevision revision, String uri, long internalID) {
		setIRIString(uri);
		setInternalID(internalID, revision);
		this.initialized = true;
	}

	public LmdbIRI(ValueStoreRevision revision, String namespace, String localName) {
		this(revision, namespace, localName, UNKNOWN_ID);
	}

	public LmdbIRI(ValueStoreRevision revision, String namespace, String localName, long internalID) {
		this.revision = revision;
		setNamespaceAndIri(namespace, localName);
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
	public void setFromInitializedValue(LmdbValue initializedValue) {
		if (initializedValue instanceof LmdbIRI) {
			LmdbIRI initializedIRI = (LmdbIRI) initializedValue;
			this.iriString = initializedIRI.iriString;
			this.localNameIdx = initializedIRI.localNameIdx;
		} else {
			throw new SailException("Trying to initialize LmdbIRI from non-IRI value");
		}
	}

	@Override
	public long getInternalID() {
		return internalID;
	}

	private void setIRIString(String iriString) {
		Objects.requireNonNull(iriString, "iriString must not be null");

		if (iriString.indexOf(':') < 0) {
			throw new IllegalArgumentException("Not a valid (absolute) IRI: " + iriString);
		}

		this.iriString = iriString;
		this.localNameIdx = -1;
	}

	@Override
	public String getNamespace() {

		init();
		if (localNameIdx < 0) {
			localNameIdx = URIUtil.getLocalNameIndex(iriString);
		}

		return iriString.substring(0, localNameIdx);
	}

	@Override
	public String getLocalName() {

		init();
		if (localNameIdx < 0) {
			localNameIdx = URIUtil.getLocalNameIndex(iriString);
		}

		return iriString.substring(localNameIdx);
	}

	@Override
	public String stringValue() {
		if (iriString != null) {
			return iriString;
		}
		init();
		return iriString;
	}

	public void init() {
		if (iriString == null && !initialized) {
			synchronized (this) {
				if (!initialized) {
					boolean resolved = revision.resolveValue(internalID, this);
					if (!resolved) {
						log.warn("Could not resolve value");
					}
					initialized = resolved;
				}
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null) {
			return false;
		}

		if (o.getClass() == LmdbIRI.class) {
			if (internalID == UNKNOWN_ID) {
				boolean equals = stringValue().equals(((IRI) o).stringValue());
				if (equals && revision.equals(((LmdbIRI) o).revision)) {
					internalID = ((LmdbIRI) o).internalID;
				}
				return equals;
			}

			LmdbIRI otherLmdbURI = (LmdbIRI) o;

			if (revision.equals(otherLmdbURI.revision)) {
				if (otherLmdbURI.internalID == UNKNOWN_ID) {
					boolean equals = stringValue().equals(((IRI) o).stringValue());
					if (equals) {
						otherLmdbURI.internalID = this.internalID;
					}
					return equals;
				}

				// LmdbURI's from the same revision of the same lmdb store, with
				// both ID's set
				boolean equal = internalID == otherLmdbURI.internalID;
				if (equal) {
					if (iriString == null) {
						iriString = otherLmdbURI.iriString;
						localNameIdx = otherLmdbURI.localNameIdx;
					} else if (otherLmdbURI.iriString == null) {
						otherLmdbURI.iriString = iriString;
						otherLmdbURI.localNameIdx = localNameIdx;
					}
				}
				return equal;
			}
		}

		if (!(o instanceof IRI)) {
			return false;
		}

		return stringValue().equals(((IRI) o).stringValue());
	}

	@Override
	public int hashCode() {
		if (this.iriString != null) {
			return this.iriString.hashCode();
		}

		init();
		return iriString.hashCode();
	}

	protected Object writeReplace() throws ObjectStreamException {
		init();
		return this;
	}

	@Override
	public String toString() {
		return stringValue();
	}

	public void setNamespaceAndIri(String namespace, String localName) {
		localNameIdx = namespace.length();
		this.iriString = namespace + localName;
	}
}
