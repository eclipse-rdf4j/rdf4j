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

import org.junit.Test;

/**
 * @author Arjohn Kampman
 */
public class URIUtilTest {

	@Test
	public void testIsCorrectURISplit() throws Exception {
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
	public void testIsValidURIReference() throws Exception {
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
	}
}
