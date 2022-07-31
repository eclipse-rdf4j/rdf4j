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
package org.eclipse.rdf4j.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.base.CoreDatatype;

/**
 * An RDF-1.1 literal consisting of a label (the lexical value), a datatype, and optionally a language tag.
 *
 * <p>
 * Value accessor methods (for instance, {@link #booleanValue()}) map literal lexical values conforming to the syntax of
 * a supported <a href="https://www.w3.org/TR/xmlschema11-2">XML Schema 1.1</a> datatype to a corresponding Java object.
 * </p>
 *
 * @author Arjohn Kampman
 *
 * @see <a href="http://www.w3.org/TR/rdf11-concepts/#section-Graph-Literal">RDF-1.1 Concepts and Abstract Syntax</a>
 * @see <a href="https://www.w3.org/TR/rdf11-concepts/#xsd-datatypes">RDF 1.1 Concepts and Abstract Syntax - &sect;5.1
 *      The XML Schema Built-in Datatypes</a>
 * @see <a href="https://www.w3.org/TR/xmlschema11-2">XML Schema Definition Language (XSD) 1.1 Part 2: Datatypes</a>
 *
 * @implSpec In order to ensure interoperability of concrete classes implementing this interface,
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
	 * Gets the datatype for this literal.
	 * <p>
	 * If {@link #getLanguage()} returns a non-empty value than this must return
	 * <a href="http://www.w3.org/1999/02/22-rdf-syntax-ns#langString">{@code rdf:langString}</a>. If no datatype was
	 * assigned to this literal by the creator, then this method must return
	 * <a href="http://www.w3.org/2001/XMLSchema#string">{@code xsd:string}</a>.
	 *
	 * @return The datatype for this literal.
	 */
	IRI getDatatype();

	/**
	 * Returns the <var>boolean</var> value of this literal.
	 *
	 * @return The <var>boolean</var> value of the literal.
	 * @throws IllegalArgumentException If the literal's label cannot be represented by a <var>boolean</var> .
	 */
	boolean booleanValue();

	/**
	 * Returns the <var>byte</var> value of this literal.
	 *
	 * @return The <var>byte</var> value of the literal.
	 * @throws NumberFormatException If the literal cannot be represented by a <var>byte</var>.
	 */
	byte byteValue();

	/**
	 * Returns the <var>short</var> value of this literal.
	 *
	 * @return The <var>short</var> value of the literal.
	 * @throws NumberFormatException If the literal's label cannot be represented by a <var>short</var>.
	 */
	short shortValue();

	/**
	 * Returns the <var>int</var> value of this literal.
	 *
	 * @return The <var>int</var> value of the literal.
	 * @throws NumberFormatException If the literal's label cannot be represented by a <var>int</var>.
	 */
	int intValue();

	/**
	 * Returns the <var>long</var> value of this literal.
	 *
	 * @return The <var>long</var> value of the literal.
	 * @throws NumberFormatException If the literal's label cannot be represented by to a <var>long</var> .
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
	 * Returns the <var>float</var> value of this literal.
	 *
	 * @return The <var>float</var> value of the literal.
	 * @throws NumberFormatException If the literal's label cannot be represented by a <var>float</var>.
	 */
	float floatValue();

	/**
	 * Returns the <var>double</var> value of this literal.
	 *
	 * @return The <var>double</var> value of the literal.
	 * @throws NumberFormatException If the literal's label cannot be represented by a <var>double</var>.
	 */
	double doubleValue();

	/**
	 * Retrieves the {@link TemporalAccessor temporal accessor} value of this literal.
	 *
	 * <p>
	 * A temporal accessor representation can be given for literals whose label conforms to the syntax of the following
	 * <a href="https://www.w3.org/TR/xmlschema11-2">XML Schema 1.1</a> date/time datatypes:
	 * </p>
	 *
	 * <ul>
	 *
	 * <li><a href="https://www.w3.org/TR/xmlschema11-2/#dateTime">xsd:dateTime</a>,</li>
	 * <li><a href="https://www.w3.org/TR/xmlschema11-2/#time">xsd:time</a>,</li>
	 * <li><a href="https://www.w3.org/TR/xmlschema11-2/#date">xsd:date</a>,</li>
	 *
	 * <li><a href="https://www.w3.org/TR/xmlschema11-2/#gYearMonth">xsd:gYearMonth</a>,</li>
	 * <li><a href="https://www.w3.org/TR/xmlschema11-2/#gYear">xsd:gYear</a>,</li>
	 * <li><a href="https://www.w3.org/TR/xmlschema11-2/#gMonthDay">xsd:gMonthDay</a>,</li>
	 * <li><a href="https://www.w3.org/TR/xmlschema11-2/#gDay">xsd:gDay</a>,</li>
	 * <li><a href="https://www.w3.org/TR/xmlschema11-2/#gMonth">xsd:gMonth</a>.</li>
	 *
	 * </ul>
	 *
	 * <p>
	 * Temporal accessor representations may be converted to specific {@link java.time} values like
	 * {@link OffsetDateTime} using target static factory methods, for instance
	 * {@code OffsetDateTime.from(literal.temporalAccessorValue())}.
	 * </p>
	 *
	 * <p>
	 * Note however that {@link java.time} doesn't include dedicated classes for some legal XML Schema date/time values,
	 * like offset dates (for instance, {@code 2020-11-16+01:00}) and {@code xsd:gDay} (for instance, {@code ---16}).
	 * </p>
	 *
	 * @return the temporal accessor value of this literal
	 *
	 * @throws DateTimeException if this literal cannot be represented by a {@link TemporalAccessor} value
	 *
	 * @since 3.5.0
	 * @author Alessandro Bollini
	 *
	 * @see <a href="https://docs.oracle.com/javase/tutorial/datetime/">The Java™ Tutorials – Trail: Date Time</a>
	 *
	 * @apiNote the <a href="https://www.w3.org/TR/xmlschema11-2/#dateTimeStamp">xsd:dateTimeStamp</a> datatype
	 *          (supported by calendar-based {@linkplain #calendarValue() value accessor} and
	 *          {@linkplain ValueFactory#createLiteral(XMLGregorianCalendar) factory methods}) was specified by
	 *          <a href="https://www.w3.org/TR/xmlschema11-2/">XML Schema Definition Language (XSD) 1.1 Part 2:
	 *          Datatypes</a> and later removed from <a href="https://www.w3.org/TR/xmlschema-2/">XML Schema Part 2:
	 *          Datatypes Second Edition</a>: it is not included among temporal datatypes automatically assigned by
	 *          {@link ValueFactory#createLiteral(TemporalAmount)} in order to provide better interoperability with the
	 *          latter version of the standard.
	 *
	 * @implSpec The default method implementation throws an {@link UnsupportedOperationException} and is only supplied
	 *           as a stop-gap measure for backward compatibility: concrete classes implementing this interface are
	 *           expected to override it.
	 */
	default TemporalAccessor temporalAccessorValue() throws DateTimeException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Retrieves the {@link TemporalAmount temporal amount} value of this literal.
	 *
	 * <p>
	 * A temporal amount representation can be given for literals whose label conforms to the syntax of the
	 * <a href="https://www.w3.org/TR/xmlschema-2/">XML Schema 2</a>
	 * <a href="https://www.w3.org/TR/xmlschema-2/#duration">xsd:duration</a> datatype.
	 * </p>
	 *
	 * <p>
	 * The adoption of the <a href="https://www.w3.org/TR/xmlschema-2/">XML Schema 2</a> definition is a known deviation
	 * from the <a href="http://www.w3.org/TR/rdf11-concepts/#section-Graph-Literal">RDF 1.1</a> standard;
	 * well-formedness rules are relaxed to consider all duration components as optional and freely mixable.
	 * </p>
	 *
	 * <p>
	 * Temporal amount representations may be converted to specific {@link java.time} values like {@link Duration} using
	 * target static factory methods, for instance {@code Duration.from(literal.temporalAmountValue())}.
	 * </p>
	 *
	 * <p>
	 * Note however that {@link java.time} doesn't include dedicated classes for legal XML Schema duration values
	 * including both date and time components (for instance, {@code P1YT23H}).
	 * </p>
	 *
	 * @return the temporal amount value of this literal
	 *
	 * @throws DateTimeException if this literal cannot be represented by a {@link TemporalAmount} value
	 *
	 * @since 3.5.0
	 * @author Alessandro Bollini
	 *
	 * @see <a href="https://docs.oracle.com/javase/tutorial/datetime/">The Java™ Tutorials – Trail: Date Time</a>
	 *
	 * @apiNote <a href="https://www.w3.org/TR/xmlschema11-2/#yearMonthDuration">xsd:yearMonthDuration</a> and
	 *          <a href="https://www.w3.org/TR/xmlschema11-2/#dayTimeDuration">xsd:xsd:dayTimeDuration</a> datatypes
	 *          (supported by calendar-based {@linkplain #calendarValue() value accessor} and
	 *          {@linkplain ValueFactory#createLiteral(XMLGregorianCalendar) factory methods}) were specified by
	 *          <a href="https://www.w3.org/TR/xmlschema11-2/">XML Schema Definition Language (XSD) 1.1 Part 2:
	 *          Datatypes</a> and later removed from <a href="https://www.w3.org/TR/xmlschema-2/">XML Schema Part 2:
	 *          Datatypes Second Edition</a>: they are not included among temporal datatypes automatically assigned by
	 *          {@link ValueFactory#createLiteral(TemporalAmount)} in order to provide better interoperability with the
	 *          latter version of the standard; interoperability with
	 *          <a href="https://www.w3.org/TR/sparql11-query/#func-timezone">SPARQL 1.1 Query Language §17.4.5.8
	 *          timezone</a> is not compromised, as the legacy {@code xsd:dayTimeDuration} return datatype is defined as
	 *          a restriction of the {@code xds:duration} datatype.
	 *
	 * @implSpec The default method implementation throws an {@link UnsupportedOperationException} and is only supplied
	 *           as a stop-gap measure for backward compatibility: concrete classes implementing this interface are
	 *           expected to override it.
	 */
	default TemporalAmount temporalAmountValue() throws DateTimeException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the {@link XMLGregorianCalendar} value of this literal. A calendar representation can be given for
	 * literals whose label conforms to the syntax of the following <a href="https://www.w3.org/TR/xmlschema11-2/">XML
	 * Schema datatypes</a>: <var>dateTime</var>, <var>time</var>, <var>date</var>, <var>gYearMonth</var>,
	 * <var>gMonthDay</var>, <var>gYear</var>, <var>gMonth</var> or <var>gDay</var>.
	 *
	 * @return The calendar value of the literal.
	 * @throws IllegalArgumentException If the literal cannot be represented by a {@link XMLGregorianCalendar}.
	 */
	XMLGregorianCalendar calendarValue();

	/**
	 * CoreDatatype is an interface for natively supported datatypes in RDF4J. This includes, among others, the XML
	 * Schema datatypes and rdf:langString. CoreDatatypes are implemented as enums and more performant and convenient to
	 * work with than IRI-based datatypes. The constant {@link CoreDatatype#NONE)} is used to represent a datatype that
	 * is not one of the supported core datatypes.
	 *
	 * @return The CoreDatatype or {@link CoreDatatype#NONE)} if the datatype matches none of the core datatypes. This
	 *         method will not return null.
	 *
	 * @implNote This method may not return null. Returning {@link CoreDatatype#NONE)} is only permitted if the datatype
	 *           does not match any of the core datatypes. A literal with a language tag must return
	 *           {@link CoreDatatype.RDF#LANGSTRING)}. A literal without a specified datatype must return
	 *           {@link CoreDatatype.XSD#STRING)}.
	 */
	CoreDatatype getCoreDatatype();

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
	 * @implNote {@linkplain #getLanguage() language} and {@linkplain #getDatatype() datatype} are deliberately not
	 *           considered in the computation (see issue
	 *           <a href="https://github.com/eclipse/rdf4j/issues/665">#655</a>)
	 */
	@Override
	int hashCode();

}
