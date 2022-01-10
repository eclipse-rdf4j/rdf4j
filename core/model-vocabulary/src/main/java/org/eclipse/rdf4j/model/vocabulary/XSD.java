/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.base.CoreDatatype;

/**
 * Constants for the standard <a href="https://www.w3.org/TR/xmlschema11-2/">XML Schema 1.1 datatypes</a>.
 *
 * @see <a href="https://www.w3.org/TR/xmlschema11-2/">XML Schema 1.1 Part 2: Datatypes</a>
 */
public class XSD {

	/** The XML Schema namespace (<var>http://www.w3.org/2001/XMLSchema#</var>). */
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

	/** <var>http://www.w3.org/2001/XMLSchema#duration</var> */
	public final static IRI DURATION = create("duration");

	/** <var>http://www.w3.org/2001/XMLSchema#dateTime</var> */
	public final static IRI DATETIME = create("dateTime");

	/** <var>http://www.w3.org/2001/XMLSchema#dateTimeStamp</var> */
	public final static IRI DATETIMESTAMP = create("dateTimeStamp");

	/** <var>http://www.w3.org/2001/XMLSchema#dayTimeDuration</var> */
	public static final IRI DAYTIMEDURATION = create("dayTimeDuration");

	/** <var>http://www.w3.org/2001/XMLSchema#time</var> */
	public final static IRI TIME = create("time");

	/** <var>http://www.w3.org/2001/XMLSchema#date</var> */
	public final static IRI DATE = create("date");

	/** <var>http://www.w3.org/2001/XMLSchema#gYearMonth</var> */
	public final static IRI GYEARMONTH = create("gYearMonth");

	/** <var>http://www.w3.org/2001/XMLSchema#gYear</var> */
	public final static IRI GYEAR = create("gYear");

	/** <var>http://www.w3.org/2001/XMLSchema#gMonthDay</var> */
	public final static IRI GMONTHDAY = create("gMonthDay");

	/** <var>http://www.w3.org/2001/XMLSchema#gDay</var> */
	public final static IRI GDAY = create("gDay");

	/** <var>http://www.w3.org/2001/XMLSchema#gMonth</var> */
	public final static IRI GMONTH = create("gMonth");

	/** <var>http://www.w3.org/2001/XMLSchema#string</var> */
	public final static IRI STRING = create("string");

	/** <var>http://www.w3.org/2001/XMLSchema#boolean</var> */
	public final static IRI BOOLEAN = create("boolean");

	/** <var>http://www.w3.org/2001/XMLSchema#base64Binary</var> */
	public final static IRI BASE64BINARY = create("base64Binary");

	/** <var>http://www.w3.org/2001/XMLSchema#hexBinary</var> */
	public final static IRI HEXBINARY = create("hexBinary");

	/** <var>http://www.w3.org/2001/XMLSchema#float</var> */
	public final static IRI FLOAT = create("float");

	/** <var>http://www.w3.org/2001/XMLSchema#decimal</var> */
	public final static IRI DECIMAL = create("decimal");

	/** <var>http://www.w3.org/2001/XMLSchema#double</var> */
	public final static IRI DOUBLE = create("double");

	/** <var>http://www.w3.org/2001/XMLSchema#anyURI</var> */
	public final static IRI ANYURI = create("anyURI");

	/** <var>http://www.w3.org/2001/XMLSchema#QName</var> */
	public final static IRI QNAME = create("QName");

	/** <var>http://www.w3.org/2001/XMLSchema#NOTATION</var> */
	public final static IRI NOTATION = create("NOTATION");

	/*
	 * Derived datatypes
	 */

	/** <var>http://www.w3.org/2001/XMLSchema#normalizedString</var> */
	public final static IRI NORMALIZEDSTRING = create("normalizedString");

	/** <var>http://www.w3.org/2001/XMLSchema#token</var> */
	public final static IRI TOKEN = create("token");

	/** <var>http://www.w3.org/2001/XMLSchema#language</var> */
	public final static IRI LANGUAGE = create("language");

	/** <var>http://www.w3.org/2001/XMLSchema#NMTOKEN</var> */
	public final static IRI NMTOKEN = create("NMTOKEN");

	/** <var>http://www.w3.org/2001/XMLSchema#NMTOKENS</var> */
	public final static IRI NMTOKENS = create("NMTOKENS");

