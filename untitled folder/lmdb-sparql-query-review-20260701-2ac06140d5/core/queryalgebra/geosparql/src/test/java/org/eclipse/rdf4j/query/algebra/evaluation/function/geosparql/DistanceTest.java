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

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.junit.jupiter.api.Test;

/**
 * @author Bart Hanssens
 */
public class DistanceTest {

	/**
	 * Test distance between two cities, in meters.
	 *
	 * @throws IOException
	 */
	@Test
	public void testDistanceAmBxl() throws IOException {
		BindingSet bs = GeoSPARQLTests.getBindingSet("distance.rq");

		assertNotNull(bs, "Bindingset is null");

		Value value = bs.getBinding("dist").getValue();
		assertNotNull(value, "Binded value is null");

		assertTrue(value instanceof Literal, "Value is not a literal");
		Literal l = (Literal) value;
		assertTrue(l.getDatatype().equals(XSD.DOUBLE), "Literal not of type double");

		assertEquals(173, l.doubleValue() / 1000, 0.5, "Distance Amsterdam-Brussels not correct");
	}

	/**
	 * Test if distance between cities is the same in both directions
	 *
	 * @throws IOException
	 */
	@Test
	public void testDistanceSame() throws IOException {
		BindingSet bs = GeoSPARQLTests.getBindingSet("distance_same.rq");

		assertNotNull(bs, "Bindingset is null");

		Value v1 = bs.getBinding("dist1").getValue();
		double ambxl = ((Literal) v1).doubleValue();

		Value v2 = bs.getBinding("dist2").getValue();
		double bxlam = ((Literal) v2).doubleValue();

		assertEquals(ambxl, bxlam, 0.1, "Distance Amsterdam-Brussels not correct");
	}
}
