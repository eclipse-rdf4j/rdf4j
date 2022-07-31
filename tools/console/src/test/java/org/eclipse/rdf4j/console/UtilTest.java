/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Bart Hanssens
 */
public class UtilTest {
	private static Repository repo;

	@BeforeAll
	public static void setupClass() {
		repo = new SailRepository(new MemoryStore());
	}

	@AfterAll
	public static void tearDownClass() {
		repo.shutDown();
	}

	@Test
	public final void testContextsTwo() {
		String ONE = "http://one.rdf4j.org";
		String TWO = "_:two";

		ValueFactory f = repo.getValueFactory();
		Resource[] check = new Resource[] { f.createIRI(ONE), f.createBNode(TWO.substring(2)) };

		String[] tokens = { "command", ONE, TWO };
		Resource[] ctxs = Util.getContexts(tokens, 1, repo);

		assertTrue(Arrays.equals(check, ctxs), "Not equal");
	}

	@Test
	public final void testContextsNull() {
		String[] tokens = { "command", "command2", "NULL" };

		Resource[] ctxs = Util.getContexts(tokens, 2, repo);
		assertTrue(ctxs[0] == null, "Not null");
	}

	@Test
	public final void testContextsInvalid() {
		String[] tokens = { "command", "invalid" };

		try {
			Resource[] ctxs = Util.getContexts(tokens, 1, repo);
			fail("No exception generated");
		} catch (IllegalArgumentException expected) {
		}
	}

	@Test
	public final void testFormatToWidth() {
		String str = "one, two, three, four, five, six, seven, eight";

		String expect = " one, two\n" + " three\n" + " four\n" + " five, six\n" + " seven\n" + " eight";
		String fmt = Util.formatToWidth(10, " ", str, ", ");
		assertTrue(expect.equals(fmt), "Format not OK");
	}
}