	/** <var>http://www.w3.org/2001/XMLSchema#Name</var> */
	public final static IRI NAME = create("Name");

	/** <var>http://www.w3.org/2001/XMLSchema#NCName</var> */
	public final static IRI NCNAME = create("NCName");

	/** <var>http://www.w3.org/2001/XMLSchema#ID</var> */
	public final static IRI ID = create("ID");

	/** <var>http://www.w3.org/2001/XMLSchema#IDREF</var> */
	public final static IRI IDREF = create("IDREF");

	/** <var>http://www.w3.org/2001/XMLSchema#IDREFS</var> */
	public final static IRI IDREFS = create("IDREFS");

	/** <var>http://www.w3.org/2001/XMLSchema#ENTITY</var> */
	public final static IRI ENTITY = create("ENTITY");

	/** <var>http://www.w3.org/2001/XMLSchema#ENTITIES</var> */
	public final static IRI ENTITIES = create("ENTITIES");

	/** <var>http://www.w3.org/2001/XMLSchema#integer</var> */
	public final static IRI INTEGER = create("integer");

	/** <var>http://www.w3.org/2001/XMLSchema#long</var> */
	public final static IRI LONG = create("long");

	/** <var>http://www.w3.org/2001/XMLSchema#int</var> */
	public final static IRI INT = create("int");

	/** <var>http://www.w3.org/2001/XMLSchema#short</var> */
	public final static IRI SHORT = create("short");

	/** <var>http://www.w3.org/2001/XMLSchema#byte</var> */
	public final static IRI BYTE = create("byte");

	/** <var>http://www.w3.org/2001/XMLSchema#nonPositiveInteger</var> */
	public final static IRI NON_POSITIVE_INTEGER = create("nonPositiveInteger");

	/** <var>http://www.w3.org/2001/XMLSchema#negativeInteger</var> */
	public final static IRI NEGATIVE_INTEGER = create("negativeInteger");

	/** <var>http://www.w3.org/2001/XMLSchema#nonNegativeInteger</var> */
	public final static IRI NON_NEGATIVE_INTEGER = create("nonNegativeInteger");

	/** <var>http://www.w3.org/2001/XMLSchema#positiveInteger</var> */
	public final static IRI POSITIVE_INTEGER = create("positiveInteger");

	/** <var>http://www.w3.org/2001/XMLSchema#unsignedLong</var> */
	public final static IRI UNSIGNED_LONG = create("unsignedLong");

	/** <var>http://www.w3.org/2001/XMLSchema#unsignedInt</var> */
	public final static IRI UNSIGNED_INT = create("unsignedInt");

	/** <var>http://www.w3.org/2001/XMLSchema#unsignedShort</var> */
	public final static IRI UNSIGNED_SHORT = create("unsignedShort");

	/** <var>http://www.w3.org/2001/XMLSchema#unsignedByte</var> */
	public final static IRI UNSIGNED_BYTE = create("unsignedByte");

	/** <var>http://www.w3.org/2001/XMLSchema#yearMonthDuration</var> */
	public static final IRI YEARMONTHDURATION = create("yearMonthDuration");

	private static IRI create(String localName) {
		return Vocabularies.createIRI(XSD.NAMESPACE, localName);
	}

	public enum Datatype {

