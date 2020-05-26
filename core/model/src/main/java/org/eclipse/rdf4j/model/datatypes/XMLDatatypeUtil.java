/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.datatypes;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.common.text.ASCIIUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * Provides methods for handling the standard XML Schema datatypes.
 *
 * @author Arjohn Kampman
 */
public class XMLDatatypeUtil {

	public static final String POSITIVE_INFINITY = "INF";

	public static final String NEGATIVE_INFINITY = "-INF";

	public static final String NaN = "NaN";

	private static DatatypeFactory dtFactory;

	static {
		try {
			dtFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	private final static Pattern P_DURATION = Pattern.compile(
			"-?P((\\d)+Y)?((\\d)+M)?((\\d)+D)?((T(\\d)+H((\\d)+M)?((\\d)+(\\.(\\d)+)?S)?)|(T(\\d)+M((\\d)+(\\.(\\d)+)?S)?)|(T(\\d)+(\\.(\\d)+)?S))?");
	private final static Pattern P_DAYTIMEDURATION = Pattern.compile(
			"-?P((\\d)+D)?((T(\\d)+H((\\d)+M)?((\\d)+(\\.(\\d)+)?S)?)|(T(\\d)+M((\\d)+(\\.(\\d)+)?S)?)|(T(\\d)+(\\.(\\d)+)?S))?");
	private final static Pattern P_YEARMONTHDURATION = Pattern.compile("-?P((\\d)+Y)?((\\d)+M)?");
	private final static Pattern P_TIMEZONE = Pattern.compile(".*(Z|[+-]((0\\d|1[0-3]):[0-5]\\d|14:00))$");
	private final static Pattern P_DATE = Pattern.compile("-?\\d{4,}-\\d\\d-\\d\\d(Z|(\\+|-)\\d\\d:\\d\\d)?");
	private final static Pattern P_TIME = Pattern.compile("\\d\\d:\\d\\d:\\d\\d(\\.\\d+)?(Z|(\\+|-)\\d\\d:\\d\\d)?");
	private final static Pattern P_GDAY = Pattern.compile("---\\d\\d(Z|(\\+|-)\\d\\d:\\d\\d)?");
	private final static Pattern P_GMONTH = Pattern.compile("--\\d\\d(Z|(\\+|-)\\d\\d:\\d\\d)?");
	private final static Pattern P_GMONTHDAY = Pattern.compile("--\\d\\d-\\d\\d(Z|(\\+|-)\\d\\d:\\d\\d)?");
	private final static Pattern P_GYEAR = Pattern.compile("-?\\d{4,}(Z|(\\+|-)\\d\\d:\\d\\d)?");
	private final static Pattern P_GYEARMONTH = Pattern.compile("-?\\d{4,}-\\d\\d(Z|(\\+|-)\\d\\d:\\d\\d)?");

	/*-------------------*
	 * Datatype checking *
	 *-------------------*/

	/**
	 * Checks whether the supplied datatype is a primitive XML Schema datatype.
	 *
	 * @param datatype
	 * @return true if the datatype is a primitive type
	 */
	public static boolean isPrimitiveDatatype(IRI datatype) {
		return datatype.equals(XSD.DURATION) || datatype.equals(XSD.DATETIME)
				|| datatype.equals(XSD.TIME) || datatype.equals(XSD.DATE)
				|| datatype.equals(XSD.GYEARMONTH) || datatype.equals(XSD.GYEAR)
				|| datatype.equals(XSD.GMONTHDAY) || datatype.equals(XSD.GDAY)
				|| datatype.equals(XSD.GMONTH) || datatype.equals(XSD.STRING)
				|| datatype.equals(XSD.BOOLEAN) || datatype.equals(XSD.BASE64BINARY)
				|| datatype.equals(XSD.HEXBINARY) || datatype.equals(XSD.FLOAT)
				|| datatype.equals(XSD.DECIMAL) || datatype.equals(XSD.DOUBLE)
				|| datatype.equals(XSD.ANYURI) || datatype.equals(XSD.QNAME)
				|| datatype.equals(XSD.NOTATION);
	}

	/**
	 * Checks whether the supplied datatype is a derived XML Schema datatype.
	 *
	 * @param datatype
	 * @return true if the datatype is a derived type
	 */
	public static boolean isDerivedDatatype(IRI datatype) {
		return datatype.equals(XSD.NORMALIZEDSTRING) || datatype.equals(XSD.TOKEN)
				|| datatype.equals(XSD.LANGUAGE) || datatype.equals(XSD.NMTOKEN)
				|| datatype.equals(XSD.NMTOKENS) || datatype.equals(XSD.NAME)
				|| datatype.equals(XSD.NCNAME) || datatype.equals(XSD.ID)
				|| datatype.equals(XSD.IDREF) || datatype.equals(XSD.IDREFS)
				|| datatype.equals(XSD.ENTITY) || datatype.equals(XSD.ENTITIES)
				|| datatype.equals(XSD.INTEGER) || datatype.equals(XSD.LONG)
				|| datatype.equals(XSD.INT) || datatype.equals(XSD.SHORT) || datatype.equals(XSD.BYTE)
				|| datatype.equals(XSD.NON_POSITIVE_INTEGER) || datatype.equals(XSD.NEGATIVE_INTEGER)
				|| datatype.equals(XSD.NON_NEGATIVE_INTEGER) || datatype.equals(XSD.POSITIVE_INTEGER)
				|| datatype.equals(XSD.UNSIGNED_LONG) || datatype.equals(XSD.UNSIGNED_INT)
				|| datatype.equals(XSD.UNSIGNED_SHORT) || datatype.equals(XSD.UNSIGNED_BYTE)
				|| datatype.equals(XSD.DAYTIMEDURATION) || datatype.equals(XSD.YEARMONTHDURATION)
				|| datatype.equals(XSD.DATETIMESTAMP);
	}

	/**
	 * Checks whether the supplied datatype is a built-in XML Schema datatype.
	 *
	 * @param datatype
	 * @return true if it is a primitive or derived XML Schema type
	 */
	public static boolean isBuiltInDatatype(IRI datatype) {
		return isPrimitiveDatatype(datatype) || isDerivedDatatype(datatype);
	}

	/**
	 * Checks whether the supplied datatype is a numeric datatype, i.e.if it is equal to xsd:float, xsd:double,
	 * xsd:decimal or one of the datatypes derived from xsd:decimal.
	 *
	 * @param datatype
	 * @return true of it is a decimal or floating point type
	 */
	public static boolean isNumericDatatype(IRI datatype) {
		return isDecimalDatatype(datatype) || isFloatingPointDatatype(datatype);
	}

	/**
	 * Checks whether the supplied datatype is equal to xsd:decimal or one of the built-in datatypes that is derived
	 * from xsd:decimal.
	 *
	 * @param datatype
	 * @return true if it is a decimal datatype
	 */
	public static boolean isDecimalDatatype(IRI datatype) {
		return datatype.equals(XSD.DECIMAL) || isIntegerDatatype(datatype);
	}

	/**
	 * Checks whether the supplied datatype is equal to xsd:integer or one of the built-in datatypes that is derived
	 * from xsd:integer.
	 *
	 * @param datatype
	 * @return true if it is an integer type
	 */
	public static boolean isIntegerDatatype(IRI datatype) {
		return datatype.equals(XSD.INTEGER) || datatype.equals(XSD.LONG) || datatype.equals(XSD.INT)
				|| datatype.equals(XSD.SHORT) || datatype.equals(XSD.BYTE)
				|| datatype.equals(XSD.NON_POSITIVE_INTEGER) || datatype.equals(XSD.NEGATIVE_INTEGER)
				|| datatype.equals(XSD.NON_NEGATIVE_INTEGER) || datatype.equals(XSD.POSITIVE_INTEGER)
				|| datatype.equals(XSD.UNSIGNED_LONG) || datatype.equals(XSD.UNSIGNED_INT)
				|| datatype.equals(XSD.UNSIGNED_SHORT) || datatype.equals(XSD.UNSIGNED_BYTE);
	}

	/**
	 * Checks whether the supplied datatype is equal to xsd:float or xsd:double.
	 *
	 * @param datatype
	 * @return true if it is a floating point type
	 */
	public static boolean isFloatingPointDatatype(IRI datatype) {
		return datatype.equals(XSD.FLOAT) || datatype.equals(XSD.DOUBLE);
	}

	/**
	 * Checks whether the supplied datatype is equal to xsd:dateTime, xsd:date, xsd:time, xsd:gYearMonth, xsd:gMonthDay,
	 * xsd:gYear, xsd:gMonth or xsd:gDay.These are the primitive datatypes that represent dates and/or times.
	 *
	 * @see XMLGregorianCalendar
	 * @param datatype
	 * @return true if it is a calendar type
	 */
	public static boolean isCalendarDatatype(IRI datatype) {
		return datatype.equals(XSD.DATETIME) || datatype.equals(XSD.DATE) || datatype.equals(XSD.TIME)
				|| datatype.equals(XSD.GYEARMONTH) || datatype.equals(XSD.GMONTHDAY)
				|| datatype.equals(XSD.GYEAR) || datatype.equals(XSD.GMONTH)
				|| datatype.equals(XSD.GDAY) || datatype.equals(XSD.DATETIMESTAMP);

	}

	/**
	 * Checks whether the supplied datatype is equal to xsd:duration, xsd:dayTimeDuration, xsd:yearMonthDuration. These
	 * are the datatypes that represents durations.
	 *
	 * @see Duration
	 * @param datatype
	 * @return true if it is a duration type
	 */
	public static boolean isDurationDatatype(IRI datatype) {
		return datatype.equals(XSD.DURATION) || datatype.equals(XSD.DAYTIMEDURATION)
				|| datatype.equals(XSD.YEARMONTHDURATION);
	}

	/**
	 * Checks whether the supplied datatype is ordered.The values of an ordered datatype can be compared to each other
	 * using operators like <tt>&lt;</tt> and <tt>&gt;</tt>.
	 *
	 * @param datatype
	 * @return true if the datatype is ordered
	 */
	public static boolean isOrderedDatatype(IRI datatype) {
		return isNumericDatatype(datatype) || isCalendarDatatype(datatype);
	}

	/*----------------*
	 * Value checking *
	 *----------------*/

	/**
	 * Verifies if the supplied lexical value is valid for the given datatype.
	 *
	 * @param value    a lexical value
	 * @param datatype an XML Schema datatatype.
	 * @return true if the supplied lexical value is valid, false otherwise.
	 */
	public static boolean isValidValue(String value, IRI datatype) {
		boolean result = true;

		if (datatype.equals(XSD.DECIMAL)) {
			result = isValidDecimal(value);
		} else if (datatype.equals(XSD.INTEGER)) {
			result = isValidInteger(value);
		} else if (datatype.equals(XSD.NEGATIVE_INTEGER)) {
			result = isValidNegativeInteger(value);
		} else if (datatype.equals(XSD.NON_POSITIVE_INTEGER)) {
			result = isValidNonPositiveInteger(value);
		} else if (datatype.equals(XSD.NON_NEGATIVE_INTEGER)) {
			result = isValidNonNegativeInteger(value);
		} else if (datatype.equals(XSD.POSITIVE_INTEGER)) {
			result = isValidPositiveInteger(value);
		} else if (datatype.equals(XSD.LONG)) {
			result = isValidLong(value);
		} else if (datatype.equals(XSD.INT)) {
			result = isValidInt(value);
		} else if (datatype.equals(XSD.SHORT)) {
			result = isValidShort(value);
		} else if (datatype.equals(XSD.BYTE)) {
			result = isValidByte(value);
		} else if (datatype.equals(XSD.UNSIGNED_LONG)) {
			result = isValidUnsignedLong(value);
		} else if (datatype.equals(XSD.UNSIGNED_INT)) {
			result = isValidUnsignedInt(value);
		} else if (datatype.equals(XSD.UNSIGNED_SHORT)) {
			result = isValidUnsignedShort(value);
		} else if (datatype.equals(XSD.UNSIGNED_BYTE)) {
			result = isValidUnsignedByte(value);
		} else if (datatype.equals(XSD.FLOAT)) {
			result = isValidFloat(value);
		} else if (datatype.equals(XSD.DOUBLE)) {
			result = isValidDouble(value);
		} else if (datatype.equals(XSD.BOOLEAN)) {
			result = isValidBoolean(value);
		} else if (datatype.equals(XSD.DATETIME)) {
			result = isValidDateTime(value);
		} else if (datatype.equals(XSD.DATETIMESTAMP)) {
			result = isValidDateTimeStamp(value);
		} else if (datatype.equals(XSD.DATE)) {
			result = isValidDate(value);
		} else if (datatype.equals(XSD.TIME)) {
			result = isValidTime(value);
		} else if (datatype.equals(XSD.GDAY)) {
			result = isValidGDay(value);
		} else if (datatype.equals(XSD.GMONTH)) {
			result = isValidGMonth(value);
		} else if (datatype.equals(XSD.GMONTHDAY)) {
			result = isValidGMonthDay(value);
		} else if (datatype.equals(XSD.GYEAR)) {
			result = isValidGYear(value);
		} else if (datatype.equals(XSD.GYEARMONTH)) {
			result = isValidGYearMonth(value);
		} else if (datatype.equals(XSD.DURATION)) {
			result = isValidDuration(value);
		} else if (datatype.equals(XSD.DAYTIMEDURATION)) {
			result = isValidDayTimeDuration(value);
		} else if (datatype.equals(XSD.YEARMONTHDURATION)) {
			result = isValidYearMonthDuration(value);
		} else if (datatype.equals(XSD.QNAME)) {
			result = isValidQName(value);
		} else if (datatype.equals(XSD.ANYURI)) {
			result = isValidAnyURI(value);
		} else if (datatype.equals(XSD.LANGUAGE)) {
			result = Literals.isValidLanguageTag(value);
		}

		return result;
	}

	/**
	 * Verifies if the supplied lexical value is a valid decimal or not.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidDecimal(String value) {
		try {
			normalizeDecimal(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid integer or not.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidInteger(String value) {
		try {
			normalizeInteger(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid negative integer or not.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidNegativeInteger(String value) {
		try {
			normalizeNegativeInteger(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid non-positive integer or not.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidNonPositiveInteger(String value) {
		try {
			normalizeNonPositiveInteger(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid non-negative integer or not.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidNonNegativeInteger(String value) {
		try {
			normalizeNonNegativeInteger(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid positive integer or not.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidPositiveInteger(String value) {
		try {
			normalizePositiveInteger(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid long or not.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidLong(String value) {
		try {
			normalizeLong(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid integer or not.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidInt(String value) {
		try {
			normalizeInt(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid short or not.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidShort(String value) {
		try {
			normalizeShort(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid byte or not.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidByte(String value) {
		try {
			normalizeByte(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid unsigned long or not.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidUnsignedLong(String value) {
		try {
			normalizeUnsignedLong(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid unsigned int.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidUnsignedInt(String value) {
		try {
			normalizeUnsignedInt(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid unsigned short or not.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidUnsignedShort(String value) {
		try {
			normalizeUnsignedShort(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid unsigned byte or not.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidUnsignedByte(String value) {
		try {
			normalizeUnsignedByte(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid float or not.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidFloat(String value) {
		try {
			normalizeFloat(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid double or not.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidDouble(String value) {
		try {
			normalizeDouble(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid boolean or not.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidBoolean(String value) {
		try {
			normalizeBoolean(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid duration.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidDuration(String value) {
		// voodoo regex for checking valid xsd:duration string. See
		// http://www.w3.org/TR/xmlschema-2/#duration for details.
		return value.length() > 1 && P_DURATION.matcher(value).matches();
	}

	/**
	 * Verifies if the supplied lexical value is a valid day-time duration ot not.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidDayTimeDuration(String value) {
		return value.length() > 1 && P_DAYTIMEDURATION.matcher(value).matches();
	}

	/**
	 * Verifies if the supplied lexical value is a valid year-month duration.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidYearMonthDuration(String value) {
		return value.length() > 1 && P_YEARMONTHDURATION.matcher(value).matches();
	}

	/**
	 * Verifies if the supplied lexical value is a valid date-time.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidDateTime(String value) {
		try {
			@SuppressWarnings("unused")
			XMLDateTime dt = new XMLDateTime(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Verifies if the supplied lexical value is a valid date-timestamp.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidDateTimeStamp(String value) {
		try {
			@SuppressWarnings("unused")
			XMLDateTime dt = new XMLDateTime(value);
			return P_TIMEZONE.matcher(value).matches();
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Determines if the supplied value is a valid xsd:date string.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidDate(String value) {
		return P_DATE.matcher(value).matches() ? isValidCalendarValue(value) : false;
	}

	/**
	 * Determines if the supplied value is a valid xsd:time string.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidTime(String value) {
		return P_TIME.matcher(value).matches() ? isValidCalendarValue(value) : false;
	}

	/**
	 * Determines if the supplied value is a valid xsd:gDay string.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidGDay(String value) {
		return P_GDAY.matcher(value).matches() ? isValidCalendarValue(value) : false;
	}

	/**
	 * Determines if the supplied value is a valid xsd:gMonth string.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidGMonth(String value) {
		return P_GMONTH.matcher(value).matches() ? isValidCalendarValue(value) : false;
	}

	/**
	 * Determines if the supplied value is a valid xsd:gMonthDay string.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidGMonthDay(String value) {
		return P_GMONTHDAY.matcher(value).matches() ? isValidCalendarValue(value) : false;
	}

	/**
	 * Determines if the supplied value is a valid xsd:gYear string.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidGYear(String value) {
		return P_GYEAR.matcher(value).matches() ? isValidCalendarValue(value) : false;
	}

	/**
	 * Determines if the supplied value is a valid xsd:gYearMonth string.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidGYearMonth(String value) {
		return P_GYEARMONTH.matcher(value).matches() ? isValidCalendarValue(value) : false;
	}

	/**
	 * Determines if the supplied value is a valid xsd:QName string. Note that this method only checks for syntax errors
	 * in the supplied string itself. It does not validate that the prefix is a declared and in-scope namespace prefix.
	 *
	 * @param value
	 * @return <tt>true</tt> if valid, <tt>false</tt> otherwise
	 */
	public static boolean isValidQName(String value) {

		String[] split = value.split(":", -2);

		if (split.length != 2) {
			return false;
		}

		// check prefix
		String prefix = split[0];
		if (!"".equals(prefix)) {
			if (!isPrefixStartChar(prefix.charAt(0))) {
				return false;
			}

			for (int i = 1; i < prefix.length(); i++) {
				if (!isNameChar(prefix.charAt(i))) {
					return false;
				}
			}
		}

		String name = split[1];

		if (!"".equals(name)) {
			// check name
			if (!isNameStartChar(name.charAt(0))) {
				return false;
			}

			for (int i = 1; i < name.length(); i++) {
				if (!isNameChar(name.charAt(i))) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Determines if the supplied value is an Internationalized Resource Identifier Reference (IRI). An anyURI value can
	 * be absolute or relative, and may have an optional fragment identifier (i.e., it may be an IRI Reference). This
	 * type should be used when the value fulfills the role of an IRI, as defined in [RFC&nbsp;3987] or its successor(s)
	 * in the IETF Standards Track.
	 *
	 * @param value
	 * @return <tt>true</tt> if a valid IRI, <tt>false</tt> otherwise
	 */
	public static boolean isValidAnyURI(String value) {
		try {
			new ParsedIRI(value.trim());
			return true;
		} catch (URISyntaxException e) {
			return false;
		}
	}

	private static boolean isPrefixStartChar(int c) {
		return ASCIIUtil.isLetter(c) || c >= 0x00C0 && c <= 0x00D6 || c >= 0x00D8 && c <= 0x00F6
				|| c >= 0x00F8 && c <= 0x02FF || c >= 0x0370 && c <= 0x037D || c >= 0x037F && c <= 0x1FFF
				|| c >= 0x200C && c <= 0x200D || c >= 0x2070 && c <= 0x218F || c >= 0x2C00 && c <= 0x2FEF
				|| c >= 0x3001 && c <= 0xD7FF || c >= 0xF900 && c <= 0xFDCF || c >= 0xFDF0 && c <= 0xFFFD
				|| c >= 0x10000 && c <= 0xEFFFF;
	}

	private static boolean isNameStartChar(int c) {
		return c == '_' || isPrefixStartChar(c);
	}

	private static boolean isNameChar(int c) {
		return isNameStartChar(c) || ASCIIUtil.isNumber(c) || c == '-' || c == 0x00B7 || c >= 0x0300 && c <= 0x036F
				|| c >= 0x203F && c <= 0x2040;
	}

	/**
	 * Determines if the supplied string can be parsed to a valid XMLGregorianCalendar value.
	 *
	 * @param value
	 * @return true if the supplied string is a parsable calendar value, false otherwise.
	 */
	private static boolean isValidCalendarValue(String value) {
		try {
			XMLDatatypeUtil.parseCalendar(value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/*---------------------*
	 * Value normalization *
	 *---------------------*/

	/**
	 * Normalizes the supplied value according to the normalization rules for the supplied datatype.
	 *
	 * @param value    The value to normalize.
	 * @param datatype The value's datatype.
	 * @return The normalized value if there are any (supported) normalization rules for the supplied datatype, or the
	 *         original supplied value otherwise.
	 * @throws IllegalArgumentException If the supplied value is illegal considering the supplied datatype.
	 */
	public static String normalize(String value, IRI datatype) {
		String result = value;

		if (datatype.equals(XSD.DECIMAL)) {
			result = normalizeDecimal(value);
		} else if (datatype.equals(XSD.INTEGER)) {
			result = normalizeInteger(value);
		} else if (datatype.equals(XSD.NEGATIVE_INTEGER)) {
			result = normalizeNegativeInteger(value);
		} else if (datatype.equals(XSD.NON_POSITIVE_INTEGER)) {
			result = normalizeNonPositiveInteger(value);
		} else if (datatype.equals(XSD.NON_NEGATIVE_INTEGER)) {
			result = normalizeNonNegativeInteger(value);
		} else if (datatype.equals(XSD.POSITIVE_INTEGER)) {
			result = normalizePositiveInteger(value);
		} else if (datatype.equals(XSD.LONG)) {
			result = normalizeLong(value);
		} else if (datatype.equals(XSD.INT)) {
			result = normalizeInt(value);
		} else if (datatype.equals(XSD.SHORT)) {
			result = normalizeShort(value);
		} else if (datatype.equals(XSD.BYTE)) {
			result = normalizeByte(value);
		} else if (datatype.equals(XSD.UNSIGNED_LONG)) {
			result = normalizeUnsignedLong(value);
		} else if (datatype.equals(XSD.UNSIGNED_INT)) {
			result = normalizeUnsignedInt(value);
		} else if (datatype.equals(XSD.UNSIGNED_SHORT)) {
			result = normalizeUnsignedShort(value);
		} else if (datatype.equals(XSD.UNSIGNED_BYTE)) {
			result = normalizeUnsignedByte(value);
		} else if (datatype.equals(XSD.FLOAT)) {
			result = normalizeFloat(value);
		} else if (datatype.equals(XSD.DOUBLE)) {
			result = normalizeDouble(value);
		} else if (datatype.equals(XSD.BOOLEAN)) {
			result = normalizeBoolean(value);
		} else if (datatype.equals(XSD.DATETIME)) {
			result = normalizeDateTime(value);
		} else if (datatype.equals(XSD.ANYURI)) {
			result = collapseWhiteSpace(value);
		}

		return result;
	}

	/**
	 * Normalizes a boolean value to its canonical representation. More specifically, the values <tt>1</tt> and
	 * <tt>0</tt> will be normalized to the canonical values <tt>true</tt> and <tt>false</tt>, respectively. Supplied
	 * canonical values will remain as is.
	 *
	 * @param value The boolean value to normalize.
	 * @return The normalized value.
	 * @throws IllegalArgumentException If the supplied value is not a legal boolean.
	 */
	public static String normalizeBoolean(String value) {
		value = collapseWhiteSpace(value);

		if (value.equals("1")) {
			return "true";
		} else if (value.equals("0")) {
			return "false";
		} else if (value.equals("true") || value.equals("false")) {
			return value;
		} else {
			throw new IllegalArgumentException("Not a legal boolean value: " + value);
		}
	}

	/**
	 * Normalizes a decimal to its canonical representation. For example: <tt>120</tt> becomes <tt>120.0</tt>,
	 * <tt>+.3</tt> becomes <tt>0.3</tt>, <tt>00012.45000</tt> becomes <tt>12.45</tt> and <tt>-.0</tt> becomes
	 * <tt>0.0</tt>.
	 *
	 * @param decimal The decimal to normalize.
	 * @return The canonical representation of <tt>decimal</tt>.
	 * @throws IllegalArgumentException If one of the supplied strings is not a legal decimal.
	 */
	public static String normalizeDecimal(String decimal) {
		decimal = collapseWhiteSpace(decimal);

		int decLength = decimal.length();
		StringBuilder result = new StringBuilder(decLength + 2);

		if (decLength == 0) {
			throwIAE("Not a legal decimal: " + decimal);
		}

		boolean isZeroPointZero = true;

		// process any sign info
		int idx = 0;
		if (decimal.charAt(idx) == '-') {
			result.append('-');
			idx++;
		} else if (decimal.charAt(idx) == '+') {
			idx++;
		}

		if (idx == decLength) {
			throwIAE("Not a legal decimal: " + decimal);
		}

		// skip any leading zeros
		while (idx < decLength && decimal.charAt(idx) == '0') {
			idx++;
		}

		// Process digits before the dot
		if (idx == decLength) {
			// decimal consists of zeros only
			result.append('0');
		} else if (idx < decLength && decimal.charAt(idx) == '.') {
			// no non-zero digit before the dot
			result.append('0');
		} else {
			isZeroPointZero = false;

			// Copy any digits before the dot
			while (idx < decLength) {
				char c = decimal.charAt(idx);
				if (c == '.') {
					break;
				}
				if (!isDigit(c)) {
					throwIAE("Not a legal decimal: " + decimal);
				}
				result.append(c);
				idx++;
			}
		}

		result.append('.');

		// Process digits after the dot
		if (idx == decLength) {
			// No dot was found in the decimal
			result.append('0');
		} else {
			idx++;

			// search last non-zero digit
			int lastIdx = decLength - 1;
			while (lastIdx >= 0 && decimal.charAt(lastIdx) == '0') {
				lastIdx--;
			}

			if (idx > lastIdx) {
				// No non-zero digits found
				result.append('0');
			} else {
				isZeroPointZero = false;

				while (idx <= lastIdx) {
					char c = decimal.charAt(idx);
					if (!isDigit(c)) {
						throwIAE("Not a legal decimal: " + decimal);
					}
					result.append(c);
					idx++;
				}
			}
		}

		if (isZeroPointZero) {
			// Make sure we don't return "-0.0"
			return "0.0";
		} else {
			return result.toString();
		}
	}

	/**
	 * Normalizes an integer to its canonical representation. For example: <tt>+120</tt> becomes <tt>120</tt> and
	 * <tt>00012</tt> becomes <tt>12</tt>.
	 *
	 * @param value The value to normalize.
	 * @return The canonical representation of <tt>value</tt>.
	 * @throws IllegalArgumentException If the supplied value is not a legal integer.
	 */
	public static String normalizeInteger(String value) {
		return normalizeIntegerValue(value, null, null);
	}

	/**
	 * Normalizes an xsd:negativeInteger.
	 */
	public static String normalizeNegativeInteger(String value) {
		return normalizeIntegerValue(value, null, "-1");
	}

	/**
	 * Normalizes an xsd:nonPositiveInteger.
	 */
	public static String normalizeNonPositiveInteger(String value) {
		return normalizeIntegerValue(value, null, "0");
	}

	/**
	 * Normalizes an xsd:nonNegativeInteger.
	 */
	public static String normalizeNonNegativeInteger(String value) {
		return normalizeIntegerValue(value, "0", null);
	}

	/**
	 * Normalizes an xsd:positiveInteger.
	 */
	public static String normalizePositiveInteger(String value) {
		return normalizeIntegerValue(value, "1", null);
	}

	/**
	 * Normalizes an xsd:long.
	 */
	public static String normalizeLong(String value) {
		return normalizeIntegerValue(value, "-9223372036854775808", "9223372036854775807");
	}

	/**
	 * Normalizes an xsd:int.
	 */
	public static String normalizeInt(String value) {
		return normalizeIntegerValue(value, "-2147483648", "2147483647");
	}

	/**
	 * Normalizes an xsd:short.
	 */
	public static String normalizeShort(String value) {
		return normalizeIntegerValue(value, "-32768", "32767");
	}

	/**
	 * Normalizes an xsd:byte.
	 */
	public static String normalizeByte(String value) {
		return normalizeIntegerValue(value, "-128", "127");
	}

	/**
	 * Normalizes an xsd:unsignedLong.
	 */
	public static String normalizeUnsignedLong(String value) {
		return normalizeIntegerValue(value, "0", "18446744073709551615");
	}

	/**
	 * Normalizes an xsd:unsignedInt.
	 */
	public static String normalizeUnsignedInt(String value) {
		return normalizeIntegerValue(value, "0", "4294967295");
	}

	/**
	 * Normalizes an xsd:unsignedShort.
	 */
	public static String normalizeUnsignedShort(String value) {
		return normalizeIntegerValue(value, "0", "65535");
	}

	/**
	 * Normalizes an xsd:unsignedByte.
	 */
	public static String normalizeUnsignedByte(String value) {
		return normalizeIntegerValue(value, "0", "255");
	}

	/**
	 * Normalizes an integer to its canonical representation and checks that the value is in the range [minValue,
	 * maxValue].
	 */
	private static String normalizeIntegerValue(String integer, String minValue, String maxValue) {
		integer = collapseWhiteSpace(integer);

		int intLength = integer.length();

		if (intLength == 0) {
			throwIAE("Not a legal integer: " + integer);
		}

		int idx = 0;

		// process any sign info
		boolean isNegative = false;
		if (integer.charAt(idx) == '-') {
			isNegative = true;
			idx++;
		} else if (integer.charAt(idx) == '+') {
			idx++;
		}

		if (idx == intLength) {
			throwIAE("Not a legal integer: " + integer);
		}

		if (integer.charAt(idx) == '0' && idx < intLength - 1) {
			// integer starts with a zero followed by more characters,
			// skip any leading zeros
			idx++;
			while (idx < intLength - 1 && integer.charAt(idx) == '0') {
				idx++;
			}
		}

		String norm = integer.substring(idx);

		// Check that all characters in 'norm' are digits
		for (int i = 0; i < norm.length(); i++) {
			if (!isDigit(norm.charAt(i))) {
				throwIAE("Not a legal integer: " + integer);
			}
		}

		if (isNegative && norm.charAt(0) != '0') {
			norm = "-" + norm;
		}

		// Check lower and upper bounds, if applicable
		if (minValue != null) {
			if (compareCanonicalIntegers(norm, minValue) < 0) {
				throwIAE("Value smaller than minimum value");
			}
		}
		if (maxValue != null) {
			if (compareCanonicalIntegers(norm, maxValue) > 0) {
				throwIAE("Value larger than maximum value");
			}
		}

		return norm;
	}

	/**
	 * Normalizes a float to its canonical representation.
	 *
	 * @param value The value to normalize.
	 * @return The canonical representation of <tt>value</tt>.
	 * @throws IllegalArgumentException If the supplied value is not a legal float.
	 */
	public static String normalizeFloat(String value) {
		return normalizeFPNumber(value, "-16777215.0", "16777215.0", "-149", "104");
	}

	/**
	 * Normalizes a double to its canonical representation.
	 *
	 * @param value The value to normalize.
	 * @return The canonical representation of <tt>value</tt>.
	 * @throws IllegalArgumentException If the supplied value is not a legal double.
	 */
	public static String normalizeDouble(String value) {
		return normalizeFPNumber(value, "-9007199254740991.0", "9007199254740991.0", "-1075", "970");
	}

	/**
	 * Normalizes a floating point number to its canonical representation.
	 *
	 * @param value The value to normalize.
	 * @return The canonical representation of <tt>value</tt>.
	 * @throws IllegalArgumentException If the supplied value is not a legal floating point number.
	 */
	public static String normalizeFPNumber(String value) {
		return normalizeFPNumber(value, null, null, null, null);
	}

	/**
	 * Normalizes a floating point lexical value to its canonical representation.
	 *
	 * @param value       The lexical value to normalize.
	 * @param minMantissa A normalized decimal indicating the lowest value that the mantissa may have.
	 * @param maxMantissa A normalized decimal indicating the highest value that the mantissa may have.
	 * @param minExponent A normalized integer indicating the lowest value that the exponent may have.
	 * @param maxExponent A normalized integer indicating the highest value that the exponent may have.
	 * @return The canonical representation of <tt>value</tt>.
	 * @throws IllegalArgumentException If the supplied value is not a legal floating point lexical value.
	 */
	private static String normalizeFPNumber(String value, String minMantissa, String maxMantissa, String minExponent,
			String maxExponent) {
		value = collapseWhiteSpace(value);

		if (value.contains(" ")) {
			// floating point lexical value can not contain spaces after collapse
			throwIAE("No space allowed in floating point lexical value (" + value + ")");
		}

		// handle special values
		if (value.equals(POSITIVE_INFINITY) || value.equals(NEGATIVE_INFINITY) || value.equals(NaN)) {
			return value;
		}

		// Search for the exponent character E or e
		int eIdx = value.indexOf('E');
		if (eIdx == -1) {
			// try lower case
			eIdx = value.indexOf('e');
		}

		// Extract mantissa and exponent
		String mantissa, exponent;
		if (eIdx == -1) {
			mantissa = normalizeDecimal(value);
			exponent = "0";
		} else {
			mantissa = normalizeDecimal(value.substring(0, eIdx));
			exponent = normalizeInteger(value.substring(eIdx + 1));
		}

		// Normalize mantissa to one non-zero digit before the dot
		int shift = 0;

		int dotIdx = mantissa.indexOf('.');
		int digitCount = dotIdx;
		if (mantissa.charAt(0) == '-') {
			digitCount--;
		}

		if (digitCount > 1) {
			// more than one digit before the dot, e.g 123.45, -10.0 or 100.0
			StringBuilder sb = new StringBuilder(mantissa.length());
			int firstDigitIdx = 0;
			if (mantissa.charAt(0) == '-') {
				sb.append('-');
				firstDigitIdx = 1;
			}
			sb.append(mantissa.charAt(firstDigitIdx));
			sb.append('.');
			sb.append(mantissa.substring(firstDigitIdx + 1, dotIdx));
			sb.append(mantissa.substring(dotIdx + 1));

			mantissa = sb.toString();

			// Check if the mantissa has excessive trailing zeros.
			// For example, 100.0 will be normalize to 1.000 and
			// -10.0 to -1.00.
			int nonZeroIdx = mantissa.length() - 1;
			while (nonZeroIdx >= 3 && mantissa.charAt(nonZeroIdx) == '0') {
				nonZeroIdx--;
			}

			if (nonZeroIdx < 3 && mantissa.charAt(0) == '-') {
				nonZeroIdx++;
			}

			if (nonZeroIdx < mantissa.length() - 1) {
				mantissa = mantissa.substring(0, nonZeroIdx + 1);
			}

			shift = 1 - digitCount;
		} else if (mantissa.startsWith("0.") || mantissa.startsWith("-0.")) {
			// Example mantissas: 0.0, -0.1, 0.00345 and 0.09
			// search first non-zero digit
			int nonZeroIdx = 2;
			boolean negative = false;
			if (mantissa.charAt(0) == '-') {
				nonZeroIdx++;
				negative = true;
			}

			while (nonZeroIdx < mantissa.length() && mantissa.charAt(nonZeroIdx) == '0') {
				nonZeroIdx++;
			}

			// 0.0 does not need any normalization:
			if (nonZeroIdx < mantissa.length()) {
				StringBuilder sb = new StringBuilder(mantissa.length());
				if (negative) {
					sb.append('-');
				}
				sb.append(mantissa.charAt(nonZeroIdx));
				sb.append('.');
				if (nonZeroIdx == mantissa.length() - 1) {
					// There was only one non-zero digit, e.g. as in 0.09
					sb.append('0');
				} else {
					sb.append(mantissa.substring(nonZeroIdx + 1));
				}

				mantissa = sb.toString();

				// subtract position for minus sign from shift if a negative number
				shift = negative ? nonZeroIdx - 2 : nonZeroIdx - 1;
			}
		}

		if (shift != 0) {
			try {
				int exp = Integer.parseInt(exponent);
				exponent = String.valueOf(exp - shift);
			} catch (NumberFormatException e) {
				throw new RuntimeException("NumberFormatException: " + e.getMessage());
			}
		}

		// Check lower and upper bounds of canonicalized representation, if
		// applicable
		if (minMantissa != null) {
			if (compareCanonicalDecimals(mantissa, minMantissa) < 0) {
				throwIAE("Mantissa smaller than minimum value (" + minMantissa + ")");
			}
		}
		if (maxMantissa != null) {
			if (compareCanonicalDecimals(mantissa, maxMantissa) > 0) {
				throwIAE("Mantissa larger than maximum value (" + maxMantissa + ")");
			}
		}
		if (minExponent != null) {
			if (compareCanonicalIntegers(exponent, minExponent) < 0) {
				throwIAE("Exponent smaller than minimum value (" + minExponent + ")");
			}
		}
		if (maxExponent != null) {
			if (compareCanonicalIntegers(exponent, maxExponent) > 0) {
				throwIAE("Exponent larger than maximum value (" + maxExponent + ")");
			}
		}

		return mantissa + "E" + exponent;
	}

	/**
	 * Normalizes an xsd:dateTime.
	 *
	 * @param value The value to normalize.
	 * @return The normalized value.
	 * @throws IllegalArgumentException If the supplied value is not a legal xsd:dateTime value.
	 */
	public static String normalizeDateTime(String value) {
		XMLDateTime dt = new XMLDateTime(value);
		dt.normalize();
		return dt.toString();
	}

	/**
	 * Replaces all occurences of #x9 (tab), #xA (line feed) and #xD (carriage return) with #x20 (space), as specified
	 * for whiteSpace facet <tt>replace</tt>.
	 */
	// private static String replaceWhiteSpace(String s) {
	// s = StringUtil.gsub("\t", " ", s);
	// s = StringUtil.gsub("\r", " ", s);
	// s = StringUtil.gsub("\n", " ", s);
	// return s;
	// }

	/**
	 * Replaces all contiguous sequences of #x9 (tab), #xA (line feed) and #xD (carriage return) with a single #x20
	 * (space) character, and removes any leading and trailing whitespace characters, as specified for whiteSpace facet
	 * <tt>collapse</tt>.
	 *
	 * @param s
	 * @return new string
	 */
	public static String collapseWhiteSpace(String s) {
		StringBuilder sb = new StringBuilder(s.length());

		StringTokenizer st = new StringTokenizer(s, "\t\r\n ");

		if (st.hasMoreTokens()) {
			sb.append(st.nextToken());
		}

		while (st.hasMoreTokens()) {
			sb.append(' ').append(st.nextToken());
		}

		return sb.toString();
	}

	/*------------------*
	 * Value comparison *
	 *------------------*/

	public static int compare(String value1, String value2, IRI datatype) {
		if (datatype.equals(XSD.DECIMAL)) {
			return compareDecimals(value1, value2);
		} else if (datatype.equals(XSD.INTEGER)) {
			return compareIntegers(value1, value2);
		} else if (datatype.equals(XSD.NEGATIVE_INTEGER)) {
			return compareNegativeIntegers(value1, value2);
		} else if (datatype.equals(XSD.NON_POSITIVE_INTEGER)) {
			return compareNonPositiveIntegers(value1, value2);
		} else if (datatype.equals(XSD.NON_NEGATIVE_INTEGER)) {
			return compareNonNegativeIntegers(value1, value2);
		} else if (datatype.equals(XSD.POSITIVE_INTEGER)) {
			return comparePositiveIntegers(value1, value2);
		} else if (datatype.equals(XSD.LONG)) {
			return compareLongs(value1, value2);
		} else if (datatype.equals(XSD.INT)) {
			return compareInts(value1, value2);
		} else if (datatype.equals(XSD.SHORT)) {
			return compareShorts(value1, value2);
		} else if (datatype.equals(XSD.BYTE)) {
			return compareBytes(value1, value2);
		} else if (datatype.equals(XSD.UNSIGNED_LONG)) {
			return compareUnsignedLongs(value1, value2);
		} else if (datatype.equals(XSD.UNSIGNED_INT)) {
			return compareUnsignedInts(value1, value2);
		} else if (datatype.equals(XSD.UNSIGNED_SHORT)) {
			return compareUnsignedShorts(value1, value2);
		} else if (datatype.equals(XSD.UNSIGNED_BYTE)) {
			return compareUnsignedBytes(value1, value2);
		} else if (datatype.equals(XSD.FLOAT)) {
			return compareFloats(value1, value2);
		} else if (datatype.equals(XSD.DOUBLE)) {
			return compareDoubles(value1, value2);
		} else if (datatype.equals(XSD.DATETIME) || datatype.equals(XSD.DATETIMESTAMP)) {
			return compareDateTime(value1, value2);
		} else {
			throw new IllegalArgumentException("datatype is not ordered");
		}
	}

	/**
	 * Compares two decimals to eachother.
	 *
	 * @param dec1
	 * @param dec2
	 * @return A negative number if <tt>dec1</tt> is smaller than <tt>dec2</tt>, <tt>0</tt> if they are equal, or
	 *         positive (&gt;0) if <tt>dec1</tt> is larger than <tt>dec2</tt>.
	 * @throws IllegalArgumentException If one of the supplied strings is not a legal decimal.
	 */
	public static int compareDecimals(String dec1, String dec2) {
		dec1 = normalizeDecimal(dec1);
		dec2 = normalizeDecimal(dec2);

		return compareCanonicalDecimals(dec1, dec2);
	}

	/**
	 * Compares two canonical decimals to each other.
	 *
	 * @param dec1
	 * @param dec2
	 * @return A negative number if <tt>dec1</tt> is smaller than <tt>dec2</tt>, <tt>0</tt> if they are equal, or
	 *         positive (&gt;0) if <tt>dec1</tt> is larger than <tt>dec2</tt>. The result is undefined when one or both
	 *         of the arguments is not a canonical decimal.
	 * @throws IllegalArgumentException If one of the supplied strings is not a legal decimal.
	 */
	public static int compareCanonicalDecimals(String dec1, String dec2) {
		if (dec1.equals(dec2)) {
			return 0;
		}

		// Check signs
		if (dec1.charAt(0) == '-' && dec2.charAt(0) != '-') {
			// dec1 is negative, dec2 is not
			return -1;
		}
		if (dec2.charAt(0) == '-' && dec1.charAt(0) != '-') {
			// dec2 is negative, dec1 is not
			return 1;
		}

		int dotIdx1 = dec1.indexOf('.');
		int dotIdx2 = dec2.indexOf('.');

		// The decimal with the most digits before the dot is the largest
		int result = dotIdx1 - dotIdx2;

		if (result == 0) {
			// equal number of digits before the dot, compare them
			for (int i = 0; result == 0 && i < dotIdx1; i++) {
				result = dec1.charAt(i) - dec2.charAt(i);
			}

			// Continue comparing digits after the dot if necessary
			int dec1Length = dec1.length();
			int dec2Length = dec2.length();
			int lastIdx = dec1Length <= dec2Length ? dec1Length : dec2Length;

			for (int i = dotIdx1 + 1; result == 0 && i < lastIdx; i++) {
				result = dec1.charAt(i) - dec2.charAt(i);
			}

			// Still equal? The decimal with the most digits is the largest
			if (result == 0) {
				result = dec1Length - dec2Length;
			}
		}

		if (dec1.charAt(0) == '-') {
			// reverse result for negative values
			result = -result;
		}

		return result;
	}

	/**
	 * Compares two integers to each other.
	 *
	 * @param int1
	 * @param int2
	 * @return A negative number if <tt>int1</tt> is smaller than <tt>int2</tt>, <tt>0</tt> if they are equal, or
	 *         positive (&gt;0) if <tt>int1</tt> is larger than <tt>int2</tt>.
	 * @throws IllegalArgumentException If one of the supplied strings is not a legal integer.
	 */
	public static int compareIntegers(String int1, String int2) {
		int1 = normalizeInteger(int1);
		int2 = normalizeInteger(int2);

		return compareCanonicalIntegers(int1, int2);
	}

	/**
	 * Compares two canonical integers to each other.
	 *
	 * @param int1
	 * @param int2
	 * @return A negative number if <tt>int1</tt> is smaller than <tt>int2</tt>, <tt>0</tt> if they are equal, or
	 *         positive (&gt;0) if <tt>int1</tt> is larger than <tt>int2</tt>. The result is undefined when one or both
	 *         of the arguments is not a canonical integer.
	 * @throws IllegalArgumentException If one of the supplied strings is not a legal integer.
	 */
	public static int compareCanonicalIntegers(String int1, String int2) {
		if (int1.equals(int2)) {
			return 0;
		}

		// Check signs
		if (int1.charAt(0) == '-' && int2.charAt(0) != '-') {
			// int1 is negative, int2 is not
			return -1;
		}
		if (int2.charAt(0) == '-' && int1.charAt(0) != '-') {
			// int2 is negative, int1 is not
			return 1;
		}

		// The integer with the most digits is the largest
		int result = int1.length() - int2.length();

		if (result == 0) {
			// equal number of digits, compare them
			for (int i = 0; result == 0 && i < int1.length(); i++) {
				result = int1.charAt(i) - int2.charAt(i);
			}
		}

		if (int1.charAt(0) == '-') {
			// reverse result for negative values
			result = -result;
		}

		return result;
	}

	public static int compareNegativeIntegers(String int1, String int2) {
		int1 = normalizeNegativeInteger(int1);
		int2 = normalizeNegativeInteger(int2);

		return compareCanonicalIntegers(int1, int2);
	}

	public static int compareNonPositiveIntegers(String int1, String int2) {
		int1 = normalizeNonPositiveInteger(int1);
		int2 = normalizeNonPositiveInteger(int2);

		return compareCanonicalIntegers(int1, int2);
	}

	public static int compareNonNegativeIntegers(String int1, String int2) {
		int1 = normalizeNonNegativeInteger(int1);
		int2 = normalizeNonNegativeInteger(int2);

		return compareCanonicalIntegers(int1, int2);
	}

	public static int comparePositiveIntegers(String int1, String int2) {
		int1 = normalizePositiveInteger(int1);
		int2 = normalizePositiveInteger(int2);

		return compareCanonicalIntegers(int1, int2);
	}

	public static int compareLongs(String int1, String int2) {
		int1 = normalizeLong(int1);
		int2 = normalizeLong(int2);

		return compareCanonicalIntegers(int1, int2);
	}

	public static int compareInts(String int1, String int2) {
		int1 = normalizeInt(int1);
		int2 = normalizeInt(int2);

		return compareCanonicalIntegers(int1, int2);
	}

	public static int compareShorts(String int1, String int2) {
		int1 = normalizeShort(int1);
		int2 = normalizeShort(int2);

		return compareCanonicalIntegers(int1, int2);
	}

	public static int compareBytes(String int1, String int2) {
		int1 = normalizeByte(int1);
		int2 = normalizeByte(int2);

		return compareCanonicalIntegers(int1, int2);
	}

	public static int compareUnsignedLongs(String int1, String int2) {
		int1 = normalizeUnsignedLong(int1);
		int2 = normalizeUnsignedLong(int2);

		return compareCanonicalIntegers(int1, int2);
	}

	public static int compareUnsignedInts(String int1, String int2) {
		int1 = normalizeUnsignedInt(int1);
		int2 = normalizeUnsignedInt(int2);

		return compareCanonicalIntegers(int1, int2);
	}

	public static int compareUnsignedShorts(String int1, String int2) {
		int1 = normalizeUnsignedShort(int1);
		int2 = normalizeUnsignedShort(int2);

		return compareCanonicalIntegers(int1, int2);
	}

	public static int compareUnsignedBytes(String int1, String int2) {
		int1 = normalizeUnsignedByte(int1);
		int2 = normalizeUnsignedByte(int2);

		return compareCanonicalIntegers(int1, int2);
	}

	/**
	 * Compares two floats to each other.
	 *
	 * @param float1
	 * @param float2
	 * @return A negative number if <tt>float1</tt> is smaller than <tt>float2</tt>, <tt>0</tt> if they are equal, or
	 *         positive (&gt;0) if <tt>float1</tt> is larger than <tt>float2</tt>.
	 * @throws IllegalArgumentException If one of the supplied strings is not a legal float or if <tt>NaN</tt> is
	 *                                  compared to a float other than <tt>NaN</tt>.
	 */
	public static int compareFloats(String float1, String float2) {
		float1 = normalizeFloat(float1);
		float2 = normalizeFloat(float2);

		return compareCanonicalFloats(float1, float2);
	}

	/**
	 * Compares two canonical floats to each other.
	 *
	 * @param float1
	 * @param float2
	 * @return A negative number if <tt>float1</tt> is smaller than <tt>float2</tt>, <tt>0</tt> if they are equal, or
	 *         positive (&gt;0) if <tt>float1</tt> is larger than <tt>float2</tt>. The result is undefined when one or
	 *         both of the arguments is not a canonical float.
	 * @throws IllegalArgumentException If one of the supplied strings is not a legal float or if <tt>NaN</tt> is
	 *                                  compared to a float other than <tt>NaN</tt>.
	 */
	public static int compareCanonicalFloats(String float1, String float2) {
		return compareCanonicalFPNumbers(float1, float2);
	}

	/**
	 * Compares two doubles to each other.
	 *
	 * @param double1
	 * @param double2
	 * @return A negative number if <tt>double1</tt> is smaller than <tt>double2</tt>, <tt>0</tt> if they are equal, or
	 *         positive (&gt;0) if <tt>double1</tt> is larger than <tt>double2</tt>.
	 * @throws IllegalArgumentException If one of the supplied strings is not a legal double or if <tt>NaN</tt> is
	 *                                  compared to a double other than <tt>NaN</tt>.
	 */
	public static int compareDoubles(String double1, String double2) {
		double1 = normalizeDouble(double1);
		double2 = normalizeDouble(double2);

		return compareCanonicalDoubles(double1, double2);
	}

	/**
	 * Compares two canonical doubles to eachother.
	 *
	 * @param double1
	 * @param double2
	 * @return A negative number if <tt>double1</tt> is smaller than <tt>double2</tt>, <tt>0</tt> if they are equal, or
	 *         positive (&gt;0) if <tt>double1</tt> is larger than <tt>double2</tt>. The result is undefined when one or
	 *         both of the arguments is not a canonical double.
	 * @throws IllegalArgumentException If one of the supplied strings is not a legal double or if <tt>NaN</tt> is
	 *                                  compared to a double other than <tt>NaN</tt>.
	 */
	public static int compareCanonicalDoubles(String double1, String double2) {
		return compareCanonicalFPNumbers(double1, double2);
	}

	/**
	 * Compares two floating point numbers to eachother.
	 *
	 * @param fp1
	 * @param fp2
	 * @return A negative number if <tt>float1</tt> is smaller than <tt>float2</tt>, <tt>0</tt> if they are equal, or
	 *         positive (&gt;0) if <tt>float1</tt> is larger than <tt>float2</tt>.
	 * @throws IllegalArgumentException If one of the supplied strings is not a legal floating point number or if
	 *                                  <tt>NaN</tt> is compared to a floating point number other than <tt>NaN</tt>.
	 */
	public static int compareFPNumbers(String fp1, String fp2) {
		fp1 = normalizeFPNumber(fp1);
		fp2 = normalizeFPNumber(fp2);

		return compareCanonicalFPNumbers(fp1, fp2);
	}

	/**
	 * Compares two canonical floating point numbers to each other.
	 *
	 * @param float1
	 * @param float2
	 * @return A negative number if <tt>float1</tt> is smaller than <tt>float2</tt>, <tt>0</tt> if they are equal, or
	 *         positive (&gt;0) if <tt>float1</tt> is larger than <tt>float2</tt>. The result is undefined when one or
	 *         both of the arguments is not a canonical floating point number.
	 * @throws IllegalArgumentException If one of the supplied strings is not a legal floating point number or if
	 *                                  <tt>NaN</tt> is compared to a floating point number other than <tt>NaN</tt>.
	 */
	public static int compareCanonicalFPNumbers(String float1, String float2) {
		// Handle special case NaN
		if (float1.equals(NaN) || float2.equals(NaN)) {
			if (float1.equals(float2)) {
				// NaN is equal to itself
				return 0;
			} else {
				throwIAE("NaN cannot be compared to other floats");
			}
		}

		// Handle special case INF
		if (float1.equals(POSITIVE_INFINITY)) {
			return (float2.equals(POSITIVE_INFINITY)) ? 0 : 1;
		} else if (float2.equals(POSITIVE_INFINITY)) {
			return -1;
		}

		// Handle special case -INF
		if (float1.equals(NEGATIVE_INFINITY)) {
			return (float2.equals(NEGATIVE_INFINITY)) ? 0 : -1;
		} else if (float2.equals(NEGATIVE_INFINITY)) {
			return 1;
		}

		// Check signs
		if (float1.charAt(0) == '-' && float2.charAt(0) != '-') {
			// float1 is negative, float2 is not
			return -1;
		}
		if (float2.charAt(0) == '-' && float1.charAt(0) != '-') {
			// float2 is negative, float1 is not
			return 1;
		}

		int eIdx1 = float1.indexOf('E');
		String mantissa1 = float1.substring(0, eIdx1);
		String exponent1 = float1.substring(eIdx1 + 1);

		int eIdx2 = float2.indexOf('E');
		String mantissa2 = float2.substring(0, eIdx2);
		String exponent2 = float2.substring(eIdx2 + 1);

		// Compare exponents
		int result = compareCanonicalIntegers(exponent1, exponent2);

		if (result != 0 && float1.charAt(0) == '-') {
			// reverse result for negative values
			result = -result;
		}

		if (result == 0) {
			// Equal exponents, compare mantissas
			result = compareCanonicalDecimals(mantissa1, mantissa2);
		}

		return result;
	}

	/**
	 * Compares two dateTime objects. <b>Important:</b> The comparison only works if both values have, or both values
	 * don't have specified a valid value for the timezone.
	 *
	 * @param value1 An xsd:dateTime value.
	 * @param value2 An xsd:dateTime value.
	 * @return <tt>-1</tt> if <tt>value1</tt> is before <tt>value2</tt> (i.e. if the dateTime object represented by
	 *         value1 is before the dateTime object represented by value2), <tt>0</tt> if both are equal and <tt>1</tt>
	 *         if <tt>value2</tt> is before <tt>value1</tt><br>
	 *         .
	 */
	public static int compareDateTime(String value1, String value2) {
		XMLDateTime dateTime1 = new XMLDateTime(value1);
		XMLDateTime dateTime2 = new XMLDateTime(value2);

		dateTime1.normalize();
		dateTime2.normalize();

		return dateTime1.compareTo(dateTime2);
	}

	/*---------------*
	 * Value parsing *
	 *---------------*/

	/**
	 * Parses the supplied xsd:boolean string and returns its value.
	 *
	 * @param s A string representation of an xsd:boolean value.
	 * @return The <tt>boolean</tt> value represented by the supplied string argument.
	 * @throws NumberFormatException If the supplied string is not a valid xsd:boolean value.
	 */
	public static boolean parseBoolean(String s) {
		return normalizeBoolean(s).equals("true");
	}

	/**
	 * Parses the supplied xsd:byte string and returns its value.
	 *
	 * @param s A string representation of an xsd:byte value.
	 * @return The <tt>byte</tt> value represented by the supplied string argument.
	 * @throws NumberFormatException If the supplied string is not a valid xsd:byte value.
	 */
	public static byte parseByte(String s) {
		s = trimPlusSign(s);
		return Byte.parseByte(s);
	}

	/**
	 * Parses the supplied xsd:short string and returns its value.
	 *
	 * @param s A string representation of an xsd:short value.
	 * @return The <tt>short</tt> value represented by the supplied string argument.
	 * @throws NumberFormatException If the supplied string is not a valid xsd:short value.
	 */
	public static short parseShort(String s) {
		s = trimPlusSign(s);
		return Short.parseShort(s);
	}

	/**
	 * Parses the supplied xsd:int strings and returns its value.
	 *
	 * @param s A string representation of an xsd:int value.
	 * @return The <tt>int</tt> value represented by the supplied string argument.
	 * @throws NumberFormatException If the supplied string is not a valid xsd:int value.
	 */
	public static int parseInt(String s) {
		s = trimPlusSign(s);
		return Integer.parseInt(s);
	}

	/**
	 * Parses the supplied xsd:long string and returns its value.
	 *
	 * @param s A string representation of an xsd:long value.
	 * @return The <tt>long</tt> value represented by the supplied string argument.
	 * @throws NumberFormatException If the supplied string is not a valid xsd:long value.
	 */
	public static long parseLong(String s) {
		s = trimPlusSign(s);
		return Long.parseLong(s);
	}

	/**
	 * Parses the supplied xsd:float string and returns its value.
	 *
	 * @param s A string representation of an xsd:float value.
	 * @return The <tt>float</tt> value represented by the supplied string argument.
	 * @throws NumberFormatException If the supplied string is not a valid xsd:float value.
	 */
	public static float parseFloat(String s) {
		if (POSITIVE_INFINITY.equals(s)) {
			return Float.POSITIVE_INFINITY;
		} else if (NEGATIVE_INFINITY.equals(s)) {
			return Float.NEGATIVE_INFINITY;
		} else if (NaN.equals(s)) {
			return Float.NaN;
		} else {
			s = trimPlusSign(s);
			return Float.parseFloat(s);
		}
	}

	/**
	 * Parses the supplied xsd:double string and returns its value.
	 *
	 * @param s A string representation of an xsd:double value.
	 * @return The <tt>double</tt> value represented by the supplied string argument.
	 * @throws NumberFormatException If the supplied string is not a valid xsd:double value.
	 */
	public static double parseDouble(String s) {
		if (POSITIVE_INFINITY.equals(s)) {
			return Double.POSITIVE_INFINITY;
		} else if (NEGATIVE_INFINITY.equals(s)) {
			return Double.NEGATIVE_INFINITY;
		} else if (NaN.equals(s)) {
			return Double.NaN;
		} else {
			s = trimPlusSign(s);
			return Double.parseDouble(s);
		}
	}

	/**
	 * Parses the supplied xsd:integer string and returns its value.
	 *
	 * @param s A string representation of an xsd:integer value.
	 * @return The integer value represented by the supplied string argument.
	 * @throws NumberFormatException If the supplied string is not a valid xsd:integer value.
	 */
	public static BigInteger parseInteger(String s) {
		s = trimPlusSign(s);
		return new BigInteger(s);
	}

	/**
	 * Parses the supplied decimal/floating point string and returns its value.
	 *
	 * @param s A string representation of an xsd:decimal or xsd:double value.
	 * @return The decimal/floating point value represented by the supplied string argument.
	 * @throws NumberFormatException If the supplied string is not a valid xsd:decimal or xsd:double value.
	 */
	public static BigDecimal parseDecimal(String s) {
		// Note: BigDecimal can handle leading plus signs itself
		return new BigDecimal(s);
	}

	/**
	 * Parses the supplied calendar value string and returns its value.
	 *
	 * @param s A string representation of an xsd:dateTime, xsd:time, xsd:date, xsd:gYearMonth, xsd:gMonthDay,
	 *          xsd:gYear, xsd:gMonth or xsd:gDay value.
	 * @return The calendar value represented by the supplied string argument.
	 * @throws IllegalArgumentException If the supplied string is not a valid calendar value.
	 */
	public static XMLGregorianCalendar parseCalendar(String s) {
		return dtFactory.newXMLGregorianCalendar(s);
	}

	/**
	 * Parses the supplied xsd:duration value string and returns its value.
	 *
	 * @param s A string representation of an xsd:duration value.
	 * @return The {@link Duration} value represented by the supplied string argument.
	 * @throws IllegalArgumentException      If the supplied string is not a valid xsd:duration value.
	 * @throws UnsupportedOperationException If implementation cannot support requested values. The XML Schema
	 *                                       specification states that values can be of an arbitrary size.
	 *                                       Implementations may chose not to or be incapable of supporting arbitrarily
	 *                                       large and/or small values. An UnsupportedOperationException will be thrown
	 *                                       with a message indicating implementation limits if implementation
	 *                                       capacities are exceeded.
	 */
	public static Duration parseDuration(String s) {
		return dtFactory.newDuration(s);
	}

	/**
	 * Removes the first character from the supplied string if this is a plus sign ('+'). Number strings with leading
	 * plus signs cannot be parsed by methods such as {@link Integer#parseInt(String)}.
	 */
	private static String trimPlusSign(String s) {
		if (s.length() > 0 && s.charAt(0) == '+') {
			return s.substring(1);
		} else {
			return s;
		}
	}

	/**
	 * Maps a datatype QName from the javax.xml.namespace package to an XML Schema 1.0 URI for the corresponding
	 * datatype. This method recognizes the XML Schema qname mentioned in {@link DatatypeConstants}.
	 *
	 * Note that Java 8 / 11 do not have constants for XML Schema 1.1 datatypes like xsd:dateTimeStamp.
	 *
	 * @param qname One of the XML Schema qnames from {@link DatatypeConstants}.
	 * @return A URI for the specified datatype.
	 * @throws IllegalArgumentException If the supplied qname was not recognized by this method.
	 * @see DatatypeConstants
	 */
	public static IRI qnameToURI(QName qname) {
		if (DatatypeConstants.DATETIME.equals(qname)) {
			return XSD.DATETIME;
		} else if (DatatypeConstants.DATE.equals(qname)) {
			return XSD.DATE;
		} else if (DatatypeConstants.TIME.equals(qname)) {
			return XSD.TIME;
		} else if (DatatypeConstants.GYEARMONTH.equals(qname)) {
			return XSD.GYEARMONTH;
		} else if (DatatypeConstants.GMONTHDAY.equals(qname)) {
			return XSD.GMONTHDAY;
		} else if (DatatypeConstants.GYEAR.equals(qname)) {
			return XSD.GYEAR;
		} else if (DatatypeConstants.GMONTH.equals(qname)) {
			return XSD.GMONTH;
		} else if (DatatypeConstants.GDAY.equals(qname)) {
			return XSD.GDAY;
		} else if (DatatypeConstants.DURATION.equals(qname)) {
			return XSD.DURATION;
		} else if (DatatypeConstants.DURATION_DAYTIME.equals(qname)) {
			return XSD.DAYTIMEDURATION;
		} else if (DatatypeConstants.DURATION_YEARMONTH.equals(qname)) {
			return XSD.YEARMONTHDURATION;
		} else {
			throw new IllegalArgumentException("QName cannot be mapped to an XML Schema URI: " + qname.toString());
		}
	}

	public static String toString(Number value) {
		double d = value.doubleValue();
		if (Double.POSITIVE_INFINITY == d) {
			return XMLDatatypeUtil.POSITIVE_INFINITY;
		} else if (Double.NEGATIVE_INFINITY == d) {
			return XMLDatatypeUtil.NEGATIVE_INFINITY;
		} else if (Double.isNaN(d)) {
			return XMLDatatypeUtil.NaN;
		} else {
			return value.toString();
		}
	}

	/*-----------------*
	 * Utility methods *
	 *-----------------*/

	/**
	 * Checks whether the supplied character is a digit.
	 */
	private static final boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	/**
	 * Throws an IllegalArgumentException that contains the supplied message.
	 */
	private static final void throwIAE(String msg) {
		throw new IllegalArgumentException(msg);
	}
}
