/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * An RDF-1.1 literal consisting of a label (the lexical value), a datatype, and optionally a language tag.
 *
 * @author Arjohn Kampman
 * @see <a href="http://www.w3.org/TR/rdf11-concepts/#section-Graph-Literal">RDF-1.1 Concepts and Abstract Syntax</a>
 *
 * @implNote In order to ensure interoperability of concrete classes implementing this interface,
 *           {@link #equals(Object)} and {@link #hashCode()} methods must be implemented exactly as described in their
 *           specs.
 */
public interface Literal extends Value {

	@Override
	default boolean isLiteral() {
		return true;
	}

	/**
	 * Gets the label (the lexical value) of this literal.
	 *
	 * @return The literal's label.
	 */
	String getLabel();

	/**
	 * Gets the language tag for this literal, normalized to lower case.
	 *
	 * @return The language tag for this literal, or {@link Optional#empty()} if it doesn't have one.
	 */
	Optional<String> getLanguage();

	/**
	 * Gets the datatype for this literal. If {@link #getLanguage()} returns a non-empty value than this must return
	 * <a href="http://www.w3.org/1999/02/22-rdf-syntax-ns#langString">{@code rdf:langString}</a>. If no datatype was
	 * assigned to this literal by the creator, then this method must return
	 * <a href="http://www.w3.org/2001/XMLSchema#string">{@code xsd:string}</a>.
	 *
	 * @return The datatype for this literal.
	 */
	IRI getDatatype();

	/**
	 * Returns the <tt>boolean</tt> value of this literal.
	 *
	 *
	 * @return The <tt>boolean</tt> value of the literal.
	 * @throws IllegalArgumentException If the literal's label cannot be represented by a <tt>boolean</tt> .
	 */
	boolean booleanValue();

	/**
	 * Returns the <tt>byte</tt> value of this literal.
	 *
	 * @return The <tt>byte</tt> value of the literal.
	 * @throws NumberFormatException If the literal cannot be represented by a <tt>byte</tt>.
	 */
	byte byteValue();

	/**
	 * Returns the <tt>short</tt> value of this literal.
	 *
	 * @return The <tt>short</tt> value of the literal.
	 * @throws NumberFormatException If the literal's label cannot be represented by a <tt>short</tt>.
	 */
	short shortValue();

	/**
	 * Returns the <tt>int</tt> value of this literal.
	 *
	 * @return The <tt>int</tt> value of the literal.
	 * @throws NumberFormatException If the literal's label cannot be represented by a <tt>int</tt>.
	 */
	int intValue();

	/**
	 * Returns the <tt>long</tt> value of this literal.
	 *
	 * @return The <tt>long</tt> value of the literal.
	 * @throws NumberFormatException If the literal's label cannot be represented by to a <tt>long</tt> .
	 */
	long longValue();

	/**
	 * Returns the integer value of this literal.
	 *
	 * @return The integer value of the literal.
	 * @throws NumberFormatException If the literal's label is not a valid integer.
	 */
	BigInteger integerValue();

	/**
	 * Returns the decimal value of this literal.
	 *
	 * @return The decimal value of the literal.
	 * @throws NumberFormatException If the literal's label is not a valid decimal.
	 */
	BigDecimal decimalValue();

	/**
	 * Returns the <tt>float</tt> value of this literal.
	 *
	 * @return The <tt>float</tt> value of the literal.
	 * @throws NumberFormatException If the literal's label cannot be represented by a <tt>float</tt>.
	 */
	float floatValue();

	/**
	 * Returns the <tt>double</tt> value of this literal.
	 *
	 * @return The <tt>double</tt> value of the literal.
	 * @throws NumberFormatException If the literal's label cannot be represented by a <tt>double</tt>.
	 */
	double doubleValue();

	/**
	 * Returns the {@link XMLGregorianCalendar} value of this literal. A calendar representation can be given for
	 * literals whose label conforms to the syntax of the following <a href="http://www.w3.org/TR/xmlschema-2/">XML
	 * Schema datatypes</a>: <tt>dateTime</tt>, <tt>time</tt>, <tt>date</tt>, <tt>gYearMonth</tt>, <tt>gMonthDay</tt>,
	 * <tt>gYear</tt>, <tt>gMonth</tt> or <tt>gDay</tt>.
	 *
	 * @return The calendar value of the literal.
	 * @throws IllegalArgumentException If the literal cannot be represented by a {@link XMLGregorianCalendar}.
	 */
	XMLGregorianCalendar calendarValue();

	/**
	 * Compares this literal to another object.
	 *
	 * @param other the object to compare this literal to
	 *
	 * @return {@code true}, if the other object is an instance of {@code Literal} and if their {@linkplain #getLabel()
	 *         labels}, {@linkplain #getLanguage() language tags} and {@linkplain #getDatatype() datatypes} are equal
	 */
	@Override
	boolean equals(Object other);

	/**
	 * Computes the hash code of this literal.
	 *
	 * @return a hash code for this literal computed as {@link #getLabel()}{@code .hashCode()}
	 *
	 * @implNote {@linkplain #getLanguage() language} amd {@linkplain #getDatatype() datatype} are deliberately not
	 *           considered in the computation (see issue
	 *           <a href="https://github.com/eclipse/rdf4j/issues/665">#655</a>)
	 */
	@Override
	int hashCode();

}
