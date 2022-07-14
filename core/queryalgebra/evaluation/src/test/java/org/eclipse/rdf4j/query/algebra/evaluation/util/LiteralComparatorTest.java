/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.util;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.Before;
import org.junit.Test;

/**
 * @author james
 */
public class LiteralComparatorTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	private final Literal one = vf.createLiteral(1);

	private final Literal ten = vf.createLiteral(10);

	private final Literal a = vf.createLiteral("a");

	private final Literal b = vf.createLiteral("b");

	private final Literal la = vf.createLiteral("a", "en");

	private final Literal lb = vf.createLiteral("b", "en");

	private final Literal lf = vf.createLiteral("a", "fr");

	private final Literal f = vf.createLiteral(false);

	private final Literal t = vf.createLiteral(true);

	private Literal date1;

	private Literal date2;

	private final Literal simple1 = vf.createLiteral("http://script.example/Latin");

	private final Literal simple2 = vf.createLiteral("http://script.example/Кириллица");

	private final Literal typed1 = vf.createLiteral("http://script.example/Latin", XSD.STRING);

	private final ValueComparator cmp = new ValueComparator();

	@Test
	public void testNumeric() throws Exception {
		assertTrue(cmp.compare(one, one) == 0);
		assertTrue(cmp.compare(one, ten) < 0);
		assertTrue(cmp.compare(ten, one) > 0);
		assertTrue(cmp.compare(ten, ten) == 0);
	}

	@Test
	public void testString() throws Exception {
		assertTrue(cmp.compare(a, a) == 0);
		assertTrue(cmp.compare(a, b) < 0);
		assertTrue(cmp.compare(b, a) > 0);
		assertTrue(cmp.compare(b, b) == 0);
	}

	@Test
	public void testSameLanguage() throws Exception {
		assertTrue(cmp.compare(la, la) == 0);
		assertTrue(cmp.compare(la, lb) < 0);
		assertTrue(cmp.compare(lb, la) > 0);
		assertTrue(cmp.compare(lb, lb) == 0);
	}

	@Test
	public void testDifferentLanguage() throws Exception {
		cmp.compare(la, lf);
	}

	@Test
	public void testBoolean() throws Exception {
		assertTrue(cmp.compare(f, f) == 0);
		assertTrue(cmp.compare(f, t) < 0);
		assertTrue(cmp.compare(t, f) > 0);
		assertTrue(cmp.compare(t, t) == 0);
	}

	@Test
	public void testDateTime() throws Exception {
		assertTrue(cmp.compare(date1, date1) == 0);
		assertTrue(cmp.compare(date1, date2) < 0);
		assertTrue(cmp.compare(date2, date1) > 0);
		assertTrue(cmp.compare(date2, date2) == 0);
	}

	@Test
	public void testBothSimple() throws Exception {
		assertTrue(cmp.compare(simple1, simple1) == 0);
		assertTrue(cmp.compare(simple1, simple2) < 0);
		assertTrue(cmp.compare(simple2, simple1) > 0);
		assertTrue(cmp.compare(simple2, simple2) == 0);
	}

	@Test
	public void testLeftSimple() throws Exception {
		assertTrue(cmp.compare(simple1, typed1) == 0);
	}

	@Test
	public void testRightSimple() throws Exception {
		assertTrue(cmp.compare(typed1, simple1) == 0);
	}

	@Test
	public void testOrder() throws Exception {
		Literal en4 = vf.createLiteral("4", "en");
		Literal nine = vf.createLiteral(9);
		List<Literal> list = new ArrayList<>();
		list.add(ten);
		list.add(en4);
		list.add(nine);
		Collections.sort(list, cmp);
		assertTrue(list.indexOf(nine) < list.indexOf(ten));
	}

	@Before
	public void setUp() throws Exception {
		DatatypeFactory factory = DatatypeFactory.newInstance();
		XMLGregorianCalendar mar = factory.newXMLGregorianCalendar("2000-03-04T20:00:00Z");
		XMLGregorianCalendar oct = factory.newXMLGregorianCalendar("2002-10-10T12:00:00-05:00");
		date1 = vf.createLiteral(mar);
		date2 = vf.createLiteral(oct);
	}
}
