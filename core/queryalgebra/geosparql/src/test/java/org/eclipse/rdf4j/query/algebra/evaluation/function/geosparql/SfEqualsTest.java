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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.junit.Test;

public class SfEqualsTest extends GeometricRelationFunctionTest {

	private final ValueFactory f = SimpleValueFactory.getInstance();

	@Override
	protected GeometricRelationFunction testedFunction() {
		return new SfEquals();
	}

	@Test
	public void matchesMultiPolygonWKT() throws IOException {

		String polygon = IOUtils.toString(
				getClass().getResourceAsStream(
						"/org/eclipse/rdf4j/query/algebra/evaluation/function/geosparql/sfequals_polygon.txt"),
				Charset.defaultCharset());
		String multiPolygon = IOUtils.toString(
				getClass().getResourceAsStream(
						"/org/eclipse/rdf4j/query/algebra/evaluation/function/geosparql/sfequals_multipolygon.txt"),
				Charset.defaultCharset());

		Literal polygonLit = f.createLiteral(polygon, GEO.WKT_LITERAL);
		Literal multiPolygonLit = f.createLiteral(multiPolygon, GEO.WKT_LITERAL);

		Literal result = (Literal) testedFunction().evaluate(f, multiPolygonLit, polygonLit);

		assertNotNull(result);
		assertThat(result).isEqualTo(BooleanLiteral.TRUE);
	}
}
