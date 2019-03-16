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
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

/**
 * A simple default implementation of the {@link Literal} interface.
 * 
 * @author Arjohn Kampman
 * @author David Huynh
 */
public class SimpleLiteral implements Literal {

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

	/**
	 * The literal's datatype.
	 */
	private IRI datatype;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected SimpleLiteral() {
	}

	/**
	 * Creates a new plain literal with the supplied label.
	 * 
	 * @param label The label for the literal, must not be <tt>null</tt>.
	 */
	protected SimpleLiteral(String label) {
		setLabel(label);
		setDatatype(XMLSchema.STRING);
	}

	/**
	 * Creates a new plain literal with the supplied label and language tag.
	 * 
	 * @param label    The label for the literal, must not be <tt>null</tt>.
	 * @param language The language tag for the literal, must not be <tt>null</tt> and not be empty.
	 */
	protected SimpleLiteral(String label, String language) {
		setLabel(label);
		setLanguage(language);
	}

	/**
	 * Creates a new datyped literal with the supplied label and datatype.
	 * 
	 * @param label    The label for the literal, must not be <tt>null</tt>.
	 * @param datatype The datatype for the literal.
	 */
	protected SimpleLiteral(String label, IRI datatype) {
		setLabel(label);
		if (RDF.LANGSTRING.equals(datatype)) {
			throw new IllegalArgumentException("datatype rdf:langString requires a language tag");
		} else if (datatype == null) {
			datatype = XMLSchema.STRING;
		}
		setDatatype(datatype);
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
		setDatatype(RDF.LANGSTRING);
	}

	@Override
	public Optional<String> getLanguage() {
		return Optional.ofNullable(language);
	}

	protected void setDatatype(IRI datatype) {
		this.datatype = datatype;
	}

	@Override
	public IRI getDatatype() {
		return datatype;
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
			else if (getLanguage().isPresent() || other.getLanguage().isPresent()) {
				return false;
			}

			return true;
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
	 * @see org.eclipse.rdf4j.rio.ntriples.NTriplesUtil#toNTriplesString(Literal)
	 */
	@Override
	public String toString() {
		if (Literals.isLanguageLiteral(this)) {
			StringBuilder sb = new StringBuilder(label.length() + language.length() + 3);
			sb.append('"').append(label).append('"');
			sb.append('@').append(language);
			return sb.toString();
		} else if (XMLSchema.STRING.equals(datatype) || datatype == null) {
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
		return XMLDatatypeUtil.parseBoolean(getLabel());
	}

	@Override
	public byte byteValue() {
		return XMLDatatypeUtil.parseByte(getLabel());
	}

	@Override
	public short shortValue() {
		return XMLDatatypeUtil.parseShort(getLabel());
	}

	@Override
	public int intValue() {
		return XMLDatatypeUtil.parseInt(getLabel());
	}

	@Override
	public long longValue() {
		return XMLDatatypeUtil.parseLong(getLabel());
	}

	@Override
	public float floatValue() {
		return XMLDatatypeUtil.parseFloat(getLabel());
	}

	@Override
	public double doubleValue() {
		return XMLDatatypeUtil.parseDouble(getLabel());
	}

	@Override
	public BigInteger integerValue() {
		return XMLDatatypeUtil.parseInteger(getLabel());
	}

	@Override
	public BigDecimal decimalValue() {
		return XMLDatatypeUtil.parseDecimal(getLabel());
	}

	@Override
	public XMLGregorianCalendar calendarValue() {
		return XMLDatatypeUtil.parseCalendar(getLabel());
	}
}
