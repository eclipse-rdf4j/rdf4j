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
package org.eclipse.rdf4j.common.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;

import org.junit.Test;

/**
 * @author Joseph Walton
 * @author James Leigh
 */
public class ParsedURITest {

	@Test
	public void absoluteHttpUriIsDescribedCorrectly() {
		ParsedURI uri = new ParsedURI("http://example.test/");
		assertTrue(uri.isAbsolute());
		assertTrue(uri.isHierarchical());
		assertEquals("http", uri.getScheme());
		assertFalse(uri.isOpaque());
	}

	@Test
	public void uriReferenceIsDescribedCorrectly() {
		ParsedURI uri = new ParsedURI("/path");
		assertFalse(uri.isAbsolute());
		assertTrue(uri.isHierarchical());
		assertNull(uri.getScheme());
		assertFalse(uri.isOpaque());
	}

	@Test
	public void jarUrisAppearAsAbsoluteAndHierarchical() {
		ParsedURI uri = new ParsedURI("jar:http://example.test/bar/baz.jar!/COM/foo/Quux.class");
		assertTrue(uri.isAbsolute());
		assertTrue(uri.isHierarchical());
		assertFalse(uri.isOpaque());
		assertEquals("/COM/foo/Quux.class", uri.getPath());

		uri.normalize();
		assertEquals("/COM/foo/Quux.class", uri.getPath());
	}

	@Test
	public void jarUriWithHttpStringifiesToOriginalForm() {
		ParsedURI uri = new ParsedURI("jar:http://example.test/bar/baz.jar!/COM/foo/Quux.class");
		assertEquals("jar:http://example.test/bar/baz.jar!/COM/foo/Quux.class", uri.toString());
	}

	@Test
	public void jarUriWithFileStringifiesToOriginalForm() {
		ParsedURI uri = new ParsedURI("jar:file:///some-file.jar!/another-file");
		assertEquals("jar:file:///some-file.jar!/another-file", uri.toString());
	}

	@Test
	public void resolvesAnAbsoluteUriRelativeToABaseJarUri() {
		ParsedURI uri = new ParsedURI("jar:file:///some-file.jar!/some-nested-file");
		assertEquals("http://example.test/", uri.resolve("http://example.test/").toString());
	}

	@Test
	public void resolvesAPathRelativeUriRelativeToABaseJarUri() {
		ParsedURI uri = new ParsedURI("jar:file:///some-file.jar!/some-nested-file");
		assertEquals("jar:file:///some-file.jar!/another-file", uri.resolve("another-file").toString());
	}

	@Test
	public void resolvesAPathAbsoluteUriRelativeToABaseJarUri() {
		ParsedURI uri = new ParsedURI("jar:file:///some-file.jar!/nested-directory/some-nested-file");
		assertEquals("jar:file:///some-file.jar!/another-file", uri.resolve("/another-file").toString());
	}

	@Test
	public void testRoundTripQueryString() throws Exception {
		assertRoundTrip(
				"http://localhost:8080/callimachus/pipelines/render-html.xpl?result&template=http%3A%2F%2Flocalhost%3A8080%2Fcallimachus%2Fconcept-view.xhtml%3Ftemplate%26realm%3Dhttp%3A%2F%2Flocalhost%3A8080%2F&this=http%3A%2F%2Flocalhost%3A8080%2Fsun&query=view");
	}

	private void assertRoundTrip(String uri) throws URISyntaxException {
		assertResolves(uri, "http://example.com/", uri);
	}

	@Test
	public void testParentFile() throws URISyntaxException {
		assertResolves("../dir", "http://example.com/dir/dir/file", "http://example.com/dir/dir");
	}

	@Test
	public void testRootFile() throws URISyntaxException {
		assertResolves("/dir", "http://example.com/dir/dir", "http://example.com/dir");
	}

