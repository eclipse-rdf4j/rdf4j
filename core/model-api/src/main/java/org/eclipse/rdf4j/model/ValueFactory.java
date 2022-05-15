/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/
package org.eclipse.rdf4j.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.base.CoreDatatype;

/**
 * A factory for creating {@link IRI IRIs}, {@link BNode blank nodes}, {@link Literal literals} and {@link Statement
 * statements} based on the RDF-1.1 Concepts and Abstract Syntax, a W3C Recommendation.
 *
 * @author Arjohn Kampman
 *
 * @see <a href="http://www.w3.org/TR/rdf11-concepts/">RDF-1.1 Concepts and Abstract Syntax</a>
 */
public interface ValueFactory {

	/**
	 * Creates a new IRI from the supplied string-representation.
	 *
	 * @param iri A string-representation of a IRI.
	 * @return An object representing the IRI.
	 * @throws IllegalArgumentException If the supplied string does not resolve to a legal (absolute) IRI.
	 */
	IRI createIRI(String iri);

	/**
	 * Creates a new IRI from the supplied namespace and local name. Calling this method is funtionally equivalent to
	 * calling {@link #createIRI(String) createIRI(namespace+localName)}, but allows the ValueFactory to reuse supplied
	 * namespace and local name strings whenever possible. Note that the values returned by {@link IRI#getNamespace()}
	 * and {@link IRI#getLocalName()} are not necessarily the same as the values that are supplied to this method.
	 *
	 * @param namespace The IRI's namespace.
	 * @param localName The IRI's local name.
	 * @throws IllegalArgumentException If the supplied namespace and localname do not resolve to a legal (absolute)
	 *                                  IRI.
	 */
	IRI createIRI(String namespace, String localName);

	/**
	 * Creates a new bNode.
	 *
	 * @return An object representing the bNode.
	 */
	BNode createBNode();

	/**
	 * Creates a new blank node with the given node identifier.
	 *
	 * @param nodeID The blank node identifier.
	 * @return An object representing the blank node.
	 */
	BNode createBNode(String nodeID);

	/**
	 * Creates a new literal with the supplied label. The return value of {@link Literal#getDatatype()} for the returned
	 * object must be <a href="http://www.w3.org/2001/XMLSchema#string">{@code xsd:string}</a>.
	 *
	 * @param label The literal's label, must not be <var>null</var>.
	 * @return A literal for the specified value.
	 */
	Literal createLiteral(String label);

	/**
	 * Creates a new literal with the supplied label and language attribute. The return value of
	 * {@link Literal#getDatatype()} for the returned object must be
	 * <a href="http://www.w3.org/1999/02/22-rdf-syntax-ns#langString">{@code rdf:langString}</a>.
	 *
	 * @param label    The literal's label, must not be <var>null</var>.
	 * @param language The literal's language attribute, must not be <var>null</var>.
	 * @return A literal for the specified value and language attribute.
	 */
	Literal createLiteral(String label, String language);

	/**
	 * Creates a new literal with the supplied label and datatype.
	 *
	 * @param label    The literal's label, must not be <var>null</var>.
	 * @param datatype The literal's datatype. If it is null, the datatype
	 *                 <a href="http://www.w3.org/2001/XMLSchema#string">{@code xsd:string}</a> will be assigned to this
	 *                 literal.
	 * @return A literal for the specified value and type.
	 */
	Literal createLiteral(String label, IRI datatype);

	/**
	 * Creates a new literal with the supplied label and datatype.
	 *
	 * @param label    The literal's label, must not be <var>null</var>.
	 * @param datatype The literal's datatype. It may not be null.
	 */
	Literal createLiteral(String label, CoreDatatype datatype);

	/**
	 * Creates a new literal with the supplied label and datatype.
	 *
	 * @param label    The literal's label, must not be <var>null</var>.
	 * @param datatype The literal's datatype. If it is null, the datatype
	 *                 <a href="http://www.w3.org/2001/XMLSchema#string">{@code xsd:string}</a> will be assigned to this
	 *                 literal.
	 */
	Literal createLiteral(String label, IRI datatype, CoreDatatype coreDatatype);

	/**
	 * Creates a new <var>xsd:boolean</var>-typed literal representing the specified value.
	 *
	 * @param value The value for the literal.
	 * @return An <var>xsd:boolean</var>-typed literal for the specified value.
	 */
	Literal createLiteral(boolean value);

	/**
	 * Creates a new <var>xsd:byte</var>-typed literal representing the specified value.
	 *
	 * @param value The value for the literal.
	 * @return An <var>xsd:byte</var>-typed literal for the specified value.
	 */
	Literal createLiteral(byte value);

	/**
	 * Creates a new <var>xsd:short</var>-typed literal representing the specified value.
	 *
	 * @param value The value for the literal.
	 * @return An <var>xsd:short</var>-typed literal for the specified value.
	 */
	Literal createLiteral(short value);

	/**
	 * Creates a new <var>xsd:int</var>-typed literal representing the specified value.
	 *
	 * @param value The value for the literal.
	 * @return An <var>xsd:int</var>-typed literal for the specified value.
	 */
	Literal createLiteral(int value);

