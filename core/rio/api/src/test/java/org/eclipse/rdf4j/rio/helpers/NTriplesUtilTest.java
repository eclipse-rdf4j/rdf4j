/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.function.Function;

/**
 * Unit tests for {@link NTriplesUtil}
 * 
 * @author Jeen Broekstra
 *
 */
public class NTriplesUtilTest {

	private StringBuilder appendable;
	private ValueFactory f = SimpleValueFactory.getInstance();

	@Before
	public void setUp() throws Exception {
		appendable = new StringBuilder();
	}

	@Test
	public void testAppendWithoutEncoding() throws Exception {
		Literal l = f.createLiteral("Äbc");
		NTriplesUtil.append(l, appendable, true, false);
		assertThat(appendable.toString()).isEqualTo("\"Äbc\"");
	}

	@Test
	public void testAppendWithEncoding() throws Exception {
		Literal l = f.createLiteral("Äbc");
		NTriplesUtil.append(l, appendable, true, true);
		assertThat(appendable.toString()).isEqualTo("\"\\u00C4bc\"");
	}

	@Test
	public void testSerializeTriple() throws IOException {
		Object[] triples = new Object[] {
				f.createTriple(f.createIRI("urn:a"), f.createIRI("urn:b"), f.createIRI("urn:c")),
				"<<<urn:a> <urn:b> <urn:c>>>",
				//
				f.createTriple(f.createTriple(f.createIRI("urn:a"), f.createIRI("urn:b"), f.createIRI("urn:c")),
						DC.SOURCE, f.createLiteral("news")),
				"<<<<<urn:a> <urn:b> <urn:c>>> <http://purl.org/dc/elements/1.1/source> \"news\">>",
				//
				f.createTriple(f.createBNode("bnode1"), f.createIRI("urn:x"),
						f.createTriple(f.createIRI("urn:a"), f.createIRI("urn:b"), f.createIRI("urn:c"))),
				"<<_:bnode1 <urn:x> <<<urn:a> <urn:b> <urn:c>>>>>"
		};

		for (int i = 0; i < triples.length; i += 2) {
			assertEquals(triples[i + 1], NTriplesUtil.toNTriplesString((Triple) triples[i]));
			assertEquals(triples[i + 1], NTriplesUtil.toNTriplesString((Resource) triples[i]));
			assertEquals(triples[i + 1], NTriplesUtil.toNTriplesString((Value) triples[i]));
			NTriplesUtil.append((Triple) triples[i], appendable);
			assertEquals(triples[i + 1], appendable.toString());
			appendable = new StringBuilder();
			NTriplesUtil.append((Resource) triples[i], appendable);
			assertEquals(triples[i + 1], appendable.toString());
			appendable = new StringBuilder();
			NTriplesUtil.append((Value) triples[i], appendable);
			assertEquals(triples[i + 1], appendable.toString());
			appendable = new StringBuilder();
		}
	}

	@Test
	public void testParseTriple() {
		String[] triples = new String[] {
				"<<<http://foo.com/bar#baz%20><http://example.com/test><<<urn:foo><urn:\\u0440>\"täst\"@de-DE>>>>",
				"<<http://foo.com/bar#baz%20 http://example.com/test <<urn:foo urn:р \"täst\"@de-DE>>>>",
				//
				"<< <http://foo.com/bar#baz%20>  <http://example.com/test>  <<  <urn:foo>  <urn:\\u0440> \"täst\"@de-DE  >>  >>",
				"<<http://foo.com/bar#baz%20 http://example.com/test <<urn:foo urn:р \"täst\"@de-DE>>>>",
				//
				"<<<<_:bnode1foobar<urn:täst>\"literál за проба\"^^<urn:test\\u0444\\U00000444>>><http://test/baz>\"test\\\\\\\"lit\">>",
				"<<<<_:bnode1foobar urn:täst \"literál за проба\"^^<urn:testфф>>> http://test/baz \"test\\\"lit\">>",
				//
				"<<  <<_:bnode1foobar<urn:täst> \"literál за проба\"^^<urn:test\\u0444\\U00000444>  >>  <http://test/baz> \"test\\\\\\\"lit\" >>",
				"<<<<_:bnode1foobar urn:täst \"literál за проба\"^^<urn:testфф>>> http://test/baz \"test\\\"lit\">>",
				// test surrogate pair range in bnode
				"<<_:test_\uD800\uDC00_\uD840\uDC00_bnode <urn:x> <urn:y>>>",
				"<<_:test_\uD800\uDC00_\uD840\uDC00_bnode urn:x urn:y>>",
				// invalid: missing closing >> for inner triple
				"<<<<_:bnode1foobar<urn:täst>\"literál за проба\"^^<urn:test\\u0444\\U00000444><http://test/baz>\"test\\\\\\\"lit\">>",
				null,
				// invalid: missing closing >> for outer triple
				"<<<<_:bnode1foobar<urn:täst>\"literál за проба\"^^<urn:test\\u0444\\U00000444>>><http://test/baz>\"test\\\\\\\"lit\"",
				null,
				// invalid: literal subject
				"<<\"test\" <urn:test> \"test\">>",
				null,
				// invalid: bnode predicate
				"<<<urn:test> _:test \"test\">>",
				null,
				// invalid: triple predicate
				"<<<urn:a> <<<urn:1> <urn:2> <urn:3>>> <urn:b>>>",
				null
		};

		for (int i = 0; i < triples.length; i += 2) {
			parseTriple(triples[i], triples[i + 1], (t) -> NTriplesUtil.parseTriple(t, f));
			parseTriple(triples[i], triples[i + 1], (t) -> (Triple) NTriplesUtil.parseValue(t, f));
			parseTriple(triples[i], triples[i + 1], (t) -> (Triple) NTriplesUtil.parseResource(t, f));
		}
	}

	private void parseTriple(String triple, String expected, Function<String, Triple> parser) {
		try {
			Triple t = parser.apply(triple);
			assertEquals(expected, t.stringValue());
		} catch (IllegalArgumentException e) {
			if (expected != null) {
				fail("Unexpected exception for valid triple: " + triple);
			}
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseIRIvsTriple() {
		NTriplesUtil.parseURI("<<<urn:a><urn:b><urn:c>>>", f);
	}
}
