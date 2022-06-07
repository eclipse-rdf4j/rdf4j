/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/
package org.eclipse.rdf4j.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.ChronoField;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.junit.Test;

/**
 * Abstract {@link Literal} test suite.
 */
public abstract class LiteralTest {

	private static final String XSD = "http://www.w3.org/2001/XMLSchema#";
	private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

	static final String XSD_BOOLEAN = XSD + "boolean";

	static final String XSD_BYTE = XSD + "byte";
	static final String XSD_SHORT = XSD + "short";
	static final String XSD_INT = XSD + "int";
	static final String XSD_LONG = XSD + "long";
	static final String XSD_FLOAT = XSD + "float";
	static final String XSD_DOUBLE = XSD + "double";
	static final String XSD_INTEGER = XSD + "integer";
	static final String XSD_DECIMAL = XSD + "decimal";

	static final String XSD_STRING = XSD + "string";

	static final String XSD_DATETIME = XSD + "dateTime";
	static final String XSD_TIME = XSD + "time";
	static final String XSD_DATE = XSD + "date";
	static final String XSD_GYEARMONTH = XSD + "gYearMonth";
	static final String XSD_GYEAR = XSD + "gYear";
	static final String XSD_GMONTHDAY = XSD + "gMonthDay";
	static final String XSD_GDAY = XSD + "gDay";
	static final String XSD_GMONTH = XSD + "gMonth";
	static final String XSD_DURATION = XSD + "duration";
	static final String XSD_DURATION_DAYTIME = XSD + "dayTimeDuration";
	static final String XSD_DURATION_YEARMONTH = XSD + "yearMonthDuration";

	static final String RDF_LANG_STRING = RDF + "langString";

	/**
	 * Creates a test literal instance.
	 *
	 * @param label the label of the literal
	 * @return a new instance of the concrete literal class under test
	 */
	protected abstract Literal literal(String label);

	/**
	 * Creates a test literal instance.
	 *
	 * @param label    the label of the literal
	 * @param language the language of the literal
	 * @return a new instance of the concrete literal class under test
	 */
	protected abstract Literal literal(String label, String language);

	/**
	 * Creates a test literal instance.
	 *
	 * @param label    the label of the literal
	 * @param datatype the datatype of the literal
	 * @return a new instance of the concrete literal class under test
	 */
	protected abstract Literal literal(String label, IRI datatype);

	/**
	 * Creates a test literal instance.
	 *
	 * @param label    the label of the literal
	 * @param datatype the CoreDatatype of the literal
	 * @return a new instance of the concrete literal class under test
	 */
	protected abstract Literal literal(String label, CoreDatatype datatype);

	/**
	 * Creates a test datatype IRI instance.
	 *
	 * @param iri the IRI of the datatype
	 * @return a new instance of the concrete datatype class under test
	 */
	protected abstract IRI datatype(String iri);

	//// Constructors //////////////////////////////////////////////////////////////////////////////////////////////////

	@Test
	public final void testPlainConstructor() {

		final String label = "label";

		final Literal literal = literal(label);

		assertThat(literal.getLabel()).isEqualTo(label);
		assertThat(literal.getLanguage()).isNotPresent();
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_STRING);

