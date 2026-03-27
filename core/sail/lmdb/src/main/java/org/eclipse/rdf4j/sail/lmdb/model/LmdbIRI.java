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
import java.lang.ref.SoftReference;
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

	private ValueStoreRevision revision;

	private long internalID = UNKNOWN_ID;

	private boolean initialized = false;

	private transient StringSlot namespace;

	private transient StringSlot localName;

	private transient SoftReference<String> iriString;

	private transient boolean hashCodeInitialized;

	private transient int hashCode;

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
		this.initialized = true;
	}

	@Override
	public void setInternalID(long internalID, ValueStoreRevision revision) {
		long previousInternalID = this.internalID;
		this.internalID = internalID;
		this.revision = revision;
		if (previousInternalID == UNKNOWN_ID && internalID != UNKNOWN_ID) {
			demoteStringState();
		}
	}

	@Override
	public ValueStoreRevision getValueStoreRevision() {
		return revision;
	}

	@Override
	public void setFromInitializedValue(LmdbValue initializedValue) {
		if (initializedValue instanceof LmdbIRI) {
			LmdbIRI initializedIRI = (LmdbIRI) initializedValue;
			setNamespaceAndIri(initializedIRI.getNamespace(), initializedIRI.getLocalName());
			if (initializedIRI.hashCodeInitialized) {
				hashCode = initializedIRI.hashCode;
				hashCodeInitialized = true;
			}
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

		int localNameIdx = URIUtil.getLocalNameIndex(iriString);
		setNamespaceAndLocalName(iriString.substring(0, localNameIdx), iriString.substring(localNameIdx),
				keepStrongStrings());
		cacheIriString(iriString);
	}

	@Override
	public String getNamespace() {
		String namespace = this.namespace == null ? null : this.namespace.getIfPresent();
		if (namespace != null) {
			return namespace;
		}

		String iriString = getIriStringIfPresent();
		if (iriString != null) {
			int localNameIdx = URIUtil.getLocalNameIndex(iriString);
			setNamespaceAndLocalName(iriString.substring(0, localNameIdx), iriString.substring(localNameIdx),
					keepStrongStrings());
			return this.namespace == null ? null : this.namespace.getIfPresent();
		}

		init();
		return this.namespace.getIfPresent();
	}

	@Override
	public String getLocalName() {
		String localName = this.localName == null ? null : this.localName.getIfPresent();
		if (localName != null) {
			return localName;
		}

		String iriString = getIriStringIfPresent();
		if (iriString != null) {
			int localNameIdx = URIUtil.getLocalNameIndex(iriString);
			setNamespaceAndLocalName(iriString.substring(0, localNameIdx), iriString.substring(localNameIdx),
					keepStrongStrings());
			return this.localName == null ? null : this.localName.getIfPresent();
		}

		init();
		return this.localName.getIfPresent();
	}

	@Override
	public String stringValue() {
		String iriString = getIriStringIfPresent();
		if (iriString != null) {
			return iriString;
		}

		String namespace = this.namespace == null ? null : this.namespace.getIfPresent();
		String localName = this.localName == null ? null : this.localName.getIfPresent();
		if (namespace == null || localName == null) {
			init();
			namespace = this.namespace.getIfPresent();
			localName = this.localName.getIfPresent();
		}

		iriString = namespace + localName;
		cacheIriString(iriString);
		return iriString;
	}

	public void init() {
		if (internalID == UNKNOWN_ID) {
			return;
		}
		if (!initialized || namespace == null || namespace.getIfPresent() == null || localName == null
				|| localName.getIfPresent() == null) {
			synchronized (this) {
				if (!initialized || namespace == null || namespace.getIfPresent() == null || localName == null
						|| localName.getIfPresent() == null) {
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
			LmdbIRI otherLmdbIRI = (LmdbIRI) o;

			if (internalID == UNKNOWN_ID) {
				boolean equals = stringValue().equals(otherLmdbIRI.stringValue());
				if (equals && revision.equals(otherLmdbIRI.revision)) {
					setInternalID(otherLmdbIRI.internalID, otherLmdbIRI.revision);
				}
				return equals;
			}

			if (revision.equals(otherLmdbIRI.revision)) {
				if (otherLmdbIRI.internalID == UNKNOWN_ID) {
					boolean equals = stringValue().equals(otherLmdbIRI.stringValue());
					if (equals) {
						otherLmdbIRI.setInternalID(this.internalID, this.revision);
					}
					return equals;
				}

				boolean equal = internalID == otherLmdbIRI.internalID;
				if (equal) {
					if (namespace == null || namespace.getIfPresent() == null || localName == null
							|| localName.getIfPresent() == null) {
						setFromInitializedValue(otherLmdbIRI);
					} else if (otherLmdbIRI.namespace == null || otherLmdbIRI.namespace.getIfPresent() == null
							|| otherLmdbIRI.localName == null || otherLmdbIRI.localName.getIfPresent() == null) {
						otherLmdbIRI.setFromInitializedValue(this);
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
		if (!hashCodeInitialized) {
			hashCode = stringValue().hashCode();
			hashCodeInitialized = true;
		}
		return hashCode;
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
		setNamespaceAndLocalName(namespace, localName, keepStrongStrings());
		cacheIriString(namespace + localName);
	}

	private void setNamespaceAndLocalName(String namespace, String localName, boolean keepStrong) {
		Objects.requireNonNull(namespace, "namespace must not be null");
		Objects.requireNonNull(localName, "localName must not be null");
		if (this.namespace == null) {
			this.namespace = new StringSlot();
		}
		if (this.localName == null) {
			this.localName = new StringSlot();
		}
		this.namespace.set(namespace, keepStrong);
		this.localName.set(localName, keepStrong);
		hashCode = (namespace + localName).hashCode();
		hashCodeInitialized = true;
	}

	private String getIriStringIfPresent() {
		return iriString == null ? null : iriString.get();
	}

	private void cacheIriString(String iriString) {
		this.iriString = new SoftReference<>(iriString);
	}

	private void demoteStringState() {
		if (namespace != null) {
			namespace.demote();
		}
		if (localName != null) {
			localName.demote();
		}
	}

	private boolean keepStrongStrings() {
		return internalID == UNKNOWN_ID;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeObject(getNamespace());
		out.writeObject(getLocalName());
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		setNamespaceAndLocalName((String) in.readObject(), (String) in.readObject(), true);
		iriString = null;
	}
}
