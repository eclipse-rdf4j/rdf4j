/*
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.repository.sparql.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.Test;

/**
 * Verifies that QueryStringUtil converts values to their SPARQL string representations.
 */
public class QueryStringUtilTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@Test
	public void testIRI() {
		IRI iri = VF.createIRI("http://example.com/test");

		assertEquals("<http://example.com/test>", QueryStringUtil.valueToString(iri));
		assertEquals("<http://example.com/test>", valueToStringWithStringBuilder(iri));
	}

	@Test
	public void testSimpleLiteral() {
		Literal literal = VF.createLiteral("simple \"literal\"");

		assertEquals("\"simple \\\"literal\\\"\"", QueryStringUtil.valueToString(literal));
		assertEquals("\"simple \\\"literal\\\"\"", valueToStringWithStringBuilder(literal));
	}

	@Test
	public void testLanguageLiteral() {
		Literal literal = VF.createLiteral("lang \"literal\"", "en");

		assertEquals("\"lang \\\"literal\\\"\"@en", QueryStringUtil.valueToString(literal));
		assertEquals("\"lang \\\"literal\\\"\"@en", valueToStringWithStringBuilder(literal));
	}

	@Test
	public void testTypedLiteral() {
		Literal literal = VF.createLiteral("typed \"literal\"", VF.createIRI("http://example.com/test"));

		assertEquals("\"typed \\\"literal\\\"\"^^<http://example.com/test>", QueryStringUtil.valueToString(literal));
		assertEquals("\"typed \\\"literal\\\"\"^^<http://example.com/test>", valueToStringWithStringBuilder(literal));
	}

	@Test
	public void testNullValue() {
		assertEquals("UNDEF", QueryStringUtil.valueToString(null));
		assertEquals("UNDEF", valueToStringWithStringBuilder(null));
	}

	@Test
	public void testBNode() {
		try {
			QueryStringUtil.valueToString(VF.createBNode());
			fail("Must throw exception");
		} catch (IllegalArgumentException e) {
			// ok
		}

		try {
			valueToStringWithStringBuilder(VF.createBNode());
			fail("Must throw exception");
		} catch (IllegalArgumentException e) {
			// ok
		}
	}

	private String valueToStringWithStringBuilder(Value value) {
		return QueryStringUtil.appendValueAsString(new StringBuilder(), value).toString();
	}
}
