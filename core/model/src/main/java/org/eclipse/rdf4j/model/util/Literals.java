/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Optional;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.SimpleLiteral;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * Various utility methods related to {@link Literal}.
 *
 * @author Arjohn Kampman
 * @author Peter Ansell
 *
 * @See {@link Values}
 */
public class Literals {

	/**
	 * Gets the label of the supplied literal. The fallback value is returned in case the supplied literal is
	 * <var>null</var>.
	 *
	 * @param l        The literal to get the label for.
	 * @param fallback The value to fall back to in case the supplied literal is <var>null</var>.
	 * @return Either the literal's label, or the fallback value.
	 */
	public static String getLabel(Literal l, String fallback) {
		return l != null ? l.getLabel() : fallback;
	}

	/**
	 * Returns the result of {@link #getLabel(Literal, String) getLabel((Literal)v, fallback} in case the supplied value
	 * is a literal, returns the fallback value otherwise.
	 */
	public static String getLabel(Value v, String fallback) {
		return v instanceof Literal ? getLabel((Literal) v, fallback) : fallback;
	}

	public static String getLabel(Optional<Value> v, String fallback) {
		return v != null ? getLabel(v.orElseGet(null), fallback) : fallback;
	}

	/**
	 * Retrieves the {@link org.eclipse.rdf4j.model.vocabulary.XSD.Datatype} value for the supplied Literal, if it has
	 * one.
	 *
	 * @param l a Literal
	 * @return an Optional {@link org.eclipse.rdf4j.model.vocabulary.XSD.Datatype} enum, if one is available. Note that
	 *         the absence of this enum does <i>not</i> indicate that the literal has no datatype, merely that it has no
	 *         cached enum representation of that datatype.
	 * @since 3.5.0
	 * @deprecated Use {@link Literal#getCoreDatatype()} instead.
	 */
	@Deprecated(since = "4.0.0", forRemoval = true)
	public static Optional<XSD.Datatype> getXsdDatatype(Literal l) {
		if (l instanceof SimpleLiteral) {
			return ((SimpleLiteral) l).getXsdDatatype();
		}
		return Optional.empty();
	}

