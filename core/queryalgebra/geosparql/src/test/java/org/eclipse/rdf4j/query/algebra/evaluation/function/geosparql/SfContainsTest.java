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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.junit.Test;

/**
 *
 * @author Bart Hanssens
 */
public class SfContainsTest extends GeometricRelationFunctionTest {

	/**
	 * Test sfContains.
	 *
	 * @throws IOException
	 */
	@Test
	public void testSfContainsDenver() throws IOException {
		List<BindingSet> list = GeoSPARQLTests.getResults("sfcontains.rq");

		assertNotNull("Resultset is null", list);
		assertEquals("Number of results must be one", 1, list.size());

		Value value = list.get(0).getBinding("city").getValue();
		assertNotNull("Binded value is null", value);

		assertTrue("Value is not an IRI", value instanceof IRI);
		IRI iri = (IRI) value;

		assertEquals("City is not Denver", "http://example.org/denver", iri.stringValue());
	}

	@Override
	protected GeometricRelationFunction testedFunction() {
		return new SfContains();
	}
}