		DURATION(create("duration"), true, true, false, false, false, false, false),
		DATETIME(create("dateTime"), true, false, false, false, false, false, true),
		DATETIMESTAMP(create("dateTimeStamp"), false, false, false, true, false, false, true),
		DAYTIMEDURATION(create("dayTimeDuration"), false, true, false, true, false, false, false),
		TIME(create("time"), true, false, false, false, false, false, true),
		DATE(create("date"), true, false, false, false, false, false, true),
		GYEARMONTH(create("gYearMonth"), true, false, false, false, false, false, true),
		GYEAR(create("gYear"), true, false, false, false, false, false, true),
		GMONTHDAY(create("gMonthDay"), true, false, false, false, false, false, true),
		GDAY(create("gDay"), true, false, false, false, false, false, true),
		GMONTH(create("gMonth"), true, false, false, false, false, false, true),
		STRING(create("string"), true, false, false, false, false, false, false),
		BOOLEAN(create("boolean"), true, false, false, false, false, false, false),
		BASE64BINARY(create("base64Binary"), true, false, false, false, false, false, false),
		HEXBINARY(create("hexBinary"), true, false, false, false, false, false, false),
		FLOAT(create("float"), true, false, false, false, false, true, false),
		DECIMAL(create("decimal"), true, false, false, false, true, false, false),
		DOUBLE(create("double"), true, false, false, false, false, true, false),
		ANYURI(create("anyURI"), true, false, false, false, false, false, false),
		QNAME(create("QName"), true, false, false, false, false, false, false),
		NOTATION(create("NOTATION"), true, false, false, false, false, false, false),
		NORMALIZEDSTRING(create("normalizedString"), false, false, false, true, false, false, false),
		TOKEN(create("token"), false, false, false, true, false, false, false),
		LANGUAGE(create("language"), false, false, false, true, false, false, false),
		NMTOKEN(create("NMTOKEN"), false, false, false, true, false, false, false),
		NMTOKENS(create("NMTOKENS"), false, false, false, true, false, false, false),
		NAME(create("Name"), false, false, false, true, false, false, false),
		NCNAME(create("NCName"), false, false, false, true, false, false, false),
		ID(create("ID"), false, false, false, true, false, false, false),
		IDREF(create("IDREF"), false, false, false, true, false, false, false),
		IDREFS(create("IDREFS"), false, false, false, true, false, false, false),
		ENTITY(create("ENTITY"), false, false, false, true, false, false, false),
		ENTITIES(create("ENTITIES"), false, false, false, true, false, false, false),
		INTEGER(create("integer"), false, false, true, true, true, false, false),
		LONG(create("long"), false, false, true, true, true, false, false),
		INT(create("int"), false, false, true, true, true, false, false),
		SHORT(create("short"), false, false, true, true, true, false, false),
		BYTE(create("byte"), false, false, true, true, true, false, false),
		NON_POSITIVE_INTEGER(create("nonPositiveInteger"), false, false, true, true, true, false, false),
		NEGATIVE_INTEGER(create("negativeInteger"), false, false, true, true, true, false, false),
		NON_NEGATIVE_INTEGER(create("nonNegativeInteger"), false, false, true, true, true, false, false),
		POSITIVE_INTEGER(create("positiveInteger"), false, false, true, true, true, false, false),
		UNSIGNED_LONG(create("unsignedLong"), false, false, true, true, true, false, false),
		UNSIGNED_INT(create("unsignedInt"), false, false, true, true, true, false, false),
		UNSIGNED_SHORT(create("unsignedShort"), false, false, true, true, true, false, false),
		UNSIGNED_BYTE(create("unsignedByte"), false, false, true, true, true, false, false),
		YEARMONTHDURATION(create("yearMonthDuration"), false, true, false, true, false, false, false);

		private final IRI iri;
		private final boolean primitive;
		private final boolean duration;
		private final boolean integer;
		private final boolean derived;
		private final boolean decimal;
		private final boolean floatingPoint;
		private final boolean calendar;
		private final CoreDatatype coreDatatype;

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
			this.coreDatatype = CoreDatatype.from(iri).orElseThrow();
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
		 * other using operators like <var>&lt;</var> and <var>&gt;</var>.
		 *
		 * @return true if the datatype is ordered
		 */
		public boolean isOrderedDatatype() {
			return isNumericDatatype() || isCalendarDatatype();
		}

		public IRI getIri() {
			return iri;
		}

		public CoreDatatype getCoreDatatype() {
			return coreDatatype;
		}

		private static Map<IRI, Optional<Datatype>> reverseLookup = new HashMap<>();

		private static Map<CoreDatatype, Optional<Datatype>> reverseLookupCoreDatatype = new EnumMap<>(
				CoreDatatype.class);

		static {
			for (Datatype value : Datatype.values()) {
				reverseLookup.put(value.iri, Optional.of(value));
				reverseLookupCoreDatatype.put(value.coreDatatype, Optional.of(value));
			}
		}

		public static Optional<Datatype> from(IRI datatype) {
			return reverseLookup.getOrDefault(datatype, Optional.empty());
		}

		public static Optional<Datatype> from(CoreDatatype datatype) {
			return reverseLookupCoreDatatype.getOrDefault(datatype, Optional.empty());
		}

	}

}
