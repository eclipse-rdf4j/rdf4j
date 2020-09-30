/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/
package org.eclipse.rdf4j.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Consumer;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.Test;

/**
 * Abstract {@link Literal} test suite.
 */
public abstract class LiteralTest {

	private static final String XSD="http://www.w3.org/2001/XMLSchema#";
	private static final String RDF="http://www.w3.org/1999/02/22-rdf-syntax-ns#";

	private static final String XSD_BOOLEAN=XSD+"boolean";

	private static final String XSD_BYTE=XSD+"byte";
	private static final String XSD_SHORT=XSD+"short";
	private static final String XSD_INT=XSD+"int";
	private static final String XSD_LONG=XSD+"long";
	private static final String XSD_FLOAT=XSD+"float";
	private static final String XSD_DOUBLE=XSD+"double";
	private static final String XSD_INTEGER=XSD+"integer";
	private static final String XSD_DECIMAL=XSD+"decimal";

	private static final String XSD_STRING=XSD+"string";

	private static final String XSD_DATE_TIME=XSD+"dateTime";
	private static final String XSD_TIME=XSD+"time";
	private static final String XSD_DATE=XSD+"date";
	private static final String XSD_G_YEAR_MONTH=XSD+"gYearMonth";
	private static final String XSD_G_MONTH_DAY=XSD+"gMonthDay";
	private static final String XSD_G_YEAR=XSD+"gYear";
	private static final String XSD_G_MONTH=XSD+"gMonth";
	private static final String XSD_G_DAY=XSD+"gDay";

	private static final String RDF_LANG_STRING=RDF+"langString";

	/**
	 * Creates a test literal instance.
	 *
	 * @param label the label of the literal
	 *
	 * @return a new instance of the concrete literal class under test
	 */
	protected abstract Literal literal(final String label);

	/**
	 * Creates a test literal instance.
	 *
	 * @param label    the label of the literal
	 * @param language the language of the literal
	 *
	 * @return a new instance of the concrete literal class under test
	 */
	protected abstract Literal literal(final String label, final String language);

	/**
	 * Creates a test literal instance.
	 *
	 * @param label    the label of the literal
	 * @param datatype the datatype of the literal
	 *
	 * @return a new instance of the concrete literal class under test
	 */
	protected abstract Literal literal(final String label, final IRI datatype);

	/**
	 * Creates a test datatype IRI instance.
	 *
	 * @param iri the IRI of the datatype
	 *
	 * @return a new instance of the concrete datatype class under test
	 */
	protected abstract IRI datatype(final String iri);

	//// Constructors //////////////////////////////////////////////////////////////////////////////////////////////////

	@Test
	public final void testPlainConstructor() {

		final String label="label";

		final Literal literal=literal(label);

		assertThat(literal.getLabel()).isEqualTo(label);
		assertThat(literal.getLanguage()).isNotPresent();
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_STRING);