	@Test
	public void testFrag() throws URISyntaxException {
		assertResolves("#frag", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/dir/file?qs#frag");
	}

	@Test
	public void testIdentity() throws URISyntaxException {
		assertResolves("", "http://example.com/dir/dir/file?qs", "http://example.com/dir/dir/file?qs");
	}

	@Test
	public void testOpaque() throws URISyntaxException {
		assertResolves("urn:test", "http://example.com/dir/dir/file?qs#frag", "urn:test");
	}

	@Test
	public void testFragment() throws URISyntaxException {
		assertResolves("#frag2", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/dir/file?qs#frag2");
	}

	@Test
	public void testQueryString() throws URISyntaxException {
		assertResolves("?qs2#frag", "http://example.com/dir/dir/file?qs#frag",
				"http://example.com/dir/dir/file?qs2#frag");
	}

	@Test
	public void testDirectory() throws URISyntaxException {
		assertResolves(".", "http://example.com/dir/dir/file?qs#frag", "http://example.com/dir/dir/");
	}

	@Test
	public void testSameDirectory() throws URISyntaxException {
		assertResolves("file2?qs#frag", "http://example.com/dir/dir/file?qs#frag",
				"http://example.com/dir/dir/file2?qs#frag");
	}

	@Test
	public void testNestedDirectory() throws URISyntaxException {
		assertResolves("nested/file?qs#frag", "http://example.com/dir/dir/file?qs#frag",
				"http://example.com/dir/dir/nested/file?qs#frag");
	}

	@Test
	public void testParentDirectory() throws URISyntaxException {
		assertResolves("../file?qs#frag", "http://example.com/dir/dir/file?qs#frag",
				"http://example.com/dir/file?qs#frag");
	}

	@Test
	public void testOtherDirectory() throws URISyntaxException {
		assertResolves("../dir2/file?qs#frag", "http://example.com/dir/dir/file?qs#frag",
				"http://example.com/dir/dir2/file?qs#frag");
	}

	@Test
	public void testSameAuthority() throws URISyntaxException {
		assertResolves("/dir2/dir/file?qs#frag", "http://example.com/dir/dir/file?qs#frag",
				"http://example.com/dir2/dir/file?qs#frag");
	}

	@Test
	public void testIdentityDir() throws URISyntaxException {
		assertResolves("", "http://example.com/dir/dir/", "http://example.com/dir/dir/");
	}

	@Test
	public void testOpaqueDir() throws URISyntaxException {
		assertResolves("urn:test", "http://example.com/dir/dir/", "urn:test");
	}

	@Test
	public void testFragmentDir() throws URISyntaxException {
		assertResolves("#frag2", "http://example.com/dir/dir/", "http://example.com/dir/dir/#frag2");
	}

	@Test
	public void testQueryStringDir() throws URISyntaxException {
		assertResolves("?qs2", "http://example.com/dir/dir/", "http://example.com/dir/dir/?qs2");
	}

	@Test
	public void testDirectoryDir() throws URISyntaxException {
		assertResolves("file", "http://example.com/dir/dir/", "http://example.com/dir/dir/file");
	}

	@Test
	public void testSameDirectoryDir() throws URISyntaxException {
		assertResolves("file2?qs#frag", "http://example.com/dir/dir/", "http://example.com/dir/dir/file2?qs#frag");
	}

	@Test
	public void testNestedDirectoryDir() throws URISyntaxException {
		assertResolves("nested/", "http://example.com/dir/dir/", "http://example.com/dir/dir/nested/");
	}

	@Test
	public void testNestedDirectoryFileDir() throws URISyntaxException {
		assertResolves("nested/file?qs#frag", "http://example.com/dir/dir/",
				"http://example.com/dir/dir/nested/file?qs#frag");
	}

	@Test
	public void testParentDirectoryDir() throws URISyntaxException {
		assertResolves("../file?qs#frag", "http://example.com/dir/dir/", "http://example.com/dir/file?qs#frag");
	}

	@Test
	public void testOtherDirectoryDir() throws URISyntaxException {
		assertResolves("../dir2/", "http://example.com/dir/dir/", "http://example.com/dir/dir2/");
	}

	@Test
	public void testOtherDirectoryFileDir() throws URISyntaxException {
		assertResolves("../dir2/file?qs#frag", "http://example.com/dir/dir/",
				"http://example.com/dir/dir2/file?qs#frag");
	}

	@Test
	public void testSameAuthorityDir() throws URISyntaxException {
		assertResolves("/dir2/dir/file?qs#frag", "http://example.com/dir/dir/",
				"http://example.com/dir2/dir/file?qs#frag");
	}

	private void assertResolves(String relative, String base, String absolute) throws URISyntaxException {
		assertEquals(absolute, new ParsedURI(base).resolve(relative).toString());
	}

}
