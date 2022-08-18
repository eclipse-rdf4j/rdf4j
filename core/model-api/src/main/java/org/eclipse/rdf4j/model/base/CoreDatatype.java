/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.base;

import java.util.Optional;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;

public interface CoreDatatype {

	CoreDatatype NONE = DefaultDatatype.NONE;

	/**
	 * Checks whether the supplied datatype is an XML Schema Datatype.
	 *
	 * @return true if the datatype is an XML Schema Datatype
	 */
	default boolean isXSDDatatype() {
		return false;
	}

	default boolean isRDFDatatype() {
		return false;
	}

	default boolean isGEODatatype() {
		return false;
	}

	default Optional<XSD> asXSDDatatype() {
		return Optional.empty();
	}

	default Optional<RDF> asRDFDatatype() {
		return Optional.empty();
	}

	default Optional<GEO> asGEODatatype() {
		return Optional.empty();
	}

	IRI getIri();

	static CoreDatatype from(IRI datatype) {
		if (datatype == null) {
			return CoreDatatype.NONE;
		}
		return CoreDatatypeHelper.getReverseLookup().getOrDefault(datatype, CoreDatatype.NONE);
	}

	enum XSD implements CoreDatatype {

		ENTITIES(iri("ENTITIES"), false, false, false, true, false, false, false),
		ENTITY(iri("ENTITY"), false, false, false, true, false, false, false),
		ID(iri("ID"), false, false, false, true, false, false, false),
		IDREF(iri("IDREF"), false, false, false, true, false, false, false),
		IDREFS(iri("IDREFS"), false, false, false, true, false, false, false),
		NCNAME(iri("NCName"), false, false, false, true, false, false, false),
		NMTOKEN(iri("NMTOKEN"), false, false, false, true, false, false, false),
		NMTOKENS(iri("NMTOKENS"), false, false, false, true, false, false, false),
		NOTATION(iri("NOTATION"), true, false, false, false, false, false, false),
		NAME(iri("Name"), false, false, false, true, false, false, false),
		QNAME(iri("QName"), true, false, false, false, false, false, false),
		ANYURI(iri("anyURI"), true, false, false, false, false, false, false),
		BASE64BINARY(iri("base64Binary"), true, false, false, false, false, false, false),
		BOOLEAN(iri("boolean"), true, false, false, false, false, false, false),
		BYTE(iri("byte"), false, false, true, true, true, false, false),
		DATE(iri("date"), true, false, false, false, false, false, true),
		DATETIME(iri("dateTime"), true, false, false, false, false, false, true),
		DATETIMESTAMP(iri("dateTimeStamp"), false, false, false, true, false, false, true),
		DAYTIMEDURATION(iri("dayTimeDuration"), false, true, false, true, false, false, false),
		DECIMAL(iri("decimal"), true, false, false, false, true, false, false),
		DOUBLE(iri("double"), true, false, false, false, false, true, false),
		DURATION(iri("duration"), true, true, false, false, false, false, false),
		FLOAT(iri("float"), true, false, false, false, false, true, false),
		GDAY(iri("gDay"), true, false, false, false, false, false, true),
		GMONTH(iri("gMonth"), true, false, false, false, false, false, true),
		GMONTHDAY(iri("gMonthDay"), true, false, false, false, false, false, true),
		GYEAR(iri("gYear"), true, false, false, false, false, false, true),
		GYEARMONTH(iri("gYearMonth"), true, false, false, false, false, false, true),
		HEXBINARY(iri("hexBinary"), true, false, false, false, false, false, false),
		INT(iri("int"), false, false, true, true, true, false, false),
		INTEGER(iri("integer"), false, false, true, true, true, false, false),
		LANGUAGE(iri("language"), false, false, false, true, false, false, false),
		LONG(iri("long"), false, false, true, true, true, false, false),
		NEGATIVE_INTEGER(iri("negativeInteger"), false, false, true, true, true, false, false),
		NON_NEGATIVE_INTEGER(iri("nonNegativeInteger"), false, false, true, true, true, false, false),
		NON_POSITIVE_INTEGER(iri("nonPositiveInteger"), false, false, true, true, true, false, false),
		NORMALIZEDSTRING(iri("normalizedString"), false, false, false, true, false, false, false),
		POSITIVE_INTEGER(iri("positiveInteger"), false, false, true, true, true, false, false),
		SHORT(iri("short"), false, false, true, true, true, false, false),
		STRING(iri("string"), true, false, false, false, false, false, false),
		TIME(iri("time"), true, false, false, false, false, false, true),
		TOKEN(iri("token"), false, false, false, true, false, false, false),
		UNSIGNED_BYTE(iri("unsignedByte"), false, false, true, true, true, false, false),
		UNSIGNED_INT(iri("unsignedInt"), false, false, true, true, true, false, false),
		UNSIGNED_LONG(iri("unsignedLong"), false, false, true, true, true, false, false),
		UNSIGNED_SHORT(iri("unsignedShort"), false, false, true, true, true, false, false),
		YEARMONTHDURATION(iri("yearMonthDuration"), false, true, false, true, false, false, false);

