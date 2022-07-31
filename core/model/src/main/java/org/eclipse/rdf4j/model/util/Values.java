/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.Objects;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.ValidatingValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * Factory methods to quickly create {@link Value} objects ( {@link IRI}, {@link Literal}, {@link BNode}, and
 * {@link Triple}) without having to create a {@link ValueFactory} first.
 * <p>
 * Example usage:
 *
 * <pre>
 * import static org.eclipse.rdf4j.model.util.Values.iri;
 *
 * ...
 * IRI foo = iri("http://example.org/foo");
 * </pre>
 * <p>
 *
 * @author Jeen Broekstra
 * @since 3.5.0
 *
 * @see Statements
 */
public class Values {

	/**
	 * Internal shared value factory used for creating all values. We use a {@link ValidatingValueFactory} to ensure
	 * created values are syntactically legal.
	 */
	private static final ValueFactory VALUE_FACTORY = new ValidatingValueFactory(SimpleValueFactory.getInstance());

	/* private constructor */

	private Values() {
	}

	/* IRI factory methods */

	/**
	 * Create a new {@link IRI} using the supplied iri string
	 *
	 * @param iri a string representing a valid (absolute) iri
	 *
	 * @return an {@link IRI} object for the supplied iri string.
	 *
	 * @throws NullPointerException     if the suppplied iri is <code>null</code>
	 * @throws IllegalArgumentException if the supplied iri string can not be parsed as a legal IRI.
	 */
	public static IRI iri(String iri) throws IllegalArgumentException {
		return iri(VALUE_FACTORY, iri);
	}

	/**
	 * Create a new {@link IRI} using the supplied iri string
	 *
	 * @param vf  the {@link ValueFactory} to use for creation of the IRI.
	 * @param iri a string representing a valid (absolute) iri
	 *
	 * @return an {@link IRI} object for the supplied iri string.
	 *
	 * @throws NullPointerException     if any of the input parameters is <code>null</code>
	 * @throws IllegalArgumentException if the supplied iri string can not be parsed as a legal IRI by the supplied
	 *                                  {@link ValueFactory} .
	 */
	public static IRI iri(ValueFactory vf, String iri) throws IllegalArgumentException {
		return vf.createIRI(Objects.requireNonNull(iri, "iri may not be null"));
	}

	/**
	 * Create a new {@link IRI} using the supplied namespace name and local name
	 *
	 * @param namespace the IRI's namespace name
	 * @param localName the IRI's local name
	 *
	 * @return an {@link IRI} object for the supplied IRI namespace name and localName.
	 *
	 * @throws NullPointerException     if any of the input parameters is <code>null</code>
	 * @throws IllegalArgumentException if the supplied iri string can not be parsed as a legal IRI.
	 */
	public static IRI iri(String namespace, String localName) throws IllegalArgumentException {
		return iri(VALUE_FACTORY, namespace, localName);
	}

	/**
	 * Create a new {@link IRI} using the supplied {@link Namespace} and local name
	 *
	 * @param namespace the IRI's {@link Namespace}
	 * @param localName the IRI's local name
	 *
	 * @return an {@link IRI} object for the supplied IRI namespace and localName.
	 *
	 * @throws NullPointerException     if any of the input parameters is <code>null</code>
	 * @throws IllegalArgumentException if the supplied iri string can not be parsed as a legal IRI.
	 * @since 3.6.0
	 */
	public static IRI iri(Namespace namespace, String localName) throws IllegalArgumentException {
		return iri(VALUE_FACTORY, Objects.requireNonNull(namespace.getName()), localName);
	}

