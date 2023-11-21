/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArrayBindingSetTest {

	private final MapBindingSet mbs = new MapBindingSet();
	private final ArrayBindingSet qbs = new ArrayBindingSet("foo");

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@BeforeEach
	public void setup() {
		qbs.getDirectSetBinding("foo").accept(vf.createIRI("urn:foo"), qbs);
		mbs.addBinding("foo", vf.createIRI("urn:foo"));
	}

	@Test
	public void testEqualsMapBindingSet() {

		ArrayBindingSet bs = new ArrayBindingSet("foo");
		assertFalse(bs.equals(qbs));
		assertFalse(bs.equals(mbs));

		bs.getDirectSetBinding("foo").accept(vf.createIRI("urn:foo"), bs);

		assertEquals(bs, qbs);
		assertEquals(bs, mbs);
		assertEquals(qbs, mbs);
	}

	@Test
	public void testHashcodeMapBindingSet() {
		assertTrue(qbs.equals(mbs));
		assertTrue(mbs.equals(qbs));
		assertEquals(qbs.hashCode(), mbs.hashCode(),
				"objects that return true on their equals() method must have identical hash codes");
	}

	/**
	 * Verifies that the BindingSet implementation honors the API spec for {@link BindingSet#equals(Object)} and
	 * {@link BindingSet#hashCode()}.
	 */
	@Test
	public void testEqualsHashcode() {
		ArrayBindingSet bs1 = new ArrayBindingSet("x", "y", "z");
		ArrayBindingSet bs2 = new ArrayBindingSet("x", "y", "z");

		bs1.getDirectSetBinding("x").accept(RDF.ALT, bs1);
		bs1.getDirectSetBinding("y").accept(RDF.BAG, bs1);
		bs1.getDirectSetBinding("z").accept(RDF.FIRST, bs1);

		bs2.getDirectSetBinding("y").accept(RDF.BAG, bs2);
		bs2.getDirectSetBinding("x").accept(RDF.ALT, bs2);
		bs2.getDirectSetBinding("z").accept(RDF.FIRST, bs2);
		assertEquals(bs1, bs2);
		assertEquals(bs1.hashCode(), bs2.hashCode());
	}

	@Test
	public void testEmptyIterator() {

		ArrayBindingSet bs = new ArrayBindingSet("foo");
		Iterator<Binding> iterator = bs.iterator();
		assertNotNull(iterator);
		assertFalse(iterator.hasNext());
	}

	@Test
	public void testOneElementIterator() {
		ArrayBindingSet bs = new ArrayBindingSet("foo");
		bs.setBinding("foo", RDF.FIRST);
		Iterator<Binding> iterator = bs.iterator();
		assertNotNull(iterator);
		assertTrue(iterator.hasNext());
		assertTrue(iterator.hasNext());
		assertTrue(iterator.hasNext());
		assertNotNull(iterator.next());
		assertFalse(iterator.hasNext());
		try {
			iterator.next();
			fail("There are no more elements");
		} catch (NoSuchElementException e) {
			assertNotNull(e);
		}
	}

	@Test
	public void testThreeElementIterator() {
		ArrayBindingSet bs = new ArrayBindingSet("first", "alt", "bag");
		bs.setBinding("first", RDF.FIRST);
		bs.setBinding("alt", RDF.ALT);
		bs.setBinding("bag", RDF.BAG);
		Iterator<Binding> iterator = bs.iterator();
		assertNotNull(iterator);
		assertTrue(iterator.hasNext());
		Binding first = iterator.next();
		assertNotNull(first);
		assertTrue(iterator.hasNext());
		Binding alt = iterator.next();
		assertNotNull(alt);
		assertEquals("alt", alt.getName());
		assertTrue(iterator.hasNext());
		Binding bag = iterator.next();
		assertNotNull(bag);
		assertEquals("bag", bag.getName());
		assertFalse(iterator.hasNext());
		try {
			iterator.next();
			fail("There are no more elements");
		} catch (NoSuchElementException e) {
			assertNotNull(e);
		}
	}

	@Test
	public void testThreeWithTwoElementsSetIterator() {
		ArrayBindingSet bs = new ArrayBindingSet("first", "alt", "bag");
		bs.setBinding("first", RDF.FIRST);
		bs.setBinding("bag", RDF.BAG);
		Iterator<Binding> iterator = bs.iterator();
		assertNotNull(iterator);
		assertTrue(iterator.hasNext());
		Binding first = iterator.next();
		assertNotNull(first);
		assertTrue(iterator.hasNext());
		Binding bag = iterator.next();
		assertNotNull(bag);
		assertEquals("bag", bag.getName());
		assertFalse(iterator.hasNext());
		try {
			iterator.next();
			fail("There are no more elements");
		} catch (NoSuchElementException e) {
			assertNotNull(e);
		}
	}
}
