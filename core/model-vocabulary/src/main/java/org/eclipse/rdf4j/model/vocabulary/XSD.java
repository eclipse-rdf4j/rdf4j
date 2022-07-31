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

	private static IRI create(String localName) {
		return Vocabularies.createIRI(org.eclipse.rdf4j.model.vocabulary.XSD.NAMESPACE, localName);
	}

	public enum Datatype {

		DURATION(CoreDatatype.XSD.DURATION.getIri(), true, true, false, false, false, false, false),
		DATETIME(CoreDatatype.XSD.DATETIME.getIri(), true, false, false, false, false, false, true),
		DATETIMESTAMP(CoreDatatype.XSD.DATETIMESTAMP.getIri(), false, false, false, true, false, false, true),
		DAYTIMEDURATION(CoreDatatype.XSD.DAYTIMEDURATION.getIri(), false, true, false, true, false, false, false),
		TIME(CoreDatatype.XSD.TIME.getIri(), true, false, false, false, false, false, true),
		DATE(CoreDatatype.XSD.DATE.getIri(), true, false, false, false, false, false, true),
		GYEARMONTH(CoreDatatype.XSD.GYEARMONTH.getIri(), true, false, false, false, false, false, true),
		GYEAR(CoreDatatype.XSD.GYEAR.getIri(), true, false, false, false, false, false, true),
		GMONTHDAY(CoreDatatype.XSD.GMONTHDAY.getIri(), true, false, false, false, false, false, true),
		GDAY(CoreDatatype.XSD.GDAY.getIri(), true, false, false, false, false, false, true),
		GMONTH(CoreDatatype.XSD.GMONTH.getIri(), true, false, false, false, false, false, true),
		STRING(CoreDatatype.XSD.STRING.getIri(), true, false, false, false, false, false, false),
		BOOLEAN(CoreDatatype.XSD.BOOLEAN.getIri(), true, false, false, false, false, false, false),
		BASE64BINARY(CoreDatatype.XSD.BASE64BINARY.getIri(), true, false, false, false, false, false, false),
		HEXBINARY(CoreDatatype.XSD.HEXBINARY.getIri(), true, false, false, false, false, false, false),
		FLOAT(CoreDatatype.XSD.FLOAT.getIri(), true, false, false, false, false, true, false),
		DECIMAL(CoreDatatype.XSD.DECIMAL.getIri(), true, false, false, false, true, false, false),
		DOUBLE(CoreDatatype.XSD.DOUBLE.getIri(), true, false, false, false, false, true, false),
		ANYURI(CoreDatatype.XSD.ANYURI.getIri(), true, false, false, false, false, false, false),
		QNAME(CoreDatatype.XSD.QNAME.getIri(), true, false, false, false, false, false, false),
		NOTATION(CoreDatatype.XSD.NOTATION.getIri(), true, false, false, false, false, false, false),
		NORMALIZEDSTRING(CoreDatatype.XSD.NORMALIZEDSTRING.getIri(), false, false, false, true, false, false, false),
		TOKEN(CoreDatatype.XSD.TOKEN.getIri(), false, false, false, true, false, false, false),
		LANGUAGE(CoreDatatype.XSD.LANGUAGE.getIri(), false, false, false, true, false, false, false),
		NMTOKEN(CoreDatatype.XSD.NMTOKEN.getIri(), false, false, false, true, false, false, false),
		NMTOKENS(CoreDatatype.XSD.NMTOKENS.getIri(), false, false, false, true, false, false, false),
		NAME(CoreDatatype.XSD.NAME.getIri(), false, false, false, true, false, false, false),
		NCNAME(CoreDatatype.XSD.NCNAME.getIri(), false, false, false, true, false, false, false),
		ID(CoreDatatype.XSD.ID.getIri(), false, false, false, true, false, false, false),
		IDREF(CoreDatatype.XSD.IDREF.getIri(), false, false, false, true, false, false, false),
		IDREFS(CoreDatatype.XSD.IDREFS.getIri(), false, false, false, true, false, false, false),
		ENTITY(CoreDatatype.XSD.ENTITY.getIri(), false, false, false, true, false, false, false),
		ENTITIES(CoreDatatype.XSD.ENTITIES.getIri(), false, false, false, true, false, false, false),
		INTEGER(CoreDatatype.XSD.INTEGER.getIri(), false, false, true, true, true, false, false),
		LONG(CoreDatatype.XSD.LONG.getIri(), false, false, true, true, true, false, false),
		INT(CoreDatatype.XSD.INT.getIri(), false, false, true, true, true, false, false),
		SHORT(CoreDatatype.XSD.SHORT.getIri(), false, false, true, true, true, false, false),
		BYTE(CoreDatatype.XSD.BYTE.getIri(), false, false, true, true, true, false, false),
		NON_POSITIVE_INTEGER(CoreDatatype.XSD.NON_POSITIVE_INTEGER.getIri(), false, false, true, true, true, false,
				false),
		NEGATIVE_INTEGER(CoreDatatype.XSD.NEGATIVE_INTEGER.getIri(), false, false, true, true, true, false, false),
		NON_NEGATIVE_INTEGER(CoreDatatype.XSD.NON_NEGATIVE_INTEGER.getIri(), false, false, true, true, true, false,
				false),
		POSITIVE_INTEGER(CoreDatatype.XSD.POSITIVE_INTEGER.getIri(), false, false, true, true, true, false, false),
		UNSIGNED_LONG(CoreDatatype.XSD.UNSIGNED_LONG.getIri(), false, false, true, true, true, false, false),
		UNSIGNED_INT(CoreDatatype.XSD.UNSIGNED_INT.getIri(), false, false, true, true, true, false, false),
		UNSIGNED_SHORT(CoreDatatype.XSD.UNSIGNED_SHORT.getIri(), false, false, true, true, true, false, false),
		UNSIGNED_BYTE(CoreDatatype.XSD.UNSIGNED_BYTE.getIri(), false, false, true, true, true, false, false),
		YEARMONTHDURATION(CoreDatatype.XSD.YEARMONTHDURATION.getIri(), false, true, false, true, false, false, false);

		private final IRI iri;
		private final boolean primitive;
		private final boolean duration;
		private final boolean integer;
		private final boolean derived;
		private final boolean decimal;
		private final boolean floatingPoint;
		private final boolean calendar;
		private final CoreDatatype.XSD coreDatatype;

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
			this.coreDatatype = CoreDatatype.from(iri).asXSDDatatype().orElseThrow();
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

		private static final Map<IRI, Optional<Datatype>> reverseLookup = new HashMap<>();

		private static final Map<CoreDatatype.XSD, Optional<Datatype>> reverseLookupXSDDatatype = new EnumMap<>(
				CoreDatatype.XSD.class);

		static {
			for (Datatype value : Datatype.values()) {
				reverseLookup.put(value.iri, Optional.of(value));
				reverseLookupXSDDatatype.put(value.coreDatatype, Optional.of(value));
			}
		}

		public static Optional<Datatype> from(IRI datatype) {
			return reverseLookup.getOrDefault(datatype, Optional.empty());
		}

		public static Optional<Datatype> from(CoreDatatype.XSD datatype) {
			if (datatype == null)
				return Optional.empty();
			return reverseLookupXSDDatatype.getOrDefault(datatype, Optional.empty());
		}

	}

}
