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
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * Default implementation of the {@link ValueFactory} interface.
 *
 * @author Arjohn Kampman
 * 
 * @implNote the implementation duplicates code from {@link AbstractValueFactory} - this is done to avoid deprecation
 *           warnings in end-user calling code - see https://github.com/eclipse/rdf4j/issues/2655. We can not
 *           remove/replace the abstract superclass without breaking binary compatibility so this will be handled in a
 *           followup, as part of a major release. See https://github.com/eclipse/rdf4j/issues/2663.
 */
@SuppressWarnings("deprecation")
public class SimpleValueFactory extends AbstractValueFactory {

	/* Constants */

	private static final SimpleValueFactory sharedInstance = new SimpleValueFactory();

	/**
	 * "universal" ID for bnode prefixes to prevent blank node clashes (unique per classloaded instance of this class)
	 */
	private static long lastBNodePrefixUID = 0;

	private static synchronized long getNextBNodePrefixUid() {
		return lastBNodePrefixUID = Math.max(System.currentTimeMillis(), lastBNodePrefixUID + 1);
	}

	private static final DatatypeFactory datatypeFactory;

	static {
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new Error("Could not instantiate javax.xml.datatype.DatatypeFactory", e);
		}
	}

	/* variables */

	/**
	 * @category variables
	 * 
	 *           The ID for the next bnode that is created.
	 */
	private int nextBNodeID;

	/**
	 * @category variables
	 * 
	 *           The prefix for any new bnode IDs.
	 */
	private String bnodePrefix;

	/**
	 * Provide a single shared instance of a SimpleValueFactory.
	 *
	 * @return a singleton instance of SimpleValueFactory.
	 */
	public static SimpleValueFactory getInstance() {
		return sharedInstance;
	}

	/**
	 * Hidden constructor to enforce singleton pattern.
	 */
	protected SimpleValueFactory() {
		initBNodeParams();
	}

	/* Public methods */

	@Override
	public IRI createIRI(String iri) {
		return new SimpleIRI(iri);
	}

	@Override
	public IRI createIRI(String namespace, String localName) {
		return createIRI(namespace + localName);
	}

	@Override
	public BNode createBNode(String nodeID) {
		return new SimpleBNode(nodeID);
	}

	@Override
	public Literal createLiteral(String value) {
		return new SimpleLiteral(value, XSD.Datatype.STRING);
	}

	@Override
	public Literal createLiteral(String value, String language) {
		return new SimpleLiteral(value, language);
	}

	@Override
	public Literal createLiteral(boolean b) {
		return b ? BooleanLiteral.TRUE : BooleanLiteral.FALSE;
	}

	@Override
	public Literal createLiteral(String value, IRI datatype) {
		return new SimpleLiteral(value, datatype);
	}

	@Override
	public Statement createStatement(Resource subject, IRI predicate, Value object) {
		return new SimpleStatement(subject, predicate, object);
	}

	@Override
	public Statement createStatement(Resource subject, IRI predicate, Value object, Resource context) {
		return new ContextStatement(subject, predicate, object, context);
	}

	@Override
	public Triple createTriple(Resource subject, IRI predicate, Value object) {
		return new SimpleTriple(subject, predicate, object);
	}

	/**
	 * Generates a new bnode prefix and resets <tt>nextBNodeID</tt> to <tt>1</tt> .
	 */
	protected void initBNodeParams() {
		// BNode prefix is based on currentTimeMillis(). Combined with a
		// sequential number per session, this gives a unique identifier.
		bnodePrefix = "node" + Long.toString(getNextBNodePrefixUid(), 32) + "x";
		nextBNodeID = 1;
	}

	@Override
	public synchronized BNode createBNode() {
		int id = nextBNodeID++;

		BNode result = createBNode(bnodePrefix + id);

		if (id == Integer.MAX_VALUE) {
			// Start with a new bnode prefix
			initBNodeParams();
		}

		return result;
	}

	/**
	 * Calls {@link #createIntegerLiteral(Number, IRI)} with the supplied value and {@link XSD#BYTE} as parameters.
	 */
	@Override
	public Literal createLiteral(byte value) {
		return createIntegerLiteral(value, XSD.Datatype.BYTE);
	}

	/**
	 * Calls {@link #createIntegerLiteral(Number, IRI)} with the supplied value and {@link XSD#SHORT} as parameters.
	 */
	@Override
	public Literal createLiteral(short value) {
		return createIntegerLiteral(value, XSD.Datatype.SHORT);
	}

	/**
	 * Calls {@link #createIntegerLiteral(Number, IRI)} with the supplied value and {@link XSD#INT} as parameters.
	 */
	@Override
	public Literal createLiteral(int value) {
		return createIntegerLiteral(value, XSD.Datatype.INT);
	}

	/**
	 * Calls {@link #createIntegerLiteral(Number, IRI)} with the supplied value and {@link XSD#LONG} as parameters.
	 */
	@Override
	public Literal createLiteral(long value) {
		return createIntegerLiteral(value, XSD.Datatype.LONG);
	}

	/**
	 * Calls {@link #createNumericLiteral(Number, IRI)} with the supplied value and datatype as parameters.
	 */
	protected Literal createIntegerLiteral(Number value, IRI datatype) {
		return createNumericLiteral(value, datatype);
	}

	protected Literal createIntegerLiteral(Number value, XSD.Datatype datatype) {
		return createNumericLiteral(value, datatype);
	}

	/**
	 * Calls {@link #createFPLiteral(Number, IRI)} with the supplied value and {@link XSD#FLOAT} as parameters.
	 */
	@Override
	public Literal createLiteral(float value) {
		return createFPLiteral(value, XSD.Datatype.FLOAT);
	}

	/**
	 * Calls {@link #createFPLiteral(Number, IRI)} with the supplied value and {@link XSD#DOUBLE} as parameters.
	 */
	@Override
	public Literal createLiteral(double value) {
		return createFPLiteral(value, XSD.Datatype.DOUBLE);
	}

	@Override
	public Literal createLiteral(BigInteger bigInteger) {
		return createIntegerLiteral(bigInteger, XSD.INTEGER);
	}

	@Override
	public Literal createLiteral(BigDecimal bigDecimal) {
		return createNumericLiteral(bigDecimal, XSD.DECIMAL);
	}

	/**
	 * Calls {@link #createNumericLiteral(Number, IRI)} with the supplied value and datatype as parameters.
	 */
	protected Literal createFPLiteral(Number value, IRI datatype) {
		return createNumericLiteral(value, datatype);
	}

	protected Literal createFPLiteral(Number value, XSD.Datatype datatype) {
		return createNumericLiteral(value, datatype);
	}

	/**
	 * Creates specific optimized subtypes of SimpleLiteral for numeric datatypes.
	 */
	protected Literal createNumericLiteral(Number number, IRI datatype) {
		if (number instanceof BigDecimal) {
			return new DecimalLiteral((BigDecimal) number, datatype);
		}
		if (number instanceof BigInteger) {
			return new IntegerLiteral((BigInteger) number, datatype);
		}
		return new NumericLiteral(number, datatype);
	}

	protected Literal createNumericLiteral(Number number, XSD.Datatype datatype) {
		if (number instanceof BigDecimal) {
			return new DecimalLiteral((BigDecimal) number, datatype);
		}
		if (number instanceof BigInteger) {
			return new IntegerLiteral((BigInteger) number, datatype);
		}
		return new NumericLiteral(number, datatype);
	}

	/**
	 * Calls {@link ValueFactory#createLiteral(String, IRI)} with the String-value of the supplied calendar and the
	 * appropriate datatype as parameters.
	 *
	 * @see XMLGregorianCalendar#toXMLFormat()
	 * @see XMLGregorianCalendar#getXMLSchemaType()
	 * @see XMLDatatypeUtil#qnameToURI(javax.xml.namespace.QName)
	 */
	@Override
	public Literal createLiteral(XMLGregorianCalendar calendar) {
		return createLiteral(calendar.toXMLFormat(), XMLDatatypeUtil.qnameToURI(calendar.getXMLSchemaType()));
	}

	/**
	 * Converts the supplied {@link Date} to a {@link XMLGregorianCalendar}, then calls
	 * {@link ValueFactory#createLiteral(XMLGregorianCalendar)}.
	 */
	@Override
	public Literal createLiteral(Date date) {
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(date);

		XMLGregorianCalendar xmlGregCalendar = datatypeFactory.newXMLGregorianCalendar(c);
		return createLiteral(xmlGregCalendar);
	}
}
