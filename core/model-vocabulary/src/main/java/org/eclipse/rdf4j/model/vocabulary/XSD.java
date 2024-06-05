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
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.base.CoreDatatype;

/**
 * Constants for the standard <a href="https://www.w3.org/TR/xmlschema11-2/">XML Schema 1.1 datatypes</a>.
 *
 * @see <a href="https://www.w3.org/TR/xmlschema11-2/">XML Schema 1.1 Part 2: Datatypes</a>
 */
public class XSD {

	/**
	 * The XML Schema namespace (<var>http://www.w3.org/2001/XMLSchema#</var>).
	 */
	public final static String NAMESPACE = CoreDatatype.XSD.NAMESPACE;

	/**
	 * Recommended prefix for XML Schema datatypes: "xsd"
	 */
	public final static String PREFIX = "xsd";

	/**
	 * An immutable {@link Namespace} constant that represents the XML Schema namespace.
	 */
	public final static Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	/*
	 * Primitive datatypes
	 */

	/** <var>http://www.w3.org/2001/XMLSchema#duration</var> */
	public final static IRI DURATION = CoreDatatype.XSD.DURATION.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#dateTime</var> */
	public final static IRI DATETIME = CoreDatatype.XSD.DATETIME.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#dateTimeStamp</var> */
	public final static IRI DATETIMESTAMP = CoreDatatype.XSD.DATETIMESTAMP.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#dayTimeDuration</var> */
	public final static IRI DAYTIMEDURATION = CoreDatatype.XSD.DAYTIMEDURATION.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#time</var> */
	public final static IRI TIME = CoreDatatype.XSD.TIME.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#date</var> */
	public final static IRI DATE = CoreDatatype.XSD.DATE.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#gYearMonth</var> */
	public final static IRI GYEARMONTH = CoreDatatype.XSD.GYEARMONTH.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#gYear</var> */
	public final static IRI GYEAR = CoreDatatype.XSD.GYEAR.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#gMonthDay</var> */
	public final static IRI GMONTHDAY = CoreDatatype.XSD.GMONTHDAY.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#gDay</var> */
	public final static IRI GDAY = CoreDatatype.XSD.GDAY.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#gMonth</var> */
	public final static IRI GMONTH = CoreDatatype.XSD.GMONTH.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#string</var> */
	public final static IRI STRING = CoreDatatype.XSD.STRING.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#boolean</var> */
	public final static IRI BOOLEAN = CoreDatatype.XSD.BOOLEAN.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#base64Binary</var> */
	public final static IRI BASE64BINARY = CoreDatatype.XSD.BASE64BINARY.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#hexBinary</var> */
	public final static IRI HEXBINARY = CoreDatatype.XSD.HEXBINARY.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#float</var> */
	public final static IRI FLOAT = CoreDatatype.XSD.FLOAT.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#decimal</var> */
	public final static IRI DECIMAL = CoreDatatype.XSD.DECIMAL.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#double</var> */
	public final static IRI DOUBLE = CoreDatatype.XSD.DOUBLE.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#anyURI</var> */
	public final static IRI ANYURI = CoreDatatype.XSD.ANYURI.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#QName</var> */
	public final static IRI QNAME = CoreDatatype.XSD.QNAME.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#NOTATION</var> */
	public final static IRI NOTATION = CoreDatatype.XSD.NOTATION.getIri();

	/*
	 * Derived datatypes
	 */

	/** <var>http://www.w3.org/2001/XMLSchema#normalizedString</var> */
	public final static IRI NORMALIZEDSTRING = CoreDatatype.XSD.NORMALIZEDSTRING.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#token</var> */
	public final static IRI TOKEN = CoreDatatype.XSD.TOKEN.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#language</var> */
	public final static IRI LANGUAGE = CoreDatatype.XSD.LANGUAGE.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#NMTOKEN</var> */
	public final static IRI NMTOKEN = CoreDatatype.XSD.NMTOKEN.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#NMTOKENS</var> */
	public final static IRI NMTOKENS = CoreDatatype.XSD.NMTOKENS.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#Name</var> */
	public final static IRI NAME = CoreDatatype.XSD.NAME.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#NCName</var> */
	public final static IRI NCNAME = CoreDatatype.XSD.NCNAME.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#ID</var> */
	public final static IRI ID = CoreDatatype.XSD.ID.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#IDREF</var> */
	public final static IRI IDREF = CoreDatatype.XSD.IDREF.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#IDREFS</var> */
	public final static IRI IDREFS = CoreDatatype.XSD.IDREFS.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#ENTITY</var> */
	public final static IRI ENTITY = CoreDatatype.XSD.ENTITY.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#ENTITIES</var> */
	public final static IRI ENTITIES = CoreDatatype.XSD.ENTITIES.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#integer</var> */
	public final static IRI INTEGER = CoreDatatype.XSD.INTEGER.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#long</var> */
	public final static IRI LONG = CoreDatatype.XSD.LONG.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#int</var> */
	public final static IRI INT = CoreDatatype.XSD.INT.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#short</var> */
	public final static IRI SHORT = CoreDatatype.XSD.SHORT.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#byte</var> */
	public final static IRI BYTE = CoreDatatype.XSD.BYTE.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#nonPositiveInteger</var> */
	public final static IRI NON_POSITIVE_INTEGER = CoreDatatype.XSD.NON_POSITIVE_INTEGER.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#negativeInteger</var> */
	public final static IRI NEGATIVE_INTEGER = CoreDatatype.XSD.NEGATIVE_INTEGER.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#nonNegativeInteger</var> */
	public final static IRI NON_NEGATIVE_INTEGER = CoreDatatype.XSD.NON_NEGATIVE_INTEGER.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#positiveInteger</var> */
	public final static IRI POSITIVE_INTEGER = CoreDatatype.XSD.POSITIVE_INTEGER.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#unsignedLong</var> */
	public final static IRI UNSIGNED_LONG = CoreDatatype.XSD.UNSIGNED_LONG.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#unsignedInt</var> */
	public final static IRI UNSIGNED_INT = CoreDatatype.XSD.UNSIGNED_INT.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#unsignedShort</var> */
	public final static IRI UNSIGNED_SHORT = CoreDatatype.XSD.UNSIGNED_SHORT.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#unsignedByte</var> */
	public final static IRI UNSIGNED_BYTE = CoreDatatype.XSD.UNSIGNED_BYTE.getIri();

	/** <var>http://www.w3.org/2001/XMLSchema#yearMonthDuration</var> */
	public final static IRI YEARMONTHDURATION = CoreDatatype.XSD.YEARMONTHDURATION.getIri();

}
