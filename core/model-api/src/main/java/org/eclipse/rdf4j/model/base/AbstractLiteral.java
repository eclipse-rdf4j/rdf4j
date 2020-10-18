/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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

	private static final String POSITIVE_INFINITY = "INF";
	private static final String NEGATIVE_INFINITY = "-INF";
	private static final String NAN = "NaN";

	private static final ThreadLocal<DatatypeFactory> DATATYPE_FACTORY = ThreadLocal.withInitial(() -> {
		try {

			return DatatypeFactory.newInstance(); // not guaranteed to be thread-safe

		} catch (DatatypeConfigurationException e) {

			throw new RuntimeException("unable to create datatype factory", e);

		}
	});

	static boolean reserved(IRI datatype) {
		return RDF_LANGSTRING.equals(datatype);
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

				.map(String::trim)

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
		return value(label -> label.equals(POSITIVE_INFINITY) ? Double.POSITIVE_INFINITY
				: label.equals(NEGATIVE_INFINITY) ? Double.NEGATIVE_INFINITY
						: label.equals(NAN) ? Double.NaN
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

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
			this.datatype = (datatype != null) ? datatype : XSD_STRING;
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
			this(
					value,
					value == Float.POSITIVE_INFINITY ? POSITIVE_INFINITY
							: value == Float.NEGATIVE_INFINITY ? NEGATIVE_INFINITY
									: Float.isNaN(value) ? NAN
											: Float.toString(value),
					XSD_FLOAT
			);
		}

		NumberLiteral(double value) {
			this(
					value,
					value == Double.POSITIVE_INFINITY ? POSITIVE_INFINITY
							: value == Double.NEGATIVE_INFINITY ? NEGATIVE_INFINITY
									: Double.isNaN(value) ? NAN
											: Double.toString(value),
					XSD_DOUBLE
			);
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

	static class CalendarLiteral extends AbstractLiteral {

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

		private static IRI datatype(QName qname) {

			final IRI datatype = DATATYPES.get(qname);

			if (datatype == null) {
				throw new IllegalArgumentException((String.format(
						"QName <%s> cannot be mapped to an XSD datatype IRI", qname
				)));
			}

			return datatype;
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