	/**
	 * Create a new {@link IRI} from a supplied prefixed name, using the supplied {@link Namespace namespaces}
	 *
	 * @param namespaces   the Namespaces from which to find the correct namespace to map the prefixed name to
	 * @param prefixedName a prefixed name that is a shorthand for a full iri, using syntax form
	 *                     <code>prefix:localName</code>. For example, <code>rdf:type</code> is a prefixed name where
	 *                     <code>rdf</code> is the prefix. If the correct {@link Namespace} definition is also supplied
	 *                     this expands to the full namespace name
	 *                     <code>http://www.w3.org/1999/02/22-rdf-syntax-ns#</code>, leading to a full IRI
	 *                     <code>http://www.w3.org/1999/02/22-rdf-syntax-ns#type</code>.
	 *
	 * @return an {@link IRI} object for the supplied IRI namespace and localName.
	 *
	 * @throws NullPointerException     if any of the input parameters is <code>null</code>
	 * @throws IllegalArgumentException if the supplied prefixed name can not be transformed to a legal IRI.
	 * @since 3.6.0
	 */
	public static IRI iri(Iterable<Namespace> namespaces, String prefixedName) throws IllegalArgumentException {
		if (prefixedName.indexOf(':') < 0) {
			throw new IllegalArgumentException("Invalid prefixed name: '" + prefixedName + "'");
		}

		final String prefix = prefixedName.substring(0, prefixedName.indexOf(':'));
		for (Namespace ns : namespaces) {
			if (prefix.equals(ns.getPrefix())) {
				return iri(ns.getName(), prefixedName.substring(prefixedName.indexOf(':') + 1));
			}
		}
		throw new IllegalArgumentException("Prefix '" + prefix + "' not identified in supplied namespaces");
	}

	/**
	 * Create a new {@link IRI} using the supplied namespace and local name
	 *
	 * @param vf        the {@link ValueFactory} to use for creation of the IRI.
	 * @param namespace the IRI's namespace
	 * @param localName the IRI's local name
	 *
	 * @return an {@link IRI} object for the supplied IRI namespace and localName.
	 *
	 * @throws NullPointerException     if any of the input parameters is <code>null</code>
	 * @throws IllegalArgumentException if the supplied iri string can not be parsed as a legal IRI by the supplied
	 *                                  {@link ValueFactory}
	 */
	public static IRI iri(ValueFactory vf, String namespace, String localName) throws IllegalArgumentException {
		return vf.createIRI(Objects.requireNonNull(namespace, "namespace may not be null"),
				Objects.requireNonNull(localName, "localName may not be null"));
	}

	/* blank node factory methods */

	/**
	 * Creates a new {@link BNode}
	 *
	 * @return a new {@link BNode}
	 */
	public static BNode bnode() {
		return bnode(VALUE_FACTORY);
	}

	/**
	 * Creates a new {@link BNode}
	 *
	 * @param vf the {@link ValueFactory} to use for creation of the {@link BNode}
	 *
	 * @return a new {@link BNode}
	 *
	 * @throws NullPointerException if any of the input parameters is <code>null</code>
	 */
	public static BNode bnode(ValueFactory vf) {
		return vf.createBNode();
	}

	/**
	 * Creates a new {@link BNode} with the supplied node identifier.
	 *
	 * @param nodeId the node identifier
	 *
	 * @return a new {@link BNode}
	 *
	 * @throws NullPointerException     if the supplied node identifier is <code>null</code>.
	 * @throws IllegalArgumentException if the supplied node identifier is not valid
	 */
	public static BNode bnode(String nodeId) throws IllegalArgumentException {
		return bnode(VALUE_FACTORY, nodeId);
	}

	/**
	 * Creates a new {@link BNode} with the supplied node identifier.
	 *
	 * @param vf     the {@link ValueFactory} to use for creation of the {@link BNode}
	 * @param nodeId the node identifier
	 *
	 * @return a new {@link BNode}
	 *
	 * @throws NullPointerException     if any of the input parameters is <code>null</code>
	 * @throws IllegalArgumentException if the supplied node identifier is not valid
	 */
	public static BNode bnode(ValueFactory vf, String nodeId) throws IllegalArgumentException {
		return vf.createBNode(Objects.requireNonNull(nodeId, "nodeId may not be null"));
	}

	/* Literal factory methods */

