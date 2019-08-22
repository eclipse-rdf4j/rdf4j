/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.junit.Before;
import org.junit.Test;

public class QueryBindingSetTest {

	private final MapBindingSet mbs = new MapBindingSet();
	private final QueryBindingSet qbs = new QueryBindingSet();

	private ValueFactory vf = SimpleValueFactory.getInstance();

	@Before
	public void setup() {
		qbs.addBinding("foo", vf.createIRI("urn:foo"));
		mbs.addBinding("foo", vf.createIRI("urn:foo"));
	}

	@Test
	public void testEqualsMapBindingSet() {

		QueryBindingSet bs = new QueryBindingSet();
		assertFalse(bs.equals(qbs));
		assertFalse(bs.equals(mbs));

		bs.addBinding("foo", vf.createIRI("urn:foo"));

		assertEquals(bs, qbs);
		assertEquals(bs, mbs);
		assertEquals(qbs, mbs);
	}

	@Test
	public void testHashcodeMapBindingSet() {
		assertTrue(qbs.equals(mbs));
		assertTrue(mbs.equals(qbs));
		assertEquals("objects that return true on their equals() method must have identical hash codes", qbs.hashCode(),
				mbs.hashCode());
	}

	/**
	 * Verifies that the BindingSet implementation honors the API spec for {@link BindingSet#equals(Object)} and
	 * {@link BindingSet#hashCode()}.
	 */
	@Test
	public void testEqualsHashcode() {
		QueryBindingSet bs1 = new QueryBindingSet();
		QueryBindingSet bs2 = new QueryBindingSet();

		bs1.addBinding("x", RDF.ALT);
		bs1.addBinding("y", RDF.BAG);
		bs1.addBinding("z", RDF.FIRST);

		bs2.addBinding("y", RDF.BAG);
		bs2.addBinding("x", RDF.ALT);
		bs2.addBinding("z", RDF.FIRST);
		assertEquals(bs1, bs2);
		assertEquals(bs1.hashCode(), bs2.hashCode());
	}

}
