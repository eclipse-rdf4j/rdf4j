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

import static org.junit.jupiter.api.Assertions.assertFalse;
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
public class SfWithinTest extends GeometricRelationFunctionTest {

	/**
	 * Test sfWithin
	 *
	 * @throws IOException
	 */
	@Test
	public void testDenverSfWithinColorado() throws IOException {
		BindingSet bs = GeoSPARQLTests.getBindingSet("sfwithin_denver.rq");

		assertNotNull(bs, "Bindingset is null");

		Value value = bs.getBinding("within").getValue();
		assertNotNull(value, "Binded value is null");

		assertTrue(value instanceof Literal, "Value is not a literal");
		Literal l = (Literal) value;
		assertTrue(l.getDatatype().equals(XSD.BOOLEAN), "Literal not of type double");

		assertTrue(l.booleanValue(), "Denver not within Colorado");
	}

	/**
	 * Test sf:within
	 *
	 * @throws java.io.IOException
	 */
	@Test
	public void testBrusselsSfWithinColorado() throws IOException {
		BindingSet bs = GeoSPARQLTests.getBindingSet("sfwithin_brussels.rq");

		assertNotNull(bs, "Bindingset is null");

		Value value = bs.getBinding("within").getValue();
		assertNotNull(value, "Binded value is null");

		assertTrue(value instanceof Literal, "Value is not a literal");
		Literal l = (Literal) value;
		assertTrue(l.getDatatype().equals(XSD.BOOLEAN), "Literal not of type double");

		assertFalse(l.booleanValue(), "Brussels within Colorado");
	}

	@Override
	protected GeometricRelationFunction testedFunction() {
		return new SfWithin();
	}
}