	/**
	 * Creates a new {@link Literal} with the supplied lexical value.
	 *
	 * @param lexicalValue the lexical value for the literal
	 *
	 * @return a new {@link Literal} of type {@link XSD#STRING}
	 *
	 * @throws NullPointerException if the supplied lexical value is <code>null</code>.
	 */
	public static Literal literal(String lexicalValue) {
		return literal(VALUE_FACTORY, lexicalValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied lexical value.
	 *
	 * @param vf           the {@link ValueFactory} to use for creation of the {@link Literal}
	 * @param lexicalValue the lexical value for the literal
	 *
	 * @return a new {@link Literal} of type {@link XSD#STRING}
	 *
	 * @throws NullPointerException if any of the input parameters is <code>null</code>
	 */
	public static Literal literal(ValueFactory vf, String lexicalValue) {
		return vf.createLiteral(Objects.requireNonNull(lexicalValue, "lexicalValue may not be null"));
	}

	/**
	 * Creates a new {@link Literal} with the supplied lexical value.
	 *
	 * @param lexicalValue the lexical value for the literal
	 * @param languageTag  the language tag for the literal.
	 *
	 * @return a new {@link Literal} of type {@link RDF#LANGSTRING}
	 *
	 * @throws NullPointerException if the supplied lexical value or language tag is <code>null</code>.
	 */
	public static Literal literal(String lexicalValue, String languageTag) {
		return literal(VALUE_FACTORY, lexicalValue, languageTag);
	}

	/**
	 * Creates a new {@link Literal} with the supplied lexical value.
	 *
	 * @param vf           the {@link ValueFactory} to use for creation of the {@link Literal}
	 * @param lexicalValue the lexical value for the literal
	 * @param languageTag  the language tag for the literal.
	 *
	 * @return a new {@link Literal} of type {@link RDF#LANGSTRING}
	 *
	 * @throws NullPointerException if any of the input parameters is <code>null</code>
	 */
	public static Literal literal(ValueFactory vf, String lexicalValue, String languageTag) {
		return vf.createLiteral(Objects.requireNonNull(lexicalValue, "lexicalValue may not be null"),
				Objects.requireNonNull(languageTag, "languageTag may not be null"));
	}

	/**
	 * Creates a new {@link Literal} with the supplied lexical value and datatype.
	 *
	 * @param lexicalValue the lexical value for the literal
	 * @param datatype     the datatype IRI
	 *
	 * @return a new {@link Literal} with the supplied lexical value and datatype
	 *
	 * @throws NullPointerException     if the supplied lexical value or datatype is <code>null</code>.
	 * @throws IllegalArgumentException if the supplied lexical value is not valid for the given datatype
	 */
	public static Literal literal(String lexicalValue, IRI datatype) throws IllegalArgumentException {
		return literal(VALUE_FACTORY, lexicalValue, datatype);
	}

	/**
	 * Creates a new {@link Literal} with the supplied lexical value and datatype.
	 *
	 * @param lexicalValue the lexical value for the literal
	 * @param datatype     the CoreDatatype
	 *
	 * @return a new {@link Literal} with the supplied lexical value and datatype
	 *
	 * @throws NullPointerException     if the supplied lexical value or datatype is <code>null</code>.
	 * @throws IllegalArgumentException if the supplied lexical value is not valid for the given datatype
	 */
	public static Literal literal(String lexicalValue, CoreDatatype datatype) throws IllegalArgumentException {
		return literal(VALUE_FACTORY, lexicalValue, datatype);
	}

	/**
	 * Creates a new {@link Literal} with the supplied lexical value and datatype.
	 *
	 * @param vf           the {@link ValueFactory} to use for creation of the {@link Literal}
	 * @param lexicalValue the lexical value for the literal
	 * @param datatype     the datatype IRI
	 *
	 * @return a new {@link Literal} with the supplied lexical value and datatype
	 *
	 * @throws NullPointerException     if any of the input parameters is <code>null</code>.
	 * @throws IllegalArgumentException if the supplied lexical value is not valid for the given datatype
	 */
	public static Literal literal(ValueFactory vf, String lexicalValue, IRI datatype) throws IllegalArgumentException {
		return vf.createLiteral(Objects.requireNonNull(lexicalValue, "lexicalValue may not be null"),
				Objects.requireNonNull(datatype, "datatype may not be null"));
	}

	/**
	 * Creates a new {@link Literal} with the supplied lexical value and datatype.
	 *
	 * @param vf           the {@link ValueFactory} to use for creation of the {@link Literal}
	 * @param lexicalValue the lexical value for the literal
	 * @param datatype     the CoreDatatype
	 *
	 * @return a new {@link Literal} with the supplied lexical value and datatype
	 *
	 * @throws NullPointerException     if any of the input parameters is <code>null</code>.
	 * @throws IllegalArgumentException if the supplied lexical value is not valid for the given datatype
	 */
	public static Literal literal(ValueFactory vf, String lexicalValue, CoreDatatype datatype)
			throws IllegalArgumentException {
		return vf.createLiteral(Objects.requireNonNull(lexicalValue, "lexicalValue may not be null"),
				Objects.requireNonNull(datatype, "datatype may not be null"));
	}

	/**
	 * Creates a new {@link Literal} with the supplied boolean value
	 *
	 * @param booleanValue a boolean value
	 *
	 * @return a {@link Literal} of type {@link XSD#BOOLEAN} with the supplied value
	 */
	public static Literal literal(boolean booleanValue) {
		return literal(VALUE_FACTORY, booleanValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied boolean value
	 *
	 * @param vf           the {@link ValueFactory} to use for creation of the {@link Literal}
	 * @param booleanValue a boolean value
	 *
	 * @return a {@link Literal} of type {@link XSD#BOOLEAN} with the supplied value
	 *
	 * @throws NullPointerException if any of the input parameters is <code>null</code>.
	 */
	public static Literal literal(ValueFactory vf, boolean booleanValue) {
		return vf.createLiteral(booleanValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied byte value
	 *
	 * @param byteValue a byte value
	 *
	 * @return a {@link Literal} of type {@link XSD#BYTE} with the supplied value
	 */
	public static Literal literal(byte byteValue) {
		return literal(VALUE_FACTORY, byteValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied byte value
	 *
	 * @param vf        the {@link ValueFactory} to use for creation of the {@link Literal}
	 * @param byteValue a byte value
	 *
	 * @return a {@link Literal} of type {@link XSD#BYTE} with the supplied value
	 *
	 * @throws NullPointerException if any of the input parameters is <code>null</code>.
	 */
	public static Literal literal(ValueFactory vf, byte byteValue) {
		return vf.createLiteral(byteValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied short value
	 *
	 * @param shortValue a short value
	 *
	 * @return a {@link Literal} of type {@link XSD#SHORT} with the supplied value
	 */
	public static Literal literal(short shortValue) {
		return literal(VALUE_FACTORY, shortValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied short value
	 *
	 * @param vf         the {@link ValueFactory} to use for creation of the {@link Literal}
	 * @param shortValue a short value
	 *
	 * @return a {@link Literal} of type {@link XSD#SHORT} with the supplied value
	 *
	 * @throws NullPointerException if any of the input parameters is <code>null</code>.
	 */
	public static Literal literal(ValueFactory vf, short shortValue) {
		return vf.createLiteral(shortValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied int value
	 *
	 * @param intValue an int value
	 *
	 * @return a {@link Literal} of type {@link XSD#INT} with the supplied value
	 */
	public static Literal literal(int intValue) {
		return literal(VALUE_FACTORY, intValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied int value
	 *
	 * @param vf       the {@link ValueFactory} to use for creation of the {@link Literal}
	 * @param intValue an int value
	 *
	 * @return a {@link Literal} of type {@link XSD#INT} with the supplied value
	 *
	 * @throws NullPointerException if any of the input parameters is <code>null</code>.
	 */
	public static Literal literal(ValueFactory vf, int intValue) {
		return vf.createLiteral(intValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied long value
	 *
	 * @param longValue a long value
	 *
	 * @return a {@link Literal} of type {@link XSD#LONG} with the supplied value
	 */
	public static Literal literal(long longValue) {
		return literal(VALUE_FACTORY, longValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied long value
	 *
	 * @param vf        the {@link ValueFactory} to use for creation of the {@link Literal}
	 * @param longValue a long value
	 *
	 * @return a {@link Literal} of type {@link XSD#LONG} with the supplied value
	 *
	 * @throws NullPointerException if any of the input parameters is <code>null</code>.
	 */
	public static Literal literal(ValueFactory vf, long longValue) {
		return vf.createLiteral(longValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied float value
	 *
	 * @param floatValue a float value
	 *
	 * @return a {@link Literal} of type {@link XSD#FLOAT} with the supplied value
	 */
	public static Literal literal(float floatValue) {
		return literal(VALUE_FACTORY, floatValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied float value
	 *
	 * @param vf         the {@link ValueFactory} to use for creation of the {@link Literal}
	 * @param floatValue a float value
	 *
	 * @return a {@link Literal} of type {@link XSD#FLOAT} with the supplied value
	 *
	 * @throws NullPointerException if any of the input parameters is <code>null</code>.
	 */
	public static Literal literal(ValueFactory vf, float floatValue) {
		return vf.createLiteral(floatValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied double value
	 *
	 * @param doubleValue a double value
	 *
	 * @return a {@link Literal} of type {@link XSD#DOUBLE} with the supplied value
	 */
	public static Literal literal(double doubleValue) {
		return literal(VALUE_FACTORY, doubleValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied double value
	 *
	 * @param vf          the {@link ValueFactory} to use for creation of the {@link Literal}
	 * @param doubleValue a double value
	 *
	 * @return a {@link Literal} of type {@link XSD#DOUBLE} with the supplied value
	 *
	 * @throws NullPointerException if any of the input parameters is <code>null</code>.
	 */
	public static Literal literal(ValueFactory vf, double doubleValue) {
		return vf.createLiteral(doubleValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied {@link BigDecimal} value
	 *
	 * @param bigDecimal a {@link BigDecimal} value
	 *
	 * @return a {@link Literal} of type {@link XSD#DECIMAL} with the supplied value
	 *
	 * @throws NullPointerException if the supplied bigDecimal is <code>null</code>.
	 */
	public static Literal literal(BigDecimal bigDecimal) {
		return literal(VALUE_FACTORY, bigDecimal);
	}

	/**
	 * Creates a new {@link Literal} with the supplied {@link BigDecimal} value
	 *
	 * @param vf         the {@link ValueFactory} to use for creation of the {@link Literal}
	 * @param bigDecimal a {@link BigDecimal} value
	 *
	 * @return a {@link Literal} of type {@link XSD#DECIMAL} with the supplied value
	 *
	 * @throws NullPointerException if any of the input parameters is <code>null</code>.
	 */
	public static Literal literal(ValueFactory vf, BigDecimal bigDecimal) {
		return vf.createLiteral(Objects.requireNonNull(bigDecimal, "bigDecimal may not be null"));
	}

	/**
	 * Creates a new {@link Literal} with the supplied {@link BigInteger} value
	 *
	 * @param bigInteger a {@link BigInteger} value
	 *
	 * @return a {@link Literal} of type {@link XSD#INTEGER} with the supplied value
	 *
	 * @throws NullPointerException if the supplied bigInteger is <code>null</code>.
	 */
	public static Literal literal(BigInteger bigInteger) {
		return literal(VALUE_FACTORY, bigInteger);
	}

	/**
	 * Creates a new {@link Literal} with the supplied {@link BigInteger} value
	 *
	 * @param vf         the {@link ValueFactory} to use for creation of the {@link Literal}
	 * @param bigInteger a {@link BigInteger} value
	 *
	 * @return a {@link Literal} of type {@link XSD#INTEGER} with the supplied value
	 *
	 * @throws NullPointerException if any of the input parameters is <code>null</code>.
	 */
	public static Literal literal(ValueFactory vf, BigInteger bigInteger) {
		return vf.createLiteral(Objects.requireNonNull(bigInteger, "bigInteger may not be null"));
	}

	/**
	 * Creates a new {@link Literal} with the supplied {@link TemporalAccessor} value
	 *
	 * @param value a {@link TemporalAccessor} value.
	 *
	 * @return a {@link Literal} with the supplied calendar value and the appropriate {@link XSD} date/time datatype for
	 *         the specific value.
	 *
	 * @throws NullPointerException     if the supplied {@link TemporalAccessor} value is <code>null</code>.
	 * @throws IllegalArgumentException if value cannot be represented by an XML Schema date/time datatype
	 */
	public static Literal literal(TemporalAccessor value) throws IllegalArgumentException {
		return literal(VALUE_FACTORY, value);
	}

	/**
	 * Creates a new {@link Literal} with the supplied {@link TemporalAccessor} value
	 *
	 * @param vf    the {@link ValueFactory} to use for creation of the {@link Literal}
	 * @param value a {@link TemporalAccessor} value.
	 *
	 * @return a {@link Literal} with the supplied calendar value and the appropriate {@link XSD} date/time datatype for
	 *         the specific value.
	 *
	 * @throws NullPointerException     if any of the input parameters is <code>null</code>..
	 * @throws IllegalArgumentException if value cannot be represented by an XML Schema date/time datatype
	 */
	public static Literal literal(ValueFactory vf, TemporalAccessor value) throws IllegalArgumentException {
		return vf.createLiteral(Objects.requireNonNull(value, "value may not be null"));
	}

	/**
	 * Creates a new typed {@link Literal} out of the supplied object, mapping the runtime type of the object to the
	 * appropriate {@link XSD} datatype. If no mapping is available, the method returns a literal with the string
	 * representation of the supplied object as the value, and {@link XSD#STRING} as the datatype.
	 * <p>
	 * Recognized types are {@link Boolean}, {@link Byte}, {@link Double}, {@link Float}, {@link Integer}, {@link Long},
	 * {@link Short}, {@link XMLGregorianCalendar } , {@link TemporalAccessor} and {@link Date}.
	 *
	 * @param object an object to be converted to a typed literal.
	 *
	 * @return a typed literal representation of the supplied object.
	 *
	 * @throws NullPointerException if the input parameter is <code>null</code>..
	 */
	public static Literal literal(Object object) {
		return literal(VALUE_FACTORY, object, false);
	}

	/**
	 * Creates a new typed {@link Literal} out of the supplied object, mapping the runtime type of the object to the
	 * appropriate {@link XSD} datatype.
	 * <p>
	 * Recognized types are {@link Boolean}, {@link Byte}, {@link Double}, {@link Float}, {@link BigDecimal},
	 * {@link Integer}, {@link BigInteger}, {@link Long}, {@link Short}, {@link XMLGregorianCalendar},
	 * {@link TemporalAccessor}, {@link TemporalAmpount} and {@link Date}.
	 *
	 * @param object            an object to be converted to a typed literal.
	 * @param failOnUnknownType If no mapping is available and <code>failOnUnknownType</code> is <code>false</code> the
	 *                          method returns a literal with the string representation of the supplied object as the
	 *                          value, and {@link XSD#STRING} as the datatype. If set to <code>true</code> the method
	 *                          throws an {@link IllegalArgumentException} if no mapping available.
	 *
	 * @return a typed literal representation of the supplied object.
	 *
	 * @throws NullPointerException if the input parameter is <code>null</code>..
	 */
	public static Literal literal(Object object, boolean failOnUnknownType) {
		return literal(VALUE_FACTORY, object, failOnUnknownType);
	}

	/**
	 * Creates a new typed {@link Literal} out of the supplied object, mapping the runtime type of the object to the
	 * appropriate {@link XSD} datatype.
	 * <p>
	 * Recognized types are {@link Boolean}, {@link Byte}, {@link Double}, {@link Float}, {@link Integer}, {@link Long},
	 * {@link Short}, {@link XMLGregorianCalendar }, {@link TemporalAccessor} and {@link Date}.
	 *
	 * @param valueFactory      the {@link ValueFactory}to use for creation of the {@link Literal}
	 * @param object            an object to be converted to a typed literal.
	 * @param failOnUnknownType If no mapping is available and <code>failOnUnknownType</code> is <code>false</code> the
	 *                          method returns a literal with the string representation of the supplied object as the
	 *                          value, and {@link XSD#STRING} as the datatype. If set to <code>true</code> the method
	 *                          throws an {@link IllegalArgumentException} if no mapping available.
	 *
	 * @return a typed literal representation of the supplied object.
	 *
	 * @throws NullPointerException     if any of the input parameters is <code>null</code>.
	 * @throws IllegalArgumentException if <code>failOnUnknownType</code> is set to <code>true</code> and the runtime
	 *                                  type of the supplied object could not be mapped.
	 */
	public static Literal literal(ValueFactory vf, Object object, boolean failOnUnknownType) {
		return createLiteralFromObject(vf, object, failOnUnknownType);
	}

	/* triple factory methods */

	/**
	 * Creates a new {@link Triple RDF-star embedded triple} with the supplied subject, predicate, and object.
	 *
	 * @param subject   the Triple subject
	 * @param predicate the Triple predicate
	 * @param object    the Triple object
	 *
	 * @return a {@link Triple} with the supplied subject, predicate, and object.
	 *
	 * @throws NullPointerException if any of the supplied input parameters is <code>null</code>.
	 */
	public static Triple triple(Resource subject, IRI predicate, Value object) {
		return triple(VALUE_FACTORY, subject, predicate, object);
	}

	/**
	 * Creates a new {@link Triple RDF-star embedded triple} with the supplied subject, predicate, and object.
	 *
	 * @param vf        the {@link ValueFactory} to use for creation of the {@link Triple}
	 * @param subject   the Triple subject
	 * @param predicate the Triple predicate
	 * @param object    the Triple object
	 *
	 * @return a {@link Triple} with the supplied subject, predicate, and object.
	 *
	 * @throws NullPointerException if any of the supplied input parameters is <code>null</code>.
	 */
	public static Triple triple(ValueFactory vf, Resource subject, IRI predicate, Value object) {
		return vf.createTriple(
				Objects.requireNonNull(subject, "subject may not be null"),
				Objects.requireNonNull(predicate, "predicate may not be null"),
				Objects.requireNonNull(object, "object may not be null")
		);
	}

	/**
	 * Creates a new {@link Triple RDF-star embedded triple} using the subject, predicate and object from the supplied
	 * {@link Statement}.
	 *
	 * @param statement the {@link Statement} from which to construct a {@link Triple}
	 *
	 * @return a {@link Triple} with the same subject, predicate, and object as the supplied Statement.
	 *
	 * @throws NullPointerException if statement is <code>null</code>.
	 */
	public static Triple triple(Statement statement) {
		Objects.requireNonNull(statement, "statement may not be null");
		return VALUE_FACTORY.createTriple(statement.getSubject(), statement.getPredicate(), statement.getObject());
	}

	/**
	 * Creates a new {@link Triple RDF-star embedded triple} using the subject, predicate and object from the supplied
	 * {@link Statement}.
	 *
	 * @param vf        the {@link ValueFactory} to use for creation of the {@link Triple}
	 * @param statement the {@link Statement} from which to construct a {@link Triple}
	 *
	 * @return a {@link Triple} with the same subject, predicate, and object as the supplied Statement.
	 *
	 * @throws NullPointerException if any of the supplied input parameters is <code>null</code>.
	 */
	public static Triple triple(ValueFactory vf, Statement statement) {
		Objects.requireNonNull(statement, "statement may not be null");
		return vf.createTriple(statement.getSubject(), statement.getPredicate(), statement.getObject());
	}

	/**
	 * Create a new {@link Namespace} object.
	 *
	 * @param prefix the prefix associated with the namespace
	 * @param name   the namespace name (typically an IRI) for the namespace.
	 * @return a {@link Namespace} object.
	 * @since 3.6.0
	 */
	public static Namespace namespace(String prefix, String name) {
		return new SimpleNamespace(prefix, name);
	}

	/**
	 * Get a {@link ValueFactory}.
	 *
	 * @return a {@link ValueFactory}.
	 */
	public static ValueFactory getValueFactory() {
		return VALUE_FACTORY;
	}

	/* private methods */

	/**
	 * Creates a typed {@link Literal} out of the supplied object, mapping the runtime type of the object to the
	 * appropriate XML Schema type. If no mapping is available, the method throws an exception if the boolean parameter
	 * is true, or if it is false it returns a literal with the string representation of the supplied object as the
	 * value, and {@link XSD#STRING} as the datatype. Recognized types are {@link Boolean}, {@link Byte},
	 * {@link Double}, {@link Float}, {@link Integer}, {@link Long}, {@link Short}, {@link XMLGregorianCalendar } , and
	 * {@link Date}.
	 *
	 * @param valueFactory            The {@link ValueFactory} to use when creating the result.
	 * @param object                  an object to be converted to a typed literal.
	 * @param throwExceptionOnFailure If true throws a {@link LiteralUtilException} when the object is not recognised.
	 *                                If false it returns a string typed literal based on the objects toString method.
	 * @return a typed literal representation of the supplied object.
	 * @throws IllegalArgumentException If the literal could not be created.
	 * @throws NullPointerException     If the object was null.
	 */
	private static Literal createLiteralFromObject(ValueFactory valueFactory, Object object,
			boolean throwExceptionOnFailure)
			throws IllegalArgumentException {
		Objects.requireNonNull(valueFactory, "valueFactory may not be null");
		Objects.requireNonNull(object, "object may not be null");

		if (object instanceof Boolean) {
			return valueFactory.createLiteral((Boolean) object);
		} else if (object instanceof Byte) {
			return valueFactory.createLiteral((Byte) object);
		} else if (object instanceof Double) {
			return valueFactory.createLiteral((Double) object);
		} else if (object instanceof Float) {
			return valueFactory.createLiteral((Float) object);
		} else if (object instanceof BigDecimal) {
			return valueFactory.createLiteral((BigDecimal) object);
		} else if (object instanceof Integer) {
			return valueFactory.createLiteral((Integer) object);
		} else if (object instanceof BigInteger) {
			return valueFactory.createLiteral((BigInteger) object);
		} else if (object instanceof Long) {
			return valueFactory.createLiteral((Long) object);
		} else if (object instanceof Short) {
			return valueFactory.createLiteral((Short) object);
		} else if (object instanceof XMLGregorianCalendar) {
			return valueFactory.createLiteral((XMLGregorianCalendar) object);
		} else if (object instanceof Date) {
			return valueFactory.createLiteral((Date) object);
		} else if (object instanceof TemporalAccessor) {
			return valueFactory.createLiteral((TemporalAccessor) object);
		} else if (object instanceof TemporalAmount) {
			return valueFactory.createLiteral((TemporalAmount) object);
		} else if (object instanceof String) {
			return valueFactory.createLiteral(object.toString(), CoreDatatype.XSD.STRING);
		} else {
			if (throwExceptionOnFailure) {
				throw new IllegalArgumentException("Unrecognized object type: " + object);
			}
			return valueFactory.createLiteral(object.toString(), CoreDatatype.XSD.STRING);
		}
	}
}
