/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import static java.util.Objects.requireNonNull;
import static org.eclipse.rdf4j.model.base.AbstractIRI.createIRI;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

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

	private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private static final String XSD = "http://www.w3.org/2001/XMLSchema#";

	private static final IRI RDF_LANG_STRING = createIRI(RDF, "langString");

	private static final IRI XSD_BOOLEAN = createIRI(XSD, "boolean");
	private static final IRI XSD_BYTE = createIRI(XSD, "byte");
	private static final IRI XSD_SHORT = createIRI(XSD, "short");
	private static final IRI XSD_INT = createIRI(XSD, "int");
	private static final IRI XSD_LONG = createIRI(XSD, "long");
	private static final IRI XSD_FLOAT = createIRI(XSD, "float");
	private static final IRI XSD_DOUBLE = createIRI(XSD, "double");
	private static final IRI XSD_INTEGER = createIRI(XSD, "integer");
	private static final IRI XSD_DECIMAL = createIRI(XSD, "decimal");
	private static final IRI XSD_STRING = createIRI(XSD, "string");

	private static final IRI XSD_DATETIME = createIRI(XSD, "dateTime");
	private static final IRI XSD_DATE = createIRI(XSD, "date");
	private static final IRI XSD_TIME = createIRI(XSD, "time");
	private static final IRI XSD_GYEARMONTH = createIRI(XSD, "gYearMonth");
	private static final IRI XSD_GMONTHDAY = createIRI(XSD, "gMonthDay");
	private static final IRI XSD_GYEAR = createIRI(XSD, "gYear");
	private static final IRI XSD_GMONTH = createIRI(XSD, "gMonth");
	private static final IRI XSD_GDAY = createIRI(XSD, "gDay");
	private static final IRI XSD_DURATION = createIRI(XSD, "duration");
	private static final IRI XSD_DURATION_DAYTIME = createIRI(XSD, "dayTimeDuration");
	private static final IRI XSD_DURATION_YEARMONTH = createIRI(XSD, "yearMonthDuration");

	private static final String POSITIVE_INFINITY = "INF";
	private static final String NEGATIVE_INFINITY = "-INF";
	private static final String NAN = "NaN";

	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

	private static final ThreadLocal<DatatypeFactory> DATATYPE_FACTORY = ThreadLocal.withInitial(() -> {
		try {

			return DatatypeFactory.newInstance(); // not guaranteed to be thread-safe

		} catch (DatatypeConfigurationException e) {

			throw new RuntimeException("unable to create datatype factory", e);

		}
	});

	/**
	 * Creates a new plain literal value.
	 *
	 * @param label the label of the literal
	 *
	 * @return a new generic {@code xsd:string} literal value
	 *
	 * @throws NullPointerException if {@code label} is {@code null}
	 */
	public static Literal createLiteral(String label) {

		if (label == null) {
			throw new NullPointerException("null label");
		}

		return new TypedLiteral(label, XSD_STRING);
	}

	/**
	 * Creates a new tagged literal value.
	 *
	 * @param label    the label of the literal
	 * @param language the language tag of the literal
	 *
	 * @return a new generic {@code rdf:langString} literal value
	 *
	 * @throws NullPointerException     if either {@code label} or {@code language} is {@code null}
	 * @throws IllegalArgumentException if {@code language} is empty
	 */
	public static Literal createLiteral(String label, String language) {

		if (label == null) {
			throw new NullPointerException("null label");
		}

		if (language == null) {
			throw new NullPointerException("null language");
		}

		if (label.isEmpty()) {
			throw new IllegalArgumentException("empty language tag");
		}

		return new TaggedLiteral(label, language);
	}

	/**
	 * Creates a new typed literal value.
	 *
	 * @param label    the label of the literal
	 * @param datatype the datatype of the literal; defaults to {@code xsd:string} if {@code null}
	 *
	 * @return a new generic typed literal value
	 *
	 * @throws NullPointerException     if {@code label} is {@code null}
	 * @throws IllegalArgumentException if {@code datatype} is reserved for internal use
	 */
	public static Literal createLiteral(String label, IRI datatype) {

		if (label == null) {
			throw new NullPointerException("null label");
		}

		if (RDF_LANG_STRING.equals(datatype)) {
			throw new IllegalArgumentException("reserved rdf:langString datatype");
		}

		return new TypedLiteral(label, datatype != null ? datatype : XSD_STRING);
	}

	/**
	 * Creates a new boolean literal value.
	 *
	 * @param value the value of the literal
	 *
	 * @return a new generic {@code xsd:boolean} literal value
	 */
	public static Literal createLiteral(boolean value) {
		return value ? BooleanLiteral.TRUE : BooleanLiteral.FALSE;
	}

	/**
	 * Creates a new byte literal value.
	 *
	 * @param value the value of the literal
	 *
	 * @return a new generic {@code xsd:byte} literal value
	 */
	public static Literal createLiteral(byte value) {
		return new NumericLiteral<>(value, String.valueOf(value), XSD_BYTE);
	}

	/**
	 * Creates a new short literal value.
	 *
	 * @param value the value of the literal
	 *
	 * @return a new generic {@code xsd:short} literal value
	 */
	public static Literal createLiteral(short value) {
		return new NumericLiteral<>(value, String.valueOf(value), XSD_SHORT);
	}

	/**
	 * Creates a new int literal value.
	 *
	 * @param value the value of the literal
	 *
	 * @return a new generic {@code xsd:int} literal value
	 */
	public static Literal createLiteral(int value) {
		return new NumericLiteral<>(value, String.valueOf(value), XSD_INT);
	}

	/**
	 * Creates a new long literal value.
	 *
	 * @param value the value of the literal
	 *
	 * @return a new generic {@code xsd:long} literal value
	 */
	public static Literal createLiteral(long value) {
		return new NumericLiteral<>(value, String.valueOf(value), XSD_LONG);
	}

	/**
	 * Creates a new float literal value.
	 *
	 * @param value the value of the literal
	 *
	 * @return a new generic {@code xsd:float} literal value
	 */
	public static Literal createLiteral(float value) {
		return new NumericLiteral<>(value, String.valueOf(value), XSD_FLOAT);
	}

	/**
	 * Creates a new double literal value.
	 *
	 * @param value the value of the literal
	 *
	 * @return a new generic {@code xsd:double} literal value
	 */
	public static Literal createLiteral(double value) {
		return new NumericLiteral<>(value, String.valueOf(value), XSD_DOUBLE);
	}

	/**
	 * Creates a new integer literal value.
	 *
	 * @param bigInteger the value of the literal
	 *
	 * @return a new generic {@code xsd:integer} literal value
	 *
	 * @throws NullPointerException if {@code bigInteger} is {@code null}
	 */
	public static Literal createLiteral(BigInteger bigInteger) {

		if (bigInteger == null) {
			throw new NullPointerException("null bigInteger value");
		}

		return new IntegerLiteral(bigInteger);
	}

	/**
	 * Creates a new decimal literal value.
	 *
	 * @param bigDecimal the value of the literal
	 *
	 * @return a new generic {@code xsd:decimal} literal value
	 *
	 * @throws NullPointerException if {@code bigDecimal} is {@code null}
	 */
	public static Literal createLiteral(BigDecimal bigDecimal) {

		if (bigDecimal == null) {
			throw new NullPointerException("null bigDecimal value");
		}

		return new DecimalLiteral(bigDecimal);
	}

	/**
	 * Creates a new calendar literal value.
	 *
	 * @param calendar the value of the literal
	 *
	 * @return a new generic literal value, with a XSD datatype consistent with the
	 *         {@linkplain XMLGregorianCalendar#getXMLSchemaType() XML schema type} of {@code calendar}
	 *
	 * @throws NullPointerException     if {@code calendar} is {@code null}
	 * @throws IllegalArgumentException if the XML schema type of {@code calendar} cannot be mapped to an XSD datatype
	 */
	public static Literal createLiteral(XMLGregorianCalendar calendar) {

		if (calendar == null) {
			throw new NullPointerException("null calendar");
		}

		return new TemporalLiteral(calendar);
	}

	/**
	 * Creates a new date literal value.
	 *
	 * @param date the value of the literal
	 *
	 * @return a new generic {@code xsd:dateTime} literal value
	 *
	 * @throws NullPointerException if {@code calendar} is {@code null}
	 */
	public static Literal createLiteral(Date date) {

		if (date == null) {
			throw new NullPointerException("null date");
		}

		final GregorianCalendar calendar = new GregorianCalendar();

		calendar.setTime(date);

		return new TemporalLiteral(DATATYPE_FACTORY.get().newXMLGregorianCalendar(calendar));
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
	protected <V> V value(Function<String, V> mapper) {
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
		return value(label -> Optional.of(label)

				.map(WHITESPACE_PATTERN::matcher)
				.map(matcher -> matcher.replaceAll(""))

				.map(normalized -> (normalized.equals("true") || normalized.equals("1")) ? Boolean.TRUE
						: (normalized.equals("false") || normalized.equals("0")) ? Boolean.FALSE
								: null
				)

				.orElse(null)
		);
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
		return value(label -> label.equals(POSITIVE_INFINITY) ? Float.POSITIVE_INFINITY
				: label.equals(NEGATIVE_INFINITY) ? Float.NEGATIVE_INFINITY
						: label.equals(NAN) ? Float.NaN
								: Float.parseFloat(label)
		);
	}

	@Override
	public double doubleValue() {
		return value(label -> label.equals(POSITIVE_INFINITY) ? Float.POSITIVE_INFINITY
				: label.equals(NEGATIVE_INFINITY) ? Float.NEGATIVE_INFINITY
						: label.equals(NAN) ? Float.NaN
								: Double.parseDouble(label)
		);
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
	public XMLGregorianCalendar calendarValue() {
		return value(label -> DATATYPE_FACTORY.get().newXMLGregorianCalendar(label));
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof Literal
				&& Objects.equals(getLabel(), ((Literal) o).getLabel())
				&& Objects.equals(getLanguage().map(this::normalize), ((Literal) o).getLanguage().map(this::normalize))
				&& Objects.equals(getDatatype(), ((Literal) o).getDatatype());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getLabel());
	}

	@Override
	public String toString() {

		final String label = Optional.ofNullable(getLabel()).orElse("");
		final String language = getLanguage().orElse(null);
		final IRI datatype = getDatatype();

		return language != null ? '"' + label + '"' + '@' + language
				: datatype == null || datatype.equals(XSD_STRING) ? '"' + label + '"'
						: '"' + label + '"' + "^^<" + datatype.stringValue() + ">";
	}

	private String normalize(String tag) {
		return tag.toUpperCase(Locale.ROOT);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static class TypedLiteral extends AbstractLiteral {

		private static final long serialVersionUID = -19640527584237291L;

		private final String label;
		private final IRI datatype;

		TypedLiteral(String label, IRI datatype) {
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

	}

	private static class TaggedLiteral extends AbstractLiteral {

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
			return RDF_LANG_STRING;
		}

	}

	private static class BooleanLiteral extends AbstractLiteral {

		private static final long serialVersionUID = -1162147873619834622L;

		private static final Literal TRUE = new BooleanLiteral(true);
		private static final Literal FALSE = new BooleanLiteral(false);

		private final boolean value;

		private final String label;

		BooleanLiteral(boolean value) {
			this.value = value;
			this.label = String.valueOf(value);
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
			return XSD_BOOLEAN;
		}

		@Override
		public boolean booleanValue() {
			return value;
		}

	}

	private static class NumericLiteral<V extends Number> extends AbstractLiteral {

		private static final long serialVersionUID = -3201912818064851702L;

		protected V value;

		private final String label;
		private final IRI datatype;

		NumericLiteral(V value, String label, IRI datatype) {
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

	private static class IntegerLiteral extends NumericLiteral<BigInteger> {

		private static final long serialVersionUID = -4274941248972496665L;

		IntegerLiteral(BigInteger value) {
			super(value, value.toString(), XSD_INTEGER);
		}

		@Override
		public BigInteger integerValue() {
			return value;
		}

		@Override
		public BigDecimal decimalValue() {
			return new BigDecimal(value);
		}

	}

	private static class DecimalLiteral extends NumericLiteral<BigDecimal> {

		private static final long serialVersionUID = -4382147098035463886L;

		DecimalLiteral(BigDecimal value) {
			super(value, value.toString(), XSD_DECIMAL);
		}

		@Override
		public BigInteger integerValue() {
			return value.toBigInteger();
		}

		@Override
		public BigDecimal decimalValue() {
			return value;
		}

	}

	private static class TemporalLiteral extends AbstractLiteral {

		private static final long serialVersionUID = 9131700079460615839L;

		private static final Map<QName, IRI> DATATYPES = datatypes();

		private static Map<QName, IRI> datatypes() {

			final Map<QName, IRI> datatypes = new HashMap<>();

			datatypes.put(DatatypeConstants.DATETIME, XSD_DATETIME);
			datatypes.put(DatatypeConstants.DATE, XSD_DATE);
			datatypes.put(DatatypeConstants.TIME, XSD_TIME);
			datatypes.put(DatatypeConstants.GYEARMONTH, XSD_GYEARMONTH);
			datatypes.put(DatatypeConstants.GMONTHDAY, XSD_GMONTHDAY);
			datatypes.put(DatatypeConstants.GYEAR, XSD_GYEAR);
			datatypes.put(DatatypeConstants.GMONTH, XSD_GMONTH);
			datatypes.put(DatatypeConstants.GDAY, XSD_GDAY);
			datatypes.put(DatatypeConstants.DURATION, XSD_DURATION);
			datatypes.put(DatatypeConstants.DURATION_DAYTIME, XSD_DURATION_DAYTIME);
			datatypes.put(DatatypeConstants.DURATION_YEARMONTH, XSD_DURATION_YEARMONTH);

			return datatypes;
		}

		private static IRI datatype(QName qName) {

			final IRI datatype = DATATYPES.get(qName);

			if (datatype == null) {
				throw new IllegalArgumentException((String.format(
						"QName <%s> cannot be mapped to an XSD datatype IRI", qName
				)));
			}

			return datatype;
		}

		private final XMLGregorianCalendar value;

		private final String label;
		private final IRI datatype;

		TemporalLiteral(XMLGregorianCalendar calendar) {

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
