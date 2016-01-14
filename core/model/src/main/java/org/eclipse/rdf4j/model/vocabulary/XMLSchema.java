/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the standard <a href="http://www.w3.org/TR/xmlschema-2/">XML
 * Schema datatypes</a>.
 * 
 * @see <a href="http://www.w3.org/TR/xmlschema-2/">XML Schema Part 2: Datatypes
 *      Second Edition</a>
 */
public class XMLSchema {

	/*
	 * The XML Schema namespace
	 */

	/** The XML Schema namespace (<tt>http://www.w3.org/2001/XMLSchema#</tt>). */
	public static final String NAMESPACE = "http://www.w3.org/2001/XMLSchema#";

	/**
	 * Recommended prefix for XML Schema datatypes: "xsd"
	 */
	public static final String PREFIX = "xsd";

	/**
	 * An immutable {@link Namespace} constant that represents the XML Schema
	 * namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	/*
	 * Primitive datatypes
	 */

	/** <tt>http://www.w3.org/2001/XMLSchema#duration</tt> */
	public final static IRI DURATION;

	/** <tt>http://www.w3.org/2001/XMLSchema#dateTime</tt> */
	public final static IRI DATETIME;

	/** <tt>http://www.w3.org/2001/XMLSchema#dayTimeDuration</tt> */
	public static final IRI DAYTIMEDURATION;

	/** <tt>http://www.w3.org/2001/XMLSchema#time</tt> */
	public final static IRI TIME;

	/** <tt>http://www.w3.org/2001/XMLSchema#date</tt> */
	public final static IRI DATE;

	/** <tt>http://www.w3.org/2001/XMLSchema#gYearMonth</tt> */
	public final static IRI GYEARMONTH;

	/** <tt>http://www.w3.org/2001/XMLSchema#gYear</tt> */
	public final static IRI GYEAR;

	/** <tt>http://www.w3.org/2001/XMLSchema#gMonthDay</tt> */
	public final static IRI GMONTHDAY;

	/** <tt>http://www.w3.org/2001/XMLSchema#gDay</tt> */
	public final static IRI GDAY;

	/** <tt>http://www.w3.org/2001/XMLSchema#gMonth</tt> */
	public final static IRI GMONTH;

	/** <tt>http://www.w3.org/2001/XMLSchema#string</tt> */
	public final static IRI STRING;

	/** <tt>http://www.w3.org/2001/XMLSchema#boolean</tt> */
	public final static IRI BOOLEAN;

	/** <tt>http://www.w3.org/2001/XMLSchema#base64Binary</tt> */
	public final static IRI BASE64BINARY;

	/** <tt>http://www.w3.org/2001/XMLSchema#hexBinary</tt> */
	public final static IRI HEXBINARY;

	/** <tt>http://www.w3.org/2001/XMLSchema#float</tt> */
	public final static IRI FLOAT;

	/** <tt>http://www.w3.org/2001/XMLSchema#decimal</tt> */
	public final static IRI DECIMAL;

	/** <tt>http://www.w3.org/2001/XMLSchema#double</tt> */
	public final static IRI DOUBLE;

	/** <tt>http://www.w3.org/2001/XMLSchema#anyURI</tt> */
	public final static IRI ANYURI;

	/** <tt>http://www.w3.org/2001/XMLSchema#QName</tt> */
	public final static IRI QNAME;

	/** <tt>http://www.w3.org/2001/XMLSchema#NOTATION</tt> */
	public final static IRI NOTATION;

	/*
	 * Derived datatypes
	 */

	/** <tt>http://www.w3.org/2001/XMLSchema#normalizedString</tt> */
	public final static IRI NORMALIZEDSTRING;

	/** <tt>http://www.w3.org/2001/XMLSchema#token</tt> */
	public final static IRI TOKEN;

	/** <tt>http://www.w3.org/2001/XMLSchema#language</tt> */
	public final static IRI LANGUAGE;

	/** <tt>http://www.w3.org/2001/XMLSchema#NMTOKEN</tt> */
	public final static IRI NMTOKEN;

	/** <tt>http://www.w3.org/2001/XMLSchema#NMTOKENS</tt> */
	public final static IRI NMTOKENS;

	/** <tt>http://www.w3.org/2001/XMLSchema#Name</tt> */
	public final static IRI NAME;

	/** <tt>http://www.w3.org/2001/XMLSchema#NCName</tt> */
	public final static IRI NCNAME;

	/** <tt>http://www.w3.org/2001/XMLSchema#ID</tt> */
	public final static IRI ID;

	/** <tt>http://www.w3.org/2001/XMLSchema#IDREF</tt> */
	public final static IRI IDREF;

	/** <tt>http://www.w3.org/2001/XMLSchema#IDREFS</tt> */
	public final static IRI IDREFS;

	/** <tt>http://www.w3.org/2001/XMLSchema#ENTITY</tt> */
	public final static IRI ENTITY;

	/** <tt>http://www.w3.org/2001/XMLSchema#ENTITIES</tt> */
	public final static IRI ENTITIES;

	/** <tt>http://www.w3.org/2001/XMLSchema#integer</tt> */
	public final static IRI INTEGER;

	/** <tt>http://www.w3.org/2001/XMLSchema#long</tt> */
	public final static IRI LONG;

	/** <tt>http://www.w3.org/2001/XMLSchema#int</tt> */
	public final static IRI INT;

	/** <tt>http://www.w3.org/2001/XMLSchema#short</tt> */
	public final static IRI SHORT;

	/** <tt>http://www.w3.org/2001/XMLSchema#byte</tt> */
	public final static IRI BYTE;

