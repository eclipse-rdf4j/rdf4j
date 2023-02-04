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
package org.eclipse.rdf4j.query.algebra.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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

}
