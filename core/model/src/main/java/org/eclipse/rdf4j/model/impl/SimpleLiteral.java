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
package org.eclipse.rdf4j.model.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.base.AbstractLiteral;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * A simple default implementation of the {@link Literal} interface.
 *
 * @author Arjohn Kampman
 * @author David Huynh
 */
public class SimpleLiteral extends AbstractLiteral {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = -1649571784782592271L;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The literal's label.
	 */
	private String label;

	/**
	 * The literal's language tag.
	 */
	private String language;
	// Cache Optional instance for the language, or null if not yet computed. Marked as transient because Optional is
	// not serializable.
	transient private Optional<String> optionalLanguageCache = null;

	/**
	 * The literal's datatype.
	 */
	private IRI datatype;

	// Cached CoreDatatype, or null if not yet computed.
	private CoreDatatype coreDatatype = null;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected SimpleLiteral() {
	}

	/**
	 * Creates a new plain literal with the supplied label.
	 *
	 * @param label The label for the literal, must not be <var>null</var>.
	 */
	protected SimpleLiteral(String label) {
		setLabel(label);
		setDatatype(org.eclipse.rdf4j.model.vocabulary.XSD.STRING);
		optionalLanguageCache = Optional.empty();
	}

	/**
	 * Creates a new plain literal with the supplied label and language tag.
	 *
	 * @param label    The label for the literal, must not be <var>null</var>.
	 * @param language The language tag for the literal, must not be <var>null</var> and not be empty.
	 */
	protected SimpleLiteral(String label, String language) {
		setLabel(label);
		setLanguage(language);
	}

	/**
	 * Creates a new datatyped literal with the supplied label and datatype.
	 *
	 * @param label    The label for the literal, must not be <var>null</var>.
	 * @param datatype The datatype for the literal.
	 */
	protected SimpleLiteral(String label, IRI datatype) {
		setLabel(label);
		if (org.eclipse.rdf4j.model.vocabulary.RDF.LANGSTRING.equals(datatype)) {
			throw new IllegalArgumentException("datatype rdf:langString requires a language tag");
		} else if (datatype == null) {
			setDatatype(CoreDatatype.XSD.STRING);
		} else {
			setDatatype(datatype);
		}
		optionalLanguageCache = Optional.empty();

	}

	/**
	 * Creates a new datatyped literal with the supplied label and datatype.
	 *
	 * @param label    The label for the literal, must not be <var>null</var>.
	 * @param datatype The datatype for the literal.
	 */
	protected SimpleLiteral(String label, IRI datatype, CoreDatatype coreDatatype) {
		assert coreDatatype != null;
		assert datatype != null;
		assert coreDatatype == CoreDatatype.NONE || datatype == coreDatatype.getIri();

		if (CoreDatatype.RDF.LANGSTRING == coreDatatype) {
			throw new IllegalArgumentException("datatype rdf:langString requires a language tag");
		}

		setLabel(label);
		setDatatype(datatype, coreDatatype);
		optionalLanguageCache = Optional.empty();
	}

	@Deprecated(since = "4.0.0", forRemoval = true)
	protected SimpleLiteral(String label, XSD.Datatype datatype) {
		setLabel(label);
		if (org.eclipse.rdf4j.model.vocabulary.RDF.LANGSTRING.equals(datatype.getIri())) {
			throw new IllegalArgumentException("datatype rdf:langString requires a language tag");
		} else if (datatype == null) {
			setDatatype(CoreDatatype.XSD.STRING);
		} else {
			setDatatype(datatype.getCoreDatatype());
		}

	}

	protected SimpleLiteral(String label, CoreDatatype datatype) {
		setLabel(label);
		if (datatype == CoreDatatype.RDF.LANGSTRING) {
			throw new IllegalArgumentException("datatype rdf:langString requires a language tag");
		} else {
			setDatatype(datatype);
		}
		optionalLanguageCache = Optional.empty();

	}

	/*---------*
	 * Methods *
	 *---------*/

	protected void setLabel(String label) {
		Objects.requireNonNull(label, "Literal label cannot be null");
		this.label = label;
	}

	@Override
	public String getLabel() {
		return label;
	}

	protected void setLanguage(String language) {
		Objects.requireNonNull(language);
		if (language.isEmpty()) {
			throw new IllegalArgumentException("Language tag cannot be empty");
		}
		this.language = language;
		optionalLanguageCache = Optional.of(language);
		setDatatype(CoreDatatype.RDF.LANGSTRING);
	}

	@Override
	public Optional<String> getLanguage() {
		if (optionalLanguageCache == null) {
			optionalLanguageCache = Optional.ofNullable(language);
		}
		return optionalLanguageCache;
	}

	protected void setDatatype(IRI datatype) {
		this.datatype = datatype;
		coreDatatype = CoreDatatype.from(datatype);
	}

