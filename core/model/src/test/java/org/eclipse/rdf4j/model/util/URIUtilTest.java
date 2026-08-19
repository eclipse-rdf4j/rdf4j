/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Arjohn Kampman
 */
public class URIUtilTest {

	@Test
	public void testIsCorrectURISplit() {
		assertTrue(URIUtil.isCorrectURISplit("http://www.example.org/page#", ""));
		assertTrue(URIUtil.isCorrectURISplit("http://www.example.org/page#", "1"));
		assertTrue(URIUtil.isCorrectURISplit("http://www.example.org/page#", "1/2"));
		assertTrue(URIUtil.isCorrectURISplit("http://www.example.org/page#", "1:2"));
		assertFalse(URIUtil.isCorrectURISplit("http://www.example.org/page#", "1#2"));
		assertTrue(URIUtil.isCorrectURISplit("http://www.example.org/page/", ""));
		assertTrue(URIUtil.isCorrectURISplit("http://www.example.org/page/", "1"));
		assertTrue(URIUtil.isCorrectURISplit("http://www.example.org/page/", "1:2"));
		assertTrue(URIUtil.isCorrectURISplit("isbn:", ""));
		assertTrue(URIUtil.isCorrectURISplit("isbn:", "1"));

		assertFalse(URIUtil.isCorrectURISplit("http://www.example.org/page#1#", "2"));
		assertFalse(URIUtil.isCorrectURISplit("http://www.example.org/page", "#1"));
		assertFalse(URIUtil.isCorrectURISplit("http://www.example.org/page/", "1/2"));
		assertFalse(URIUtil.isCorrectURISplit("http://www.example.org/page/", "1#2"));
		assertFalse(URIUtil.isCorrectURISplit("http://www.example.org/page", "2"));
		assertFalse(URIUtil.isCorrectURISplit("http://www.example.org/page/1:", "2"));
		assertFalse(URIUtil.isCorrectURISplit("isbn:", "1#2"));
		assertFalse(URIUtil.isCorrectURISplit("isbn:", "1/2"));
		assertFalse(URIUtil.isCorrectURISplit("isbn:", "1:2"));

	}

	@Test
	public void testIsValidURIReference() {
		assertTrue(URIUtil.isValidURIReference("http://example.org/foo/bar/"));
		assertTrue("whitespace should be allowed",
				URIUtil.isValidURIReference("http://example.org/foo/bar with a lot of space/"));
		assertTrue("unwise chars should be allowed",
				URIUtil.isValidURIReference("http://example.org/foo/bar/unwise{<characters>}"));
		assertTrue("query params in single quotes should be allowed",
				URIUtil.isValidURIReference("http://example.org/foo/bar?query='blah'"));
		assertTrue("query params in double quotes should be allowed",
				URIUtil.isValidURIReference("http://example.org/foo/bar?query=\"blah\"&foo=bar"));
		assertTrue("short simple urns should be allowed", URIUtil.isValidURIReference("urn:p1"));
		assertTrue("Escaped special char should be allowed",
				URIUtil.isValidURIReference("http://example.org/foo\\u00ea/bar/"));
		assertTrue("fragment identifier should be allowed",
				URIUtil.isValidURIReference("http://example.org/foo/bar#fragment1"));
		assertTrue("Unescaped special char should be allowed",
				URIUtil.isValidURIReference("http://example.org/foo®/bar/"));
		assertFalse("control char should not be allowed",
				URIUtil.isValidURIReference("http://example.org/foo\u0001/bar/"));
		assertFalse("relative uri should fail", URIUtil.isValidURIReference("foo/bar/"));
		assertFalse("single column is not a valid uri", URIUtil.isValidURIReference(":"));
		assertTrue("reserved char is allowed in non-conflicting spot",
				URIUtil.isValidURIReference("http://foo.com/b!ar/"));
		assertFalse("reserved char should not be allowed in conflicting spot",
				URIUtil.isValidURIReference("http;://foo.com/bar/"));
	}