		public static final String NAMESPACE = "http://www.w3.org/2001/XMLSchema#";

		private static IRI iri(String localName) {
			return new InternedIRI(NAMESPACE, localName);
		}

		private final IRI iri;
		private final boolean primitive;
		private final boolean duration;
		private final boolean integer;
		private final boolean derived;
		private final boolean decimal;
		private final boolean floatingPoint;
		private final boolean calendar;
		private final boolean builtIn;
		private final boolean numeric;
		private final boolean ordered;

		// Creating optionals are expensive so we precompute one
		private final Optional<XSD> optional;

		XSD(IRI iri, boolean primitive, boolean duration, boolean integer, boolean derived, boolean decimal,
				boolean floatingPoint, boolean calendar) {
			this.iri = iri;
			this.primitive = primitive;
			this.duration = duration;
			this.integer = integer;
			this.derived = derived;
			this.decimal = decimal;
			this.floatingPoint = floatingPoint;
			this.calendar = calendar;

			this.builtIn = primitive || derived;
			this.numeric = decimal || floatingPoint;
			this.ordered = numeric || calendar;

			this.optional = Optional.of(this);
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
			return builtIn;
		}

		/**
		 * Checks whether the supplied datatype is a numeric datatype, i.e.if it is equal to xsd:float, xsd:double,
		 * xsd:decimal or one of the datatypes derived from xsd:decimal.
		 *
		 * @return true of it is a decimal or floating point type
		 */
		public boolean isNumericDatatype() {
			return numeric;
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
			return ordered;
		}

		@Override
		public boolean isXSDDatatype() {
			return true;
		}

		public IRI getIri() {
			return iri;
		}

		@Override
		public Optional<XSD> asXSDDatatype() {
			return optional;
		}

		@Override
		public String toString() {
			return iri.toString();
		}

	}

	enum RDF implements CoreDatatype {

		HTML(iri("HTML")),
		XMLLITERAL(iri("XMLLiteral")),
		LANGSTRING(iri("langString"));

		public static final String NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

		private static IRI iri(String localName) {
			return new InternedIRI(NAMESPACE, localName);
		}

		private final IRI iri;

		// Creating optionals are expensive so we precompute one
		private final Optional<RDF> optional;

		RDF(IRI iri) {
			this.iri = iri;
			this.optional = Optional.of(this);
		}

		@Override
		public boolean isRDFDatatype() {
			return true;
		}

		public IRI getIri() {
			return iri;
		}

		@Override
		public Optional<RDF> asRDFDatatype() {
			return optional;
		}

		@Override
		public String toString() {
			return iri.toString();
		}

	}

	enum GEO implements CoreDatatype {

		WKT_LITERAL(iri("wktLiteral"));

		public static final String NAMESPACE = "http://www.opengis.net/ont/geosparql#";

		private static IRI iri(String localName) {
			return new InternedIRI(NAMESPACE, localName);
		}

		private final IRI iri;

		// Creating optionals are expensive so we precompute one
		private final Optional<GEO> optional;

		GEO(IRI iri) {
			this.iri = iri;
			this.optional = Optional.of(this);
		}

		@Override
		public boolean isGEODatatype() {
			return true;
		}

		public IRI getIri() {
			return iri;
		}

		@Override
		public Optional<GEO> asGEODatatype() {
			return optional;
		}

		@Override
		public String toString() {
			return iri.toString();
		}
	}

}

/**
 * This needs to be its own enum because we need it to be serializable.
 */
enum DefaultDatatype implements CoreDatatype {
	NONE;

	@Override
	public IRI getIri() {
		throw new IllegalStateException();
	}
}
