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
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.base.AbstractLiteral;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.sail.lmdb.ValueStoreRevision;

public class LmdbLiteral extends AbstractLiteral implements LmdbValue {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = 5198968663650168819L;

	/*----------*
	 * Variable *
	 *----------*/

	/**
	 * The literal's label.
	 */
	private String label;

	/**
	 * The literal's language tag.
	 */
	private String language;

	/**
	 * The literal's datatype.
	 */
	private IRI datatype;

	/**
	 * The literal's core datatype. This value is null if there is no know datatype.
	 */
	private CoreDatatype coreDatatype;

	private volatile ValueStoreRevision revision;

	private volatile long internalID;

	private volatile boolean initialized = false;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public LmdbLiteral(ValueStoreRevision revision, long internalID) {
		super();
		setInternalID(internalID, revision);
		coreDatatype = null;
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, long internalID) {
		this.label = label;
		coreDatatype = CoreDatatype.XSD.STRING;
		datatype = CoreDatatype.XSD.STRING.getIri();
		setInternalID(internalID, revision);
		this.initialized = true;
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, String lang) {
		this(revision, label, lang, UNKNOWN_ID);
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, String lang, long internalID) {
		this.label = label;
		this.language = lang;
		coreDatatype = CoreDatatype.RDF.LANGSTRING;
		datatype = CoreDatatype.RDF.LANGSTRING.getIri();
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
		this.label = label;
		this.datatype = datatype;
		this.coreDatatype = null;
		setInternalID(internalID, revision);
		this.initialized = true;
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, IRI datatype, CoreDatatype coreDatatype,
			long internalID) {
		this.label = label;
		assert datatype != null;
		assert coreDatatype != null;
		assert coreDatatype == CoreDatatype.NONE || coreDatatype.getIri() == datatype;
		this.datatype = datatype;
		this.coreDatatype = coreDatatype;
		setInternalID(internalID, revision);
		this.initialized = true;
	}

	public LmdbLiteral(ValueStoreRevision revision, String label, CoreDatatype coreDatatype, long internalID) {
		this.label = label;
		this.coreDatatype = coreDatatype;
		this.datatype = coreDatatype.getIri();
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
	public IRI getDatatype() {
		init();
		return datatype;
	}

	@Override
	public CoreDatatype getCoreDatatype() {
		init();
		if (coreDatatype == null) {
			coreDatatype = CoreDatatype.from(datatype);
		}
		return coreDatatype;
	}

	public void setDatatype(IRI datatype) {
		this.datatype = datatype;
		coreDatatype = null;
	}

	public void setDatatype(CoreDatatype coreDatatype) {
		this.coreDatatype = coreDatatype;
		datatype = coreDatatype.getIri();
	}

	@Override
	public String getLabel() {
		init();
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	@Override
	public Optional<String> getLanguage() {
		init();
		return Optional.ofNullable(language);
	}

	public void setLanguage(String language) {
		this.language = language;
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

		if (o instanceof LmdbLiteral && internalID != UNKNOWN_ID) {
			LmdbLiteral otherLmdbLiteral = (LmdbLiteral) o;

			if (otherLmdbLiteral.internalID != UNKNOWN_ID
					&& revision.equals(otherLmdbLiteral.revision)) {
				// LmdbLiteral's from the same revision of the same lmdb store,
				// with both ID's set
				return internalID == otherLmdbLiteral.internalID;
			}
		}

		init();
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		init();
		return super.hashCode();
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
}
