/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Bart.Hanssens
 */
public class ModelCollectorTest {
	private final int nrStmts = 1000;
	private final Set<Statement> stmts = new HashSet<>();
	private final ValueFactory F = SimpleValueFactory.getInstance();

	@Before
	public void setUp() throws Exception {
		for (int i = 0; i < nrStmts; i++) {
			stmts.add(F.createStatement(F.createIRI("http://www.example.com/" + i), RDFS.LABEL, F.createLiteral(i)));
		}
	}

	@Test
	public void testCollector() {
		Model m = stmts.stream().collect(ModelCollector.toModel());
		assertEquals("Number of statements does not match", m.size(), nrStmts);
	}

	@Test
	public void testCollectorParallel() {
		Model m = stmts.parallelStream().collect(ModelCollector.toModel());
		assertEquals("Number of statements does not match", m.size(), nrStmts);
	}
}