	/** <tt>http://www.w3.org/2001/XMLSchema#nonPositiveInteger</tt> */
	public final static IRI NON_POSITIVE_INTEGER;

	/** <tt>http://www.w3.org/2001/XMLSchema#negativeInteger</tt> */
	public final static IRI NEGATIVE_INTEGER;

	/** <tt>http://www.w3.org/2001/XMLSchema#nonNegativeInteger</tt> */
	public final static IRI NON_NEGATIVE_INTEGER;

	/** <tt>http://www.w3.org/2001/XMLSchema#positiveInteger</tt> */
	public final static IRI POSITIVE_INTEGER;

	/** <tt>http://www.w3.org/2001/XMLSchema#unsignedLong</tt> */
	public final static IRI UNSIGNED_LONG;

	/** <tt>http://www.w3.org/2001/XMLSchema#unsignedInt</tt> */
	public final static IRI UNSIGNED_INT;

	/** <tt>http://www.w3.org/2001/XMLSchema#unsignedShort</tt> */
	public final static IRI UNSIGNED_SHORT;

	/** <tt>http://www.w3.org/2001/XMLSchema#unsignedByte</tt> */
	public final static IRI UNSIGNED_BYTE;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();

		DURATION = factory.createIRI(XMLSchema.NAMESPACE, "duration");

		DATETIME = factory.createIRI(XMLSchema.NAMESPACE, "dateTime");

		DAYTIMEDURATION = factory.createIRI(NAMESPACE, "dayTimeDuration");

		TIME = factory.createIRI(XMLSchema.NAMESPACE, "time");

		DATE = factory.createIRI(XMLSchema.NAMESPACE, "date");

		GYEARMONTH = factory.createIRI(XMLSchema.NAMESPACE, "gYearMonth");

		GYEAR = factory.createIRI(XMLSchema.NAMESPACE, "gYear");

		GMONTHDAY = factory.createIRI(XMLSchema.NAMESPACE, "gMonthDay");

		GDAY = factory.createIRI(XMLSchema.NAMESPACE, "gDay");

		GMONTH = factory.createIRI(XMLSchema.NAMESPACE, "gMonth");

		STRING = factory.createIRI(XMLSchema.NAMESPACE, "string");

		BOOLEAN = factory.createIRI(XMLSchema.NAMESPACE, "boolean");

		BASE64BINARY = factory.createIRI(XMLSchema.NAMESPACE, "base64Binary");

		HEXBINARY = factory.createIRI(XMLSchema.NAMESPACE, "hexBinary");

		FLOAT = factory.createIRI(XMLSchema.NAMESPACE, "float");

		DECIMAL = factory.createIRI(XMLSchema.NAMESPACE, "decimal");

		DOUBLE = factory.createIRI(XMLSchema.NAMESPACE, "double");

		ANYURI = factory.createIRI(XMLSchema.NAMESPACE, "anyURI");

		QNAME = factory.createIRI(XMLSchema.NAMESPACE, "QName");

		NOTATION = factory.createIRI(XMLSchema.NAMESPACE, "NOTATION");

		NORMALIZEDSTRING = factory.createIRI(XMLSchema.NAMESPACE, "normalizedString");

		TOKEN = factory.createIRI(XMLSchema.NAMESPACE, "token");

		LANGUAGE = factory.createIRI(XMLSchema.NAMESPACE, "language");

		NMTOKEN = factory.createIRI(XMLSchema.NAMESPACE, "NMTOKEN");

		NMTOKENS = factory.createIRI(XMLSchema.NAMESPACE, "NMTOKENS");

		NAME = factory.createIRI(XMLSchema.NAMESPACE, "Name");

		NCNAME = factory.createIRI(XMLSchema.NAMESPACE, "NCName");

		ID = factory.createIRI(XMLSchema.NAMESPACE, "ID");

		IDREF = factory.createIRI(XMLSchema.NAMESPACE, "IDREF");

		IDREFS = factory.createIRI(XMLSchema.NAMESPACE, "IDREFS");

		ENTITY = factory.createIRI(XMLSchema.NAMESPACE, "ENTITY");

		ENTITIES = factory.createIRI(XMLSchema.NAMESPACE, "ENTITIES");

		INTEGER = factory.createIRI(XMLSchema.NAMESPACE, "integer");

		LONG = factory.createIRI(XMLSchema.NAMESPACE, "long");

		INT = factory.createIRI(XMLSchema.NAMESPACE, "int");

		SHORT = factory.createIRI(XMLSchema.NAMESPACE, "short");

		BYTE = factory.createIRI(XMLSchema.NAMESPACE, "byte");

		NON_POSITIVE_INTEGER = factory.createIRI(XMLSchema.NAMESPACE, "nonPositiveInteger");

		NEGATIVE_INTEGER = factory.createIRI(XMLSchema.NAMESPACE, "negativeInteger");

		NON_NEGATIVE_INTEGER = factory.createIRI(XMLSchema.NAMESPACE, "nonNegativeInteger");

		POSITIVE_INTEGER = factory.createIRI(XMLSchema.NAMESPACE, "positiveInteger");

		UNSIGNED_LONG = factory.createIRI(XMLSchema.NAMESPACE, "unsignedLong");

		UNSIGNED_INT = factory.createIRI(XMLSchema.NAMESPACE, "unsignedInt");

		UNSIGNED_SHORT = factory.createIRI(XMLSchema.NAMESPACE, "unsignedShort");

		UNSIGNED_BYTE = factory.createIRI(XMLSchema.NAMESPACE, "unsignedByte");
	}
}
