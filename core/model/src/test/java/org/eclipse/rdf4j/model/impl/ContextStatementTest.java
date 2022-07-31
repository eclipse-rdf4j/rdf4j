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
package org.eclipse.rdf4j.model.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ContextStatementTest {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	private static final IRI s1 = vf.createIRI("urn:s1");

	private static final IRI p1 = vf.createIRI("urn:p1");

	private static final IRI o1 = vf.createIRI("urn:o1");

	private static final IRI o2 = vf.createIRI("urn:o2");

	private static final IRI g1 = vf.createIRI("urn:g1");

	private static final IRI g2 = vf.createIRI("urn:g2");

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		Statement st1 = vf.createStatement(s1, p1, o1);
		Statement st2 = vf.createStatement(s1, p1, o1, g1);
		Statement st3 = vf.createStatement(s1, p1, o2);
		Statement st4 = vf.createStatement(s1, p1, o1, g1);
		Statement st5 = vf.createStatement(s1, p1, o1, g2);

		assertNotEquals(st1, st2);
		assertNotEquals(st1, st3);
		assertEquals(st2, st4);
		assertNotEquals(st2, st5);

		Set<Statement> set = new HashSet<>();
		set.add(st1);
		set.add(st2);
		set.add(st3);
		set.add(st4);
		set.add(st5);

		assertEquals(4, set.size());

	}

}
