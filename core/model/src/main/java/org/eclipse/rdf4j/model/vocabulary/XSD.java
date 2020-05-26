/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the standard <a href="https://www.w3.org/TR/xmlschema11-2/">XML Schema 1.1 datatypes</a>.
 *
 * @see <a href="https://www.w3.org/TR/xmlschema11-2/">XML Schema 1.1 Part 2: Datatypes</a>
 */
public class XSD {

	/** The XML Schema namespace (<tt>http://www.w3.org/2001/XMLSchema#</tt>). */
	public static final String NAMESPACE = "http://www.w3.org/2001/XMLSchema#";

	/**
	 * Recommended prefix for XML Schema datatypes: "xsd"
	 */
	public static final String PREFIX = "xsd";

	/**
	 * An immutable {@link Namespace} constant that represents the XML Schema namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	/*
	 * Primitive datatypes
	 */

	/** <tt>http://www.w3.org/2001/XMLSchema#duration</tt> */
	public final static IRI DURATION = create("duration");

	/** <tt>http://www.w3.org/2001/XMLSchema#dateTime</tt> */
	public final static IRI DATETIME = create("dateTime");

	/** <tt>http://www.w3.org/2001/XMLSchema#dateTimeStamp</tt> */
	public final static IRI DATETIMESTAMP = create("dateTimeStamp");

	/** <tt>http://www.w3.org/2001/XMLSchema#dayTimeDuration</tt> */
	public static final IRI DAYTIMEDURATION = create("dayTimeDuration");

	/** <tt>http://www.w3.org/2001/XMLSchema#time</tt> */
	public final static IRI TIME = create("time");

	/** <tt>http://www.w3.org/2001/XMLSchema#date</tt> */
	public final static IRI DATE = create("date");

	/** <tt>http://www.w3.org/2001/XMLSchema#gYearMonth</tt> */
	public final static IRI GYEARMONTH = create("gYearMonth");

	/** <tt>http://www.w3.org/2001/XMLSchema#gYear</tt> */
	public final static IRI GYEAR = create("gYear");

	/** <tt>http://www.w3.org/2001/XMLSchema#gMonthDay</tt> */
	public final static IRI GMONTHDAY = create("gMonthDay");

	/** <tt>http://www.w3.org/2001/XMLSchema#gDay</tt> */
	public final static IRI GDAY = create("gDay");

	/** <tt>http://www.w3.org/2001/XMLSchema#gMonth</tt> */
	public final static IRI GMONTH = create("gMonth");

	/** <tt>http://www.w3.org/2001/XMLSchema#string</tt> */
	public final static IRI STRING = create("string");

	/** <tt>http://www.w3.org/2001/XMLSchema#boolean</tt> */
	public final static IRI BOOLEAN = create("boolean");

	/** <tt>http://www.w3.org/2001/XMLSchema#base64Binary</tt> */
	public final static IRI BASE64BINARY = create("base64Binary");

	/** <tt>http://www.w3.org/2001/XMLSchema#hexBinary</tt> */
	public final static IRI HEXBINARY = create("hexBinary");

	/** <tt>http://www.w3.org/2001/XMLSchema#float</tt> */
	public final static IRI FLOAT = create("float");

	/** <tt>http://www.w3.org/2001/XMLSchema#decimal</tt> */
	public final static IRI DECIMAL = create("decimal");

	/** <tt>http://www.w3.org/2001/XMLSchema#double</tt> */
	public final static IRI DOUBLE = create("double");

	/** <tt>http://www.w3.org/2001/XMLSchema#anyURI</tt> */
	public final static IRI ANYURI = create("anyURI");

	/** <tt>http://www.w3.org/2001/XMLSchema#QName</tt> */
	public final static IRI QNAME = create("QName");

	/** <tt>http://www.w3.org/2001/XMLSchema#NOTATION</tt> */
	public final static IRI NOTATION = create("NOTATION");

	/*
	 * Derived datatypes
	 */

	/** <tt>http://www.w3.org/2001/XMLSchema#normalizedString</tt> */
	public final static IRI NORMALIZEDSTRING = create("normalizedString");

