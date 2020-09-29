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

	private static final long serialVersionUID=-1649571784782592271L;

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
		setDatatype(XSD.STRING);
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
	 * Creates a new datatyped literal with the supplied label and datatype.
	 *
	 * @param label    The label for the literal, must not be <tt>null</tt>.
	 * @param datatype The datatype for the literal.
	 */
	protected SimpleLiteral(String label, IRI datatype) {
		setLabel(label);
		if (RDF.LANGSTRING.equals(datatype)) {
			throw new IllegalArgumentException("datatype rdf:langString requires a language tag");
		} else if (datatype == null) {
			datatype=XSD.STRING;
		}
		setDatatype(datatype);
	}

	/*---------*
	 * Methods *
	 *---------*/

	protected void setLabel(String label) {
		Objects.requireNonNull(label, "Literal label cannot be null");
		this.label=label;
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
		this.language=language;
		setDatatype(RDF.LANGSTRING);
	}

	@Override
	public Optional<String> getLanguage() {
		return Optional.ofNullable(language);
	}

	protected void setDatatype(IRI datatype) {
		this.datatype=datatype;
	}

	@Override
	public IRI getDatatype() {
		return datatype;
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

}
