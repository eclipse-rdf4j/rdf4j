/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

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
import org.eclipse.rdf4j.model.base.AbstractIRI.GenericIRI;

/**
 * Base class for {@link Literal}, offering common functionality.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
@SuppressWarnings("UseOfObsoleteDateTimeApi")
public abstract class AbstractLiteral implements Literal {

	private static final long serialVersionUID = -1286527360744086451L;

	private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private static final String XSD = "http://www.w3.org/2001/XMLSchema#";

	private static final IRI RDF_LANGSTRING = new GenericIRI(RDF, "langString");

	private static final IRI XSD_STRING = new GenericIRI(XSD, "string");
	private static final IRI XSD_BOOLEAN = new GenericIRI(XSD, "boolean");

	private static final IRI XSD_BYTE = new GenericIRI(XSD, "byte");
	private static final IRI XSD_SHORT = new GenericIRI(XSD, "short");
	private static final IRI XSD_INT = new GenericIRI(XSD, "int");
	private static final IRI XSD_LONG = new GenericIRI(XSD, "long");
	private static final IRI XSD_FLOAT = new GenericIRI(XSD, "float");
	private static final IRI XSD_DOUBLE = new GenericIRI(XSD, "double");
	private static final IRI XSD_INTEGER = new GenericIRI(XSD, "integer");
	private static final IRI XSD_DECIMAL = new GenericIRI(XSD, "decimal");

	private static final IRI XSD_DATETIME = new GenericIRI(XSD, "dateTime");
	private static final IRI XSD_DATE = new GenericIRI(XSD, "date");
	private static final IRI XSD_TIME = new GenericIRI(XSD, "time");
	private static final IRI XSD_GYEARMONTH = new GenericIRI(XSD, "gYearMonth");
	private static final IRI XSD_GMONTHDAY = new GenericIRI(XSD, "gMonthDay");
	private static final IRI XSD_GYEAR = new GenericIRI(XSD, "gYear");
	private static final IRI XSD_GMONTH = new GenericIRI(XSD, "gMonth");
	private static final IRI XSD_GDAY = new GenericIRI(XSD, "gDay");
	private static final IRI XSD_DURATION = new GenericIRI(XSD, "duration");
	private static final IRI XSD_DURATION_DAYTIME = new GenericIRI(XSD, "dayTimeDuration");
	private static final IRI XSD_DURATION_YEARMONTH = new GenericIRI(XSD, "yearMonthDuration");

	static boolean reserved(IRI datatype) {
		return RDF_LANGSTRING.equals(datatype);
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

					return datatype.equals(XSD_STRING) ? label
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

		private String label;
		private IRI datatype;

		TypedLiteral(String label) {
			this.label = label;
			this.datatype = XSD_STRING;
		}

		TypedLiteral(String label, IRI datatype) {
			this.label = label;
			this.datatype = datatype != null ? datatype : XSD_STRING;
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

	}

	static class TaggedLiteral extends AbstractLiteral {

		private static final long serialVersionUID = -19640527584237291L;

		private String label;
		private String language;

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
			return RDF_LANGSTRING;
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

		private boolean value;

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
			return XSD_BOOLEAN;
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

		private String label;
		private IRI datatype;

		NumberLiteral(byte value) {
			this(value, Byte.toString(value), XSD_BYTE);
		}

		NumberLiteral(short value) {
			this(value, Short.toString(value), XSD_SHORT);
		}

		NumberLiteral(int value) {
			this(value, Integer.toString(value), XSD_INT);
		}

		NumberLiteral(long value) {
			this(value, Long.toString(value), XSD_LONG);
		}

		NumberLiteral(float value) {
			this(value, toString(value), XSD_FLOAT);
		}

		NumberLiteral(double value) {
			this(value, toString(value), XSD_DOUBLE);
		}

		NumberLiteral(Number value, String label, IRI datatype) {
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
			return datatype;
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

	}

	static class IntegerLiteral extends NumberLiteral {

		private static final long serialVersionUID = -4274941248972496665L;

		IntegerLiteral(BigInteger value) {
			super(value, value.toString(), XSD_INTEGER);
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
			super(value, value.toPlainString(), XSD_DECIMAL);
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

		private static final Map<Integer, IRI> DATATYPES = datatypes();
		private static final Map<IRI, DateTimeFormatter> FORMATTERS = formatters();

		static TemporalAccessor parseTemporalAccessor(String label) throws DateTimeException {

			final TemporalAccessor value = formatter(label).parse(label);

			if (!DATATYPES.containsKey(key(value))) {
				throw new DateTimeException(String.format(
						"label <%s> is not a valid lexical representation of an XML Schema date/time datatype", label
				));
			}

			return value;
		}

		private static Map<Integer, IRI> datatypes() {

			final Map<Integer, IRI> datatypes = new HashMap<>();

			final int date = key(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
			final int time = key(HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE);
			final int nano = key(NANO_OF_SECOND);
			final int zone = key(OFFSET_SECONDS);

			datatypes.put(date + time, XSD_DATETIME);
			datatypes.put(date + time + nano, XSD_DATETIME);
			datatypes.put((date + time + zone), XSD_DATETIME);
			datatypes.put((date + time + nano + zone), XSD_DATETIME);

			datatypes.put(time, XSD_TIME);
			datatypes.put(time + nano, XSD_TIME);
			datatypes.put(time + zone, XSD_TIME);
			datatypes.put(time + nano + zone, XSD_TIME);

			datatypes.put(date, XSD_DATE);
			datatypes.put(date + zone, XSD_DATE);

			datatypes.put(key(YEAR, MONTH_OF_YEAR), XSD_GYEARMONTH);
			datatypes.put(key(YEAR), XSD_GYEAR);
			datatypes.put(key(MONTH_OF_YEAR, DAY_OF_MONTH), XSD_GMONTHDAY);
			datatypes.put(key(DAY_OF_MONTH), XSD_GDAY);
			datatypes.put(key(MONTH_OF_YEAR), XSD_GMONTH);

			return datatypes;
		}

		private static Map<IRI, DateTimeFormatter> formatters() {

			final Map<IRI, DateTimeFormatter> formatters = new HashMap<>();

			formatters.put(XSD_DATETIME, DATETIME_FORMATTER);
			formatters.put(XSD_TIME, OFFSET_TIME_FORMATTER);
			formatters.put(XSD_DATE, OFFSET_DATE_FORMATTER);

			formatters.put(XSD_GYEARMONTH, LOCAL_DATE_FORMATTER);
			formatters.put(XSD_GYEAR, LOCAL_DATE_FORMATTER);
			formatters.put(XSD_GMONTHDAY, DASH_FORMATTER);
			formatters.put(XSD_GDAY, DASH_FORMATTER);
			formatters.put(XSD_GMONTH, DASH_FORMATTER);

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

		private TemporalAccessor value;

		private String label;
		private IRI datatype;

		TemporalAccessorLiteral(TemporalAccessor value) {

			this.value = value;

			final IRI datatype = DATATYPES.get(key(value));

			if (datatype == null) {
				throw new IllegalArgumentException(String.format(
						"value <%s> cannot be represented by an XML Schema date/time datatype", value
				));
			}

			this.label = FORMATTERS.get(datatype).format(value);
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
			return datatype;
		}

		@Override
		public TemporalAccessor temporalAccessorValue() {
			return value;
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

		private TemporalAmount value;

		private String label;

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
			return XSD_DURATION;
		}

		@Override
		public TemporalAmount temporalAmountValue() throws DateTimeException {
			return value;
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

		private static final Map<QName, IRI> DATATYPES = datatypes();

		private static Map<QName, IRI> datatypes() {

			final Map<QName, IRI> datatypes = new HashMap<>();

			datatypes.put(DatatypeConstants.DATETIME, XSD_DATETIME);
			datatypes.put(DatatypeConstants.TIME, XSD_TIME);
			datatypes.put(DatatypeConstants.DATE, XSD_DATE);

			datatypes.put(DatatypeConstants.GYEARMONTH, XSD_GYEARMONTH);
			datatypes.put(DatatypeConstants.GYEAR, XSD_GYEAR);
			datatypes.put(DatatypeConstants.GMONTHDAY, XSD_GMONTHDAY);
			datatypes.put(DatatypeConstants.GDAY, XSD_GDAY);
			datatypes.put(DatatypeConstants.GMONTH, XSD_GMONTH);

			datatypes.put(DatatypeConstants.DURATION, XSD_DURATION);
			datatypes.put(DatatypeConstants.DURATION_DAYTIME, XSD_DURATION_DAYTIME);
			datatypes.put(DatatypeConstants.DURATION_YEARMONTH, XSD_DURATION_YEARMONTH);

			return datatypes;
		}

		private static IRI datatype(QName qname) {

			final IRI datatype = DATATYPES.get(qname);

			if (datatype == null) {
				throw new IllegalArgumentException(String.format(
						"QName <%s> cannot be mapped to an XML Schema date/time datatype", qname
				));
			}

			return datatype;
		}

		private static XMLGregorianCalendar parseCalendar(String label) {
			return DATATYPE_FACTORY.get().newXMLGregorianCalendar(label);
		}

		private XMLGregorianCalendar value;

		private String label;
		private IRI datatype;

		CalendarLiteral(GregorianCalendar calendar) {
			this(DATATYPE_FACTORY.get().newXMLGregorianCalendar(calendar));
		}

		CalendarLiteral(XMLGregorianCalendar calendar) {

			this.value = calendar;

			this.label = calendar.toXMLFormat();
			this.datatype = datatype(calendar.getXMLSchemaType());
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
		public XMLGregorianCalendar calendarValue() {
			return value;
		}

	}

}
