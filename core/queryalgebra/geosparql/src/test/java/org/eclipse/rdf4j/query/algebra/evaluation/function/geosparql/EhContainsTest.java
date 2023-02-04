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
package org.eclipse.rdf4j.query.algebra.evaluation.function.geosparql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.junit.jupiter.api.Test;

/**
 * @author Bart Hanssens
 */
public class EhContainsTest extends GeometricRelationFunctionTest {

	/**
	 * Test ehContains.
	 *
	 * @throws IOException
	 */
	@Test
	public void testEhContainsDenver() throws IOException {
		List<BindingSet> list = GeoSPARQLTests.getResults("ehcontains.rq");

		assertNotNull(list, "Resultset is null");
		assertEquals(1, list.size(), "Number of results must be one");

		Value value = list.get(0).getBinding("city").getValue();
		assertNotNull(value, "Binded value is null");

		assertTrue(value instanceof IRI, "Value is not an IRI");
		IRI iri = (IRI) value;

		assertEquals("http://example.org/denver", iri.stringValue(), "City is not Denver");
	}

	@Override
	protected GeometricRelationFunction testedFunction() {
		return new EhContains();
	}
}
