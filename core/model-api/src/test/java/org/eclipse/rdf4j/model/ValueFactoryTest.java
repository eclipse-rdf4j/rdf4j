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

package org.eclipse.rdf4j.model;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.SECOND_OF_DAY;
import static java.time.temporal.ChronoUnit.HALF_DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.YEARS;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_BOOLEAN;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_BYTE;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_DATE;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_DATETIME;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_DECIMAL;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_DOUBLE;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_DURATION;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_FLOAT;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_GDAY;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_GMONTH;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_GMONTHDAY;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_GYEAR;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_GYEARMONTH;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_INT;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_INTEGER;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_LONG;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_SHORT;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_TIME;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Date;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.Test;

/**
 * Abstract {@link ValueFactory} test suite.
 *
 * @author Alessandro Bollini
 * @author jeen
 * @since 3.5.0
 */
public abstract class ValueFactoryTest {

	/**
	 * Creates a test value factory instance.
	 *
	 * @return a new instance of the concrete value factory class under test
	 */
	protected abstract ValueFactory factory();

	@Test
	public void testCreateBNode() {

		final BNode bnode = factory().createBNode();

		assertThat(bnode).isNotNull();
		assertThat(bnode.getID()).isNotNull();
	}

	@Test
	public void testCreateLiteralBoolean() {

		final Literal _true = factory().createLiteral(true);

		assertThat(_true).isNotNull();
		assertThat(_true.booleanValue()).isTrue();
		assertThat(_true.getLabel()).isEqualTo("true");
		assertThat(_true.getDatatype().stringValue()).isEqualTo(XSD_BOOLEAN);

		final Literal _false = factory().createLiteral(false);

		assertThat(_false).isNotNull();
		assertThat(_false.booleanValue()).isFalse();
		assertThat(_false.getLabel()).isEqualTo("false");
		assertThat(_false.getDatatype().stringValue()).isEqualTo(XSD_BOOLEAN);
	}

