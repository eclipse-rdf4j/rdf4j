/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Optional;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;

public enum CoreDatatype {

	XSD_DURATION(xsdIRI("duration"), true, true, false, false, false, false, false, true, false),
	XSD_DATETIME(xsdIRI("dateTime"), true, false, false, false, false, false, true, true, false),
	XSD_DATETIMESTAMP(xsdIRI("dateTimeStamp"), false, false, false, true, false, false, true, true, false),
	XSD_DAYTIMEDURATION(xsdIRI("dayTimeDuration"), false, true, false, true, false, false, false, true, false),
	XSD_TIME(xsdIRI("time"), true, false, false, false, false, false, true, true, false),
	XSD_DATE(xsdIRI("date"), true, false, false, false, false, false, true, true, false),
	XSD_GYEARMONTH(xsdIRI("gYearMonth"), true, false, false, false, false, false, true, true, false),
	XSD_GYEAR(xsdIRI("gYear"), true, false, false, false, false, false, true, true, false),
	XSD_GMONTHDAY(xsdIRI("gMonthDay"), true, false, false, false, false, false, true, true, false),
	XSD_GDAY(xsdIRI("gDay"), true, false, false, false, false, false, true, true, false),
	XSD_GMONTH(xsdIRI("gMonth"), true, false, false, false, false, false, true, true, false),
	XSD_STRING(xsdIRI("string"), true, false, false, false, false, false, false, true, false),
	XSD_BOOLEAN(xsdIRI("boolean"), true, false, false, false, false, false, false, true, false),
	XSD_BASE64BINARY(xsdIRI("base64Binary"), true, false, false, false, false, false, false, true, false),
	XSD_HEXBINARY(xsdIRI("hexBinary"), true, false, false, false, false, false, false, true, false),
	XSD_FLOAT(xsdIRI("float"), true, false, false, false, false, true, false, true, false),
	XSD_DECIMAL(xsdIRI("decimal"), true, false, false, false, true, false, false, true, false),
	XSD_DOUBLE(xsdIRI("double"), true, false, false, false, false, true, false, true, false),
	XSD_ANYURI(xsdIRI("anyURI"), true, false, false, false, false, false, false, true, false),
	XSD_QNAME(xsdIRI("QName"), true, false, false, false, false, false, false, true, false),
	XSD_NOTATION(xsdIRI("NOTATION"), true, false, false, false, false, false, false, true, false),
	XSD_NORMALIZEDSTRING(xsdIRI("normalizedString"), false, false, false, true, false, false, false, true, false),
	XSD_TOKEN(xsdIRI("token"), false, false, false, true, false, false, false, true, false),
	XSD_LANGUAGE(xsdIRI("language"), false, false, false, true, false, false, false, true, false),
	XSD_NMTOKEN(xsdIRI("NMTOKEN"), false, false, false, true, false, false, false, true, false),
	XSD_NMTOKENS(xsdIRI("NMTOKENS"), false, false, false, true, false, false, false, true, false),
	XSD_NAME(xsdIRI("Name"), false, false, false, true, false, false, false, true, false),
	XSD_NCNAME(xsdIRI("NCName"), false, false, false, true, false, false, false, true, false),
	XSD_ID(xsdIRI("ID"), false, false, false, true, false, false, false, true, false),
	XSD_IDREF(xsdIRI("IDREF"), false, false, false, true, false, false, false, true, false),
	XSD_IDREFS(xsdIRI("IDREFS"), false, false, false, true, false, false, false, true, false),
	XSD_ENTITY(xsdIRI("ENTITY"), false, false, false, true, false, false, false, true, false),
	XSD_ENTITIES(xsdIRI("ENTITIES"), false, false, false, true, false, false, false, true, false),
	XSD_INTEGER(xsdIRI("integer"), false, false, true, true, true, false, false, true, false),
	XSD_LONG(xsdIRI("long"), false, false, true, true, true, false, false, true, false),
	XSD_INT(xsdIRI("int"), false, false, true, true, true, false, false, true, false),
	XSD_SHORT(xsdIRI("short"), false, false, true, true, true, false, false, true, false),
	XSD_BYTE(xsdIRI("byte"), false, false, true, true, true, false, false, true, false),
	XSD_NON_POSITIVE_INTEGER(xsdIRI("nonPositiveInteger"), false, false, true, true, true, false, false, true, false),
	XSD_NEGATIVE_INTEGER(xsdIRI("negativeInteger"), false, false, true, true, true, false, false, true, false),
	XSD_NON_NEGATIVE_INTEGER(xsdIRI("nonNegativeInteger"), false, false, true, true, true, false, false, true, false),
	XSD_POSITIVE_INTEGER(xsdIRI("positiveInteger"), false, false, true, true, true, false, false, true, false),
	XSD_UNSIGNED_LONG(xsdIRI("unsignedLong"), false, false, true, true, true, false, false, true, false),
	XSD_UNSIGNED_INT(xsdIRI("unsignedInt"), false, false, true, true, true, false, false, true, false),
	XSD_UNSIGNED_SHORT(xsdIRI("unsignedShort"), false, false, true, true, true, false, false, true, false),
	XSD_UNSIGNED_BYTE(xsdIRI("unsignedByte"), false, false, true, true, true, false, false, true, false),
	XSD_YEARMONTHDURATION(xsdIRI("yearMonthDuration"), false, true, false, true, false, false, false, true, false),
	RDF_LANGSTRING(rdfIRI("langString"), false, false, false, false, false, false, false, false, true);

