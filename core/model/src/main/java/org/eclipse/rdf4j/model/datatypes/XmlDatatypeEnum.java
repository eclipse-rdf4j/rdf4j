package org.eclipse.rdf4j.model.datatypes;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import com.google.common.collect.Sets;

public enum XmlDatatypeEnum {

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

	IRI iri;
	boolean primitive;
	private boolean duration;
	private boolean integer;
	private boolean derived;
	private boolean decimal;
	private boolean floatingPoint;
	private boolean calendar;

	XmlDatatypeEnum(IRI iri) {
		this.iri = iri;
	}

	XmlDatatypeEnum(IRI iri, boolean primitive, boolean duration, boolean integer, boolean derived, boolean decimal,
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
	 * @param datatype
	 * @return true if the datatype is a primitive type
	 */
	public boolean isPrimitiveDatatype() {
		return primitive;
	}

	/**
	 * Checks whether the supplied datatype is a derived XML Schema datatype.
	 *
	 * @param datatype
	 * @return true if the datatype is a derived type
	 */
	public boolean isDerivedDatatype() {
		return derived;
	}

	/**
	 * Checks whether the supplied datatype is a built-in XML Schema datatype.
	 *
	 * @param datatype
	 * @return true if it is a primitive or derived XML Schema type
	 */
	public boolean isBuiltInDatatype() {
		return isPrimitiveDatatype() || isDerivedDatatype();
	}

	/**
	 * Checks whether the supplied datatype is a numeric datatype, i.e.if it is equal to xsd:float, xsd:double,
	 * xsd:decimal or one of the datatypes derived from xsd:decimal.
	 *
	 * @param datatype
	 * @return true of it is a decimal or floating point type
	 */
	public boolean isNumericDatatype() {
		return isDecimalDatatype() || isFloatingPointDatatype();
	}

	/**
	 * Checks whether the supplied datatype is equal to xsd:decimal or one of the built-in datatypes that is derived
	 * from xsd:decimal.
	 *
	 * @param datatype
	 * @return true if it is a decimal datatype
	 */
	public boolean isDecimalDatatype() {
		return decimal;
	}

	/**
	 * Checks whether the supplied datatype is equal to xsd:integer or one of the built-in datatypes that is derived
	 * from xsd:integer.
	 *
	 * @param datatype
	 * @return true if it is an integer type
	 */
	public boolean isIntegerDatatype() {
		return integer;
	}

	/**
	 * Checks whether the supplied datatype is equal to xsd:float or xsd:double.
	 *
	 * @param datatype
	 * @return true if it is a floating point type
	 */
	public boolean isFloatingPointDatatype() {
		return floatingPoint;
	}

	/**
	 * Checks whether the supplied datatype is equal to xsd:dateTime, xsd:date, xsd:time, xsd:gYearMonth, xsd:gMonthDay,
	 * xsd:gYear, xsd:gMonth or xsd:gDay.These are the primitive datatypes that represent dates and/or times.
	 *
	 * @see XMLGregorianCalendar
	 * @param datatype
	 * @return true if it is a calendar type
	 */
	public boolean isCalendarDatatype() {
		return calendar;
	}

	/**
	 * Checks whether the supplied datatype is equal to xsd:duration, xsd:dayTimeDuration, xsd:yearMonthDuration. These
	 * are the datatypes that represents durations.
	 *
	 * @see Duration
	 * @param datatype
	 * @return true if it is a duration type
	 */
	public boolean isDurationDatatype() {
		return duration;
	}

	/**
	 * Checks whether the supplied datatype is ordered.The values of an ordered datatype can be compared to each other
	 * using operators like <tt>&lt;</tt> and <tt>&gt;</tt>.
	 *
	 * @param datatype
	 * @return true if the datatype is ordered
	 */
	public boolean isOrderedDatatype() {
		return isNumericDatatype() || isCalendarDatatype();
	}

	static HashMap<IRI, XmlDatatypeEnum> reverseLookup = new HashMap<>();

	static {
		for (XmlDatatypeEnum value : XmlDatatypeEnum.values()) {
			reverseLookup.put(value.iri, value);
		}
	}

	public static Optional<XmlDatatypeEnum> from(IRI datatype) {
		return Optional.ofNullable(reverseLookup.get(datatype));
	}

	public IRI getIri() {
		return iri;
	}
}