		assertThatNullPointerException().isThrownBy(() -> literal(null));
	}

	@Test
	public final void testPlainConstructorWithLongLabel() {

		final StringBuilder label = new StringBuilder(1000000);

		for (int i = 0; i < 1000000; i++) {
			label.append(Integer.toHexString(i % 16));
		}

		final Literal literal = literal(label.toString());

		assertThat(literal.getLabel()).isEqualTo(label.toString());
		assertThat(literal.getLanguage()).isNotPresent();
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_STRING);

	}

	@Test
	public final void testTaggedConstructor() {

		final String label = "label";
		final String language = "en";

		final Literal literal = literal(label, language);

		assertThat(literal.getLabel()).isEqualTo(label);
		assertThat(literal.getLanguage()).contains(language);
		assertThat(literal.getDatatype().stringValue()).isEqualTo(RDF_LANG_STRING);

		assertThatNullPointerException().isThrownBy(() -> literal(null, (String) null));
		assertThatNullPointerException().isThrownBy(() -> literal("", (String) null));
		assertThatNullPointerException().isThrownBy(() -> literal(null, ""));
		assertThatNullPointerException().isThrownBy(() -> literal(null, (IRI) null));

		assertThatIllegalArgumentException().isThrownBy(() -> literal("", ""));

	}

	@Test
	public final void testTypedConstructor() {

		final String label = "label";
		final String datatype = "http://examplle.org/datatype";

		final Literal literal = literal(label, datatype(datatype));

		assertThat(literal.getLabel()).isEqualTo(label);
		assertThat(literal.getLanguage()).isNotPresent();
		assertThat(literal.getDatatype().stringValue()).isEqualTo(datatype);

		assertThatNullPointerException().isThrownBy(() -> literal(null, (IRI) null));
		assertThatNullPointerException().isThrownBy(() -> literal(null, datatype(XSD_STRING)));
		assertThatNullPointerException().isThrownBy(() -> literal(null, datatype(RDF_LANG_STRING)));

		assertThatIllegalArgumentException().isThrownBy(() -> literal("", datatype(RDF_LANG_STRING)));

	}

	@Test
	public final void testTypedConstructorNullDatatype() {

		String label = "label";
		IRI datatype = null;

		Literal literal = literal(label, datatype);

		assertThat(literal.getLabel()).isEqualTo(label);
		assertThat(literal.getLanguage()).isNotPresent();
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_STRING);

	}

	//// String Value //////////////////////////////////////////////////////////////////////////////////////////////////

	@Test
	public void testStringValue() {

		final String label = "literal";
		final String language = "en";
		final IRI datatype = datatype(XSD_DECIMAL);

		assertThat(literal(label).stringValue()).isEqualTo(label);
		assertThat(literal(label, language).stringValue()).isEqualTo(label);
		assertThat(literal(label, datatype).stringValue()).isEqualTo(label);
	}

	//// Object Values /////////////////////////////////////////////////////////////////////////////////////////////////

	@Test
	public void testBooleanValue() {

		final IRI datatype = datatype(XSD_BOOLEAN);

		assertThat(literal("true", datatype).booleanValue()).isTrue();
		assertThat(literal("false", datatype).booleanValue()).isFalse();

		assertThat(literal("1", datatype).booleanValue()).isTrue();
		assertThat(literal("0", datatype).booleanValue()).isFalse();

		assertThat(literal("\ttrue", datatype).booleanValue()).isTrue();
		assertThat(literal("false\t", datatype).booleanValue()).isFalse();

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).booleanValue());

	}

	@Test
	public final void testByteValue() {

		final IRI datatype = datatype(XSD_BYTE);
		final Class<Byte> type = Byte.class;

		assertThat(literal("100", datatype).byteValue()).isInstanceOf(type).isEqualTo((byte) 100);
		assertThat(literal("+100", datatype).byteValue()).isInstanceOf(type).isEqualTo((byte) 100);
		assertThat(literal("-100", datatype).byteValue()).isInstanceOf(type).isEqualTo((byte) -100);

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).byteValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).booleanValue());

	}

	@Test
	public final void testShortValue() {

		final IRI datatype = datatype(XSD_SHORT);
		final Class<Short> type = Short.class;

		assertThat(literal("100", datatype).shortValue()).isInstanceOf(type).isEqualTo((short) 100);
		assertThat(literal("+100", datatype).shortValue()).isInstanceOf(type).isEqualTo((short) 100);
		assertThat(literal("-100", datatype).shortValue()).isInstanceOf(type).isEqualTo((short) -100);

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).shortValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).shortValue());

	}

	@Test
	public final void testIntValue() {

		final IRI datatype = datatype(XSD_INT);
		final Class<Integer> type = Integer.class;

		assertThat(literal("100", datatype).intValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("+100", datatype).intValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("-100", datatype).intValue()).isInstanceOf(type).isEqualTo(-100);

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).intValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).intValue());

	}

	@Test
	public final void testLongValue() {

		final IRI datatype = datatype(XSD_LONG);
		final Class<Long> type = Long.class;

		assertThat(literal("100", datatype).longValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("+100", datatype).longValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("-100", datatype).longValue()).isInstanceOf(type).isEqualTo(-100);

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).longValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).longValue());

	}

	@Test
	public final void testFloatValue() {

		final IRI datatype = datatype(XSD_FLOAT);
		final Class<Float> type = Float.class;

		assertThat(literal("100", datatype).floatValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("+100", datatype).floatValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("-100", datatype).floatValue()).isInstanceOf(type).isEqualTo(-100);
		assertThat(literal("100.0", datatype).floatValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("10e1", datatype).floatValue()).isInstanceOf(type).isEqualTo(100);

		assertThat(literal("INF", datatype).floatValue()).isInstanceOf(type).isEqualTo(Float.POSITIVE_INFINITY);
		assertThat(literal("-INF", datatype).floatValue()).isInstanceOf(type).isEqualTo(Float.NEGATIVE_INFINITY);
		assertTrue(Float.isNaN(literal("NaN", datatype).floatValue()));

		// assertThatIllegalArgumentException().as("not normalized")
		// .isThrownBy(() -> literal("\t100", datatype).floatValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).floatValue());

	}

	@Test
	public final void testDoubleValue() {

		final IRI datatype = datatype(XSD_DOUBLE);
		final Class<Double> type = Double.class;

		assertThat(literal("100", datatype).doubleValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("+100", datatype).doubleValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("-100", datatype).doubleValue()).isInstanceOf(type).isEqualTo(-100);
		assertThat(literal("100.0", datatype).doubleValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("10e1", datatype).doubleValue()).isInstanceOf(type).isEqualTo(100);

		assertThat(literal("INF", datatype).doubleValue()).isInstanceOf(type).isEqualTo(Double.POSITIVE_INFINITY);
		assertThat(literal("-INF", datatype).doubleValue()).isInstanceOf(type).isEqualTo(Double.NEGATIVE_INFINITY);
		assertTrue(Double.isNaN(literal("NaN", datatype).doubleValue()));

		// assertThatIllegalArgumentException().as("not normalized")
		// .isThrownBy(() -> literal("\t100", datatype).doubleValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).doubleValue());

	}

	@Test
	public final void testIntegerValue() {

		final IRI datatype = datatype(XSD_INTEGER);
		final Class<BigInteger> type = BigInteger.class;

		assertThat(literal("100", datatype).integerValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("+100", datatype).integerValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("-100", datatype).integerValue()).isInstanceOf(type).isEqualTo(-100);

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).integerValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).integerValue());

	}

	@Test
	public final void testDecimalValue() {

		final IRI datatype = datatype(XSD_DECIMAL);
		final Class<BigDecimal> type = BigDecimal.class;

		assertThat(literal("100", datatype).decimalValue()).isInstanceOf(type).isEqualTo(new BigDecimal("100"));
		assertThat(literal("+100", datatype).decimalValue()).isInstanceOf(type).isEqualTo(new BigDecimal("100"));
		assertThat(literal("-100", datatype).decimalValue()).isInstanceOf(type).isEqualTo(new BigDecimal("-100"));
		assertThat(literal("100.0", datatype).decimalValue()).isInstanceOf(type).isEqualTo(new BigDecimal("100.0"));
		assertThat(literal("10e1", datatype).decimalValue()).isInstanceOf(type).isEqualTo(new BigDecimal("1.0e2"));

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).decimalValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).decimalValue());

	}

	@Test
	public final void testTemporalDateTimeValue() {

		final String integral = "2020-09-29T01:02:03";
		final String fractional = "2020-09-29T01:02:03.004";

		final String offset = "2020-09-29T01:02:03+05:00";
		final String zero = "2020-09-29T01:02:03Z";

		assertThat(LocalDateTime.from(literal(integral, XSD_DATETIME).temporalAccessorValue()))
				.isEqualTo(LocalDateTime.parse(integral));

		assertThat(LocalDateTime.from(literal(fractional, XSD_DATETIME).temporalAccessorValue()))
				.isEqualTo(LocalDateTime.parse(fractional));

		assertThat(OffsetDateTime.from(literal(offset, XSD_DATETIME).temporalAccessorValue()))
				.isEqualTo(OffsetDateTime.parse(offset));

		assertThat(OffsetDateTime.from(literal(zero, XSD_DATETIME).temporalAccessorValue()))
				.isEqualTo(OffsetDateTime.parse(zero));

		Stream.of(

				"0001-01-01T00:00:00",
				"0001-01-01T00:00:00.0",
				"0001-01-01T00:00:00Z",
				"0001-01-01T00:00:00.0Z",
				"0001-01-01T00:00:00+00:00",
				"0001-01-01T00:00:00.0+00:00",
				"0001-01-01T00:00:00.0-00:00",
				"0001-01-01T00:00:00.0+14:00",
				"0001-01-01T00:00:00.0-14:00",
				"0001-05-31T00:00:00.00",
				"0001-07-31T00:00:00.00",
				"0001-08-31T00:00:00.00",
				"0001-10-31T00:00:00.00",
				"0001-12-31T00:00:00.00",
				"-0001-01-01T00:00:00",
				"1234-12-31T23:59:59",
				"1234-12-31T24:00:00",
				// "12345-12-31T24:00:00",
				// "1234-12-31T24:00:00.1234567890",
				"2004-02-29T00:00:00"

		)
				.forEach(value -> assertThatCode(() -> literal(value, XSD_DATETIME).temporalAccessorValue())
						.as(value)
						.doesNotThrowAnyException()
				);

	}

	@Test
	public final void testTemporalTimeValue() {

		final String integral = "01:02:03";
		final String fractional = "01:02:03.004";

		final String offset = "01:02:03+05:00";
		final String zero = "01:02:03Z";

		assertThat(LocalTime.from(literal(integral, XSD_TIME).temporalAccessorValue()))
				.isEqualTo(LocalTime.parse(integral));

		assertThat(LocalTime.from(literal(fractional, XSD_TIME).temporalAccessorValue()))
				.isEqualTo(LocalTime.parse(fractional));

		assertThat(OffsetTime.from(literal(offset, XSD_TIME).temporalAccessorValue()))
				.isEqualTo(OffsetTime.parse(offset));

		assertThat(OffsetTime.from(literal(zero, XSD_TIME).temporalAccessorValue()))
				.isEqualTo(OffsetTime.parse(zero));
	}

	@Test
	public final void testTemporalDateValue() {

		final String local = "2020-11-14";
		final String offset = "2020-11-14+05:00";
		final String zero = "2020-11-14Z";

		assertThat(LocalDate.from(literal(local, XSD_DATE).temporalAccessorValue()))
				.isEqualTo(LocalDate.parse(local));

		assertThat(LocalDate.from(literal(offset, XSD_DATE).temporalAccessorValue()))
				.isEqualTo(LocalDate.parse(offset.substring(0, 10))); // OffsetDate not supported by java.time

		assertThat(LocalDate.from(literal(zero, XSD_DATE).temporalAccessorValue()))
				.isEqualTo(LocalDate.parse(offset.substring(0, 10))); // OffsetDate not supported by java.time

	}

	@Test
	public final void testTemporalGYearMonthValue() {

		final String base = "2020-11";

		assertThat(YearMonth.from(literal(base, XSD_GYEARMONTH).temporalAccessorValue()))
				.isEqualTo(YearMonth.parse(base));

	}

	@Test
	public final void testTemporalGYearValue() {

		final String local = "2020";

		assertThat(Year.from(literal(local, XSD_GYEAR).temporalAccessorValue()))
				.isEqualTo(Year.parse(local));

	}

	@Test
	public final void testTemporalGMonthDayValue() {

		final String local = "--11-14";

		assertThat(MonthDay.from(literal(local, XSD_GMONTHDAY).temporalAccessorValue()))
				.isEqualTo(MonthDay.parse(local));

	}

	@Test
	public final void testTemporalGDayValue() {

		final String local = "---14";

		assertThat(literal(local, XSD_GDAY).temporalAccessorValue().get(ChronoField.DAY_OF_MONTH))
				.isEqualTo(14);

	}

	@Test
	public final void testTemporalGMonthValue() {

		final String local = "--11";

		assertThat(Month.from(literal(local, XSD_GMONTH).temporalAccessorValue()))
				.isEqualTo(Month.NOVEMBER);

	}

	@Test
	public final void testTemporalAccessorMalformedValue() {

		assertThatExceptionOfType(DateTimeException.class)
				.isThrownBy(() -> literal("", XSD_DATETIME).temporalAccessorValue());

		assertThatExceptionOfType(DateTimeException.class)
				.isThrownBy(() -> literal("--", XSD_DATETIME).temporalAccessorValue());

		assertThatExceptionOfType(DateTimeException.class).as("no time components")
				.isThrownBy(() -> literal("2020-11-16T", XSD_DATETIME).temporalAccessorValue());

		assertThatExceptionOfType(DateTimeException.class).as("missing fractional digits after dot")
				.isThrownBy(() -> literal("2020-11-16T11:12:13.", XSD_DATETIME).temporalAccessorValue());

		assertThatExceptionOfType(DateTimeException.class)
				.isThrownBy(() -> literal("malformed", XSD_DATETIME).temporalAccessorValue());

		assertThatExceptionOfType(DateTimeException.class).as("no time components")
				.isThrownBy(() -> literal("2020-11-16T", XSD_DATETIME).temporalAccessorValue());

		assertThatExceptionOfType(DateTimeException.class).as("missing fractional digits after dot")
				.isThrownBy(() -> literal("2020-11-16T11:12:13.", XSD_DATETIME).temporalAccessorValue());

		Stream.of(

				"foo", "Mon, 11 Jul 2005 09:22:29 +0200",
				"0001-01-01T00:00",
				"0001-01-01T00:00.00",
				"0001-13-01T00:00:00.00",
				"0001-01-32T00:00:00.00",
				// "0001-02-30T00:00:00.00",
				// "2005-02-29T00:00:00", // not a leap year
				// "0001-04-31T00:00:00.00",
				"0001-01-01T25:00:00.00",
				"0001-01-01T00:61:00.00",
				"0001-01-01T00:00:61.00",
				"0001-01-01T00:00.00+15:00",
				"0001-01-01T00:00.00-15:00",
				"001-01-01T00:00:00.0",
				"0001-1-01T00:00:00.0",
				"0001-01-1T00:00:00.0",
				"0001-01-01T0:00:00.0",
				"0001-01-01T00:0:00.0",
				"0001-01-01T00:00:0.0",
				"0001/01-01T00:00:00.0",
				"0001-01/01T00:00:00.0",
				"0001-01-01t00:00:00.0",
				"0001-01-01T00.00:00.0",
				"0001-01-01T00:00.00.0",
				"0001-01-01T00:00:00:0",
				"0001-01-01T00:00.00+0:00",
				"0001-01-01T00:00.00+00:0",
				"0001-jan-01T00:00:00",
				"0001-01-01T00:00:00+00:00Z",
				"0001-01-01T24:01:00", "0001-01-01T24:00:01",
				"00001-01-01T00:00:00",
				"0001-001-01T00:00:00",
				"0001-01-001T00:00:00",
				"0001-01-01T000:00:00",
				"0001-01-01T00:000:00",
				"0001-01-01T00:00:000",
				"0001-01-01T00:00:000",
				"0001-01-01T00:00:00z",
				"0001-01-01T00:00:00+05",
				"0001-01-01T00:00:00+0500",
				"0001-01-01T00:00:00GMT",
				"0001-01-01T00:00:00PST",
				"0001-01-01T00:00:00GMT+05",
				// "0000-01-01T00:00:00",
				"-0000-01-01T00:00:00",
				"+0001-01-01T00:00:00"

		)
				.forEach(value -> assertThatExceptionOfType(DateTimeException.class)
						.as(value)
						.isThrownBy(() -> literal(value, XSD_DATETIME).temporalAccessorValue())
				);

	}

	@Test
	public final void testTemporalDurationValue() {

		final String period = "P1Y2M3D";
		final String duration = "PT1H2M3.4S";

		assertThat(Period.from(literal(period, XSD_DURATION).temporalAmountValue()))
				.isEqualTo(Period.parse(period));

		assertThat(Period.from(literal("-P1Y2M3D", XSD_DURATION).temporalAmountValue()))
				.isEqualTo(Period.parse(period).negated());

		assertThat(Duration.from(literal(duration, XSD_DURATION).temporalAmountValue()))
				.isEqualTo(Duration.parse(duration));

	}

	@Test
	public final void testTemporalAmountMalformedValue() {

		assertThatExceptionOfType(DateTimeException.class)
				.isThrownBy(() -> literal("", XSD_DURATION).temporalAmountValue());

		assertThatExceptionOfType(DateTimeException.class)
				.isThrownBy(() -> literal("malformed", XSD_DURATION).temporalAmountValue());

		assertThatExceptionOfType(DateTimeException.class).as("no  components")
				.isThrownBy(() -> literal("P", XSD_DURATION).temporalAmountValue());

		assertThatExceptionOfType(DateTimeException.class).as("no time components")
				.isThrownBy(() -> literal("P1Y2MT", XSD_DURATION).temporalAmountValue());

		assertThatExceptionOfType(DateTimeException.class).as("negative component")
				.isThrownBy(() -> literal("P-1347M ", XSD_DURATION).temporalAmountValue());

		assertThatExceptionOfType(DateTimeException.class).as("no time separator")
				.isThrownBy(() -> literal("P1Y1S ", XSD_DURATION).temporalAmountValue());

		assertThatExceptionOfType(DateTimeException.class).as("missing fractional digits after dot")
				.isThrownBy(() -> literal("PT1.S", XSD_DURATION).temporalAmountValue());
	}

	@Test
	public final void testCalendarValue() throws DatatypeConfigurationException {

		final Class<XMLGregorianCalendar> type = XMLGregorianCalendar.class;

		final DatatypeFactory factory = DatatypeFactory.newInstance();

		final Function<Consumer<XMLGregorianCalendar>, XMLGregorianCalendar> setup = consumer -> {

			final XMLGregorianCalendar calendar = factory.newXMLGregorianCalendar();

			consumer.accept(calendar);

			return calendar;

		};

		assertThat(literal("2020-09-29T01:02:03.004Z", XSD_DATETIME).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(setup.apply(calendar -> {
					calendar.setYear(2020);
					calendar.setMonth(9);
					calendar.setDay(29);
					calendar.setTime(1, 2, 3, 4);
					calendar.setTimezone(0);
				}));

		assertThat(literal("01:02:03.004", XSD_TIME).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(setup.apply(calendar -> {
					calendar.setTime(1, 2, 3, 4);
				}));

		assertThat(literal("2020-09-29", XSD_DATE).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(setup.apply(calendar -> {
					calendar.setYear(2020);
					calendar.setMonth(9);
					calendar.setDay(29);
				}));

		assertThat(literal("2020-09", XSD_GYEARMONTH).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(setup.apply(calendar -> {
					calendar.setYear(2020);
					calendar.setMonth(9);
				}));

		assertThat(literal("--09-29", XSD_GMONTHDAY).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(setup.apply(calendar -> {
					calendar.setMonth(9);
					calendar.setDay(29);
				}));

		assertThat(literal("2020", XSD_GYEAR).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(setup.apply(calendar -> {
					calendar.setYear(2020);
				}));

		assertThat(literal("--09", XSD_GMONTH).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(setup.apply(calendar -> {
					calendar.setMonth(9);
				}));

		assertThat(literal("---29", XSD_GDAY).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(setup.apply(calendar -> {
					calendar.setDay(29);
				}));

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", XSD_DATETIME).calendarValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", XSD_DATETIME).calendarValue());

	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test
	public void testEqualsAndHashCode() {

		final Literal plain = literal("plain");
		final Literal tagged = literal("tagged", "en");
		final Literal typed = literal("typed", datatype("http://example.org/datatype"));

		final Literal _plain = literal(plain.getLabel());
		final Literal _tagged = literal(tagged.getLabel(), tagged.getLanguage().orElse(""));
		final Literal _typed = literal(typed.getLabel(), typed.getDatatype());

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

		assertThat(tagged.hashCode())
				.as("computed according to contract")
				.isEqualTo(tagged.getLabel().hashCode()); // !!! label >> label+language+datatype

	}

	@Test
	public final void testEqualsAndHashCodeCaseInsensitiveLanguage() {

		final Literal lowercase = literal("label", "en");
		final Literal uppercase = literal("label", "EN");

		assertThat(lowercase).isEqualTo(uppercase);
		assertThat(lowercase.hashCode()).isEqualTo(uppercase.hashCode());
	}

	@Test
	public final void testEqualsAndHashCodeXSDString() {

		// in RDF 1.1, there is no distinction between plain and string-typed literals

		final Literal plain = literal("label");
		final Literal typed = literal("label", datatype(XSD_STRING));

		assertThat(plain).isEqualTo(typed);
		assertThat(plain.hashCode()).isEqualTo(typed.hashCode());
	}

	@Test
	public final void testCoreDatatypePlainConstructor() {

		String label = "label";

		Literal literal = literal(label);

		assertThat(literal.getLabel()).isEqualTo(label);
		assertThat(literal.getLanguage()).isNotPresent();
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.XSD.STRING);

		assertThatNullPointerException().isThrownBy(() -> literal(null));
	}

	@Test
	public final void testCoreDatatypePlainConstructorWithLongLabel() {

		StringBuilder label = new StringBuilder(1000000);

		for (int i = 0; i < 1000000; i++) {
			label.append(Integer.toHexString(i % 16));
		}

		Literal literal = literal(label.toString());

		assertThat(literal.getLabel()).isEqualTo(label.toString());
		assertThat(literal.getLanguage()).isNotPresent();
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.XSD.STRING);

	}

	@Test
	public final void testCoreDatatypeTaggedConstructor() {

		String label = "label";
		String language = "en";

		Literal literal = literal(label, language);

		assertThat(literal.getLabel()).isEqualTo(label);
		assertThat(literal.getLanguage()).contains(language);
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.RDF.LANGSTRING);

		assertThatNullPointerException().isThrownBy(() -> literal(null, (String) null));
		assertThatNullPointerException().isThrownBy(() -> literal("", (String) null));
		assertThatNullPointerException().isThrownBy(() -> literal(null, ""));
		assertThatNullPointerException().isThrownBy(() -> literal(null, (CoreDatatype) null));

		assertThatIllegalArgumentException().isThrownBy(() -> literal("", ""));

	}

	@Test
	public final void testCoreDatatypeTypedConstructor() {

		String label = "label";
		String datatype = "http://examplle.org/datatype";

		Literal literal = literal(label, datatype(datatype));

		assertThat(literal.getLabel()).isEqualTo(label);
		assertThat(literal.getLanguage()).isNotPresent();
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.NONE);

		assertThatNullPointerException().isThrownBy(() -> literal(null, (CoreDatatype) null));
		assertThatNullPointerException().isThrownBy(() -> literal(null, CoreDatatype.XSD.STRING));
		assertThatNullPointerException().isThrownBy(() -> literal(null, CoreDatatype.RDF.LANGSTRING));

		assertThatIllegalArgumentException().isThrownBy(() -> literal("", CoreDatatype.RDF.LANGSTRING));

	}

	@Test(expected = NullPointerException.class)
	public final void testCoreDatatypeTypedConstructorNullDatatype() {
		literal("label", ((CoreDatatype) null));
	}

	//// String Value //////////////////////////////////////////////////////////////////////////////////////////////////

	@Test
	public void testCoreDatatypeStringValue() {

		String label = "literal";
		String language = "en";
		CoreDatatype datatype = CoreDatatype.XSD.DECIMAL;

		assertThat(literal(label).stringValue()).isEqualTo(label);
		assertThat(literal(label, language).stringValue()).isEqualTo(label);
		assertThat(literal(label, datatype).stringValue()).isEqualTo(label);
	}

	//// Object Values /////////////////////////////////////////////////////////////////////////////////////////////////

	@Test
	public void testCoreDatatypeBooleanValue() {

		CoreDatatype datatype = CoreDatatype.XSD.BOOLEAN;

		assertThat(literal("true", datatype).booleanValue()).isTrue();
		assertThat(literal("false", datatype).booleanValue()).isFalse();

		assertThat(literal("1", datatype).booleanValue()).isTrue();
		assertThat(literal("0", datatype).booleanValue()).isFalse();

		assertThat(literal("\ttrue", datatype).booleanValue()).isTrue();
		assertThat(literal("false\t", datatype).booleanValue()).isFalse();

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).booleanValue());

	}

	@Test
	public final void testCoreDatatypeByteValue() {

		CoreDatatype datatype = CoreDatatype.XSD.BYTE;
		Class<Byte> type = Byte.class;

		assertThat(literal("100", datatype).byteValue()).isInstanceOf(type).isEqualTo((byte) 100);
		assertThat(literal("+100", datatype).byteValue()).isInstanceOf(type).isEqualTo((byte) 100);
		assertThat(literal("-100", datatype).byteValue()).isInstanceOf(type).isEqualTo((byte) -100);

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).byteValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).booleanValue());

	}

	@Test
	public final void testCoreDatatypeShortValue() {

		CoreDatatype datatype = CoreDatatype.XSD.SHORT;
		Class<Short> type = Short.class;

		assertThat(literal("100", datatype).shortValue()).isInstanceOf(type).isEqualTo((short) 100);
		assertThat(literal("+100", datatype).shortValue()).isInstanceOf(type).isEqualTo((short) 100);
		assertThat(literal("-100", datatype).shortValue()).isInstanceOf(type).isEqualTo((short) -100);

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).shortValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).shortValue());

	}

	@Test
	public final void testCoreDatatypeIntValue() {

		CoreDatatype datatype = CoreDatatype.XSD.INT;
		Class<Integer> type = Integer.class;

		assertThat(literal("100", datatype).intValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("+100", datatype).intValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("-100", datatype).intValue()).isInstanceOf(type).isEqualTo(-100);

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).intValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).intValue());

	}

	@Test
	public final void testCoreDatatypeLongValue() {

		CoreDatatype datatype = CoreDatatype.XSD.LONG;
		Class<Long> type = Long.class;

		assertThat(literal("100", datatype).longValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("+100", datatype).longValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("-100", datatype).longValue()).isInstanceOf(type).isEqualTo(-100);

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).longValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).longValue());

	}

	@Test
	public final void testCoreDatatypeFloatValue() {

		CoreDatatype datatype = CoreDatatype.XSD.FLOAT;
		Class<Float> type = Float.class;

		assertThat(literal("100", datatype).floatValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("+100", datatype).floatValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("-100", datatype).floatValue()).isInstanceOf(type).isEqualTo(-100);
		assertThat(literal("100.0", datatype).floatValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("10e1", datatype).floatValue()).isInstanceOf(type).isEqualTo(100);

		assertThat(literal("INF", datatype).floatValue()).isInstanceOf(type).isEqualTo(Float.POSITIVE_INFINITY);
		assertThat(literal("-INF", datatype).floatValue()).isInstanceOf(type).isEqualTo(Float.NEGATIVE_INFINITY);
		assertTrue(Float.isNaN(literal("NaN", datatype).floatValue()));

		// assertThatIllegalArgumentException().as("not normalized")
		// .isThrownBy(() -> literal("\t100", datatype).floatValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).floatValue());

	}

	@Test
	public final void testCoreDatatypeDoubleValue() {

		CoreDatatype datatype = CoreDatatype.XSD.DOUBLE;
		Class<Double> type = Double.class;

		assertThat(literal("100", datatype).doubleValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("+100", datatype).doubleValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("-100", datatype).doubleValue()).isInstanceOf(type).isEqualTo(-100);
		assertThat(literal("100.0", datatype).doubleValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("10e1", datatype).doubleValue()).isInstanceOf(type).isEqualTo(100);

		assertThat(literal("INF", datatype).doubleValue()).isInstanceOf(type).isEqualTo(Double.POSITIVE_INFINITY);
		assertThat(literal("-INF", datatype).doubleValue()).isInstanceOf(type).isEqualTo(Double.NEGATIVE_INFINITY);
		assertTrue(Double.isNaN(literal("NaN", datatype).doubleValue()));

		// assertThatIllegalArgumentException().as("not normalized")
		// .isThrownBy(() -> literal("\t100", datatype).doubleValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).doubleValue());

	}

	@Test
	public final void testCoreDatatypeIntegerValue() {

		CoreDatatype datatype = CoreDatatype.XSD.INTEGER;
		Class<BigInteger> type = BigInteger.class;

		assertThat(literal("100", datatype).integerValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("+100", datatype).integerValue()).isInstanceOf(type).isEqualTo(100);
		assertThat(literal("-100", datatype).integerValue()).isInstanceOf(type).isEqualTo(-100);

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).integerValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).integerValue());

	}

	@Test
	public final void testCoreDatatypeDecimalValue() {

		CoreDatatype datatype = CoreDatatype.XSD.DECIMAL;
		Class<BigDecimal> type = BigDecimal.class;

		assertThat(literal("100", datatype).decimalValue()).isInstanceOf(type).isEqualTo(new BigDecimal("100"));
		assertThat(literal("+100", datatype).decimalValue()).isInstanceOf(type).isEqualTo(new BigDecimal("100"));
		assertThat(literal("-100", datatype).decimalValue()).isInstanceOf(type).isEqualTo(new BigDecimal("-100"));
		assertThat(literal("100.0", datatype).decimalValue()).isInstanceOf(type).isEqualTo(new BigDecimal("100.0"));
		assertThat(literal("10e1", datatype).decimalValue()).isInstanceOf(type).isEqualTo(new BigDecimal("1.0e2"));

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", datatype).decimalValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", datatype).decimalValue());

	}

	@Test
	public final void testCoreDatatypeTemporalDateTimeValue() {

		String integral = "2020-09-29T01:02:03";
		String fractional = "2020-09-29T01:02:03.004";

		String offset = "2020-09-29T01:02:03+05:00";
		String zero = "2020-09-29T01:02:03Z";

		assertThat(LocalDateTime.from(literal(integral, CoreDatatype.XSD.DATETIME).temporalAccessorValue()))
				.isEqualTo(LocalDateTime.parse(integral));

		assertThat(LocalDateTime.from(literal(fractional, CoreDatatype.XSD.DATETIME).temporalAccessorValue()))
				.isEqualTo(LocalDateTime.parse(fractional));

		assertThat(OffsetDateTime.from(literal(offset, CoreDatatype.XSD.DATETIME).temporalAccessorValue()))
				.isEqualTo(OffsetDateTime.parse(offset));

		assertThat(OffsetDateTime.from(literal(zero, CoreDatatype.XSD.DATETIME).temporalAccessorValue()))
				.isEqualTo(OffsetDateTime.parse(zero));

		Stream.of(

				"0001-01-01T00:00:00",
				"0001-01-01T00:00:00.0",
				"0001-01-01T00:00:00Z",
				"0001-01-01T00:00:00.0Z",
				"0001-01-01T00:00:00+00:00",
				"0001-01-01T00:00:00.0+00:00",
				"0001-01-01T00:00:00.0-00:00",
				"0001-01-01T00:00:00.0+14:00",
				"0001-01-01T00:00:00.0-14:00",
				"0001-05-31T00:00:00.00",
				"0001-07-31T00:00:00.00",
				"0001-08-31T00:00:00.00",
				"0001-10-31T00:00:00.00",
				"0001-12-31T00:00:00.00",
				"-0001-01-01T00:00:00",
				"1234-12-31T23:59:59",
				"1234-12-31T24:00:00",
				// "12345-12-31T24:00:00",
				// "1234-12-31T24:00:00.1234567890",
				"2004-02-29T00:00:00"

		)
				.forEach(
						value -> assertThatCode(() -> literal(value, CoreDatatype.XSD.DATETIME).temporalAccessorValue())
								.as(value)
								.doesNotThrowAnyException()
				);

	}

	@Test
	public final void testCoreDatatypeTemporalTimeValue() {

		String integral = "01:02:03";
		String fractional = "01:02:03.004";

		String offset = "01:02:03+05:00";
		String zero = "01:02:03Z";

		assertThat(LocalTime.from(literal(integral, CoreDatatype.XSD.TIME).temporalAccessorValue()))
				.isEqualTo(LocalTime.parse(integral));

		assertThat(LocalTime.from(literal(fractional, CoreDatatype.XSD.TIME).temporalAccessorValue()))
				.isEqualTo(LocalTime.parse(fractional));

		assertThat(OffsetTime.from(literal(offset, CoreDatatype.XSD.TIME).temporalAccessorValue()))
				.isEqualTo(OffsetTime.parse(offset));

		assertThat(OffsetTime.from(literal(zero, CoreDatatype.XSD.TIME).temporalAccessorValue()))
				.isEqualTo(OffsetTime.parse(zero));
	}

	@Test
	public final void testCoreDatatypeTemporalDateValue() {

		String local = "2020-11-14";
		String offset = "2020-11-14+05:00";
		String zero = "2020-11-14Z";

		assertThat(LocalDate.from(literal(local, CoreDatatype.XSD.DATE).temporalAccessorValue()))
				.isEqualTo(LocalDate.parse(local));

		assertThat(LocalDate.from(literal(offset, CoreDatatype.XSD.DATE).temporalAccessorValue()))
				.isEqualTo(LocalDate.parse(offset.substring(0, 10))); // OffsetDate not supported by java.time

		assertThat(LocalDate.from(literal(zero, CoreDatatype.XSD.DATE).temporalAccessorValue()))
				.isEqualTo(LocalDate.parse(offset.substring(0, 10))); // OffsetDate not supported by java.time

	}

	@Test
	public final void testCoreDatatypeTemporalGYearMonthValue() {

		String base = "2020-11";

		assertThat(YearMonth.from(literal(base, CoreDatatype.XSD.GYEARMONTH).temporalAccessorValue()))
				.isEqualTo(YearMonth.parse(base));

	}

	@Test
	public final void testCoreDatatypeTemporalGYearValue() {

		String local = "2020";

		assertThat(Year.from(literal(local, CoreDatatype.XSD.GYEAR).temporalAccessorValue()))
				.isEqualTo(Year.parse(local));

	}

	@Test
	public final void testCoreDatatypeTemporalGMonthDayValue() {

		String local = "--11-14";

		assertThat(MonthDay.from(literal(local, CoreDatatype.XSD.GMONTHDAY).temporalAccessorValue()))
				.isEqualTo(MonthDay.parse(local));

	}

	@Test
	public final void testCoreDatatypeTemporalGDayValue() {

		String local = "---14";

		assertThat(literal(local, CoreDatatype.XSD.GDAY).temporalAccessorValue().get(ChronoField.DAY_OF_MONTH))
				.isEqualTo(14);

	}

	@Test
	public final void testCoreDatatypeTemporalGMonthValue() {

		String local = "--11";

		assertThat(Month.from(literal(local, CoreDatatype.XSD.GMONTH).temporalAccessorValue()))
				.isEqualTo(Month.NOVEMBER);

	}

	@Test
	public final void testCoreDatatypeTemporalAccessorMalformedValue() {

		assertThatExceptionOfType(DateTimeException.class)
				.isThrownBy(() -> literal("", CoreDatatype.XSD.DATETIME).temporalAccessorValue());

		assertThatExceptionOfType(DateTimeException.class)
				.isThrownBy(() -> literal("--", CoreDatatype.XSD.DATETIME).temporalAccessorValue());

		assertThatExceptionOfType(DateTimeException.class).as("no time components")
				.isThrownBy(() -> literal("2020-11-16T", CoreDatatype.XSD.DATETIME).temporalAccessorValue());

		assertThatExceptionOfType(DateTimeException.class).as("missing fractional digits after dot")
				.isThrownBy(() -> literal("2020-11-16T11:12:13.", CoreDatatype.XSD.DATETIME).temporalAccessorValue());

		assertThatExceptionOfType(DateTimeException.class)
				.isThrownBy(() -> literal("malformed", CoreDatatype.XSD.DATETIME).temporalAccessorValue());

		assertThatExceptionOfType(DateTimeException.class).as("no time components")
				.isThrownBy(() -> literal("2020-11-16T", CoreDatatype.XSD.DATETIME).temporalAccessorValue());

		assertThatExceptionOfType(DateTimeException.class).as("missing fractional digits after dot")
				.isThrownBy(() -> literal("2020-11-16T11:12:13.", CoreDatatype.XSD.DATETIME).temporalAccessorValue());

		Stream.of(

				"foo", "Mon, 11 Jul 2005 09:22:29 +0200",
				"0001-01-01T00:00",
				"0001-01-01T00:00.00",
				"0001-13-01T00:00:00.00",
				"0001-01-32T00:00:00.00",
				// "0001-02-30T00:00:00.00",
				// "2005-02-29T00:00:00", // not a leap year
				// "0001-04-31T00:00:00.00",
				"0001-01-01T25:00:00.00",
				"0001-01-01T00:61:00.00",
				"0001-01-01T00:00:61.00",
				"0001-01-01T00:00.00+15:00",
				"0001-01-01T00:00.00-15:00",
				"001-01-01T00:00:00.0",
				"0001-1-01T00:00:00.0",
				"0001-01-1T00:00:00.0",
				"0001-01-01T0:00:00.0",
				"0001-01-01T00:0:00.0",
				"0001-01-01T00:00:0.0",
				"0001/01-01T00:00:00.0",
				"0001-01/01T00:00:00.0",
				"0001-01-01t00:00:00.0",
				"0001-01-01T00.00:00.0",
				"0001-01-01T00:00.00.0",
				"0001-01-01T00:00:00:0",
				"0001-01-01T00:00.00+0:00",
				"0001-01-01T00:00.00+00:0",
				"0001-jan-01T00:00:00",
				"0001-01-01T00:00:00+00:00Z",
				"0001-01-01T24:01:00", "0001-01-01T24:00:01",
				"00001-01-01T00:00:00",
				"0001-001-01T00:00:00",
				"0001-01-001T00:00:00",
				"0001-01-01T000:00:00",
				"0001-01-01T00:000:00",
				"0001-01-01T00:00:000",
				"0001-01-01T00:00:000",
				"0001-01-01T00:00:00z",
				"0001-01-01T00:00:00+05",
				"0001-01-01T00:00:00+0500",
				"0001-01-01T00:00:00GMT",
				"0001-01-01T00:00:00PST",
				"0001-01-01T00:00:00GMT+05",
				// "0000-01-01T00:00:00",
				"-0000-01-01T00:00:00",
				"+0001-01-01T00:00:00"

		)
				.forEach(value -> assertThatExceptionOfType(DateTimeException.class)
						.as(value)
						.isThrownBy(() -> literal(value, CoreDatatype.XSD.DATETIME).temporalAccessorValue())
				);

	}

	@Test
	public final void testCoreDatatypeTemporalDurationValue() {

		String period = "P1Y2M3D";
		String duration = "PT1H2M3.4S";

		assertThat(Period.from(literal(period, CoreDatatype.XSD.DURATION).temporalAmountValue()))
				.isEqualTo(Period.parse(period));

		assertThat(Period.from(literal("-P1Y2M3D", CoreDatatype.XSD.DURATION).temporalAmountValue()))
				.isEqualTo(Period.parse(period).negated());

		assertThat(Duration.from(literal(duration, CoreDatatype.XSD.DURATION).temporalAmountValue()))
				.isEqualTo(Duration.parse(duration));

	}

	@Test
	public final void testCoreDatatypeTemporalAmountMalformedValue() {

		assertThatExceptionOfType(DateTimeException.class)
				.isThrownBy(() -> literal("", CoreDatatype.XSD.DURATION).temporalAmountValue());

		assertThatExceptionOfType(DateTimeException.class)
				.isThrownBy(() -> literal("malformed", CoreDatatype.XSD.DURATION).temporalAmountValue());

		assertThatExceptionOfType(DateTimeException.class).as("no  components")
				.isThrownBy(() -> literal("P", CoreDatatype.XSD.DURATION).temporalAmountValue());

		assertThatExceptionOfType(DateTimeException.class).as("no time components")
				.isThrownBy(() -> literal("P1Y2MT", CoreDatatype.XSD.DURATION).temporalAmountValue());

		assertThatExceptionOfType(DateTimeException.class).as("negative component")
				.isThrownBy(() -> literal("P-1347M ", CoreDatatype.XSD.DURATION).temporalAmountValue());

		assertThatExceptionOfType(DateTimeException.class).as("no time separator")
				.isThrownBy(() -> literal("P1Y1S ", CoreDatatype.XSD.DURATION).temporalAmountValue());

		assertThatExceptionOfType(DateTimeException.class).as("missing fractional digits after dot")
				.isThrownBy(() -> literal("PT1.S", CoreDatatype.XSD.DURATION).temporalAmountValue());
	}

	@Test
	public final void testCoreDatatypeCalendarValue() throws DatatypeConfigurationException {

		Class<XMLGregorianCalendar> type = XMLGregorianCalendar.class;

		DatatypeFactory factory = DatatypeFactory.newInstance();

		Function<Consumer<XMLGregorianCalendar>, XMLGregorianCalendar> setup = consumer -> {

			XMLGregorianCalendar calendar = factory.newXMLGregorianCalendar();

			consumer.accept(calendar);

			return calendar;

		};

		assertThat(literal("2020-09-29T01:02:03.004Z", CoreDatatype.XSD.DATETIME).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(setup.apply(calendar -> {
					calendar.setYear(2020);
					calendar.setMonth(9);
					calendar.setDay(29);
					calendar.setTime(1, 2, 3, 4);
					calendar.setTimezone(0);
				}));

		assertThat(literal("01:02:03.004", CoreDatatype.XSD.TIME).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(setup.apply(calendar -> {
					calendar.setTime(1, 2, 3, 4);
				}));

		assertThat(literal("2020-09-29", CoreDatatype.XSD.DATE).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(setup.apply(calendar -> {
					calendar.setYear(2020);
					calendar.setMonth(9);
					calendar.setDay(29);
				}));

		assertThat(literal("2020-09", CoreDatatype.XSD.GYEARMONTH).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(setup.apply(calendar -> {
					calendar.setYear(2020);
					calendar.setMonth(9);
				}));

		assertThat(literal("--09-29", CoreDatatype.XSD.GMONTHDAY).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(setup.apply(calendar -> {
					calendar.setMonth(9);
					calendar.setDay(29);
				}));

		assertThat(literal("2020", CoreDatatype.XSD.GYEAR).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(setup.apply(calendar -> {
					calendar.setYear(2020);
				}));

		assertThat(literal("--09", CoreDatatype.XSD.GMONTH).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(setup.apply(calendar -> {
					calendar.setMonth(9);
				}));

		assertThat(literal("---29", CoreDatatype.XSD.GDAY).calendarValue())
				.isInstanceOf(type)
				.isEqualTo(setup.apply(calendar -> {
					calendar.setDay(29);
				}));

		assertThatIllegalArgumentException().as("not normalized")
				.isThrownBy(() -> literal("\t100", CoreDatatype.XSD.DATETIME).calendarValue());

		assertThatIllegalArgumentException().as("malformed")
				.isThrownBy(() -> literal("malformed", CoreDatatype.XSD.DATETIME).calendarValue());

	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test
	public void testCoreDatatypeEqualsAndHashCode() {

		Literal plain = literal("plain");
		Literal tagged = literal("tagged", "en");
		Literal typed = literal("typed", datatype("http://example.org/datatype"));

		Literal _plain = literal(plain.getLabel());
		Literal _tagged = literal(tagged.getLabel(), tagged.getLanguage().orElse(""));
		Literal _typed = literal(typed.getLabel(), typed.getDatatype());

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
		assertThat(typed).isNotEqualTo(literal(typed.getLabel(), "http://example.org/other"));

		// hashCode() should return identical values for literals for which equals() is true

		assertThat(plain.hashCode()).isEqualTo(_plain.hashCode());
		assertThat(tagged.hashCode()).isEqualTo(_tagged.hashCode());
		assertThat(typed.hashCode()).isEqualTo(_typed.hashCode());

		assertThat(tagged.hashCode())
				.as("computed according to contract")
				.isEqualTo(tagged.getLabel().hashCode()); // !!! label >> label+language+datatype

	}

	@Test
	public final void testCoreDatatypeEqualsAndHashCodeCaseInsensitiveLanguage() {

		Literal lowercase = literal("label", "en");
		Literal uppercase = literal("label", "EN");

		assertThat(lowercase).isEqualTo(uppercase);
		assertThat(lowercase.hashCode()).isEqualTo(uppercase.hashCode());
	}

	@Test
	public final void testCoreDatatypeEqualsAndHashCodeXSDString() {

		// in RDF 1.1, there is no distinction between plain and string-typed literals

		Literal plain = literal("label");
		Literal typed = literal("label", CoreDatatype.XSD.STRING);

		assertThat(plain).isEqualTo(typed);
		assertThat(plain.hashCode()).isEqualTo(typed.hashCode());
	}

	@Test
	public final void testSerializationWithCoreDatatypeXsd() {
		Literal literal = literal("1", datatype(XSD_INT));

		byte[] bytes = objectToBytes(literal);
		Literal roundTrip = (Literal) bytesToObject(bytes);

		assertEquals(CoreDatatype.XSD.INT, roundTrip.getCoreDatatype());
	}

	@Test
	public final void testSerializationWithCoreDatatypeRdfLangString() {
		Literal literal = literal("hei", "no");
		assertEquals(CoreDatatype.RDF.LANGSTRING, literal.getCoreDatatype());

		byte[] bytes = objectToBytes(literal);
		Literal roundTrip = (Literal) bytesToObject(bytes);

		assertEquals(CoreDatatype.RDF.LANGSTRING, roundTrip.getCoreDatatype());
	}

	@Test
	public final void testSerializationWithCoreDatatypeGEO() {
		Literal literal = literal("1", CoreDatatype.GEO.WKT_LITERAL);

		byte[] bytes = objectToBytes(literal);
		Literal roundTrip = (Literal) bytesToObject(bytes);

		assertEquals(CoreDatatype.GEO.WKT_LITERAL, roundTrip.getCoreDatatype());
	}

	@Test
	public final void testSerializationWithCoreDatatype4() {
		Literal literal = literal("1", datatype("http://example.org/dt1"));
		assertEquals(CoreDatatype.XSD.NONE, literal.getCoreDatatype());

		byte[] bytes = objectToBytes(literal);
		Literal roundTrip = (Literal) bytesToObject(bytes);

		assertEquals(CoreDatatype.XSD.NONE, roundTrip.getCoreDatatype());
	}

	private byte[] objectToBytes(Serializable object) {
		try (var byteArrayOutputStream = new ByteArrayOutputStream()) {
			try (var objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
				objectOutputStream.writeObject(object);
			}
			return byteArrayOutputStream.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Object bytesToObject(byte[] str) {
		try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(str)) {
			try (ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
				return objectInputStream.readObject();
			}
		} catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}