	/**
	 * Gets the byte value of the supplied literal. The fallback value is returned in case {@link Literal#byteValue()}
	 * throws a {@link NumberFormatException}.
	 *
	 * @param l        The literal to get the byte value for.
	 * @param fallback The value to fall back to in case no byte value could gotten from the literal.
	 * @return Either the literal's byte value, or the fallback value.
	 */
	public static byte getByteValue(Literal l, byte fallback) {
		try {
			return l.byteValue();
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	/**
	 * Returns the result of {@link #getByteValue(Literal, byte) getByteValue((Literal)value, fallback)} in case the
	 * supplied value is a literal, returns the fallback value otherwise.
	 */
	public static byte getByteValue(Value v, byte fallback) {
		if (v instanceof Literal) {
			return getByteValue((Literal) v, fallback);
		} else {
			return fallback;
		}
	}

	/**
	 * Gets the short value of the supplied literal. The fallback value is returned in case {@link Literal#shortValue()}
	 * throws a {@link NumberFormatException}.
	 *
	 * @param l        The literal to get the short value for.
	 * @param fallback The value to fall back to in case no short value could gotten from the literal.
	 * @return Either the literal's short value, or the fallback value.
	 */
	public static short getShortValue(Literal l, short fallback) {
		try {
			return l.shortValue();
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	/**
	 * Returns the result of {@link #getShortValue(Literal, short) getShortValue((Literal)value, fallback)} in case the
	 * supplied value is a literal, returns the fallback value otherwise.
	 */
	public static short getShortValue(Value v, short fallback) {
		if (v instanceof Literal) {
			return getShortValue((Literal) v, fallback);
		} else {
			return fallback;
		}
	}

	/**
	 * Gets the int value of the supplied literal. The fallback value is returned in case {@link Literal#intValue()}
	 * throws a {@link NumberFormatException}.
	 *
	 * @param l        The literal to get the int value for.
	 * @param fallback The value to fall back to in case no int value could gotten from the literal.
	 * @return Either the literal's int value, or the fallback value.
	 */
	public static int getIntValue(Literal l, int fallback) {
		try {
			return l.intValue();
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	/**
	 * Returns the result of {@link #getIntValue(Literal, int) getIntValue((Literal)value, fallback)} in case the
	 * supplied value is a literal, returns the fallback value otherwise.
	 */
	public static int getIntValue(Value v, int fallback) {
		if (v instanceof Literal) {
			return getIntValue((Literal) v, fallback);
		} else {
			return fallback;
		}
	}

	/**
	 * Gets the long value of the supplied literal. The fallback value is returned in case {@link Literal#longValue()}
	 * throws a {@link NumberFormatException}.
	 *
	 * @param l        The literal to get the long value for.
	 * @param fallback The value to fall back to in case no long value could gotten from the literal.
	 * @return Either the literal's long value, or the fallback value.
	 */
	public static long getLongValue(Literal l, long fallback) {
		try {
			return l.longValue();
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	/**
	 * Returns the result of {@link #getLongValue(Literal, long) getLongValue((Literal)value, fallback)} in case the
	 * supplied value is a literal, returns the fallback value otherwise.
	 */
	public static long getLongValue(Value v, long fallback) {
		if (v instanceof Literal) {
			return getLongValue((Literal) v, fallback);
		} else {
			return fallback;
		}
	}

	/**
	 * Gets the integer value of the supplied literal. The fallback value is returned in case
	 * {@link Literal#integerValue()} throws a {@link NumberFormatException}.
	 *
	 * @param l        The literal to get the integer value for.
	 * @param fallback The value to fall back to in case no integer value could gotten from the literal.
	 * @return Either the literal's integer value, or the fallback value.
	 */
	public static BigInteger getIntegerValue(Literal l, BigInteger fallback) {
		try {
			return l.integerValue();
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	/**
	 * Returns the result of {@link #getIntegerValue(Literal, BigInteger) getIntegerValue((Literal)value, fallback)} in
	 * case the supplied value is a literal, returns the fallback value otherwise.
	 */
	public static BigInteger getIntegerValue(Value v, BigInteger fallback) {
		if (v instanceof Literal) {
			return getIntegerValue((Literal) v, fallback);
		} else {
			return fallback;
		}
	}

	/**
	 * Gets the decimal value of the supplied literal. The fallback value is returned in case
	 * {@link Literal#decimalValue()} throws a {@link NumberFormatException}.
	 *
	 * @param l        The literal to get the decimal value for.
	 * @param fallback The value to fall back to in case no decimal value could gotten from the literal.
	 * @return Either the literal's decimal value, or the fallback value.
	 */
	public static BigDecimal getDecimalValue(Literal l, BigDecimal fallback) {
		try {
			return l.decimalValue();
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	/**
	 * Returns the result of {@link #getDecimalValue(Literal, BigDecimal) getDecimalValue((Literal)value, fallback)} in
	 * case the supplied value is a literal, returns the fallback value otherwise.
	 */
	public static BigDecimal getDecimalValue(Value v, BigDecimal fallback) {
		if (v instanceof Literal) {
			return getDecimalValue((Literal) v, fallback);
		} else {
			return fallback;
		}
	}

	/**
	 * Gets the float value of the supplied literal. The fallback value is returned in case {@link Literal#floatValue()}
	 * throws a {@link NumberFormatException}.
	 *
	 * @param l        The literal to get the float value for.
	 * @param fallback The value to fall back to in case no float value could gotten from the literal.
	 * @return Either the literal's float value, or the fallback value.
	 */
	public static float getFloatValue(Literal l, float fallback) {
		try {
			return l.floatValue();
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	/**
	 * Returns the result of {@link #getFloatValue(Literal, float) getFloatValue((Literal)value, fallback)} in case the
	 * supplied value is a literal, returns the fallback value otherwise.
	 */
	public static float getFloatValue(Value v, float fallback) {
		if (v instanceof Literal) {
			return getFloatValue((Literal) v, fallback);
		} else {
			return fallback;
		}
	}

	/**
	 * Gets the double value of the supplied literal. The fallback value is returned in case
	 * {@link Literal#doubleValue()} throws a {@link NumberFormatException}.
	 *
	 * @param l        The literal to get the double value for.
	 * @param fallback The value to fall back to in case no double value could gotten from the literal.
	 * @return Either the literal's double value, or the fallback value.
	 */
	public static double getDoubleValue(Literal l, double fallback) {
		try {
			return l.doubleValue();
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	/**
	 * Returns the result of {@link #getDoubleValue(Literal, double) getDoubleValue((Literal)value, fallback)} in case
	 * the supplied value is a literal, returns the fallback value otherwise.
	 */
	public static double getDoubleValue(Value v, double fallback) {
		if (v instanceof Literal) {
			return getDoubleValue((Literal) v, fallback);
		} else {
			return fallback;
		}
	}

	/**
	 * Gets the boolean value of the supplied literal. The fallback value is returned in case
	 * {@link Literal#booleanValue()} throws a {@link NumberFormatException}.
	 *
	 * @param l        The literal to get the boolean value for.
	 * @param fallback The value to fall back to in case no boolean value could gotten from the literal.
	 * @return Either the literal's boolean value, or the fallback value.
	 */
	public static boolean getBooleanValue(Literal l, boolean fallback) {
		try {
			return l.booleanValue();
		} catch (IllegalArgumentException e) {
			return fallback;
		}
	}

	/**
	 * Returns the result of {@link #getBooleanValue(Literal, boolean) getBooleanValue((Literal)value, fallback)} in
	 * case the supplied value is a literal, returns the fallback value otherwise.
	 */
	public static boolean getBooleanValue(Value v, boolean fallback) {
		if (v instanceof Literal) {
			return getBooleanValue((Literal) v, fallback);
		} else {
			return fallback;
		}
	}

	/**
	 * Gets the calendar value of the supplied literal. The fallback value is returned in case
	 * {@link Literal#calendarValue()} throws a {@link NumberFormatException}.
	 *
	 * @param l        The literal to get the calendar value for.
	 * @param fallback The value to fall back to in case no calendar value could gotten from the literal.
	 * @return Either the literal's calendar value, or the fallback value.
	 */
	public static XMLGregorianCalendar getCalendarValue(Literal l, XMLGregorianCalendar fallback) {
		try {
			return l.calendarValue();
		} catch (IllegalArgumentException e) {
			return fallback;
		}
	}

	/**
	 * Gets the {@link Duration} value of the supplied literal. The fallback value is returned in case
	 * {@link XMLDatatypeUtil#parseDuration(String)} throws an exception.
	 *
	 * @param l        The literal to get the {@link Duration} value for.
	 * @param fallback The value to fall back to in case no Duration value could gotten from the literal.
	 * @return Either the literal's Duration value, or the fallback value.
	 */
	public static Duration getDurationValue(Literal l, Duration fallback) {
		try {
			return XMLDatatypeUtil.parseDuration(l.getLabel());
		} catch (IllegalArgumentException | UnsupportedOperationException e) {
			return fallback;
		}
	}

	/**
	 * Returns the result of {@link #getCalendarValue(Literal, XMLGregorianCalendar) getCalendarValue((Literal)value,
	 * fallback)} in case the supplied value is a literal, returns the fallback value otherwise.
	 */
	public static XMLGregorianCalendar getCalendarValue(Value v, XMLGregorianCalendar fallback) {
		if (v instanceof Literal) {
			return getCalendarValue((Literal) v, fallback);
		} else {
			return fallback;
		}
	}

	/**
	 * Creates a typed {@link Literal} out of the supplied object, mapping the runtime type of the object to the
	 * appropriate XML Schema type. If no mapping is available, the method returns a literal with the string
	 * representation of the supplied object as the value, and {@link XSD#STRING} as the datatype. Recognized types are
	 * {@link Boolean}, {@link Byte}, {@link Double}, {@link Float}, {@link Integer}, {@link Long}, {@link Short},
	 * {@link XMLGregorianCalendar } , and {@link Date}.
	 *
	 * @param valueFactory
	 * @param object       an object to be converted to a typed literal.
	 * @return a typed literal representation of the supplied object.
	 * @throws NullPointerException If the object was null.
	 * @deprecated since 3.5.0 - use {@link Values#literal(Object)} instead.
	 */
	@Deprecated
	public static Literal createLiteral(ValueFactory valueFactory, Object object) {
		try {
			return createLiteral(valueFactory, object, false);
		} catch (LiteralUtilException e) {
			// This should not happen by design
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Creates a typed {@link Literal} out of the supplied object, mapping the runtime type of the object to the
	 * appropriate XML Schema type. If no mapping is available, the method throws a {@link LiteralUtilException}.
	 * Recognized types are {@link Boolean}, {@link Byte}, {@link Double}, {@link Float}, {@link Integer}, {@link Long},
	 * {@link Short}, {@link XMLGregorianCalendar } , and {@link Date}.
	 *
	 * @param valueFactory
	 * @param object       an object to be converted to a typed literal.
	 * @return a typed literal representation of the supplied object.
	 * @throws LiteralUtilException If the literal could not be created.
	 * @throws NullPointerException If the object was null.
	 * @deprecated since 3.5.0 - use {@link Values#literal(Object, boolean)} instead.
	 */
	@Deprecated
	public static Literal createLiteralOrFail(ValueFactory valueFactory, Object object) throws LiteralUtilException {
		return createLiteral(valueFactory, object, true);
	}

	/**
	 * Creates a typed {@link Literal} out of the supplied object, mapping the runtime type of the object to the
	 * appropriate XML Schema type. If no mapping is available, the method throws an exception if the boolean parameter
	 * is true, or if it is false it returns a literal with the string representation of the supplied object as the
	 * value, and {@link XSD#STRING} as the datatype. Recognized types are {@link Boolean}, {@link Byte},
	 * {@link Double}, {@link Float}, {@link Integer}, {@link Long}, {@link Short}, {@link XMLGregorianCalendar } , and
	 * {@link Date}.
	 *
	 * @param valueFactory            The {@link ValueFactory} to use when creating the result.
	 * @param object                  an object to be converted to a typed literal.
	 * @param throwExceptionOnFailure If true throws a {@link LiteralUtilException} when the object is not recognised.
	 *                                If false it returns a string typed literal based on the objects toString method.
	 * @return a typed literal representation of the supplied object.
	 * @throws LiteralUtilException If the literal could not be created.
	 * @throws NullPointerException If the object was null.
	 */
	private static Literal createLiteral(ValueFactory valueFactory, Object object, boolean throwExceptionOnFailure)
			throws LiteralUtilException {
		if (object == null) {
			throw new NullPointerException("Cannot create a literal from a null");
		}

		if (object instanceof Boolean) {
			return valueFactory.createLiteral((Boolean) object);
		} else if (object instanceof Byte) {
			return valueFactory.createLiteral((Byte) object);
		} else if (object instanceof Double) {
			return valueFactory.createLiteral((Double) object);
		} else if (object instanceof Float) {
			return valueFactory.createLiteral((Float) object);
		} else if (object instanceof Integer) {
			return valueFactory.createLiteral((Integer) object);
		} else if (object instanceof Long) {
			return valueFactory.createLiteral((Long) object);
		} else if (object instanceof Short) {
			return valueFactory.createLiteral((Short) object);
		} else if (object instanceof XMLGregorianCalendar) {
			return valueFactory.createLiteral((XMLGregorianCalendar) object);
		} else if (object instanceof Date) {
			return valueFactory.createLiteral((Date) object);
		} else if (object instanceof String) {
			return valueFactory.createLiteral(object.toString(), CoreDatatype.XSD.STRING);
		} else {
			if (throwExceptionOnFailure) {
				throw new LiteralUtilException("Did not recognise object when creating literal");
			}
			return valueFactory.createLiteral(object.toString(), CoreDatatype.XSD.STRING);
		}
	}

	/**
	 * Helper method for determining whether a literal could be created from an object using a {@link ValueFactory}.
	 *
	 * @param object an object to check for the possibility of being converted to a typed literal.
	 * @return True if a literal could be created from the given object, based solely on its type and the methods
	 *         available on the {@link ValueFactory} interface and false otherwise. Returns false if the object is null.
	 * @deprecated since 3.5.0
	 */
	@Deprecated
	public static boolean canCreateLiteral(Object object) {
		if (object == null) {
			// Cannot create a literal from a null
			// Avoid throwing a NullPointerException here to enable universal
			// usage
			// of this method
			return false;
		}

		return object instanceof Boolean || object instanceof Byte || object instanceof Double
				|| object instanceof Float
				|| object instanceof Integer || object instanceof Long || object instanceof Short
				|| object instanceof XMLGregorianCalendar || object instanceof Date || object instanceof String;
	}

	/**
	 * Helper method to determine whether a literal is a language literal, and not a typed literal.
	 *
	 * @param literal The literal to check
	 * @return True if the literal has a language tag attached to it and false otherwise.
	 */
	public static boolean isLanguageLiteral(Literal literal) {
		return literal.getCoreDatatype() == CoreDatatype.RDF.LANGSTRING;
	}

	/**
	 * Normalizes the given <a href="https://tools.ietf.org/html/bcp47">BCP47</a> language tag according to the rules
	 * defined in <a href="https://www.rfc-editor.org/rfc/rfc5646.html#section-2.1.1">RFC 5646, section 2.1.1</a>:
	 * <p>
	 * <blockquote> All subtags, including extension and private use subtags, use lowercase letters with two exceptions:
	 * two-letter and four-letter subtags that neither appear at the start of the tag nor occur after singletons. Such
	 * two-letter subtags are all uppercase (as in the tags "en-CA-x-ca" or "sgn-BE-FR") and four- letter subtags are
	 * titlecase (as in the tag "az-Latn-x-latn"). </blockquote>
	 *
	 * @param languageTag An unnormalized, valid, language tag
	 * @return A normalized version of the given language tag
	 * @throws IllformedLocaleException If the given language tag is ill-formed according to the rules specified in
	 *                                  BCP47 (RFC 5646).
	 */
	public static String normalizeLanguageTag(String languageTag) throws IllformedLocaleException {
		// check if language tag is well-formed
		new Locale.Builder().setLanguageTag(languageTag);

		// all subtags are case-insensitive
		String normalizedTag = languageTag.toLowerCase();

		String[] subtags = normalizedTag.split("-");
		for (int i = 1; i < subtags.length; i++) {
			String subtag = subtags[i];

			if (subtag.length() == 2) {
				// exception 1: two-letter subtags not at the starte and not preceded by a singleton are upper case
				if (subtags[i - 1].length() > 1 && subtag.matches("\\w\\w")) {
					subtags[i] = subtag.toUpperCase();
				}
			} else if (subtag.length() == 4) {
				// exception 2: four-letter subtags not at the start and not preceded by a singleton are title case
				if (subtags[i - 1].length() > 1 && subtag.matches("\\w\\w\\w\\w")) {
					subtags[i] = subtag.substring(0, 1).toUpperCase() + subtag.substring(1);
				}
			}
		}

		return String.join("-", subtags);
	}

	/**
	 * Checks if the given string is a well-formed <a href="https://tools.ietf.org/html/bcp47">BCP47</a> language tag
	 * according to the rules defined in <a href="https://www.rfc-editor.org/rfc/rfc5646.html#section-2.1.1">RFC 5646,
	 * section 2.1.1</a>.
	 *
	 * @param languageTag A language tag
	 * @return <code>true</code> if the given language tag is well-formed according to the rules specified in BCP47.
	 */
	public static boolean isValidLanguageTag(String languageTag) {
		try {
			new Locale.Builder().setLanguageTag(languageTag);
			return true;
		} catch (IllformedLocaleException e) {
			return false;
		}
	}

	/**
	 * Implements language range filtering for SPARQL langMatches
	 * (https://www.w3.org/TR/sparql11-query/#func-langMatches).
	 *
	 * @param langTag   the tag to filter
	 * @param langRange the range to filter against
	 * @return true if langTag matches langRange
	 */
	public static boolean langMatches(String langTag, String langRange) {
		boolean result = false;
		if (langRange.equals("*")) {
			result = langTag.length() > 0;
		} else if (langTag.length() == langRange.length()) {
			result = langTag.equalsIgnoreCase(langRange);
		} else if (langTag.length() > langRange.length()) {
			// check if the range is a prefix of the tag
			String prefix = langTag.substring(0, langRange.length());
			result = prefix.equalsIgnoreCase(langRange) && langTag.charAt(langRange.length()) == '-';
		}
		return result;
	}

	protected Literals() {
		// Protected default constructor to prevent instantiation
	}
}
