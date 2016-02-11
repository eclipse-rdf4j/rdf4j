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
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.junit.Before;
import org.junit.Test;

public class QueryBindingSetTest {

	private final MapBindingSet mbs = new MapBindingSet();

	private final QueryBindingSet qbs = new QueryBindingSet();

	private ValueFactory vf = ValueFactoryImpl.getInstance();

	@Before
	public void setup() {
		qbs.addBinding("foo", vf.createURI("urn:foo"));
		mbs.addBinding("foo", vf.createURI("urn:foo"));
	}

	@Test
	public void testEqualsObject() {

		QueryBindingSet bs = new QueryBindingSet();
		assertFalse(bs.equals(qbs));
		assertFalse(bs.equals(mbs));

		bs.addBinding("foo", vf.createURI("urn:foo"));

		assertEquals(bs, qbs);
		assertEquals(bs, mbs);
		assertEquals(qbs, mbs);
	}

	@Test
	public void testHashCode() {
		assertTrue(qbs.equals(mbs));
		assertTrue(mbs.equals(qbs));
		assertEquals("objects that return true on their equals() method must have identical hash codes",
				qbs.hashCode(), mbs.hashCode());
	}

}
