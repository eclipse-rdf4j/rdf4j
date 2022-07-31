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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.junit.Test;

/**
 *
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

		assertNotNull("Bindingset is null", bs);

		Value value = bs.getBinding("within").getValue();
		assertNotNull("Binded value is null", value);

		assertTrue("Value is not a literal", value instanceof Literal);
		Literal l = (Literal) value;
		assertTrue("Literal not of type double", l.getDatatype().equals(XSD.BOOLEAN));

		assertTrue("Denver not within Colorado", l.booleanValue());
	}

	/**
	 * Test sf:within
	 *
	 * @throws java.io.IOException
	 */
	@Test
	public void testBrusselsSfWithinColorado() throws IOException {
		BindingSet bs = GeoSPARQLTests.getBindingSet("sfwithin_brussels.rq");

		assertNotNull("Bindingset is null", bs);

		Value value = bs.getBinding("within").getValue();
		assertNotNull("Binded value is null", value);

		assertTrue("Value is not a literal", value instanceof Literal);
		Literal l = (Literal) value;
		assertTrue("Literal not of type double", l.getDatatype().equals(XSD.BOOLEAN));

		assertFalse("Brussels within Colorado", l.booleanValue());
	}

	@Override
	protected GeometricRelationFunction testedFunction() {
		return new SfWithin();
	}
}