	/**
	 * Creates a new <var>xsd:long</var>-typed literal representing the specified value.
	 *
	 * @param value The value for the literal.
	 * @return An <var>xsd:long</var>-typed literal for the specified value.
	 */
	Literal createLiteral(long value);

	/**
	 * Creates a new <var>xsd:float</var>-typed literal representing the specified value.
	 *
	 * @param value The value for the literal.
	 * @return An <var>xsd:float</var>-typed literal for the specified value.
	 */
	Literal createLiteral(float value);

	/**
	 * Creates a new <var>xsd:double</var>-typed literal representing the specified value.
	 *
	 * @param value The value for the literal.
	 * @return An <var>xsd:double</var>-typed literal for the specified value.
	 */
	Literal createLiteral(double value);

	/**
	 * Creates a new literal representing the specified bigDecimal that is typed as an <var>xsd:decimal</var>.
	 *
	 * @param bigDecimal The value for the literal.
	 * @return An <var>xsd:decimal</var>-typed literal for the specified value.
	 */
	Literal createLiteral(BigDecimal bigDecimal);

	/**
	 * Creates a new literal representing the specified bigInteger that is typed as an <var>xsd:integer</var>.
	 *
	 * @param bigInteger The value for the literal.
	 * @return An <var>xsd:integer</var>-typed literal for the specified value.
	 */
	Literal createLiteral(BigInteger bigInteger);

	/**
	 * Creates a new literal representing a temporal accessor value.
	 *
	 * @param value the temporal accessor value for the literal
	 *
	 * @return a literal representing the specified temporal accessor {@code value} with the appropriate
	 *         {@linkplain Literal#temporalAccessorValue() XML Schema date/time datatype}
	 *
	 * @throws NullPointerException     if {@code value} is {@code null}
	 * @throws IllegalArgumentException if {@code value} cannot be represented by an XML Schema date/time datatype
	 *
	 * @since 3.5.0
	 * @author Alessandro Bollini
	 *
	 * @apiNote See datatype-related API notes for {@link Literal#temporalAccessorValue()}.
	 *
	 * @implNote the default method implementation throws an {@link UnsupportedOperationException} and is only supplied
	 *           as a stop-gap measure for backward compatibility: concrete classes implementing this interface are
	 *           expected to override it.
	 */
	default Literal createLiteral(TemporalAccessor value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Creates a new literal representing a temporal amount value.
	 *
	 * @param value the temporal amount value for the literal
	 *
	 * @return a literal representing the specified temporal amount {@code value} with the appropriate
	 *         {@linkplain Literal#temporalAmountValue() XML Schema duration datatype}
	 *
	 * @throws NullPointerException     if {@code value} is {@code null}
	 * @throws IllegalArgumentException if {@code value} cannot be represented by an XML Schema duration datatype
	 *
	 * @since 3.5.0
	 * @author Alessandro Bollini
	 *
	 * @apiNote See datatype-related API notes for {@link Literal#temporalAmountValue()}.
	 *
	 * @implNote the default method implementation throws an {@link UnsupportedOperationException} and is only supplied
	 *           as a stop-gap measure for backward compatibility: concrete classes implementing this interface are
	 *           expected to override it.
	 */
	default Literal createLiteral(TemporalAmount value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Creates a new literal representing the specified calendar that is typed using the appropriate XML Schema
	 * date/time datatype.
	 *
	 * @param calendar The value for the literal.
	 * @return A typed literal for the specified calendar.
	 */
	Literal createLiteral(XMLGregorianCalendar calendar);

	/**
	 * Creates a new literal representing the specified date that is typed using the appropriate XML Schema date/time
	 * datatype.
	 *
	 * @param date The value for the literal.
	 * @return A typed literal for the specified date.
	 */
	Literal createLiteral(Date date);

	/**
	 * Creates a new statement with the supplied subject, predicate and object.
	 *
	 * @param subject   The statement's subject.
	 * @param predicate The statement's predicate.
	 * @param object    The statement's object.
	 * @return The created statement.
	 */
	Statement createStatement(Resource subject, IRI predicate, Value object);

	/**
	 * Creates a new statement with the supplied subject, predicate and object and associated context.
	 *
	 * @param subject   The statement's subject.
	 * @param predicate The statement's predicate.
	 * @param object    The statement's object.
	 * @param context   The statement's context.
	 * @return The created statement.
	 */
	Statement createStatement(Resource subject, IRI predicate, Value object, Resource context);

	/**
	 * Creates a new RDF-star triple with the supplied subject, predicate and object.
	 *
	 * @param subject   The statement's subject.
	 * @param predicate The statement's predicate.
	 * @param object    The statement's object.
	 * @return The created triple.
	 * @implNote This temporary default method is only supplied as a stop-gap for backward compatibility, but throws an
	 *           {@link UnsupportedOperationException}. Concrete implementations are expected to override.
	 * @since 3.2.0
	 */
	default Triple createTriple(Resource subject, IRI predicate, Value object) {
		throw new UnsupportedOperationException();
	}

}
