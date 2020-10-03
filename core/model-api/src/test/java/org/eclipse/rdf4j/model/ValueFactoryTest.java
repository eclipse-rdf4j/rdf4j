/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_BOOLEAN;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_BYTE;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_DATETIME;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_DECIMAL;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_DOUBLE;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_FLOAT;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_INT;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_INTEGER;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_LONG;
import static org.eclipse.rdf4j.model.LiteralTest.XSD_SHORT;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;

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

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.floatValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo("42.0");
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_FLOAT);

	}

	@Test
	public void testCreateLiteralDouble() {

		final double value = 42.0d;

		final Literal literal = factory().createLiteral(value);

		assertThat(literal).isNotNull();
		assertThat(literal.doubleValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo("42.0");
		assertThat(literal.getDatatype().stringValue()).isEqualTo(XSD_DOUBLE);
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
