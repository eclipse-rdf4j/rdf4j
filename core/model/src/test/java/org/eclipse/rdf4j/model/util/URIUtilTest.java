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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
		assertTrue(URIUtil.isValidURIReference("http://example.org/foo/bar with a lot of space/"),
				"whitespace should be allowed");
		assertTrue(URIUtil.isValidURIReference("http://example.org/foo/bar/unwise{<characters>}"),
				"unwise chars should be allowed");
		assertTrue(URIUtil.isValidURIReference("http://example.org/foo/bar?query='blah'"),
				"query params in single quotes should be allowed");
		assertTrue(URIUtil.isValidURIReference("http://example.org/foo/bar?query=\"blah\"&foo=bar"),
				"query params in double quotes should be allowed");
		assertTrue(URIUtil.isValidURIReference("urn:p1"), "short simple urns should be allowed");
		assertTrue(URIUtil.isValidURIReference("http://example.org/foo\\u00ea/bar/"),
				"Escaped special char should be allowed");
		assertTrue(URIUtil.isValidURIReference("http://example.org/foo/bar#fragment1"),
				"fragment identifier should be allowed");
		assertTrue(URIUtil.isValidURIReference("http://example.org/foo®/bar/"),
				"Unescaped special char should be allowed");
		assertFalse(URIUtil.isValidURIReference("http://example.org/foo\u0001/bar/"),
				"control char should not be allowed");
		assertFalse(URIUtil.isValidURIReference("foo/bar/"), "relative uri should fail");
		assertFalse(URIUtil.isValidURIReference(":"), "single column is not a valid uri");
		assertTrue(URIUtil.isValidURIReference("http://foo.com/b!ar/"),
				"reserved char is allowed in non-conflicting spot");
		assertFalse(URIUtil.isValidURIReference("http;://foo.com/bar/"),
				"reserved char should not be allowed in conflicting spot");
	}

	@Test
	public void controlCharacterInURI() {
		assertFalse(URIUtil.isValidURIReference("http://example.org/foo\u001F/bar/"),
				"URI containing Unicode control char should be invalid");
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
	}
}
