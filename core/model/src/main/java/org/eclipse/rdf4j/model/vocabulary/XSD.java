/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import java.util.HashMap;
import java.util.Optional;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

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
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

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
		return Vocabularies.createIRI(XSD.NAMESPACE, localName);
	}

	public enum Datatype {

		DURATION(XSD.DURATION, true, true, false, false, false, false, false),
		DATETIME(XSD.DATETIME, true, false, false, false, false, false, true),
		DATETIMESTAMP(XSD.DATETIMESTAMP, false, false, false, true, false, false, true),
		DAYTIMEDURATION(XSD.DAYTIMEDURATION, false, true, false, true, false, false, false),
		TIME(XSD.TIME, true, false, false, false, false, false, true),
		DATE(XSD.DATE, true, false, false, false, false, false, true),
		GYEARMONTH(XSD.GYEARMONTH, true, false, false, false, false, false, true),
		GYEAR(XSD.GYEAR, true, false, false, false, false, false, true),
		GMONTHDAY(XSD.GMONTHDAY, true, false, false, false, false, false, true),
		GDAY(XSD.GDAY, true, false, false, false, false, false, true),
		GMONTH(XSD.GMONTH, true, false, false, false, false, false, true),
		STRING(XSD.STRING, true, false, false, false, false, false, false),
		BOOLEAN(XSD.BOOLEAN, true, false, false, false, false, false, false),
		BASE64BINARY(XSD.BASE64BINARY, true, false, false, false, false, false, false),
		HEXBINARY(XSD.HEXBINARY, true, false, false, false, false, false, false),
		FLOAT(XSD.FLOAT, true, false, false, false, false, true, false),
		DECIMAL(XSD.DECIMAL, true, false, false, false, true, false, false),
		DOUBLE(XSD.DOUBLE, true, false, false, false, false, true, false),
		ANYURI(XSD.ANYURI, true, false, false, false, false, false, false),
		QNAME(XSD.QNAME, true, false, false, false, false, false, false),
		NOTATION(XSD.NOTATION, true, false, false, false, false, false, false),
		NORMALIZEDSTRING(XSD.NORMALIZEDSTRING, false, false, false, true, false, false, false),
		TOKEN(XSD.TOKEN, false, false, false, true, false, false, false),
		LANGUAGE(XSD.LANGUAGE, false, false, false, true, false, false, false),
		NMTOKEN(XSD.NMTOKEN, false, false, false, true, false, false, false),
		NMTOKENS(XSD.NMTOKENS, false, false, false, true, false, false, false),
		NAME(XSD.NAME, false, false, false, true, false, false, false),
		NCNAME(XSD.NCNAME, false, false, false, true, false, false, false),
		ID(XSD.ID, false, false, false, true, false, false, false),
		IDREF(XSD.IDREF, false, false, false, true, false, false, false),
		IDREFS(XSD.IDREFS, false, false, false, true, false, false, false),
		ENTITY(XSD.ENTITY, false, false, false, true, false, false, false),
		ENTITIES(XSD.ENTITIES, false, false, false, true, false, false, false),
		INTEGER(XSD.INTEGER, false, false, true, true, true, false, false),
		LONG(XSD.LONG, false, false, true, true, true, false, false),
		INT(XSD.INT, false, false, true, true, true, false, false),
		SHORT(XSD.SHORT, false, false, true, true, true, false, false),
		BYTE(XSD.BYTE, false, false, true, true, true, false, false),
		NON_POSITIVE_INTEGER(XSD.NON_POSITIVE_INTEGER, false, false, true, true, true, false, false),
		NEGATIVE_INTEGER(XSD.NEGATIVE_INTEGER, false, false, true, true, true, false, false),
		NON_NEGATIVE_INTEGER(XSD.NON_NEGATIVE_INTEGER, false, false, true, true, true, false, false),
		POSITIVE_INTEGER(XSD.POSITIVE_INTEGER, false, false, true, true, true, false, false),
		UNSIGNED_LONG(XSD.UNSIGNED_LONG, false, false, true, true, true, false, false),
		UNSIGNED_INT(XSD.UNSIGNED_INT, false, false, true, true, true, false, false),
		UNSIGNED_SHORT(XSD.UNSIGNED_SHORT, false, false, true, true, true, false, false),
		UNSIGNED_BYTE(XSD.UNSIGNED_BYTE, false, false, true, true, true, false, false),
		YEARMONTHDURATION(XSD.YEARMONTHDURATION, false, true, false, true, false, false, false);

		private final IRI iri;
		private final boolean primitive;
		private final boolean duration;
		private final boolean integer;
		private final boolean derived;
		private final boolean decimal;
		private final boolean floatingPoint;
		private final boolean calendar;

		Datatype(IRI iri, boolean primitive, boolean duration, boolean integer, boolean derived, boolean decimal,
				boolean floatingPoint, boolean calendar) {
			this.iri = iri;
			this.primitive = primitive;
			this.duration = duration;
			this.integer = integer;
			this.derived = derived;
			this.decimal = decimal;
			this.floatingPoint = floatingPoint;
			this.calendar = calendar;
		}

		/**
		 * Checks whether the supplied datatype is a primitive XML Schema datatype.
		 *
		 * @return true if the datatype is a primitive type
		 */
		public boolean isPrimitiveDatatype() {
			return primitive;
		}

		/**
		 * Checks whether the supplied datatype is a derived XML Schema datatype.
		 *
		 * @return true if the datatype is a derived type
		 */
		public boolean isDerivedDatatype() {
			return derived;
		}

		/**
		 * Checks whether the supplied datatype is a built-in XML Schema datatype.
		 *
		 * @return true if it is a primitive or derived XML Schema type
		 */
		public boolean isBuiltInDatatype() {
			return isPrimitiveDatatype() || isDerivedDatatype();
		}

		/**
		 * Checks whether the supplied datatype is a numeric datatype, i.e.if it is equal to xsd:float, xsd:double,
		 * xsd:decimal or one of the datatypes derived from xsd:decimal.
		 *
		 * @return true of it is a decimal or floating point type
		 */
		public boolean isNumericDatatype() {
			return isDecimalDatatype() || isFloatingPointDatatype();
		}

		/**
		 * Checks whether the supplied datatype is equal to xsd:decimal or one of the built-in datatypes that is derived
		 * from xsd:decimal.
		 *
		 * @return true if it is a decimal datatype
		 */
		public boolean isDecimalDatatype() {
			return decimal;
		}

		/**
		 * Checks whether the supplied datatype is equal to xsd:integer or one of the built-in datatypes that is derived
		 * from xsd:integer.
		 *
		 * @return true if it is an integer type
		 */
		public boolean isIntegerDatatype() {
			return integer;
		}

		/**
		 * Checks whether the supplied datatype is equal to xsd:float or xsd:double.
		 *
		 * @return true if it is a floating point type
		 */
		public boolean isFloatingPointDatatype() {
			return floatingPoint;
		}

		/**
		 * Checks whether the supplied datatype is equal to xsd:dateTime, xsd:date, xsd:time, xsd:gYearMonth,
		 * xsd:gMonthDay, xsd:gYear, xsd:gMonth or xsd:gDay.These are the primitive datatypes that represent dates
		 * and/or times.
		 *
		 * @return true if it is a calendar type
		 *
		 * @see XMLGregorianCalendar
		 */
		public boolean isCalendarDatatype() {
			return calendar;
		}

		/**
		 * Checks whether the supplied datatype is equal to xsd:duration, xsd:dayTimeDuration, xsd:yearMonthDuration.
		 * These are the datatypes that represents durations.
		 *
		 * @return true if it is a duration type
		 *
		 * @see Duration
		 */
		public boolean isDurationDatatype() {
			return duration;
		}

		/**
		 * Checks whether the supplied datatype is ordered.The values of an ordered datatype can be compared to each
		 * other using operators like <tt>&lt;</tt> and <tt>&gt;</tt>.
		 *
		 * @return true if the datatype is ordered
		 */
		public boolean isOrderedDatatype() {
			return isNumericDatatype() || isCalendarDatatype();
		}

		static HashMap<IRI, Optional<Datatype>> reverseLookup = new HashMap<>();

		static {
			for (Datatype value : Datatype.values()) {
				reverseLookup.put(value.iri, Optional.of(value));
			}
		}

		public static Optional<Datatype> from(IRI datatype) {
			return reverseLookup.getOrDefault(datatype, Optional.empty());
		}

		public IRI getIri() {
			return iri;
		}

	}

}
