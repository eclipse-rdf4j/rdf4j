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
import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * A factory for creating {@link IRI IRIs}, {@link BNode blank nodes},
 * {@link Literal literals} and {@link Statement statements}.
 * 
 * @author Arjohn Kampman
 */
public interface ValueFactory {

	/**
	 * Creates a new IRI from the supplied string-representation.
	 * 
	 * @param iri
	 *        A string-representation of a IRI.
	 * @return An object representing the IRI.
	 * @throws IlllegalArgumentException
	 *         If the supplied string does not resolve to a legal (absolute) IRI.
	 */
	public IRI createIRI(String iri);

	/**
	 * Creates a new URI from the supplied string-representation.
	 * 
	 * @param uri
	 *        A string-representation of a URI.
	 * @return An object representing the URI.
	 * @throws IlllegalArgumentException
	 *         If the supplied string does not resolve to a legal (absolute) URI.
	 * @deprecated since 4.0. Use {{@link #createIRI(String)} instead.
	 */
	@Deprecated
	public default URI createURI(String uri) {
		return createIRI(uri);
	}

	/**
	 * Creates a new IRI from the supplied namespace and local name. Calling this
	 * method is funtionally equivalent to calling {@link #createIRI(String)
	 * createIRI(namespace+localName)}, but allows the ValueFactory to reuse
	 * supplied namespace and local name strings whenever possible. Note that the
	 * values returned by {@link IRI#getNamespace()} and
	 * {@link IRI#getLocalName()} are not necessarily the same as the values that
	 * are supplied to this method.
	 * 
	 * @param namespace
	 *        The IRI's namespace.
	 * @param localName
	 *        The IRI's local name.
	 * @throws IllegalArgumentException
	 *         If the supplied namespace and localname do not resolve to a legal
	 *         (absolute) IRI.
	 */
	public IRI createIRI(String namespace, String localName);

	/**
	 * Creates a new URI from the supplied namespace and local name.
	 * 
	 * @param uri
	 *        A string-representation of a URI.
	 * @return An object representing the URI.
	 * @throws IlllegalArgumentException
	 *         If the supplied string does not resolve to a legal (absolute) URI.
	 * @deprecated since 4.0. Use {{@link #createIRI(String, String)} instead.
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
	 * @param nodeID
	 *        The blank node identifier.
	 * @return An object representing the blank node.
	 */
	public BNode createBNode(String nodeID);

	/**
	 * Creates a new literal with the supplied label.
	 * 
	 * @param label
	 *        The literal's label.
	 */
	public Literal createLiteral(String label);

	/**
	 * Creates a new literal with the supplied label and language attribute.
	 * 
	 * @param label
	 *        The literal's label.
	 * @param language
	 *        The literal's language attribute, or <tt>null</tt> if the literal
	 *        doesn't have a language.
	 */
	public Literal createLiteral(String label, String language);

	/**
	 * Creates a new literal with the supplied label and datatype.
	 * 
	 * @param label
	 *        The literal's label.
	 * @param datatype
	 *        The literal's datatype, or <tt>null</tt> if the literal doesn't
	 *        have a datatype.
	 */
	public Literal createLiteral(String label, IRI datatype);

	/**
	 * Creates a new literal with the supplied label and datatype.
	 * 
	 * @param label
	 *        The literal's label.
	 * @param datatype
	 *        The literal's datatype, or <tt>null</tt> if the literal doesn't
	 *        have a datatype.
	 * @deprecated since 4.0. Use {@link #createLiteral(String, IRI)} instead.
	 */
	@Deprecated
	public default Literal createLiteral(String label, URI datatype) {
		return createLiteral(label, (IRI)datatype);
	}

	/**
	 * Creates a new <tt>xsd:boolean</tt>-typed literal representing the
	 * specified value.
	 * 
	 * @param value
	 *        The value for the literal.
	 * @return An <tt>xsd:boolean</tt>-typed literal for the specified value.
	 */
	public Literal createLiteral(boolean value);

	/**
	 * Creates a new <tt>xsd:byte</tt>-typed literal representing the specified
	 * value.
	 * 
	 * @param value
	 *        The value for the literal.
	 * @return An <tt>xsd:byte</tt>-typed literal for the specified value.
	 */
	public Literal createLiteral(byte value);