	protected void setDatatype(IRI datatype, CoreDatatype coreDatatype) {
		assert datatype != null;
		assert coreDatatype != null;
		assert coreDatatype == CoreDatatype.NONE || datatype == coreDatatype.getIri();

		this.datatype = datatype;
		this.coreDatatype = coreDatatype;

	}

	@Deprecated(since = "4.0.0", forRemoval = true)
	protected void setDatatype(XSD.Datatype datatype) {
		this.datatype = datatype.getIri();
		coreDatatype = datatype.getCoreDatatype();
	}

	protected void setDatatype(CoreDatatype datatype) {
		Objects.requireNonNull(datatype);
		this.datatype = datatype.getIri();
		this.coreDatatype = datatype;
	}

	@Override
	public IRI getDatatype() {
		return datatype;
	}

	/**
	 * @deprecated Use {@link #getCoreDatatype()} instead.
	 * @return
	 */
	@Deprecated(since = "4.0.0", forRemoval = true)
	public Optional<XSD.Datatype> getXsdDatatype() {
		CoreDatatype coreDatatype = getCoreDatatype();

		return org.eclipse.rdf4j.model.vocabulary.XSD.Datatype.from(coreDatatype.asXSDDatatype().orElse(null));
	}

	// Overrides Object.equals(Object), implements Literal.equals(Object)
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof Literal) {
			Literal other = (Literal) o;

			CoreDatatype coreDatatype = getCoreDatatype();

			// Compare core datatypes
			if (coreDatatype != ((Literal) o).getCoreDatatype()) {
				return false;
			} else if (coreDatatype == CoreDatatype.NONE) {
				// Compare other datatypes
				if (!datatype.equals(other.getDatatype())) {
					return false;
				}
			}

			// Compare labels
			if (!label.equals(other.getLabel())) {
				return false;
			}

			Optional<String> language = getLanguage();
			Optional<String> otherLanguage = other.getLanguage();

			if (language.isPresent() && otherLanguage.isPresent()) {
				return language.get().equalsIgnoreCase(otherLanguage.get());
			}
			// If only one has a language, then return false
			else {
				return language.isEmpty() && otherLanguage.isEmpty();
			}
		}

		return false;
	}

	// overrides Object.hashCode(), implements Literal.hashCode()
	@Override
	public int hashCode() {
		return label.hashCode();
	}

	/**
	 * Returns the label of the literal with its language or datatype. Note that this method does not escape the quoted
	 * label.
	 *
	 * @see org.eclipse.rdf4j.rio.ntriples.NTriplesUtil#toNTriplesString(org.eclipse.rdf4j.model.Literal)
	 */
	@Override
	public String toString() {
		if (Literals.isLanguageLiteral(this)) {
			StringBuilder sb = new StringBuilder(label.length() + language.length() + 3);
			sb.append('"').append(label).append('"');
			sb.append('@').append(language);
			return sb.toString();
		} else if (org.eclipse.rdf4j.model.vocabulary.XSD.STRING.equals(datatype) || datatype == null) {
			StringBuilder sb = new StringBuilder(label.length() + 2);
			sb.append('"').append(label).append('"');
			return sb.toString();
		} else {
			StringBuilder sb = new StringBuilder(label.length() + datatype.stringValue().length() + 6);
			sb.append('"').append(label).append('"');
			sb.append("^^<").append(datatype.toString()).append(">");
			return sb.toString();
		}
	}

	@Override
	public String stringValue() {
		return label;
	}

	@Override
	public boolean booleanValue() {
		return XMLDatatypeUtil.parseBoolean(label);
	}

	@Override
	public byte byteValue() {
		return XMLDatatypeUtil.parseByte(label);
	}

	@Override
	public short shortValue() {
		return XMLDatatypeUtil.parseShort(label);
	}

	@Override
	public int intValue() {
		return XMLDatatypeUtil.parseInt(label);
	}

	@Override
	public long longValue() {
		return XMLDatatypeUtil.parseLong(label);
	}

	@Override
	public float floatValue() {
		return XMLDatatypeUtil.parseFloat(label);
	}

	@Override
	public double doubleValue() {
		return XMLDatatypeUtil.parseDouble(label);
	}

	@Override
	public BigInteger integerValue() {
		return XMLDatatypeUtil.parseInteger(label);
	}

	@Override
	public BigDecimal decimalValue() {
		return XMLDatatypeUtil.parseDecimal(label);
	}

	@Override
	public XMLGregorianCalendar calendarValue() {
		return XMLDatatypeUtil.parseCalendar(label);
	}

	@Override
	public CoreDatatype getCoreDatatype() {
		if (coreDatatype == null) {
			coreDatatype = CoreDatatype.from(datatype);
		}
		return coreDatatype;
	}

}
