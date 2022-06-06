/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.geosparql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.Test;

/**
 * @author jeen
 */
public class BufferTest {

	private final Buffer buffer = new Buffer();

	private final ValueFactory f = SimpleValueFactory.getInstance();

	private final Literal point = f.createLiteral("POINT(23.708496093749996 37.95719224376526)", GEO.WKT_LITERAL);

	private final IRI unit = f.createIRI("http://www.opengis.net/def/uom/OGC/1.0/metre");

	@Test
	public void testEvaluateWithIntRadius() {
		Value result = buffer.evaluate(f, point, f.createLiteral(1), unit);
		assertNotNull(result);
	}

	@Test
	public void testEvaluateWithDoubleRadius() {
		Value result = buffer.evaluate(f, point, f.createLiteral(1.0), unit);
		assertNotNull(result);
	}

	@Test
	public void testEvaluateWithDecimalRadius() {
		Value result = buffer.evaluate(f, point, f.createLiteral("1.0", XSD.DECIMAL), unit);
		assertNotNull(result);
	}

	@Test
	public void resultIsPolygonWKT() {
		Literal result = (Literal) buffer.evaluate(f, point, f.createLiteral(1), unit);
		assertNotNull(result);
		assertThat(result.getDatatype()).isEqualTo(GEO.WKT_LITERAL);
		assertThat(result.getLabel()).startsWith("POLYGON ((23.708505");
	}

	@Test(expected = ValueExprEvaluationException.class)
	public void testEvaluateWithInvalidRadius() {
		buffer.evaluate(f, point, f.createLiteral("foobar", XSD.DECIMAL), unit);
	}

	@Test(expected = ValueExprEvaluationException.class)
	public void testEvaluateWithInvalidUnit() {
		buffer.evaluate(f, point, f.createLiteral(1.0), FOAF.PERSON);
	}
}
