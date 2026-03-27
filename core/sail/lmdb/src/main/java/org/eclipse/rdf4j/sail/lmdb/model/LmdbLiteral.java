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
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.base.AbstractLiteral;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.sail.lmdb.ValueStoreRevision;

public class LmdbLiteral extends AbstractLiteral implements LmdbValue {

	private static final long serialVersionUID = 5198968663650168819L;

	private transient StringSlot label;

	private transient StringSlot language;

	private IRI datatype;

	private CoreDatatype coreDatatype;

	private ValueStoreRevision revision;

	private long internalID = UNKNOWN_ID;

	private boolean initialized = false;

	private transient boolean hashCodeInitialized;

	private transient int hashCode;

	public LmdbLiteral(ValueStoreRevision revision, long internalID) {
		super();
		setInternalID(internalID, revision);
		coreDatatype = null;
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, long internalID) {
		setLabel(label);
		setDatatype(CoreDatatype.XSD.STRING);
		setInternalID(internalID, revision);
		this.initialized = true;
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, String lang) {
		this(revision, label, lang, UNKNOWN_ID);
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, String lang, long internalID) {
		setLabel(label);
		setLanguage(lang);
		setDatatype(CoreDatatype.RDF.LANGSTRING);
		setInternalID(internalID, revision);
		this.initialized = true;
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, IRI datatype) {
		this(revision, label, datatype, UNKNOWN_ID);
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, IRI datatype, CoreDatatype coreDatatype) {
		this(revision, label, datatype, coreDatatype, UNKNOWN_ID);
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, CoreDatatype datatype) {
		this(revision, label, datatype, UNKNOWN_ID);
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, IRI datatype, long internalID) {
		setLabel(label);
		setDatatype(datatype);
		setInternalID(internalID, revision);
		this.initialized = true;
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, IRI datatype, CoreDatatype coreDatatype,
			long internalID) {
		setLabel(label);
		assert datatype != null;
		assert coreDatatype != null;
		assert coreDatatype == CoreDatatype.NONE || coreDatatype.getIri() == datatype;
		this.coreDatatype = coreDatatype;
		this.datatype = datatype;
		setInternalID(internalID, revision);
		this.initialized = true;
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, CoreDatatype coreDatatype, long internalID) {
		setLabel(label);
		setDatatype(coreDatatype);
		setInternalID(internalID, revision);
		this.initialized = true;
	}

	@Override
	public void setInternalID(long internalID, ValueStoreRevision revision) {
		long previousInternalID = this.internalID;
		this.internalID = internalID;
		this.revision = revision;
		if (previousInternalID == UNKNOWN_ID && internalID != UNKNOWN_ID) {
			if (label != null) {
				label.demote();
			}
			if (language != null) {
				language.demote();
			}
		}
	}

	@Override
	public ValueStoreRevision getValueStoreRevision() {
		return revision;
	}

	@Override
	public void setFromInitializedValue(LmdbValue initializedValue) {
		if (initializedValue instanceof LmdbLiteral) {
			LmdbLiteral lmdbLiteral = (LmdbLiteral) initializedValue;
			setLabel(lmdbLiteral.getLabel());
			setLanguage(lmdbLiteral.getLanguage().orElse(null));
			CoreDatatype initializedCoreDatatype = lmdbLiteral.getCoreDatatype();
			if (initializedCoreDatatype != CoreDatatype.NONE) {
				setDatatype(initializedCoreDatatype);
			} else {
				setDatatype(lmdbLiteral.getDatatype());
			}
		} else {
			throw new IllegalArgumentException("Initialized value is not of type LmdbLiteral");
		}
	}

	@Override
	public long getInternalID() {
		return internalID;
	}

	@Override
	public IRI getDatatype() {
		init();
		if (datatype != null) {
			return datatype;
		}
		if (coreDatatype != null && coreDatatype != CoreDatatype.NONE) {
			datatype = coreDatatype.getIri();
		}
		return datatype;
	}

	@Override
	public CoreDatatype getCoreDatatype() {
		init();
		if (coreDatatype == null) {
			coreDatatype = datatype == null ? CoreDatatype.NONE : CoreDatatype.from(datatype);
		}
		return coreDatatype;
	}

	public void setDatatype(IRI datatype) {
		this.datatype = datatype;
		this.coreDatatype = datatype == null ? CoreDatatype.NONE : CoreDatatype.from(datatype);
	}

	public void setDatatype(CoreDatatype coreDatatype) {
		this.coreDatatype = coreDatatype;
		if (coreDatatype == null) {
			datatype = null;
		} else if (coreDatatype != CoreDatatype.NONE) {
			datatype = coreDatatype.getIri();
		}
	}

	@Override
	public String getLabel() {
		init();
		return label == null ? null : label.getIfPresent();
	}

	public void setLabel(String label) {
		assert label != null;
		if (this.label == null) {
			this.label = new StringSlot();
		}
		this.label.set(label, keepStrongStrings());
		hashCode = label.hashCode();
		hashCodeInitialized = true;
	}

	@Override
	public Optional<String> getLanguage() {
		if (coreDatatype == null) {
			init();
		} else if (coreDatatype == CoreDatatype.RDF.LANGSTRING
				&& (language == null || language.getIfPresent() == null)) {
			init();
		}
		return Optional.ofNullable(language == null ? null : language.getIfPresent());
	}

	public void setLanguage(String language) {
		if (language == null) {
			this.language = null;
			return;
		}
		if (this.language == null) {
			this.language = new StringSlot();
		}
		this.language.set(language, keepStrongStrings());
	}

	public void init() {
		if (internalID == UNKNOWN_ID) {
			return;
		}
		if (!initialized || label == null || label.getIfPresent() == null || coreDatatype == null
				|| coreDatatype == CoreDatatype.NONE && datatype == null
				|| coreDatatype == CoreDatatype.RDF.LANGSTRING
						&& (language == null || language.getIfPresent() == null)) {
			synchronized (this) {
				if (!initialized || label == null || label.getIfPresent() == null || coreDatatype == null
						|| coreDatatype == CoreDatatype.NONE && datatype == null
						|| coreDatatype == CoreDatatype.RDF.LANGSTRING
								&& (language == null || language.getIfPresent() == null)) {
					boolean resolved = revision.resolveValue(internalID, this);
					initialized = resolved;
					assert resolved;
				}
			}
		}
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
				return internalID == otherLmdbLiteral.internalID;
			}
		}

		init();
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		if (!hashCodeInitialized) {
			hashCode = getLabel().hashCode();
			hashCodeInitialized = true;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		init();
		return super.toString();
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
		out.writeObject(getLabel());
		out.writeObject(getLanguage().orElse(null));
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		label = new StringSlot();
		label.setStrong((String) in.readObject());
		String language = (String) in.readObject();
		if (language != null) {
			this.language = new StringSlot();
			this.language.setStrong(language);
		} else {
			this.language = null;
		}
		hashCode = label.getIfPresent().hashCode();
		hashCodeInitialized = true;
	}
}