	private static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema#";
	private static final String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

	private static IRI xsdIRI(String localName) {
		return new AbstractIRI.GenericIRI(XSD_NAMESPACE, localName);
	}

	private static IRI rdfIRI(String localName) {
		return new AbstractIRI.GenericIRI(RDF_NAMESPACE, localName);
	}

	private final IRI iri;
	private final boolean primitive;
	private final boolean duration;
	private final boolean integer;
	private final boolean derived;
	private final boolean decimal;
	private final boolean floatingPoint;
	private final boolean calendar;
	private final boolean xsdDatatype;
	private final boolean rdfDatatype;
	private final Optional<CoreDatatype> optional;

	CoreDatatype(IRI iri, boolean primitive, boolean duration, boolean integer, boolean derived, boolean decimal,
			boolean floatingPoint, boolean calendar, boolean xsdDatatype, boolean rdfDatatype) {
		this.iri = iri;
		this.primitive = primitive;
		this.duration = duration;
		this.integer = integer;
		this.derived = derived;
		this.decimal = decimal;
		this.floatingPoint = floatingPoint;
		this.calendar = calendar;
		this.xsdDatatype = xsdDatatype;
		this.rdfDatatype = rdfDatatype;
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
	 * Checks whether the supplied datatype is equal to xsd:dateTime, xsd:date, xsd:time, xsd:gYearMonth, xsd:gMonthDay,
	 * xsd:gYear, xsd:gMonth or xsd:gDay.These are the primitive datatypes that represent dates and/or times.
	 *
	 * @return true if it is a calendar type
	 *
	 * @see XMLGregorianCalendar
	 */
	public boolean isCalendarDatatype() {
		return calendar;
	}

	/**
	 * Checks whether the supplied datatype is equal to xsd:duration, xsd:dayTimeDuration, xsd:yearMonthDuration. These
	 * are the datatypes that represents durations.
	 *
	 * @return true if it is a duration type
	 *
	 * @see Duration
	 */
	public boolean isDurationDatatype() {
		return duration;
	}

	/**
	 * Checks whether the supplied datatype is ordered.The values of an ordered datatype can be compared to each other
	 * using operators like <var>&lt;</var> and <var>&gt;</var>.
	 *
	 * @return true if the datatype is ordered
	 */
	public boolean isOrderedDatatype() {
		return isNumericDatatype() || isCalendarDatatype();
	}

	/**
	 * Checks whether the supplied datatype is an XML Schema Datatype.
	 *
	 * @return true if the datatype is an XML Schema Datatype
	 */
	public boolean isXsdDatatype() {
		return xsdDatatype;
	}

	public IRI getIri() {
		return iri;
	}

	public Optional<CoreDatatype> asOptional() {
		return optional;
	}

	private static final HashMap<IRI, Optional<CoreDatatype>> reverseLookup = new HashMap<>();

	static {
		for (CoreDatatype value : CoreDatatype.values()) {
			reverseLookup.put(value.iri, Optional.of(value));
		}
	}

	public static Optional<CoreDatatype> from(IRI datatype) {
		if (datatype == null)
			return Optional.empty();
		return reverseLookup.getOrDefault(datatype, Optional.empty());
	}

	public static class Cache implements Serializable {

		private static final long serialVersionUID = 6739573;

		private CoreDatatype coreDatatype;
		private boolean cached = false;

		private Cache() {
		}

		public static Cache from(CoreDatatype datatype) {
			Cache cache = new Cache();
			cache.setDatatype(datatype);
			return cache;
		}

		public static Cache empty() {
			return new Cache();
		}

		public void setDatatype(CoreDatatype coreDatatype) {
			this.coreDatatype = coreDatatype;
			this.cached = true;
		}

		public void clearCache() {
			this.cached = false;
			this.coreDatatype = null;
		}

		public Optional<CoreDatatype> getCached(IRI datatype) {
			if (!cached) {
				coreDatatype = CoreDatatype.from(datatype).orElse(null);
				cached = true;
			}
			if (coreDatatype != null)
				return coreDatatype.asOptional();
			return Optional.empty();
		}

	}

}