	@Test
	public void testCreateLiteralByte() {

		final byte value = 42;

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.byteValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo("42");
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_BYTE);
	}

	@Test
	public void testCreateLiteralShort() {

		final short value = 42;

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.shortValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo("42");
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_SHORT);
	}

	@Test
	public void testCreateLiteralInt() {

		final int value = 42;

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.intValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo("42");
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_INT);
	}

	@Test
	public void testCreateLiteralLong() {

		final long value = 42L;

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.longValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo("42");
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_LONG);
	}

	@Test
	public void testCreateLiteralFloat() {

		final float value = 42.0f;
		final ValueFactory factory = factory();

		final Literal literal = factory.createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.floatValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo("42.0");
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_FLOAT);

		assertThat(factory.createLiteral(Float.POSITIVE_INFINITY).getLabel()).isEqualTo("INF");
		assertThat(factory.createLiteral(Float.NEGATIVE_INFINITY).getLabel()).isEqualTo("-INF");
		assertThat(factory.createLiteral(Float.NaN).getLabel()).isEqualTo("NaN");

	}

	@Test
	public void testCreateLiteralDouble() {

		final double value = 42.0d;
		final ValueFactory factory = factory();

		final Literal literal = factory.createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.doubleValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo("42.0");
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_DOUBLE);

		assertThat(factory.createLiteral(Double.POSITIVE_INFINITY).getLabel()).isEqualTo("INF");
		assertThat(factory.createLiteral(Double.NEGATIVE_INFINITY).getLabel()).isEqualTo("-INF");
		assertThat(factory.createLiteral(Double.NaN).getLabel()).isEqualTo("NaN");

	}

	@Test
	public void testCreateLiteralInteger() {

		final BigInteger value = new BigInteger("42");

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.integerValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo("42");
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_INTEGER);
	}

	@Test
	public void testCreateLiteralDecimal() {

		final BigDecimal value = new BigDecimal("42.0");

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.decimalValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo("42.0");
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_DECIMAL);
	}

	@Test
	public void testCreateLiteralTemporalNull() {
		assertThatNullPointerException().isThrownBy(() -> factory().createLiteral((TemporalAccessor) null));
	}

	@Test
	public void testCreateLiteralTemporalLocalDateTime() {

		final LocalDateTime value = LocalDateTime.parse("2020-09-30T01:02:03.004");

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.temporalAccessorValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo(value.toString());
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_DATETIME);

	}

	@Test
	public void testCreateLiteralTemporalOffsetDateTime() {

		final OffsetDateTime value = OffsetDateTime.parse("2020-09-30T01:02:03.004Z");

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.temporalAccessorValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo(value.toString());
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_DATETIME);

	}

	@Test
	public void testCreateLiteralTemporalLocalTime() {

		final LocalTime value = LocalTime.parse("01:02:03.004");

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.temporalAccessorValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo(value.toString());
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_TIME);

	}

	@Test
	public void testCreateLiteralTemporalOffsetTime() {

		final OffsetTime value = OffsetTime.parse("01:02:03.004Z");

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.temporalAccessorValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo(value.toString());
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_TIME);

	}

	@Test
	public void testCreateLiteralTemporalLocalDate() {

		final LocalDate value = LocalDate.parse("2020-11-14");

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.temporalAccessorValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo(value.toString());
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_DATE);

	}

	@Test
	public void testCreateLiteralTemporalOffsetDate() {

		final String label = "2020-09-30Z";

		final TemporalAccessor value = new DateTimeFormatterBuilder() // OffsetDate not supported by java.time
				.append(DateTimeFormatter.ISO_LOCAL_DATE)
				.appendOffsetId()
				.toFormatter()
				.parse(label);

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.temporalAccessorValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo(label);
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_DATE);

	}

	@Test
	public void testCreateLiteralTemporalYearMonth() {

		final YearMonth value = YearMonth.parse("2020-09");

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.temporalAccessorValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo(value.toString());
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_GYEARMONTH);

	}

	@Test
	public void testCreateLiteralTemporalYear() {

		final Year value = Year.parse("2020");

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.temporalAccessorValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo(value.toString());
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_GYEAR);

	}

	@Test
	public void testCreateLiteralTemporalMonthDay() {

		final MonthDay value = MonthDay.parse("--11-14");

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.temporalAccessorValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo(value.toString());
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_GMONTHDAY);

	}

	@Test
	public void testCreateLiteralTemporalDay() {

		final TemporalAccessor value = new TemporalAccessor() {

			@Override
			public boolean isSupported(TemporalField field) {
				return field.equals(DAY_OF_MONTH);
			}

			@Override
			public long getLong(TemporalField field) {
				if (field == DAY_OF_MONTH) {
					return 14;
				} else {
					throw new UnsupportedTemporalTypeException(field.toString());
				}
			}

		};

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.temporalAccessorValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo("---14");
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_GDAY);

	}

	@Test
	public void testCreateLiteralTemporalMonth() {

		final Month value = Month.NOVEMBER;

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.temporalAccessorValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo("--11");
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_GMONTH);

	}

	@Test
	public void testCreateLiteralTemporalAccessorUnsupported() {

		final TemporalAccessor accessor = new TemporalAccessor() {

			@Override
			public boolean isSupported(TemporalField field) {
				return field.equals(SECOND_OF_DAY);
			}

			@Override
			public long getLong(TemporalField field) {
				if (field == SECOND_OF_DAY) {
					return 123;
				} else {
					throw new UnsupportedTemporalTypeException(field.toString());
				}
			}

		};

		assertThatIllegalArgumentException().isThrownBy(() -> factory().createLiteral(accessor));

	}

	@Test
	public void testCreateLiteralTemporalPeriod() {

		final Period value = Period.parse("P1Y");

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.temporalAmountValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo(value.toString());
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_DURATION);

	}

	@Test
	public void testCreateLiteralTemporalDuration() {

		final Duration value = Duration.ofSeconds(7);

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.temporalAmountValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo(value.toString());
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_DURATION);

	}

	@Test
	public void testCreateLiteralTemporalAmountUnsupported() {

		class TestAmount implements TemporalAmount {

			private final List<TemporalUnit> units;

			private TestAmount(ChronoUnit... units) {
				this.units = unmodifiableList(asList(units));
			}

			@Override
			public long get(TemporalUnit unit) {
				return unit.isDateBased() ? 1 : -1;
			}

			@Override
			public List<TemporalUnit> getUnits() {
				return units;
			}

			@Override
			public Temporal addTo(Temporal temporal) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Temporal subtractFrom(Temporal temporal) {
				throw new UnsupportedOperationException();
			}

		}

		assertThatIllegalArgumentException().as("unsupported components")
				.isThrownBy(() -> factory().createLiteral(new TestAmount(HALF_DAYS)));

		assertThatIllegalArgumentException().as("mixed sign components")
				.isThrownBy(() -> factory().createLiteral(new TestAmount(YEARS, HOURS)));

	}

	@Test
	public void testCreateLiteralXMLGregorianCalendar() throws DatatypeConfigurationException {

		final XMLGregorianCalendar value = DatatypeFactory.newInstance()
				.newXMLGregorianCalendar("2020-09-30T01:02:03.004Z");

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.calendarValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo(value.toString());
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_DATETIME);

	}

	@Test
	public void testCreateLiteralDate() throws DatatypeConfigurationException {

		final Date date = new Date(2020, 9, 30, 1, 2, 3);
		final String string = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(date);

		final Literal literal = factory().createLiteral(date);

		assertThat(literal).isNotNull();
		assertThat(literal.calendarValue()).isEqualTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(string));
		assertThat(literal.getLabel()).isEqualTo(string);
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_DATETIME);

	}

}
