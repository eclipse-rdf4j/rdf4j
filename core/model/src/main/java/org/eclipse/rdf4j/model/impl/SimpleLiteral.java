/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
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
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
	transient private Optional<String> optionalLanguage = Optional.empty();

	/**
	 * The literal's datatype.
	 */
	private IRI datatype;

	private final boolean plain;
	private final boolean simple;
	private final boolean lateInit;

	// The XSD.Datatype enum that matches the datatype IRI for this literal. This value is calculated on the fly and
	// cached in this variable. `null` means we have not calculated and cached this value yet. We are not worried about
	// race conditions, since calculating this value multiple times must lead to the same effective result. Transient is
	// only used to stop this field from be serialised.
	transient private Optional<XSD.Datatype> xsdDatatype = null;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected SimpleLiteral() {
		plain = false;
		simple = false;
		lateInit = true;
	}

	/**
	 * Creates a new plain literal with the supplied label.
	 *
	 * @param label The label for the literal, must not be <var>null</var>.
	 */
	protected SimpleLiteral(String label) {
		setLabel(label);
		setDatatype(XSD.STRING);
		plain = true;
		simple = true;
		lateInit = false;
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
		plain = true;
		simple = false;
		lateInit = false;
	}

	/**
	 * Creates a new datatyped literal with the supplied label and datatype.
	 *
	 * @param label    The label for the literal, must not be <var>null</var>.
	 * @param datatype The datatype for the literal.
	 */
	protected SimpleLiteral(String label, IRI datatype) {
		setLabel(label);
		if (RDF.LANGSTRING.equals(datatype)) {
			throw new IllegalArgumentException("datatype rdf:langString requires a language tag");
		} else if (datatype == null) {
			setDatatype(XSD.Datatype.STRING);
			plain = true;
			simple = true;
		} else {
			setDatatype(datatype);
			plain = simple = datatype.equals(XSD.STRING); // can not be rdf:langString
		}
		lateInit = false;
	}

	protected SimpleLiteral(String label, XSD.Datatype datatype) {
		setLabel(label);
		if (datatype == null) {
			setDatatype(XSD.Datatype.STRING);
			plain = true;
			simple = true;
		} else {
			setDatatype(datatype);
			plain = simple = datatype == XSD.Datatype.STRING; // can not be rdf:langString
		}
		lateInit = false;

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
		this.optionalLanguage = Optional.of(language);
		setDatatype(RDF.LANGSTRING);
	}

	@Override
	public Optional<String> getLanguage() {
		if (optionalLanguage == null) {
			optionalLanguage = Optional.ofNullable(language);
		}
		return optionalLanguage;
	}

	protected void setDatatype(IRI datatype) {
		this.datatype = datatype;
	}

	protected void setDatatype(XSD.Datatype datatype) {
		this.datatype = datatype.getIri();
		this.xsdDatatype = Optional.of(datatype);
	}

	@Override
	public IRI getDatatype() {
		return datatype;
	}

	public Optional<XSD.Datatype> getXsdDatatype() {
		// we are caching the optional value, so null means that we haven't cached anything yet
		if (xsdDatatype == null) {
			xsdDatatype = XSD.Datatype.from(datatype);
		}
		return xsdDatatype;
	}

	// Overrides Object.equals(Object), implements Literal.equals(Object)
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof Literal) {
			Literal other = (Literal) o;

			// Compare labels
			if (!label.equals(other.getLabel())) {
				return false;
			}

			// Compare datatypes
			if (!datatype.equals(other.getDatatype())) {
				return false;
			}

			if (getLanguage().isPresent() && other.getLanguage().isPresent()) {
				return getLanguage().get().equalsIgnoreCase(other.getLanguage().get());
			}
			// If only one has a language, then return false
			else {
				return !getLanguage().isPresent() && !other.getLanguage().isPresent();
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
		} else if (XSD.STRING.equals(datatype) || datatype == null) {
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
	public boolean isPlainLiteral() {
		if (!lateInit) {
			return plain;
		} else {
			return language != null || getXsdDatatype().stream().anyMatch(datatype -> datatype == XSD.Datatype.STRING);
		}
	}

	@Override
	public boolean isSimpleLiteral() {
		if (!lateInit) {
			return simple;
		} else {
			return language == null && getXsdDatatype().stream().anyMatch(datatype -> datatype == XSD.Datatype.STRING);
		}
	}
}