	/**
	 * Creates a new <tt>xsd:short</tt>-typed literal representing the specified
	 * value.
	 * 
	 * @param value
	 *        The value for the literal.
	 * @return An <tt>xsd:short</tt>-typed literal for the specified value.
	 */
	public Literal createLiteral(short value);

	/**
	 * Creates a new <tt>xsd:int</tt>-typed literal representing the specified
	 * value.
	 * 
	 * @param value
	 *        The value for the literal.
	 * @return An <tt>xsd:int</tt>-typed literal for the specified value.
	 */
	public Literal createLiteral(int value);

	/**
	 * Creates a new <tt>xsd:long</tt>-typed literal representing the specified
	 * value.
	 * 
	 * @param value
	 *        The value for the literal.
	 * @return An <tt>xsd:long</tt>-typed literal for the specified value.
	 */
	public Literal createLiteral(long value);

	/**
	 * Creates a new <tt>xsd:float</tt>-typed literal representing the specified
	 * value.
	 * 
	 * @param value
	 *        The value for the literal.
	 * @return An <tt>xsd:float</tt>-typed literal for the specified value.
	 */
	public Literal createLiteral(float value);

	/**
	 * Creates a new <tt>xsd:double</tt>-typed literal representing the specified
	 * value.
	 * 
	 * @param value
	 *        The value for the literal.
	 * @return An <tt>xsd:double</tt>-typed literal for the specified value.
	 */
	public Literal createLiteral(double value);

	/**
	 * Creates a new literal representing the specified bigDecimal that is typed
	 * using the appropriate XML Schema date/time datatype.
	 * 
	 * @since 4.0
	 */
	public Literal createLiteral(BigDecimal bigDecimal);

	/**
	 * Creates a new literal representing the specified bigInteger that is typed
	 * using the appropriate XML Schema date/time datatype.
	 * 
	 * @since 4.0
	 */
	public Literal createLiteral(BigInteger bigInteger);

	/**
	 * Creates a new literal representing the specified calendar that is typed
	 * using the appropriate XML Schema date/time datatype.
	 * 
	 * @param calendar
	 *        The value for the literal.
	 * @return An typed literal for the specified calendar.
	 */
	public Literal createLiteral(XMLGregorianCalendar calendar);

	/**
	 * Creates a new literal representing the specified date that is typed using
	 * the appropriate XML Schema date/time datatype.
	 * 
	 * @since 2.7.0
	 */
	public Literal createLiteral(Date date);

	/**
	 * Creates a new statement with the supplied subject, predicate and object.
	 * 
	 * @param subject
	 *        The statement's subject.
	 * @param predicate
	 *        The statement's predicate.
	 * @param object
	 *        The statement's object.
	 * @return The created statement.
	 */
	public Statement createStatement(Resource subject, IRI predicate, Value object);

	/**
	 * Creates a new statement with the supplied subject, predicate and object.
	 * 
	 * @param subject
	 *        The statement's subject.
	 * @param predicate
	 *        The statement's predicate.
	 * @param object
	 *        The statement's object.
	 * @return The created statement.
	 * @deprecated since 4.0. Use {@link #createStatement(Resource, IRI, Value)}
	 *             instead.
	 */
	@Deprecated
	public default Statement createStatement(Resource subject, URI predicate, Value object) {
		return createStatement(subject, (IRI)predicate, object);
	}

	/**
	 * Creates a new statement with the supplied subject, predicate and object
	 * and associated context.
	 * 
	 * @param subject
	 *        The statement's subject.
	 * @param predicate
	 *        The statement's predicate.
	 * @param object
	 *        The statement's object.
	 * @param context
	 *        The statement's context.
	 * @return The created statement.
	 */
	public Statement createStatement(Resource subject, IRI predicate, Value object, Resource context);

	/**
	 * Creates a new statement with the supplied subject, predicate and object
	 * and associated context.
	 * 
	 * @param subject
	 *        The statement's subject.
	 * @param predicate
	 *        The statement's predicate.
	 * @param object
	 *        The statement's object.
	 * @return The created statement.
	 * @deprecated since 4.0. Use
	 *             {@link #createStatement(Resource, IRI, Value, Resource)}
	 *             instead.
	 */
	@Deprecated
	public default Statement createStatement(Resource subject, URI predicate, Value object, Resource context) {
		return createStatement(subject, (IRI)predicate, object, context);
	}
}