	@Test
	public void controlCharacterInURI() {
		assertFalse("URI containing Unicode control char should be invalid",
				URIUtil.isValidURIReference("http://example.org/foo\u001F/bar/"));
	}

	@Test
	public void isValidLocalName() {
		assertTrue(URIUtil.isValidLocalName("2bar"));
		assertTrue(URIUtil.isValidLocalName("foobar"));
		assertTrue(URIUtil.isValidLocalName("_foobar"));
		assertTrue(URIUtil.isValidLocalName("foo-bar"));
		assertTrue(URIUtil.isValidLocalName("foo.bar"));
		assertTrue(URIUtil.isValidLocalName(":foobar"));
		assertTrue(URIUtil.isValidLocalName(":foobär"));
		assertTrue(URIUtil.isValidLocalName(""));

		assertFalse(URIUtil.isValidLocalName(" "));
		assertFalse(URIUtil.isValidLocalName("foo$bar"));
		assertFalse(URIUtil.isValidLocalName("$foobar"));
		assertFalse(URIUtil.isValidLocalName("foo~bar"));
		assertFalse(URIUtil.isValidLocalName("~foobar"));
		assertFalse(URIUtil.isValidLocalName("-foobar"));
		assertFalse(URIUtil.isValidLocalName("[foobar]"));
		assertFalse(URIUtil.isValidLocalName("foobar]"));
		assertFalse(URIUtil.isValidLocalName("(foobar)"));
		assertFalse(URIUtil.isValidLocalName("foobar)"));
		assertFalse(URIUtil.isValidLocalName("{foobar}"));
		assertFalse(URIUtil.isValidLocalName("foobar}"));
		assertFalse(URIUtil.isValidLocalName(".foobar"));
		assertFalse(URIUtil.isValidLocalName("foo\tbar"));
		assertFalse(URIUtil.isValidLocalName("foo\rbar"));
		assertFalse(URIUtil.isValidLocalName("foo\tbar"));
		assertFalse(URIUtil.isValidLocalName("foo\nbar"));
		assertFalse(URIUtil.isValidLocalName("*foobar"));
		assertTrue(URIUtil.isValidLocalName("fo\\'obar"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"http://example.org/x",
			"https://example.org/x?y=z#frag",
			"urn:isbn:0451450523",
			"mailto:user@example.org",
			"tel:+1-816-555-1212",
			"file:///tmp/data.ttl",
			"a:b", // single-letter scheme is valid
			"z:", // scheme with empty hier-part
			"news:comp.infosystems",
			"scheme+ext.1-x:path", // ALPHA *( ALPHA / DIGIT / + / - / . )
			"HTTP://EXAMPLE.ORG/x" // scheme is case-insensitive
	})
	void absoluteIris(String iri) {
		Assertions.assertTrue(URIUtil.isAbsoluteIri(iri),
				"should be classified as absolute: " + iri);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"", // same-document reference
			"#frag",
			"#a:b", // colon in fragment
			"?q=a:b", // colon in query
			"path/foo:bar", // colon in a later path segment
			"./a:b", // dot-segment then colon — classic RFC 3986 example
			"foo/bar",
			"../up/one",
			"//example.org/x", // network-path reference: relative!
			"/rooted/path",
			":foo", // empty scheme is not a scheme
			"1http://example.org/", // scheme must start with ALPHA
			"-http:x",
			"+ssh:x",
			"ht tp://example.org/", // space before colon breaks scheme
			"héllo:x", // non-ASCII letter — not a valid scheme
			"http٣x:y", // Arabic-Indic digit in scheme
			"ｈttp://example.org/" // fullwidth letter
	})
	void relativeReferences(String iri) {
		Assertions.assertFalse(URIUtil.isAbsoluteIri(iri),
				"should be classified as relative: " + iri);
	}
}