		assertThatNullPointerException().isThrownBy(() -> literal(null));
	}

	@Test
	public final void testPlainConstructorWithLongLabel() {

		final StringBuilder label=new StringBuilder(1000000);

		for ( int i=0; i < 1000000; i++ ) {
			label.append(Integer.toHexString(i%16));
		}

		final Literal literal=literal(label.toString());

		assertThat(literal.getLabel()).isEqualTo(label.toString());
		assertThat(literal.getLanguage()).isNotPresent();
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_STRING);

	}

	@Test
	public final void testTaggedConstructor() {

		final String label="label";
		final String language="en";

		final Literal literal=literal(label, language);

		assertThat(literal.getLabel()).isEqualTo(label);
		assertThat(literal.getLanguage()).contains(language);
		assertThat(literal.getDatatype().stringValue()).isEqualTo(RDF_LANG_STRING);

		assertThatNullPointerException().isThrownBy(() -> literal(null, (String)null));
		assertThatNullPointerException().isThrownBy(() -> literal("", (String)null));
		assertThatNullPointerException().isThrownBy(() -> literal(null, ""));
		assertThatNullPointerException().isThrownBy(() -> literal(null, (IRI)null));

		assertThatIllegalArgumentException().isThrownBy(() -> literal("", ""));

	}

	@Test
	public final void testTypedConstructor() {

		final String label="label";
		final String datatype="http://examplle.org/datatype";

		final Literal literal=literal(label, datatype(datatype));

		assertThat(literal.getLabel()).isEqualTo(label);
		assertThat(literal.getLanguage()).isNotPresent();
		assertThat(literal.getDatatype().stringValue()).isEqualTo(datatype);

		assertThatNullPointerException().isThrownBy(() -> literal(null, (IRI)null));
		assertThatNullPointerException().isThrownBy(() -> literal(null, datatype(XSD_STRING)));
		assertThatNullPointerException().isThrownBy(() -> literal(null, datatype(RDF_LANG_STRING)));

		assertThatIllegalArgumentException().isThrownBy(() -> literal("", datatype(RDF_LANG_STRING)));

	}

	@Test
	public final void testTypedConstructorNullDatatype() {

		final String label="label";
		final IRI datatype=null;

		final Literal literal=literal(label, datatype);

		assertThat(literal.getLabel()).isEqualTo(label);
		assertThat(literal.getLanguage()).isNotPresent();
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_STRING);

	}

	//// String Value //////////////////////////////////////////////////////////////////////////////////////////////////

	@Test
	public void testStringValue() {

		final String label="literal";
		final String language="en";
		final IRI datatype=datatype(XSD_DECIMAL);

		assertThat(literal(label).stringValue()).isEqualTo(label);
		assertThat(literal(label, language).stringValue()).isEqualTo(label);
		assertThat(literal(label, datatype).stringValue()).isEqualTo(label);
	}

	//// Object Values /////////////////////////////////////////////////////////////////////////////////////////////////

	@Test
	public void testBooleanValue() {

		final IRI datatype=datatype(XSD_BOOLEAN);

		assertThat(literal("true", datatype).booleanValue()).isTrue();
		assertThat(literal("false", datatype).booleanValue()).isFalse();

		assertThat(literal("1", datatype).booleanValue()).isTrue();
		assertThat(literal("0", datatype).booleanValue()).isFalse();

		assertThat(literal("\ttrue", datatype).booleanValue()).isTrue();
		assertThat(literal("false\t", datatype).booleanValue()).isFalse();

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).booleanValue());

		// assertThatIllegalArgumentException().as("mis-typed").isThrownBy(() ->
		// literal("true", iri(XSD_STRING)).booleanValue()
		// );

	}

	@Test
	public final void testByteValue() {

		final IRI datatype=datatype(XSD_BYTE);
		final Class<Byte> type=Byte.class;

		assertThat(literal("100", datatype).byteValue()).isInstanceOf(type).isEqualTo((byte)100);
		assertThat(literal("+100", datatype).byteValue()).isInstanceOf(type).isEqualTo((byte)100);
		assertThat(literal("-100", datatype).byteValue()).isInstanceOf(type).isEqualTo((byte)-100);

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).byteValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).booleanValue());

		// assertThatIllegalArgumentException().as("mis-typed").isThrownBy(() ->
		// literal("100", iri(XSD_STRING)).byteValue()
		// );

	}

	@Test
	public final void testShortValue() {

		final IRI datatype=datatype(XSD_SHORT);
		final Class<Short> type=Short.class;

		assertThat(literal("100", datatype).shortValue()).isInstanceOf(type).isEqualTo((short)100);
		assertThat(literal("+100", datatype).shortValue()).isInstanceOf(type).isEqualTo((short)100);
		assertThat(literal("-100", datatype).shortValue()).isInstanceOf(type).isEqualTo((short)-100);

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).shortValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).shortValue());

		// assertThatIllegalArgumentException().as("mis-typed").isThrownBy(() ->
		// literal("100", iri(XSD_STRING)).shortValue()
		// );

	}

	@Test
	public final void testIntValue() {

		final IRI datatype=datatype(XSD_INT);
		final Class<Integer> type=Integer.class;

		assertThat(literal("100", datatype).intValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("+100", datatype).intValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("-100", datatype).intValue()).isInstanceOf(type).isEqualTo(-100);

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).intValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).intValue());

		// assertThatIllegalArgumentException().as("mis-typed").isThrownBy(() ->
		// literal("100", iri(XSD_STRING)).intValue()
		// );

	}

	@Test
	public final void testLongValue() {

		final IRI datatype=datatype(XSD_LONG);
		final Class<Long> type=Long.class;

		assertThat(literal("100", datatype).longValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("+100", datatype).longValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("-100", datatype).longValue()).isInstanceOf(type).isEqualTo(-100);

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).longValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).longValue());

		// assertThatIllegalArgumentException().as("mis-typed").isThrownBy(() ->
		// literal("100", iri(XSD_STRING)).longValue()
		// );

	}

	@Test
	public final void testFloatValue() {

		final IRI datatype=datatype(XSD_FLOAT);
		final Class<Float> type=Float.class;

		assertThat(literal("100", datatype).floatValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("+100", datatype).floatValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("-100", datatype).floatValue()).isInstanceOf(type).isEqualTo(-100);
		assertThat(literal("100.0", datatype).floatValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("10e1", datatype).floatValue()).isInstanceOf(type).isEqualTo(100);

		// assertThatIllegalArgumentException().as("not normalized").isThrownBy(() ->
		// literal("\t100", datatype).floatValue()
		// );

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).floatValue());

		// assertThatIllegalArgumentException().as("mis-typed").isThrownBy(() ->
		// literal("100", iri(XSD_STRING)).longValue()
		// );

	}

	@Test
	public final void testDoubleValue() {

		final IRI datatype=datatype(XSD_DOUBLE);
		final Class<Double> type=Double.class;

		assertThat(literal("100", datatype).doubleValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("+100", datatype).doubleValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("-100", datatype).doubleValue()).isInstanceOf(type).isEqualTo(-100);
		assertThat(literal("100.0", datatype).doubleValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("10e1", datatype).doubleValue()).isInstanceOf(type).isEqualTo(100);

		// assertThatIllegalArgumentException().as("not normalized").isThrownBy(() ->
		// literal("\t100", datatype).doubleValue()
		// );

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).doubleValue());

		// assertThatIllegalArgumentException().as("mis-typed").isThrownBy(() ->
		// literal("100", iri(XSD_STRING)).doubleValue()
		// );

	}

	@Test
	public final void testIntegerValue() {

		final IRI datatype=datatype(XSD_INTEGER);
		final Class<BigInteger> type=BigInteger.class;

		assertThat(literal("100", datatype).integerValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("+100", datatype).integerValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("-100", datatype).integerValue()).isInstanceOf(type).isEqualTo(-100);

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).integerValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).integerValue());

		// assertThatIllegalArgumentException().as("mis-typed").isThrownBy(() ->
		// literal("100", iri(XSD_STRING)).integerValue()
		// );

	}

	@Test
	public final void testDecimalValue() {

		final IRI datatype=datatype(XSD_DECIMAL);
		final Class<BigDecimal> type=BigDecimal.class;

		assertThat(literal("100", datatype).decimalValue()).isInstanceOf(type).isEqualTo(new BigDecimal("100"));
		assertThat(literal("+100", datatype).decimalValue()).isInstanceOf(type).isEqualTo(new BigDecimal("100"));
		assertThat(literal("-100", datatype).decimalValue()).isInstanceOf(type).isEqualTo(new BigDecimal("-100"));
		assertThat(literal("100.0", datatype).decimalValue()).isInstanceOf(type).isEqualTo(new BigDecimal("100.0"));
		assertThat(literal("10e1", datatype).decimalValue()).isInstanceOf(type).isEqualTo(new BigDecimal("1.0e2"));

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).decimalValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).decimalValue());

		// assertThatIllegalArgumentException().as("mis-typed").isThrownBy(() ->
		// literal("100", iri(XSD_STRING)).decimalValue()
		// );

	}

	@Test
	public final void testCalendarValue() throws DatatypeConfigurationException {

		final Class<XMLGregorianCalendar> type=XMLGregorianCalendar.class;

		assertThat(literal("2020-09-29T01:02:03.004Z", XSD_DATE_TIME).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(calendar(calendar -> {
					calendar.setYear(2020);
					calendar.setMonth(9);
					calendar.setDay(29);
					calendar.setTime(1, 2, 3, 4);
					calendar.setTimezone(0);
				}));

		assertThat(literal("01:02:03.004", XSD_TIME).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(calendar(calendar -> {
					calendar.setTime(1, 2, 3, 4);
				}));

		assertThat(literal("2020-09-29", XSD_DATE).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(calendar(calendar -> {
					calendar.setYear(2020);
					calendar.setMonth(9);
					calendar.setDay(29);
				}));

		assertThat(literal("2020-09", XSD_G_YEAR_MONTH).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(calendar(calendar -> {
					calendar.setYear(2020);
					calendar.setMonth(9);
				}));

		// assertThat(literal("09-29", XSD_G_MONTH_DAY).calendarValue())
		// .isInstanceOf(type)
		// .isEqualTo(calendar(calendar -> {
		// calendar.setYear(2020);
		// calendar.setMonth(9);
		// }));

		assertThat(literal("2020", XSD_G_YEAR).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(calendar(calendar -> {
					calendar.setYear(2020);
				}));

		// assertThat(literal("-09", XSD_G_MONTH).calendarValue())
		// .isInstanceOf(type)
		// .isEqualTo(calendar(calendar -> {
		// calendar.setMonth(9);
		// }));

		// assertThat(literal("--29", XSD_G_DAY).calendarValue())
		// .isInstanceOf(type)
		// .isEqualTo(calendar(calendar -> {
		// calendar.setDay(29);
		// }));

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", XSD_DATE_TIME).calendarValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", XSD_DATE_TIME).calendarValue());

		// assertThatIllegalArgumentException().as("mis-typed").isThrownBy(() ->
		// literal("100", iri(XSD_STRING)).calendarValue()
		// );

	}

	private XMLGregorianCalendar calendar(Consumer<XMLGregorianCalendar> setup) throws DatatypeConfigurationException {

		final XMLGregorianCalendar calendar=DatatypeFactory.newInstance().newXMLGregorianCalendar();

		setup.accept(calendar);

		return calendar;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test
	public void testEqualsAndHashCode() {

		final Literal plain=literal("plain");
		final Literal tagged=literal("tagged", "en");
		final Literal typed=literal("typed", datatype("http://example.org/datatype"));

		final Literal _plain=literal(plain.getLabel());
		final Literal _tagged=literal(tagged.getLabel(), tagged.getLanguage().orElse(""));
		final Literal _typed=literal(typed.getLabel(), typed.getDatatype());

		assertThat(plain).isEqualTo(plain);
		assertThat(plain).isEqualTo(_plain);

		assertThat(tagged).isEqualTo(tagged);
		assertThat(tagged).isEqualTo(_tagged);

		assertThat(typed).isEqualTo(typed);
		assertThat(typed).isEqualTo(_typed);

		assertThat(plain).isNotEqualTo(null);
		assertThat(plain).isNotEqualTo(new Object());

		assertThat(plain).isNotEqualTo(tagged);
		assertThat(plain).isNotEqualTo(typed);
		assertThat(tagged).isNotEqualTo(typed);

		assertThat(plain).isNotEqualTo(literal("other"));
		assertThat(tagged).isNotEqualTo(literal(tagged.getLabel(), "other"));
		assertThat(typed).isNotEqualTo(literal(typed.getLabel(), datatype("http://example.org/other")));

		// hashCode() should return identical values for literals for which equals() is true

		assertThat(plain.hashCode()).isEqualTo(_plain.hashCode());
		assertThat(tagged.hashCode()).isEqualTo(_tagged.hashCode());
		assertThat(typed.hashCode()).isEqualTo(_typed.hashCode());

	}

	@Test
	public final void testEqualsAndHashCodeCaseInsensitiveLanguage() {

		final Literal lowercase=literal("label", "en");
		final Literal uppercase=literal("label", "EN");

		assertThat(lowercase).isEqualTo(uppercase);
		assertThat(lowercase.hashCode()).isEqualTo(uppercase.hashCode());
	}

	@Test
	public final void testEqualsAndHashCodeXSDString() {

		// in RDF 1.1, there is no distinction between plain and string-typed literals

		final Literal plain=literal("label");
		final Literal typed=literal("label", datatype(XSD_STRING));

		assertThat(plain).isEqualTo(typed);
		assertThat(plain.hashCode()).isEqualTo(typed.hashCode());
	}

}
