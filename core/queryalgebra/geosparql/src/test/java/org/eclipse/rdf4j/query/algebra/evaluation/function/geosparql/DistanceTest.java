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

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.junit.Test;

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

		assertNotNull("Bindingset is null", bs);

		Value value = bs.getBinding("dist").getValue();
		assertNotNull("Binded value is null", value);

		assertTrue("Value is not a literal", value instanceof Literal);
		Literal l = (Literal) value;
		assertTrue("Literal not of type double", l.getDatatype().equals(XSD.DOUBLE));

		assertEquals("Distance Amsterdam-Brussels not correct", 173, l.doubleValue() / 1000, 0.5);
	}

	/**
	 * Test if distance between cities is the same in both directions
	 *
	 * @throws IOException
	 */
	@Test
	public void testDistanceSame() throws IOException {
		BindingSet bs = GeoSPARQLTests.getBindingSet("distance_same.rq");

		assertNotNull("Bindingset is null", bs);

		Value v1 = bs.getBinding("dist1").getValue();
		double ambxl = ((Literal) v1).doubleValue();

		Value v2 = bs.getBinding("dist2").getValue();
		double bxlam = ((Literal) v2).doubleValue();

		assertEquals("Distance Amsterdam-Brussels not correct", ambxl, bxlam, 0.1);
	}

}
