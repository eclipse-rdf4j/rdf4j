/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

/**
 * Base class for {@link Literal}, offering common functionality.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public abstract class AbstractLiteral implements Literal {

	private static final long serialVersionUID = -1286527360744086451L;

	static final String XSD_STRING = "http://www.w3.org/2001/XMLSchema#string";
	static final String RDF_LANG_STRING = "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString";

	private static final String POSITIVE_INFINITY = "INF";
	private static final String NEGATIVE_INFINITY = "-INF";
	private static final String NAN = "NaN";

	private static final Pattern WhitespacePattern = Pattern.compile("\\s+");

	private static final ThreadLocal<DatatypeFactory> factory = ThreadLocal.withInitial(() -> {
		try {

			return DatatypeFactory.newInstance(); // not guaranteed to be thread-safe

		} catch (final DatatypeConfigurationException e) {

			throw new RuntimeException("unable to create datatype factory", e);

		}
	});

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
	 * @throws NullPointerException if {@code mapper} is null
	 */
	protected <V> V value(final Function<String, V> mapper) {
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

				.map(WhitespacePattern::matcher)
				.map(matcher -> matcher.replaceAll(""))

				.map(normalized -> (normalized.equals("true") || normalized.equals("1")) ? TRUE
						: (normalized.equals("false") || normalized.equals("0")) ? FALSE
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
		return value(label -> factory.get().newXMLGregorianCalendar(label));
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
		return Objects.hashCode(getLabel()); // !!! include language/datatype (after #2561 is resolved)
	}

	/**
	 * Returns the label of the literal with its language or datatype.
	 * <p>
	 * Note that this method does not escape the quoted label.
	 */
	@Override
	public String toString() {

		final String label = Optional.ofNullable(getLabel()).orElse("");
		final String language = getLanguage().orElse(null);
		final String datatype = Optional.ofNullable(getDatatype()).map(Value::stringValue).orElse(XSD_STRING);

		return language != null ? '"' + label + '"' + '@' + language
				: datatype.equals(XSD_STRING) ? '"' + label + '"'
						: '"' + label + '"' + "^^<" + datatype + ">";
	}

	private String normalize(String tag) {
		return tag.toUpperCase(Locale.ROOT);
	}

}
