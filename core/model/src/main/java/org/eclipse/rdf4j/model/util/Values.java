/******************************************************************************* 
 * Copyright (c) 2020 Eclipse RDF4J contributors. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Distribution License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php. 
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.ValidatingValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * Convenience functions to quickly create {@link Value} objects without having to create a {@link ValueFactory} first.
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
	 * @return an {@link IRI} object for the supplied iri string.
	 * @throws NullPointerException     if the suppplied iri is <code>null</code>
	 * @throws IllegalArgumentException if the supplied iri string can not be parsed as a legal IRI.
	 */
	public static IRI iri(String iri) throws IllegalArgumentException {
		return VALUE_FACTORY.createIRI(Objects.requireNonNull(iri, "iri may not be null"));
	}

	/**
	 * Create a new {@link IRI} using the supplied namespace and local name
	 * 
	 * @param namespace the IRI's namespace
	 * @param localName the IRI's local name
	 * @return an {@link IRI} object for the supplied IRI namespace and localName.
	 * @throws NullPointerException     if the suppplied namespace or localName is <code>null</code>.
	 * @throws IllegalArgumentException if the supplied iri string can not be parsed as a legal IRI.
	 */
	public static IRI iri(String namespace, String localName) throws IllegalArgumentException {
		return VALUE_FACTORY.createIRI(Objects.requireNonNull(namespace, "namespace may not be null"),
				Objects.requireNonNull(localName, "localName may not be null"));
	}

	/* blank node factory methods */

	/**
	 * Creates a new {@link BNode}
	 * 
	 * @return a new {@link BNode}
	 */
	public static BNode bnode() {
		return VALUE_FACTORY.createBNode();
	}

	/**
	 * Creates a new {@link BNode} with the supplied node identifier.
	 * 
	 * @param nodeId the node identifier
	 * @return a new {@link BNode}
	 * @throws NullPointerException     if the supplied node identifier is <code>null</code>.
	 * @throws IllegalArgumentException if the supplied node identifier is not valid
	 */
	public static BNode bnode(String nodeId) throws IllegalArgumentException {
		return VALUE_FACTORY.createBNode(Objects.requireNonNull(nodeId, "nodeId may not be null"));
	}

	/* literal factory methods */

	/**
	 * Creates a new {@link Literal} with the supplied lexical value.
	 * 
	 * @param lexicalValue the lexical value for the literal
	 * @return a new {@link Literal} of type {@link XSD#STRING}
	 * @throws NullPointerException if the supplied lexical value is <code>null</code>.
	 */
	public static Literal literal(String lexicalValue) {
		return VALUE_FACTORY.createLiteral(Objects.requireNonNull(lexicalValue, "lexicalValue may not be null"));
	}

	/**
	 * Creates a new {@link Literal} with the supplied lexical value and datatype.
	 * 
	 * @param lexicalValue the lexical value for the literal
	 * @param datatype     the datatype URI
	 * @return a new {@link Literal} with the supplied lexical value and datatype
	 * @throws NullPointerException     if the supplied lexical value or datatype is <code>null</code>.
	 * @throws IllegalArgumentException if the supplied lexical value is not valid for the given datatype
	 */
	public static Literal literal(String lexicalValue, IRI datatype) throws IllegalArgumentException {
		return VALUE_FACTORY.createLiteral(Objects.requireNonNull(lexicalValue, "lexicalValue may not be null"),
				Objects.requireNonNull(datatype, "datatype may not be null"));
	}

	/**
	 * Creates a new {@link Literal} with the supplied boolean value
	 * 
	 * @param booleanValue a boolean value
	 * @return a {@link Literal} of type {@link XSD#BOOLEAN} with the supplied value
	 */
	public static Literal literal(boolean booleanValue) {
		return VALUE_FACTORY.createLiteral(booleanValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied byte value
	 * 
	 * @param byteValue a byte value
	 * @return a {@link Literal} of type {@link XSD#BYTE} with the supplied value
	 */
	public static Literal literal(byte byteValue) {
		return VALUE_FACTORY.createLiteral(byteValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied short value
	 * 
	 * @param shortValue a short value
	 * @return a {@link Literal} of type {@link XSD#SHORT} with the supplied value
	 */
	public static Literal literal(short shortValue) {
		return VALUE_FACTORY.createLiteral(shortValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied int value
	 * 
	 * @param intValue an int value
	 * @return a {@link Literal} of type {@link XSD#INT} with the supplied value
	 */
	public static Literal literal(int intValue) {
		return VALUE_FACTORY.createLiteral(intValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied long value
	 * 
	 * @param longValue a long value
	 * @return a {@link Literal} of type {@link XSD#LONG} with the supplied value
	 */
	public static Literal literal(long longValue) {
		return VALUE_FACTORY.createLiteral(longValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied float value
	 * 
	 * @param floatValue an float value
	 * @return a {@link Literal} of type {@link XSD#FLOAT} with the supplied value
	 */
	public static Literal literal(float floatValue) {
		return VALUE_FACTORY.createLiteral(floatValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied double value
	 * 
	 * @param doubleValue a double value
	 * @return a {@link Literal} of type {@link XSD#DOUBLE} with the supplied value
	 */
	public static Literal literal(double doubleValue) {
		return VALUE_FACTORY.createLiteral(doubleValue);
	}

	/**
	 * Creates a new {@link Literal} with the supplied {@link BigDecimal} value
	 * 
	 * @param bigDecimal a {@link BigDecimal} value
	 * @return a {@link Literal} of type {@link XSD#DECIMAL} with the supplied value
	 * @throws NullPointerException if the supplied bigDecimal is <code>null</code>.
	 */
	public static Literal literal(BigDecimal bigDecimal) {
		return VALUE_FACTORY.createLiteral(Objects.requireNonNull(bigDecimal, "bigDecimal may not be null"));
	}

	/**
	 * Creates a new {@link Literal} with the supplied {@link BigInteger} value
	 * 
	 * @param bigInteger a {@link BigInteger} value
	 * @return a {@link Literal} of type {@link XSD#INTEGER} with the supplied value
	 * @throws NullPointerException if the supplied bigInteger is <code>null</code>.
	 */
	public static Literal literal(BigInteger bigInteger) {
		return VALUE_FACTORY.createLiteral(Objects.requireNonNull(bigInteger, "bigInteger may not be null"));
	}

	/**
	 * Creates a new {@link Literal} with the supplied {@link TemporalAccessor} value
	 * 
	 * @param value a {@link TemporalAccessor} value.
	 * @return a {@link Literal} with the supplied calendar value and the appropriate {@link XSD} date/time datatype for
	 *         the specific value.
	 * @throws NullPointerException     if the supplied {@link TemporalAccessor} value is <code>null</code>.
	 * @throws IllegalArgumentException if value cannot be represented by an XML Schema date/time datatype
	 */
	public static Literal literal(TemporalAccessor value) throws IllegalArgumentException {
		return VALUE_FACTORY.createLiteral(Objects.requireNonNull(value, "value may not be null"));
	}

	/* triple factory methods */

	/**
	 * Creates a new {@link Triple RDF* embedded triple} with the supplied subject, predicate, and object.
	 *
	 * @param subject   the Triple subject
	 * @param predicate the Triple predicate
	 * @param object    the Triple object
	 * @return a {@link Triple} with the supplied subject, predicate, and object.
	 * @throws NullPointerException if any of the supplied input parameters is <code>null</code>.
	 */
	public static Triple triple(Resource subject, IRI predicate, Value object) {
		return VALUE_FACTORY.createTriple(
				Objects.requireNonNull(subject, "subject may not be null"),
				Objects.requireNonNull(predicate, "predicate may not be null"),
				Objects.requireNonNull(object, "object may not be null")
		);
	}

	/**
	 * Creates a new {@link Triple RDF* embedded triple} using the subject, predicate and object from the supplied
	 * {@link Statement}.
	 *
	 * @param statement the {@link Statement} from which to construct a {@link Triple}
	 * @return a {@link Triple} with the same subject, predicate, and object as the supplied Statement.
	 */
	public static Triple triple(Statement statement) {
		Objects.requireNonNull(statement, "statement may not be null");
		return VALUE_FACTORY.createTriple(statement.getSubject(), statement.getPredicate(), statement.getObject());
	}

	/**
	 * Get a {@link ValueFactory}.
	 * 
	 * @return a {@link ValueFactory}.
	 */
	public static ValueFactory getValueFactory() {
		return VALUE_FACTORY;
	}

}
