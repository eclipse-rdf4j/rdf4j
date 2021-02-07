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
	public IRI createIRI(String iri);

	/**
	 * Creates a new URI from the supplied string-representation.
	 *
	 * @param uri A string-representation of a URI.
	 * @return An object representing the URI.
	 * @throws IllegalArgumentException If the supplied string does not resolve to a legal (absolute) URI.
	 * @deprecated Use {{@link #createIRI(String)} instead.
	 */
	@Deprecated
	public default URI createURI(String uri) {
		return createIRI(uri);
	}

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
	public IRI createIRI(String namespace, String localName);

	/**
	 * Creates a new URI from the supplied namespace and local name.
	 *
	 * @param namespace The IRI's namespace.
	 * @param localName The IRI's local name.
	 * @return An object representing the URI.
	 * @throws IllegalArgumentException If the supplied string does not resolve to a legal (absolute) URI.
	 * @deprecated Use {@link #createIRI(String, String)} instead.
	 */
	@Deprecated
	public default URI createURI(String namespace, String localName) {
		return createIRI(namespace, localName);
	}

	/**
	 * Creates a new bNode.
	 *
	 * @return An object representing the bNode.
	 */
	public BNode createBNode();

	/**
	 * Creates a new blank node with the given node identifier.
	 *
	 * @param nodeID The blank node identifier.
	 * @return An object representing the blank node.
	 */
	public BNode createBNode(String nodeID);

	/**
	 * Creates a new literal with the supplied label. The return value of {@link Literal#getDatatype()} for the returned
	 * object must be <a href="http://www.w3.org/2001/XMLSchema#string">{@code xsd:string}</a>.
	 *
	 * @param label The literal's label, must not be <tt>null</tt>.
	 */
	public Literal createLiteral(String label);

	/**
	 * Creates a new literal with the supplied label and language attribute. The return value of
	 * {@link Literal#getDatatype()} for the returned object must be
	 * <a href="http://www.w3.org/1999/02/22-rdf-syntax-ns#langString">{@code rdf:langString}</a>.
	 *
	 * @param label    The literal's label, must not be <tt>null</tt>.
	 * @param language The literal's language attribute, must not be <tt>null</tt>.
	 */
	public Literal createLiteral(String label, String language);

	/**
	 * Creates a new literal with the supplied label and datatype.
	 *
	 * @param label    The literal's label, must not be <tt>null</tt>.
	 * @param datatype The literal's datatype. If it is null, the datatype
	 *                 <a href="http://www.w3.org/2001/XMLSchema#string">{@code xsd:string}</a> will be assigned to this
	 *                 literal.
	 */
	public Literal createLiteral(String label, IRI datatype);

	/**
	 * Creates a new literal with the supplied label and datatype.
	 *
	 * @param label    The literal's label.
	 * @param datatype The literal's datatype. If it is null, the datatype
	 *                 <a href="http://www.w3.org/2001/XMLSchema#string">{@code xsd:string}</a> will be assigned to this
	 *                 literal.
	 * @deprecated Use {@link #createLiteral(String, IRI)} instead.
	 */
	@Deprecated
	public default Literal createLiteral(String label, URI datatype) {
		return createLiteral(label, (IRI) datatype);
	}

	/**
	 * Creates a new <tt>xsd:boolean</tt>-typed literal representing the specified value.
	 *
	 * @param value The value for the literal.
	 * @return An <tt>xsd:boolean</tt>-typed literal for the specified value.
	 */
	public Literal createLiteral(boolean value);

	/**
	 * Creates a new <tt>xsd:byte</tt>-typed literal representing the specified value.
	 *
	 * @param value The value for the literal.
	 * @return An <tt>xsd:byte</tt>-typed literal for the specified value.
	 */
	public Literal createLiteral(byte value);

	/**
	 * Creates a new <tt>xsd:short</tt>-typed literal representing the specified value.
	 *
	 * @param value The value for the literal.
	 * @return An <tt>xsd:short</tt>-typed literal for the specified value.
	 */
	public Literal createLiteral(short value);

	/**
	 * Creates a new <tt>xsd:int</tt>-typed literal representing the specified value.
	 *
	 * @param value The value for the literal.
	 * @return An <tt>xsd:int</tt>-typed literal for the specified value.
	 */
	public Literal createLiteral(int value);

	/**
	 * Creates a new <tt>xsd:long</tt>-typed literal representing the specified value.
	 *
	 * @param value The value for the literal.
	 * @return An <tt>xsd:long</tt>-typed literal for the specified value.
	 */
	public Literal createLiteral(long value);

	/**
	 * Creates a new <tt>xsd:float</tt>-typed literal representing the specified value.
	 *
	 * @param value The value for the literal.
	 * @return An <tt>xsd:float</tt>-typed literal for the specified value.
	 */
	public Literal createLiteral(float value);

	/**
	 * Creates a new <tt>xsd:double</tt>-typed literal representing the specified value.
	 *
	 * @param value The value for the literal.
	 * @return An <tt>xsd:double</tt>-typed literal for the specified value.
	 */
	public Literal createLiteral(double value);

	/**
	 * Creates a new literal representing the specified bigDecimal that is typed as an <tt>xsd:Decimal</tt>.
	 */
	public Literal createLiteral(BigDecimal bigDecimal);

	/**
	 * Creates a new literal representing the specified bigInteger that is typed as an <tt>xsd:Integer</tt>.
	 */
	public Literal createLiteral(BigInteger bigInteger);

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
	public default Literal createLiteral(TemporalAccessor value) {
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
	public default Literal createLiteral(TemporalAmount value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Creates a new literal representing the specified calendar that is typed using the appropriate XML Schema
	 * date/time datatype.
	 *
	 * @param calendar The value for the literal.
	 * @return A typed literal for the specified calendar.
	 */
	public Literal createLiteral(XMLGregorianCalendar calendar);

	/**
	 * Creates a new literal representing the specified date that is typed using the appropriate XML Schema date/time
	 * datatype.
	 */
	public Literal createLiteral(Date date);

	/**
	 * Creates a new statement with the supplied subject, predicate and object.
	 *
	 * @param subject   The statement's subject.
	 * @param predicate The statement's predicate.
	 * @param object    The statement's object.
	 * @return The created statement.
	 */
	public Statement createStatement(Resource subject, IRI predicate, Value object);

	/**
	 * Creates a new statement with the supplied subject, predicate and object.
	 *
	 * @param subject   The statement's subject.
	 * @param predicate The statement's predicate.
	 * @param object    The statement's object.
	 * @return The created statement.
	 * @deprecated Use {@link #createStatement(Resource, IRI, Value)} instead.
	 */
	@Deprecated
	public default Statement createStatement(Resource subject, URI predicate, Value object) {
		return createStatement(subject, (IRI) predicate, object);
	}

	/**
	 * Creates a new statement with the supplied subject, predicate and object and associated context.
	 *
	 * @param subject   The statement's subject.
	 * @param predicate The statement's predicate.
	 * @param object    The statement's object.
	 * @param context   The statement's context.
	 * @return The created statement.
	 */
	public Statement createStatement(Resource subject, IRI predicate, Value object, Resource context);

	/**
	 * Creates a new statement with the supplied subject, predicate and object and associated context.
	 *
	 * @param subject   The statement's subject.
	 * @param predicate The statement's predicate.
	 * @param object    The statement's object.
	 * @return The created statement.
	 * @deprecated Use {@link #createStatement(Resource, IRI, Value, Resource)} instead.
	 */
	@Deprecated
	public default Statement createStatement(Resource subject, URI predicate, Value object, Resource context) {
		return createStatement(subject, (IRI) predicate, object, context);
	}

	/**
	 * Creates a new RDF* triple with the supplied subject, predicate and object.
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