	/** <tt>http://www.w3.org/2001/XMLSchema#token</tt> */
	public final static IRI TOKEN = create("token");

	/** <tt>http://www.w3.org/2001/XMLSchema#language</tt> */
	public final static IRI LANGUAGE = create("language");

	/** <tt>http://www.w3.org/2001/XMLSchema#NMTOKEN</tt> */
	public final static IRI NMTOKEN = create("NMTOKEN");

	/** <tt>http://www.w3.org/2001/XMLSchema#NMTOKENS</tt> */
	public final static IRI NMTOKENS = create("NMTOKENS");

	/** <tt>http://www.w3.org/2001/XMLSchema#Name</tt> */
	public final static IRI NAME = create("Name");

	/** <tt>http://www.w3.org/2001/XMLSchema#NCName</tt> */
	public final static IRI NCNAME = create("NCName");

	/** <tt>http://www.w3.org/2001/XMLSchema#ID</tt> */
	public final static IRI ID = create("ID");

	/** <tt>http://www.w3.org/2001/XMLSchema#IDREF</tt> */
	public final static IRI IDREF = create("IDREF");

	/** <tt>http://www.w3.org/2001/XMLSchema#IDREFS</tt> */
	public final static IRI IDREFS = create("IDREFS");

	/** <tt>http://www.w3.org/2001/XMLSchema#ENTITY</tt> */
	public final static IRI ENTITY = create("ENTITY");

	/** <tt>http://www.w3.org/2001/XMLSchema#ENTITIES</tt> */
	public final static IRI ENTITIES = create("ENTITIES");

	/** <tt>http://www.w3.org/2001/XMLSchema#integer</tt> */
	public final static IRI INTEGER = create("integer");

	/** <tt>http://www.w3.org/2001/XMLSchema#long</tt> */
	public final static IRI LONG = create("long");

	/** <tt>http://www.w3.org/2001/XMLSchema#int</tt> */
	public final static IRI INT = create("int");

	/** <tt>http://www.w3.org/2001/XMLSchema#short</tt> */
	public final static IRI SHORT = create("short");

	/** <tt>http://www.w3.org/2001/XMLSchema#byte</tt> */
	public final static IRI BYTE = create("byte");

	/** <tt>http://www.w3.org/2001/XMLSchema#nonPositiveInteger</tt> */
	public final static IRI NON_POSITIVE_INTEGER = create("nonPositiveInteger");

	/** <tt>http://www.w3.org/2001/XMLSchema#negativeInteger</tt> */
	public final static IRI NEGATIVE_INTEGER = create("negativeInteger");

	/** <tt>http://www.w3.org/2001/XMLSchema#nonNegativeInteger</tt> */
	public final static IRI NON_NEGATIVE_INTEGER = create("nonNegativeInteger");

	/** <tt>http://www.w3.org/2001/XMLSchema#positiveInteger</tt> */
	public final static IRI POSITIVE_INTEGER = create("positiveInteger");

	/** <tt>http://www.w3.org/2001/XMLSchema#unsignedLong</tt> */
	public final static IRI UNSIGNED_LONG = create("unsignedLong");

	/** <tt>http://www.w3.org/2001/XMLSchema#unsignedInt</tt> */
	public final static IRI UNSIGNED_INT = create("unsignedInt");

	/** <tt>http://www.w3.org/2001/XMLSchema#unsignedShort</tt> */
	public final static IRI UNSIGNED_SHORT = create("unsignedShort");

	/** <tt>http://www.w3.org/2001/XMLSchema#unsignedByte</tt> */
	public final static IRI UNSIGNED_BYTE = create("unsignedByte");

	/** <tt>http://www.w3.org/2001/XMLSchema#yearMonthDuration</tt> */
	public static final IRI YEARMONTHDURATION = create("yearMonthDuration");

	private static IRI create(String localName) {
		return SimpleValueFactory.getInstance().createIRI(XSD.NAMESPACE, localName);
	}
}
