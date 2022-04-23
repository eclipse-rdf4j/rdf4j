/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.util;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.Test;

/**
 * @author james
 */
public class ValueComparatorTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	private final BNode bnode1 = vf.createBNode();

	private final BNode bnode2 = vf.createBNode();

	private final IRI uri1 = vf.createIRI("http://script.example/Latin");

	private final IRI uri2 = vf.createIRI("http://script.example/Кириллица");

	private final IRI uri3 = vf.createIRI("http://script.example/日本語");

	private final Literal typed1 = vf.createLiteral("http://script.example/Latin", XSD.STRING);

	private final ValueComparator cmp = new ValueComparator();

	@Test
	public void testBothNull() throws Exception {
		assertTrue(cmp.compare(null, null) == 0);
	}

	@Test
	public void testLeftNull() throws Exception {
		assertTrue(cmp.compare(null, typed1) < 0);
	}

	@Test
	public void testRightNull() throws Exception {
		assertTrue(cmp.compare(typed1, null) > 0);
	}

	@Test
	public void testBothBnode() throws Exception {
		assertTrue(cmp.compare(bnode1, bnode1) == 0);
		assertTrue(cmp.compare(bnode2, bnode2) == 0);
		assertTrue(cmp.compare(bnode1, bnode2) != cmp.compare(bnode2, bnode1));
		assertTrue(cmp.compare(bnode1, bnode2) == -1 * cmp.compare(bnode2, bnode1));
	}

	@Test
	public void testLeftBnode() throws Exception {
		assertTrue(cmp.compare(bnode1, typed1) < 0);
	}

	@Test
	public void testRightBnode() throws Exception {
		assertTrue(cmp.compare(typed1, bnode1) > 0);
	}

	@Test
	public void testBothURI() throws Exception {
		assertTrue(cmp.compare(uri1, uri1) == 0);
		assertTrue(cmp.compare(uri1, uri2) < 0);
		assertTrue(cmp.compare(uri1, uri3) < 0);
		assertTrue(cmp.compare(uri2, uri1) > 0);
		assertTrue(cmp.compare(uri2, uri2) == 0);
		assertTrue(cmp.compare(uri2, uri3) < 0);
		assertTrue(cmp.compare(uri3, uri1) > 0);
		assertTrue(cmp.compare(uri3, uri2) > 0);
		assertTrue(cmp.compare(uri3, uri3) == 0);
	}

	@Test
	public void testLeftURI() throws Exception {
		assertTrue(cmp.compare(uri1, typed1) < 0);
	}

	@Test
	public void testRightURI() throws Exception {
		assertTrue(cmp.compare(typed1, uri1) > 0);
	}

	/**
	 * Tests whether xsd:int's are properly sorted in a list with mixed value types.
	 */
	@Test
	public void testOrder1() throws Exception {
		Literal en4 = vf.createLiteral("4", "en");
		Literal int10 = vf.createLiteral(10);
		Literal int9 = vf.createLiteral(9);

		List<Literal> valueList = Arrays.asList(en4, int10, int9);
		Collections.sort(valueList, cmp);

		assertTrue(valueList.indexOf(int9) < valueList.indexOf(int10));
	}

	/**
	 * Tests whether various numerics are properly sorted in a list with mixed value types.
	 */
	@Test
	public void testOrder2() throws Exception {
		Literal en4 = vf.createLiteral("4", "en");
		Literal int10 = vf.createLiteral(10);
		Literal int9 = vf.createLiteral(9);
		Literal plain9 = vf.createLiteral("9");
		Literal integer5 = vf.createLiteral("5", XSD.INTEGER);
		Literal float9 = vf.createLiteral(9f);
		Literal plain4 = vf.createLiteral("4");
		Literal plain10 = vf.createLiteral("10");

		List<Literal> valueList = Arrays.asList(en4, int10, int9, plain9, integer5, float9, plain4, plain10);
		Collections.sort(valueList, cmp);

		assertTrue(valueList.indexOf(integer5) < valueList.indexOf(float9));
		assertTrue(valueList.indexOf(integer5) < valueList.indexOf(int9));
		assertTrue(valueList.indexOf(integer5) < valueList.indexOf(int10));
		assertTrue(valueList.indexOf(float9) < valueList.indexOf(int10));
		assertTrue(valueList.indexOf(int9) < valueList.indexOf(int10));
		assertTrue(valueList.indexOf(int9) < valueList.indexOf(int10));
	}

	/**
	 * Tests whether numerics of different types are properly sorted. The list also contains a datatype that would be
	 * sorted between the numerics if the datatypes were to be sorted alphabetically.
	 */
	@Test
	public void testOrder3() throws Exception {
		Literal year1234 = vf.createLiteral("1234", XSD.GYEAR);
		Literal float2000 = vf.createLiteral(2000f);
		Literal int1000 = vf.createLiteral(1000);

		List<Literal> valueList = Arrays.asList(year1234, float2000, int1000);
		Collections.sort(valueList, cmp);
		assertTrue(valueList.indexOf(int1000) < valueList.indexOf(float2000));
	}

	@Test
	public void testNonStrictComparisons() throws Exception {
		cmp.setStrict(false);
		assertTrue(cmp.isStrict() == false);
		Literal date1 = vf.createLiteral("2019-09-02", XSD.DATE);
		Literal date2 = vf.createLiteral("2018", XSD.GYEAR);
		assertTrue(cmp.compare(date1, date2) > 0);
	}

	@Test
	public void testStrictComparisons() throws Exception {
		cmp.setStrict(true);
		assertTrue(cmp.isStrict() == true);
		Literal date1 = vf.createLiteral("2019-09-02", XSD.DATE);
		Literal date2 = vf.createLiteral("2018", XSD.GYEAR);
		assertTrue(cmp.compare(date1, date2) < 0);
	}
}
