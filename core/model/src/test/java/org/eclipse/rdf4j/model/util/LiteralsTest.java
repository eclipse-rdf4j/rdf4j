/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.IllformedLocaleException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Literals}.
 *
 * @author Peter Ansell
 */
public class LiteralsTest {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();
	private static final Model model = new LinkedHashModel();
	private static final IRI foo = vf.createIRI("http://example.org/foo");
	private static final IRI bar = vf.createIRI("http://example.org/bar");

	private static final String[][] expectedTagNormalizations = {
			{ "en", "en" },
			{ "EN", "en" },
			{ "AR-Latn", "ar-Latn" },
			{ "AZ-latn-x-LATN", "az-Latn-x-latn" },
			{ "Az-latn-X-Latn", "az-Latn-x-latn" },
			{ "BER-LATN-x-Dialect", "ber-Latn-x-dialect" },
			{ "BNT", "bnt" },
			{ "Egy-Latn", "egy-Latn" },
			{ "en-ca-x-ca", "en-CA-x-ca" },
			{ "EN-ca-X-Ca", "en-CA-x-ca" },
			{ "En-Ca-X-Ca", "en-CA-x-ca" },
			{ "en-GB", "en-GB" },
			{ "EN-UK", "en-UK" },
			{ "EN-US", "en-US" },
			{ "FA-LATN-X-MIDDLE", "fa-Latn-x-middle" },
			{ "FA-X-middle", "fa-x-middle" },
			{ "Grc-latn-x-liturgic", "grc-Latn-x-liturgic" },
			{ "Grc-x-liturgic", "grc-x-liturgic" },
			{ "He-Latn", "he-Latn" },
			{ "Ja-Latn", "ja-Latn" },
			{ "Ko-Latn", "ko-Latn" },
			{ "La-x-liturgic", "la-x-liturgic" },
			{ "La-x-medieval", "la-x-medieval" },
			{ "NN", "nn" },
			{ "QQQ-002", "qqq-002" },
			{ "qqq-142", "qqq-142" },
			{ "QQQ-ET", "qqq-ET" },
			{ "ru-Latn-UA", "ru-Latn-UA" },
			{ "ru-Latn-ua", "ru-Latn-UA" },
			{ "RU-LATN-UA", "ru-Latn-UA" },
			{ "SGN-BE-FR", "sgn-BE-FR" },
			{ "sgn-be-fr", "sgn-BE-FR" },
			{ "UND", "und" },
			{ "X-BYZANTIN-LATN", "x-byzantin-Latn" },
			{ "X-Frisian", "x-frisian" },
			{ "X-KHASIAN", "x-khasian" },
			{ "x-local", "x-local" },
			{ "zh-Hant", "zh-Hant" },
			{ "zh-Latn-pinyin", "zh-Latn-pinyin" },
			{ "Zh-latn-PINYIN-X-NoTone", "zh-Latn-pinyin-x-notone" },
			{ "zh-Latn-wadegile", "zh-Latn-wadegile" }
	};

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#normalizeLanguageTag(String)} .
	 */
	@Test
	public void testNormaliseBCP47Tag() {

		for (String[] expectedNormalization : expectedTagNormalizations) {
			assertThat(Literals.normalizeLanguageTag(expectedNormalization[0])).isEqualTo(expectedNormalization[1]);
		}

		// invalid
		assertThatExceptionOfType(IllformedLocaleException.class)
				.isThrownBy(() -> Literals.normalizeLanguageTag("ru-ua-latn"));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#getLabel(org.eclipse.rdf4j.model.Literal, java.lang.String)} .
	 */
	@Disabled
	@Test
	public final void testGetLabelLiteralString() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#getLabel(org.eclipse.rdf4j.model.Value, java.lang.String)} .
	 */
	@Disabled
	@Test
	public final void testGetLabelValueString() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#getByteValue(org.eclipse.rdf4j.model.Literal, byte)}
	 * .
	 */
	@Disabled
	@Test
	public final void testGetByteValueLiteralByte() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#getByteValue(org.eclipse.rdf4j.model.Value, byte)} .
	 */
	@Disabled
	@Test
	public final void testGetByteValueValueByte() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#getShortValue(org.eclipse.rdf4j.model.Literal, short)} .
	 */
	@Disabled
	@Test
	public final void testGetShortValueLiteralShort() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#getShortValue(org.eclipse.rdf4j.model.Value, short)}
	 * .
	 */
	@Disabled
	@Test
	public final void testGetShortValueValueShort() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#getIntValue(org.eclipse.rdf4j.model.Literal, int)} .
	 */
	@Disabled
	@Test
	public final void testGetIntValueLiteralInt() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#getIntValue(org.eclipse.rdf4j.model.Value, int)} .
	 */
	@Disabled
	@Test
	public final void testGetIntValueValueInt() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#getLongValue(org.eclipse.rdf4j.model.Literal, long)}
	 * .
	 */
	@Disabled
	@Test
	public final void testGetLongValueLiteralLong() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#getLongValue(org.eclipse.rdf4j.model.Value, long)} .
	 */
	@Disabled
	@Test
	public final void testGetLongValueValueLong() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#getIntegerValue(org.eclipse.rdf4j.model.Literal, java.math.BigInteger)}
	 * .
	 */
	@Disabled
	@Test
	public final void testGetIntegerValueLiteralBigInteger() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#getIntegerValue(org.eclipse.rdf4j.model.Value, java.math.BigInteger)}
	 * .
	 */
	@Disabled
	@Test
	public final void testGetIntegerValueValueBigInteger() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#getDecimalValue(org.eclipse.rdf4j.model.Literal, java.math.BigDecimal)}
	 * .
	 */
	@Disabled
	@Test
	public final void testGetDecimalValueLiteralBigDecimal() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#getDecimalValue(org.eclipse.rdf4j.model.Value, java.math.BigDecimal)}
	 * .
	 */
	@Disabled
	@Test
	public final void testGetDecimalValueValueBigDecimal() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#getFloatValue(org.eclipse.rdf4j.model.Literal, float)} .
	 */
	@Disabled
	@Test
	public final void testGetFloatValueLiteralFloat() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#getFloatValue(org.eclipse.rdf4j.model.Value, float)}
	 * .
	 */
	@Disabled
	@Test
	public final void testGetFloatValueValueFloat() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#getDoubleValue(org.eclipse.rdf4j.model.Literal, double)} .
	 */
	@Disabled
	@Test
	public final void testGetDoubleValueLiteralDouble() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#getDoubleValue(org.eclipse.rdf4j.model.Value, double)} .
	 */
	@Disabled
	@Test
	public final void testGetDoubleValueValueDouble() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#getBooleanValue(org.eclipse.rdf4j.model.Literal, boolean)} .
	 */
	@Disabled
	@Test
	public final void testGetBooleanValueLiteralBoolean() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#getBooleanValue(org.eclipse.rdf4j.model.Value, boolean)} .
	 */
	@Disabled
	@Test
	public final void testGetBooleanValueValueBoolean() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#getCalendarValue(org.eclipse.rdf4j.model.Literal, javax.xml.datatype.XMLGregorianCalendar)}
	 * .
	 */
	@Disabled
	@Test
	public final void testGetCalendarValueLiteralXMLGregorianCalendar() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#getCalendarValue(org.eclipse.rdf4j.model.Value, javax.xml.datatype.XMLGregorianCalendar)}
	 * .
	 */
	@Disabled
	@Test
	public final void testGetCalendarValueValueXMLGregorianCalendar() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetDurationValueLiteralDuration() throws Exception {
		DatatypeFactory dtFactory = DatatypeFactory.newInstance();

		Duration fallback = dtFactory.newDuration(true, 1, 1, 1, 1, 1, 1);

		Duration result = Literals.getDurationValue(vf.createLiteral("P5Y"), fallback);

		assertNotNull(result);
		assertFalse(result.equals(fallback));
		assertEquals(5, result.getYears());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteral(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralObjectNull() {

		Object obj = null;
		try {
			Literals.createLiteral(SimpleValueFactory.getInstance(), obj);
			fail("Did not find expected exception");
		} catch (NullPointerException npe) {
			assertTrue(npe.getMessage().contains("Cannot create a literal from a null"));
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteral(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralObjectBoolean() {

		Object obj = Boolean.TRUE;
		Literal l = Literals.createLiteral(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.BOOLEAN);
		assertTrue(l.booleanValue());

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteral(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralObjectByte() {

		Object obj = new Integer(42).byteValue();
		Literal l = Literals.createLiteral(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.BYTE);
		assertEquals(l.getLabel(), "42");

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteral(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralObjectDouble() {

		Object obj = new Double(42);
		Literal l = Literals.createLiteral(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.DOUBLE);
		assertEquals(l.getLabel(), "42.0");

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteral(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralObjectFloat() {

		Object obj = new Float(42);
		Literal l = Literals.createLiteral(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.FLOAT);
		assertEquals(l.getLabel(), "42.0");

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteral(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralObjectInteger() {

		Object obj = new Integer(4);
		Literal l = Literals.createLiteral(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.INT);
		assertEquals(l.getLabel(), "4");

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteral(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralObjectLong() {

		Object obj = new Long(42);
		Literal l = Literals.createLiteral(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.LONG);
		assertEquals(l.getLabel(), "42");

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteral(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralObjectShort() {

		Object obj = Short.parseShort("42");
		Literal l = Literals.createLiteral(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.SHORT);
		assertEquals("42", l.getLabel());

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteral(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralObjectXMLGregorianCalendar() {

		GregorianCalendar c = new GregorianCalendar();
		c.setTime(new Date());
		try {
			Object obj = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
			Literal l = Literals.createLiteral(SimpleValueFactory.getInstance(), obj);
			assertNotNull(l);
			assertEquals(l.getDatatype(), XSD.DATETIME);
			// TODO check lexical value?
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
			fail("Could not instantiate javax.xml.datatype.DatatypeFactory");
		}

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteral(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralObjectDate() {

		Object obj = new Date();
		Literal l = Literals.createLiteral(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.DATETIME);

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteral(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralObjectString() {

		Object obj = "random unique string";
		Literal l = Literals.createLiteral(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.STRING);
		assertEquals(l.getLabel(), "random unique string");

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteral(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralObjectObject() {

		Object obj = new Object();
		Literal l = Literals.createLiteral(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.STRING);

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteralOrFail(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralOrFailObjectNull() {

		Object obj = null;
		try {
			Literals.createLiteralOrFail(SimpleValueFactory.getInstance(), obj);
			fail("Did not find expected exception");
		} catch (NullPointerException npe) {
			assertTrue(npe.getMessage().contains("Cannot create a literal from a null"));
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteralOrFail(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralOrFailObjectBoolean() {

		Object obj = Boolean.TRUE;
		Literal l = Literals.createLiteralOrFail(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.BOOLEAN);
		assertTrue(l.booleanValue());

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteralOrFail(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralOrFailObjectByte() {

		Object obj = new Integer(42).byteValue();
		Literal l = Literals.createLiteralOrFail(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.BYTE);
		assertEquals(l.getLabel(), "42");

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteralOrFail(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralOrFailObjectDouble() {

		Object obj = new Double(42);
		Literal l = Literals.createLiteralOrFail(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.DOUBLE);
		assertEquals(l.getLabel(), "42.0");

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteralOrFail(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralOrFailObjectFloat() {

		Object obj = new Float(42);
		Literal l = Literals.createLiteralOrFail(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.FLOAT);
		assertEquals(l.getLabel(), "42.0");

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteralOrFail(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralOrFailObjectInteger() {

		Object obj = new Integer(4);
		Literal l = Literals.createLiteralOrFail(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.INT);
		assertEquals(l.getLabel(), "4");

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteralOrFail(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralOrFailObjectLong() {

		Object obj = new Long(42);
		Literal l = Literals.createLiteralOrFail(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.LONG);
		assertEquals(l.getLabel(), "42");

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteralOrFail(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralOrFailObjectShort() {

		Object obj = Short.parseShort("42");
		Literal l = Literals.createLiteralOrFail(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.SHORT);
		assertEquals("42", l.getLabel());

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteralOrFail(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralOrFailObjectXMLGregorianCalendar() {

		GregorianCalendar c = new GregorianCalendar();
		c.setTime(new Date());
		try {
			Object obj = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
			Literal l = Literals.createLiteralOrFail(SimpleValueFactory.getInstance(), obj);
			assertNotNull(l);
			assertEquals(l.getDatatype(), XSD.DATETIME);
			// TODO check lexical value?
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
			fail("Could not instantiate javax.xml.datatype.DatatypeFactory");
		}

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteralOrFail(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralOrFailObjectDate() {

		Object obj = new Date();
		Literal l = Literals.createLiteralOrFail(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.DATETIME);

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteralOrFail(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralOrFailObjectString() {

		Object obj = "random unique string";
		Literal l = Literals.createLiteralOrFail(SimpleValueFactory.getInstance(), obj);
		assertNotNull(l);
		assertEquals(l.getDatatype(), XSD.STRING);
		assertEquals(l.getLabel(), "random unique string");

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteralOrFail(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCreateLiteralOrFailObjectObject() {

		Object obj = new Object();
		try {
			Literal l = Literals.createLiteralOrFail(SimpleValueFactory.getInstance(), obj);
			fail("Did not receive expected exception");
		} catch (LiteralUtilException e) {
			assertTrue(e.getMessage().contains("Did not recognise object when creating literal"));
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.util.Literals#createLiteral(org.eclipse.rdf4j.model.ValueFactory, java.lang.Object)}
	 * .
	 */
	@Test
	public void testCanCreateLiteralObjectNull() {

		Object obj = null;
		assertFalse(Literals.canCreateLiteral(obj));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#canCreateLiteral(Object)} .
	 */
	@Test
	public void testCanCreateLiteralObjectBoolean() {

		Object obj = Boolean.TRUE;
		assertTrue(Literals.canCreateLiteral(obj));

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#canCreateLiteral(Object)} .
	 */
	@Test
	public void testCanCreateLiteralObjectByte() {

		Object obj = new Integer(42).byteValue();
		assertTrue(Literals.canCreateLiteral(obj));

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#canCreateLiteral(Object)} .
	 */
	@Test
	public void testCanCreateLiteralObjectDouble() {

		Object obj = new Double(42);
		assertTrue(Literals.canCreateLiteral(obj));

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#canCreateLiteral(Object)} .
	 */
	@Test
	public void testCanCreateLiteralObjectFloat() {

		Object obj = new Float(42);
		assertTrue(Literals.canCreateLiteral(obj));

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#canCreateLiteral(Object)} .
	 */
	@Test
	public void testCanCreateLiteralObjectInteger() {

		Object obj = new Integer(4);
		assertTrue(Literals.canCreateLiteral(obj));

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#canCreateLiteral(Object)} .
	 */
	@Test
	public void testCanCreateLiteralObjectLong() {

		Object obj = new Long(42);
		assertTrue(Literals.canCreateLiteral(obj));

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#canCreateLiteral(Object)} .
	 */
	@Test
	public void testCanCreateLiteralObjectShort() {

		Object obj = Short.parseShort("42");
		assertTrue(Literals.canCreateLiteral(obj));

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#canCreateLiteral(Object)} .
	 */
	@Test
	public void testCanCreateLiteralObjectXMLGregorianCalendar() throws Exception {

		GregorianCalendar c = new GregorianCalendar();
		c.setTime(new Date());
		Object obj = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
		assertTrue(Literals.canCreateLiteral(obj));

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#canCreateLiteral(Object)} .
	 */
	@Test
	public void testCanCreateLiteralObjectDate() {

		Object obj = new Date();
		assertTrue(Literals.canCreateLiteral(obj));

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#canCreateLiteral(Object)} .
	 */
	@Test
	public void testCanCreateLiteralObjectString() {

		Object obj = "random unique string";
		assertTrue(Literals.canCreateLiteral(obj));

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Literals#canCreateLiteral(Object)} .
	 */
	@Test
	public void testCanCreateLiteralObjectObject() {

		Object obj = new Object();
		assertFalse(Literals.canCreateLiteral(obj));

	}

}
