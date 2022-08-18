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

package org.eclipse.rdf4j.model.base;

import static java.lang.Math.abs;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.OFFSET_SECONDS;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.time.temporal.ChronoUnit.YEARS;
import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;

/**
 * Base class for {@link Literal}, offering common functionality.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
@SuppressWarnings("UseOfObsoleteDateTimeApi")
public abstract class AbstractLiteral implements Literal {

	private static final long serialVersionUID = -1286527360744086451L;

	static boolean reserved(IRI datatype) {
		return CoreDatatype.RDF.LANGSTRING.getIri().equals(datatype);
	}

	static boolean reserved(CoreDatatype datatype) {
		return CoreDatatype.RDF.LANGSTRING == datatype;
	}

	/**
	 * Converts this literal to a value.
	 *
	 * @param mapper a function mapping from the label of this literal to its value; returns a {@code null} value or
	 *               throws an {@code IllegalArgumentException} if the label of this literal doesn't represent a value
	 *               of the expected type
	 * @param <V>    the expected value type
	 *
	 * @return the value returned by {@code mapper}
	 *
	 * @throws NullPointerException if {@code mapper} is {@code null}
	 */
	private <V> V value(Function<String, V> mapper) {
		return Optional
				.of(getLabel())
				.map(requireNonNull(mapper, "null mapper"))
				.orElseThrow(() -> new IllegalArgumentException("malformed value"));
	}

	@Override
	public String stringValue() {
		return getLabel();
	}

	@Override
	public boolean booleanValue() {
		return value(BooleanLiteral::parseBoolean);
	}

	@Override
	public byte byteValue() {
		return value(Byte::parseByte);
	}

	@Override
	public short shortValue() {
		return value(Short::parseShort);
	}

	@Override
	public int intValue() {
		return value(Integer::parseInt);
	}

	@Override
	public long longValue() {
		return value(Long::parseLong);
	}

	@Override
	public float floatValue() {
		return value(NumberLiteral::parseFloat);
	}

	@Override
	public double doubleValue() {
		return value(NumberLiteral::parseDouble);
	}

	@Override
	public BigInteger integerValue() {
		return value(BigInteger::new);
	}

	@Override
	public BigDecimal decimalValue() {
		return value(BigDecimal::new);
	}

	@Override
	public TemporalAccessor temporalAccessorValue() throws DateTimeException {
		return value(TemporalAccessorLiteral::parseTemporalAccessor);
	}

	@Override
	public TemporalAmount temporalAmountValue() throws DateTimeException {
		return value(TemporalAmountLiteral::parseTemporalAmount);
	}

	@Override
	public XMLGregorianCalendar calendarValue() {
		return value(CalendarLiteral::parseCalendar);
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof Literal
				&& getLabel().equals(((Literal) o).getLabel())
				&& getDatatype().equals(((Literal) o).getDatatype())
				&& equals(getLanguage(), ((Literal) o).getLanguage());
	}

	@Override
	public int hashCode() {
		return getLabel().hashCode();
	}

	@Override
	public String toString() {

		final String label = '"' + getLabel() + '"';

		return getLanguage()

				.map(language -> label + '@' + language)

				.orElseGet(() -> {

					final IRI datatype = getDatatype();

					return datatype.equals(CoreDatatype.XSD.STRING) ? label
							: label + "^^<" + datatype.stringValue() + ">";

				});
	}

	private boolean equals(Optional<String> x, Optional<String> y) {

		final boolean px = x.isPresent();
		final boolean py = y.isPresent();

		return px && py && x.get().equalsIgnoreCase(y.get()) || !px && !py;
	}

	static class TypedLiteral extends AbstractLiteral {

		private static final long serialVersionUID = -19640527584237291L;

		private final String label;
		private final CoreDatatype coreDatatype;
		private final IRI datatype;

		TypedLiteral(String label) {
			this.label = label;
			this.coreDatatype = CoreDatatype.XSD.STRING;
			this.datatype = CoreDatatype.XSD.STRING.getIri();
		}

		TypedLiteral(String label, IRI datatype) {
			this.label = label;
			if (datatype == null) {
				this.datatype = CoreDatatype.XSD.STRING.getIri();
				this.coreDatatype = CoreDatatype.XSD.STRING;
			} else {
				this.datatype = datatype;
				this.coreDatatype = CoreDatatype.from(datatype);
			}
		}

		TypedLiteral(String label, CoreDatatype datatype) {
			this.label = label;
			this.coreDatatype = Objects.requireNonNull(datatype);
			this.datatype = datatype.getIri();
		}

		TypedLiteral(String label, IRI datatype, CoreDatatype coreDatatype) {
			assert datatype != null;
			assert coreDatatype != null;
			assert coreDatatype == CoreDatatype.NONE || datatype == coreDatatype.getIri();

			this.label = label;
			this.datatype = datatype;
			this.coreDatatype = coreDatatype;
		}

		@Override
		public String getLabel() {
			return label;
		}

		@Override
		public Optional<String> getLanguage() {
			return Optional.empty();
		}

		@Override
		public IRI getDatatype() {
			return datatype;
		}

		@Override
		public CoreDatatype getCoreDatatype() {
			return coreDatatype;
		}
	}

	static class TaggedLiteral extends AbstractLiteral {

		private static final long serialVersionUID = -19640527584237291L;

		private final String label;
		private final String language;

		TaggedLiteral(String label, String language) {
			this.label = label;
			this.language = language;
		}

		@Override
		public String getLabel() {
			return label;
		}

		@Override
		public Optional<String> getLanguage() {
			return Optional.of(language);
		}

		@Override
		public IRI getDatatype() {
			return CoreDatatype.RDF.LANGSTRING.getIri();
		}

		@Override
		public CoreDatatype.RDF getCoreDatatype() {
			return CoreDatatype.RDF.LANGSTRING;
		}
	}

	static class BooleanLiteral extends AbstractLiteral {

		private static final long serialVersionUID = -1162147873619834622L;

		static Boolean parseBoolean(String label) {
			return Optional.of(label)

					.map(String::trim)

					.map(normalized -> normalized.equals("true") || normalized.equals("1") ? Boolean.TRUE
							: normalized.equals("false") || normalized.equals("0") ? Boolean.FALSE
									: null
					)

					.orElse(null);
		}

		private final boolean value;

		BooleanLiteral(boolean value) {
			this.value = value;
		}

		@Override
		public String getLabel() {
			return value ? "true" : "false";
		}

		@Override
		public Optional<String> getLanguage() {
			return Optional.empty();
		}

		@Override
		public IRI getDatatype() {
			return CoreDatatype.XSD.BOOLEAN.getIri();
		}

		@Override
		public CoreDatatype.XSD getCoreDatatype() {
			return CoreDatatype.XSD.BOOLEAN;
		}

		@Override
		public boolean booleanValue() {
			return value;
		}

	}

	static class NumberLiteral extends AbstractLiteral {

		private static final long serialVersionUID = -3201912818064851702L;

		private static final String POSITIVE_INFINITY = "INF";
		private static final String NEGATIVE_INFINITY = "-INF";
		private static final String NAN = "NaN";

		static float parseFloat(String label) {
			return label.equals(POSITIVE_INFINITY) ? Float.POSITIVE_INFINITY
					: label.equals(NEGATIVE_INFINITY) ? Float.NEGATIVE_INFINITY
							: label.equals(NAN) ? Float.NaN
									: Float.parseFloat(label);
		}

		static double parseDouble(String label) {
			return label.equals(POSITIVE_INFINITY) ? Double.POSITIVE_INFINITY
					: label.equals(NEGATIVE_INFINITY) ? Double.NEGATIVE_INFINITY
							: label.equals(NAN) ? Double.NaN
									: Double.parseDouble(label);
		}

		private static String toString(float value) {
			return value == Float.POSITIVE_INFINITY ? POSITIVE_INFINITY
					: value == Float.NEGATIVE_INFINITY ? NEGATIVE_INFINITY
							: Float.isNaN(value) ? NAN
									: Float.toString(value);
		}

		private static String toString(double value) {
			return value == Double.POSITIVE_INFINITY ? POSITIVE_INFINITY
					: value == Double.NEGATIVE_INFINITY ? NEGATIVE_INFINITY
							: Double.isNaN(value) ? NAN
									: Double.toString(value);
		}

		protected Number value;

		private final String label;
		private final CoreDatatype.XSD datatype;

		NumberLiteral(byte value) {
			this(value, Byte.toString(value), CoreDatatype.XSD.BYTE);
		}

		NumberLiteral(short value) {
			this(value, Short.toString(value), CoreDatatype.XSD.SHORT);
		}

		NumberLiteral(int value) {
			this(value, Integer.toString(value), CoreDatatype.XSD.INT);
		}

		NumberLiteral(long value) {
			this(value, Long.toString(value), CoreDatatype.XSD.LONG);
		}

		NumberLiteral(float value) {
			this(value, toString(value), CoreDatatype.XSD.FLOAT);
		}

		NumberLiteral(double value) {
			this(value, toString(value), CoreDatatype.XSD.DOUBLE);
		}

		NumberLiteral(Number value, String label, CoreDatatype.XSD datatype) {
			this.value = value;
			this.label = label;
			this.datatype = datatype;
		}

		@Override
		public String getLabel() {
			return label;
		}

		@Override
		public Optional<String> getLanguage() {
			return Optional.empty();
		}

		@Override
		public IRI getDatatype() {
			return datatype.getIri();
		}

		@Override
		public byte byteValue() {
			return value.byteValue();
		}

		@Override
		public short shortValue() {
			return value.shortValue();
		}

		@Override
		public int intValue() {
			return value.intValue();
		}

		@Override
		public long longValue() {
			return value.longValue();
		}

		@Override
		public float floatValue() {
			return value.floatValue();
		}

		@Override
		public double doubleValue() {
			return value.doubleValue();
		}

		@Override
		public CoreDatatype getCoreDatatype() {
			return datatype;
		}
	}

	static class IntegerLiteral extends NumberLiteral {

		private static final long serialVersionUID = -4274941248972496665L;

		IntegerLiteral(BigInteger value) {
			super(value, value.toString(), CoreDatatype.XSD.INTEGER);
		}

		@Override
		public BigInteger integerValue() {
			return (BigInteger) value;
		}

		@Override
		public BigDecimal decimalValue() {
			return new BigDecimal((BigInteger) value);
		}

	}

	static class DecimalLiteral extends NumberLiteral {

		private static final long serialVersionUID = -4382147098035463886L;

		DecimalLiteral(BigDecimal value) {
			super(value, value.toPlainString(), CoreDatatype.XSD.DECIMAL);
		}

		@Override
		public BigInteger integerValue() {
			return ((BigDecimal) value).toBigInteger();
		}

		@Override
		public BigDecimal decimalValue() {
			return (BigDecimal) value;
		}

	}

	static class TemporalAccessorLiteral extends AbstractLiteral {

		private static final long serialVersionUID = -6089251668767105663L;

		private static final ChronoField[] FIELDS = {
				YEAR, MONTH_OF_YEAR, DAY_OF_MONTH,
				HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE, NANO_OF_SECOND,
				OFFSET_SECONDS
		};

		private static final DateTimeFormatter LOCAL_TIME_FORMATTER = new DateTimeFormatterBuilder()

				.appendValue(HOUR_OF_DAY, 2)
				.appendLiteral(':')
				.appendValue(MINUTE_OF_HOUR, 2)
				.appendLiteral(':')
				.appendValue(SECOND_OF_MINUTE, 2)

				.optionalStart()
				.appendFraction(NANO_OF_SECOND, 1, 9, true)

				.toFormatter();

		private static final DateTimeFormatter OFFSET_TIME_FORMATTER = new DateTimeFormatterBuilder()

				.append(LOCAL_TIME_FORMATTER)

				.optionalStart()
				.appendOffsetId()

				.toFormatter();

		private static final DateTimeFormatter LOCAL_DATE_FORMATTER = new DateTimeFormatterBuilder()

				.appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)

				.optionalStart()
				.appendLiteral('-')
				.appendValue(MONTH_OF_YEAR, 2)

				.optionalStart()
				.appendLiteral('-')
				.appendValue(DAY_OF_MONTH, 2)

				.toFormatter();

		private static final DateTimeFormatter OFFSET_DATE_FORMATTER = new DateTimeFormatterBuilder()

				.append(LOCAL_DATE_FORMATTER)

				.optionalStart()
				.appendOffsetId()

				.toFormatter();

		private static final DateTimeFormatter DATETIME_FORMATTER = new DateTimeFormatterBuilder()

				.append(LOCAL_DATE_FORMATTER)

				.optionalStart()
				.appendLiteral('T')
				.append(LOCAL_TIME_FORMATTER)
				.optionalEnd()

				.optionalStart()
				.appendOffsetId()

				.toFormatter();

		private static final DateTimeFormatter DASH_FORMATTER = new DateTimeFormatterBuilder()

				.appendLiteral("--")

				.optionalStart()
				.appendValue(MONTH_OF_YEAR, 2)
				.optionalEnd()

				.optionalStart()
				.appendLiteral('-')
				.appendValue(DAY_OF_MONTH, 2)

				.toFormatter();

		private static final Map<Integer, CoreDatatype.XSD> DATATYPES = datatypes();
		private static final Map<CoreDatatype.XSD, DateTimeFormatter> FORMATTERS = formatters();

		static TemporalAccessor parseTemporalAccessor(String label) throws DateTimeException {

			TemporalAccessor value = formatter(label).parse(label);

			if (!DATATYPES.containsKey(key(value))) {
				throw new DateTimeException(String.format(
						"label <%s> is not a valid lexical representation of an XML Schema date/time datatype", label
				));
			}

			return value;
		}

		private static Map<Integer, CoreDatatype.XSD> datatypes() {

			int date = key(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
			int time = key(HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE);
			int nano = key(NANO_OF_SECOND);
			int zone = key(OFFSET_SECONDS);

			Map<Integer, CoreDatatype.XSD> datatypes = new HashMap<>();

			datatypes.put(date + time, CoreDatatype.XSD.DATETIME);
			datatypes.put(date + time + nano, CoreDatatype.XSD.DATETIME);
			datatypes.put((date + time + zone), CoreDatatype.XSD.DATETIME);
			datatypes.put((date + time + nano + zone), CoreDatatype.XSD.DATETIME);

			datatypes.put(time, CoreDatatype.XSD.TIME);
			datatypes.put(time + nano, CoreDatatype.XSD.TIME);
			datatypes.put(time + zone, CoreDatatype.XSD.TIME);
			datatypes.put(time + nano + zone, CoreDatatype.XSD.TIME);

			datatypes.put(date, CoreDatatype.XSD.DATE);
			datatypes.put(date + zone, CoreDatatype.XSD.DATE);

			datatypes.put(key(YEAR, MONTH_OF_YEAR), CoreDatatype.XSD.GYEARMONTH);
			datatypes.put(key(YEAR), CoreDatatype.XSD.GYEAR);
			datatypes.put(key(MONTH_OF_YEAR, DAY_OF_MONTH), CoreDatatype.XSD.GMONTHDAY);
			datatypes.put(key(DAY_OF_MONTH), CoreDatatype.XSD.GDAY);
			datatypes.put(key(MONTH_OF_YEAR), CoreDatatype.XSD.GMONTH);

			return datatypes;
		}

		private static Map<CoreDatatype.XSD, DateTimeFormatter> formatters() {

			final Map<CoreDatatype.XSD, DateTimeFormatter> formatters = new EnumMap<>(CoreDatatype.XSD.class);

			formatters.put(CoreDatatype.XSD.DATETIME, DATETIME_FORMATTER);
			formatters.put(CoreDatatype.XSD.TIME, OFFSET_TIME_FORMATTER);
			formatters.put(CoreDatatype.XSD.DATE, OFFSET_DATE_FORMATTER);

			formatters.put(CoreDatatype.XSD.GYEARMONTH, LOCAL_DATE_FORMATTER);
			formatters.put(CoreDatatype.XSD.GYEAR, LOCAL_DATE_FORMATTER);
			formatters.put(CoreDatatype.XSD.GMONTHDAY, DASH_FORMATTER);
			formatters.put(CoreDatatype.XSD.GDAY, DASH_FORMATTER);
			formatters.put(CoreDatatype.XSD.GMONTH, DASH_FORMATTER);

			return formatters;
		}

		private static DateTimeFormatter formatter(String label) {
			if (label.startsWith("--")) {

				return DASH_FORMATTER;

			} else if (label.length() >= 8 && label.charAt(2) == ':') {

				return OFFSET_TIME_FORMATTER;

			} else {

				return DATETIME_FORMATTER;

			}
		}

		private static int key(TemporalAccessor value) {
			return key(value::isSupported, FIELDS);
		}

		private static int key(ChronoField... fields) {
			return key(field -> true, fields);
		}

		private static int key(Predicate<ChronoField> include, ChronoField... fields) {

			int index = 0;

			for (ChronoField field : fields) {
				if (include.test(field)) {
					index += 1 << field.ordinal() + 1;
				}
			}

			return index;
		}

		private final TemporalAccessor value;

		private final String label;
		private final CoreDatatype.XSD datatype;

		TemporalAccessorLiteral(TemporalAccessor value) {

			this.value = value;

			datatype = DATATYPES.get(key(value));

			if (datatype == null) {
				throw new IllegalArgumentException(String.format(
						"value <%s> cannot be represented by an XML Schema date/time datatype", value
				));
			}

			this.label = FORMATTERS.get(datatype).format(value);
		}

		@Override
		public String getLabel() {
			return label;
		}

		@Override
		public Optional<String> getLanguage() {
			return Optional.empty();
		}

		@Override
		public IRI getDatatype() {
			return datatype.getIri();
		}

		@Override
		public TemporalAccessor temporalAccessorValue() {
			return value;
		}

		@Override
		public CoreDatatype getCoreDatatype() {
			return datatype;
		}

	}

	static class TemporalAmountLiteral extends AbstractLiteral {

		private static final long serialVersionUID = -447302801371093467L;

		private static final Collection<ChronoUnit> UNITS = EnumSet.of(
				YEARS, MONTHS, DAYS, HOURS, MINUTES, SECONDS, NANOS
		);

		private static final Pattern PATTERN = Pattern.compile("(?<sign>-)?" +
				"P" +
				"(?:(?<" + YEARS + ">\\d+)Y)?" +
				"(?:(?<" + MONTHS + ">\\d+)M)?" +
				"(?:(?<" + DAYS + ">\\d+)D)?" +
				"(?<time>T)?" +
				"(?:(?<" + HOURS + ">\\d+)H)?" +
				"(?:(?<" + MINUTES + ">\\d+)M)?" +
				"(?:(?<" + SECONDS + ">\\d+)(?:\\.(?<" + NANOS + ">\\d+))?S)?"
		);

		private final TemporalAmount value;

		private final String label;

		TemporalAmountLiteral(TemporalAmount value) {

			final List<TemporalUnit> units = value.getUnits();

			if (units.isEmpty() || !UNITS.containsAll(units)) {
				throw new IllegalArgumentException(String.format(
						"value <%s> cannot be represented by an XML Schema duration datatype", value
				));
			}

			this.value = value;
			this.label = toString(value);
		}

		static TemporalAmount parseTemporalAmount(CharSequence label) {

			final Matcher matcher = PATTERN.matcher(label);

			if (!matcher.matches()) {
				throw new DateTimeException(String.format(
						"label <%s> is not a valid lexical representation of an XML Schema duration datatype", label
				));
			}

			final boolean sign = matcher.group("sign") != null;
			final boolean time = matcher.group("time") != null;

			final Map<ChronoUnit, Long> components = new EnumMap<>(ChronoUnit.class);

			for (final ChronoUnit unit : UNITS) {

				final String group = matcher.group(unit.toString());

				if (group != null) {
					try {

						final long value = Long.parseUnsignedLong(unit == NANOS
								? (group + "000000000").substring(0, 9)
								: group
						);

						components.put(unit, sign ? -value : value);

					} catch (final NumberFormatException e) {

						throw new DateTimeParseException(e.getMessage(), group, matcher.start(unit.toString()));

					}
				}

			}

			if (components.isEmpty() || time && Stream.of(HOURS, MINUTES, SECONDS).noneMatch(components::containsKey)) {
				throw new DateTimeException(String.format(
						"label <%s> is not a valid lexical representation of an XML Schema duration datatype", label
				));
			}

			return new ComponentTemporalAmount(components);

		}

		private static String toString(TemporalAmount value) {

			final Collection<TemporalUnit> units = value.getUnits();

			final long years = units.contains(YEARS) ? value.get(YEARS) : 0L;
			final long months = units.contains(MONTHS) ? value.get(MONTHS) : 0L;
			final long days = units.contains(DAYS) ? value.get(DAYS) : 0L;

			final long hours = units.contains(HOURS) ? value.get(HOURS) : 0L;
			final long minutes = units.contains(MINUTES) ? value.get(MINUTES) : 0L;
			final long seconds = units.contains(SECONDS) ? value.get(SECONDS) : 0L;
			final long nanos = units.contains(NANOS) ? value.get(NANOS) : 0L;

			final boolean positive = years > 0 || months > 0 || days > 0
					|| hours > 0 || minutes > 0 || seconds > 0 || nanos > 0;

			final boolean negative = years < 0 || months < 0 || days < 0
					|| hours < 0 || minutes < 0 || seconds < 0 || nanos < 0;

			if (positive && negative) {
				throw new IllegalArgumentException(String.format(
						"value <%s> cannot be represented by an XML Schema duration datatype", value
				));
			}

			final StringBuilder builder = new StringBuilder(3 * value.getUnits().size());

			if (negative) {
				builder.append('-');
			}

			builder.append("P");

			if (years != 0L) {
				builder.append(abs(years)).append("Y");
			}

			if (months != 0L) {
				builder.append(abs(months)).append("M");
			}

			if (days != 0L) {
				builder.append(abs(days)).append("D");
			}

			if (hours != 0L || minutes != 0L || seconds != 0L || nanos != 0L) {
				builder.append("T");
			}

			if (hours != 0L) {
				builder.append(abs(hours)).append("H");
			}

			if (minutes != 0L) {
				builder.append(abs(minutes)).append("M");
			}

			if (nanos != 0L) {

				builder.append(abs(seconds) + abs(nanos) / 1_000_000_000L)
						.append('.')
						.append(String.format("%09d", abs(nanos) % 1_000_000_000L))
						.append("S");

			} else if (seconds != 0L) {

				builder.append(abs(seconds)).append("S");

			}

			return builder.toString();
		}

		@Override
		public String getLabel() {
			return label;
		}

		@Override
		public Optional<String> getLanguage() {
			return Optional.empty();
		}

		@Override
		public IRI getDatatype() {
			return CoreDatatype.XSD.DURATION.getIri();
		}

		@Override
		public TemporalAmount temporalAmountValue() throws DateTimeException {
			return value;
		}

		@Override
		public CoreDatatype.XSD getCoreDatatype() {
			return CoreDatatype.XSD.DURATION;
		}

	}

	static class CalendarLiteral extends AbstractLiteral {

		private static final long serialVersionUID = 9131700079460615839L;

		private static final ThreadLocal<DatatypeFactory> DATATYPE_FACTORY = ThreadLocal.withInitial(() -> {
			try {

				return DatatypeFactory.newInstance(); // not guaranteed to be thread-safe

			} catch (DatatypeConfigurationException e) {

				throw new RuntimeException("unable to create datatype factory", e);

			}
		});

		private static final Map<QName, CoreDatatype.XSD> DATATYPES = datatypes();

		private static Map<QName, CoreDatatype.XSD> datatypes() {

			final Map<QName, CoreDatatype.XSD> datatypes = new HashMap<>();

			datatypes.put(DatatypeConstants.DATETIME, CoreDatatype.XSD.DATETIME);
			datatypes.put(DatatypeConstants.TIME, CoreDatatype.XSD.TIME);
			datatypes.put(DatatypeConstants.DATE, CoreDatatype.XSD.DATE);

			datatypes.put(DatatypeConstants.GYEARMONTH, CoreDatatype.XSD.GYEARMONTH);
			datatypes.put(DatatypeConstants.GYEAR, CoreDatatype.XSD.GYEAR);
			datatypes.put(DatatypeConstants.GMONTHDAY, CoreDatatype.XSD.GMONTHDAY);
			datatypes.put(DatatypeConstants.GDAY, CoreDatatype.XSD.GDAY);
			datatypes.put(DatatypeConstants.GMONTH, CoreDatatype.XSD.GMONTH);

			datatypes.put(DatatypeConstants.DURATION, CoreDatatype.XSD.DURATION);
			datatypes.put(DatatypeConstants.DURATION_DAYTIME, CoreDatatype.XSD.DAYTIMEDURATION);
			datatypes.put(DatatypeConstants.DURATION_YEARMONTH, CoreDatatype.XSD.YEARMONTHDURATION);

			return datatypes;
		}

		private static XMLGregorianCalendar parseCalendar(String label) {
			return DATATYPE_FACTORY.get().newXMLGregorianCalendar(label);
		}

		private final XMLGregorianCalendar value;

		private final String label;
		private final CoreDatatype.XSD datatype;

		CalendarLiteral(GregorianCalendar calendar) {
			this(DATATYPE_FACTORY.get().newXMLGregorianCalendar(calendar));
		}

		CalendarLiteral(XMLGregorianCalendar calendar) {

			this.value = calendar;

			this.label = calendar.toXMLFormat();
			QName qname = calendar.getXMLSchemaType();

			datatype = DATATYPES.get(qname);

			if (datatype == null) {
				throw new IllegalArgumentException(String.format(
						"QName <%s> cannot be mapped to an XML Schema date/time datatype", qname
				));
			}
		}

		@Override
		public String getLabel() {
			return label;
		}

		@Override
		public Optional<String> getLanguage() {
			return Optional.empty();
		}

		@Override
		public IRI getDatatype() {
			return datatype.getIri();
		}

		@Override
		public XMLGregorianCalendar calendarValue() {
			return value;
		}

		@Override
		public CoreDatatype getCoreDatatype() {
			return datatype;
		}
	}

}
