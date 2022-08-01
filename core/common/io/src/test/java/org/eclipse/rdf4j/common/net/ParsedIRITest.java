/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.common.net;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * @author Joseph Walton
 * @author James Leigh
 */
public class ParsedIRITest {

	public static final String GEN = ":/?#[]@";

	public static final String SUB = "!$&'()*+,;=";

	public static final String UNRESERVED = "-._~";

	public static final String ILLEGAL = "<>\" {}|\\^`";

	public static final String DIGIT = "0123456789";

	public static final String LOWER = "abcdefghijklmnopqrstuvwxyz";

	public static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	public static final String BASE = "http://example.com/base";

	public static final String QUERY = "http://example.com/?query";

	public static final String FRAGMENT = "http://example.com/#fragment";

	@Test
	public void hostWithLeadingDigit() {
		String[] hosts = { "1example.com", "1.example.com" };
		for (String host : hosts) {
			assertEquals(host, ParsedIRI.create("http://" + host).getHost());
		}
	}

	@Test(expected = URISyntaxException.class)
	public void testIncorrectIPv4() throws URISyntaxException {
		ParsedIRI iri = new ParsedIRI("http://127.0.0.256/");
	}

	@Test
	public void testUnknownSchemeHostProcessing() throws URISyntaxException {
		ParsedIRI uri = new ParsedIRI("bundleresource://385.fwk19480900/test.ttl");
		assertThat(uri.isAbsolute());
		assertThat(uri.getScheme()).isEqualTo("bundleresource");
		assertThat(uri.isOpaque());
		assertThat(uri.getHost()).isEqualTo("385.fwk19480900");
	}

	@Test(expected = URISyntaxException.class)
	public void testHttpSchemeHostProcessing() throws URISyntaxException {
		ParsedIRI uri = new ParsedIRI("http://385.fwk19480900/test.ttl");
	}

	@Test
	public void absoluteHttpUriIsDescribedCorrectly() throws URISyntaxException {
		ParsedIRI uri = new ParsedIRI("http://example.test/");
		assertTrue(uri.isAbsolute());
		assertEquals("http", uri.getScheme());
		assertFalse(uri.isOpaque());
	}

	@Test
	public void absoluteHttpUriWithHashIsDescribedCorrectly() throws URISyntaxException {
		ParsedIRI uri = new ParsedIRI("http://example.test#");
		assertTrue(uri.isAbsolute());
		assertEquals("http", uri.getScheme());
		assertFalse(uri.isOpaque());
	}

	@Test
	public void absoluteIpAddrDescribedCorrectly() throws URISyntaxException {
		ParsedIRI uri = new ParsedIRI("http://127.0.0.1/path");
		assertTrue(uri.isAbsolute());
		assertEquals(uri.getHost(), "127.0.0.1");
		assertEquals(uri.getPort(), -1);
		assertEquals(uri.getPath(), "/path");
	}

	@Test
	public void absoluteIpAddrNoPath() throws URISyntaxException {
		ParsedIRI uri = new ParsedIRI("http://178.62.246.130");
		assertTrue(uri.isAbsolute());
		assertEquals(uri.getHost(), "178.62.246.130");
		assertEquals(uri.getPort(), -1);
		assertEquals(uri.getPath(), "");
	}

	@Test
	public void absoluteIpAddrWithPortDescribedCorrectly() throws URISyntaxException {
		ParsedIRI uri = new ParsedIRI("http://127.0.0.1:3333/path");
		assertTrue(uri.isAbsolute());
		assertEquals(uri.getHost(), "127.0.0.1");
		assertEquals(uri.getPort(), 3333);
		assertEquals(uri.getPath(), "/path");
	}

	@Test
	public void uriReferenceIsDescribedCorrectly() throws URISyntaxException {
		ParsedIRI uri = new ParsedIRI("/path");
		assertFalse(uri.isAbsolute());
		assertNull(uri.getScheme());
		assertFalse(uri.isOpaque());
	}

	@Test
	public void jarUrisAppearAsAbsoluteAndHierarchical() throws URISyntaxException {
		ParsedIRI uri = new ParsedIRI("jar:http://example.test/bar/baz.jar!/COM/foo/Quux.class");
		assertTrue(uri.isAbsolute());
		assertFalse(uri.isOpaque());
		assertEquals("/bar/baz.jar!/COM/foo/Quux.class", uri.getPath());
		assertEquals("/bar/baz.jar!/COM/foo/Quux.class", uri.normalize().getPath());
	}

	@Test
	public void osgiBundleUri() throws URISyntaxException {
		ParsedIRI uri = new ParsedIRI("bundle://159.0:1/org/eclipse/rdf4j/repository/config/system.ttl");
		assertTrue(uri.isAbsolute());
		assertEquals("bundle", uri.getScheme());
	}

	@Test
	public void jarUriWithHttpStringifiesToOriginalForm() throws URISyntaxException {
		ParsedIRI uri = new ParsedIRI("jar:http://example.test/bar/baz.jar!/COM/foo/Quux.class");
		assertEquals("jar:http://example.test/bar/baz.jar!/COM/foo/Quux.class", uri.toString());
	}

	@Test
	public void jarUriWithFileStringifiesToOriginalForm() throws URISyntaxException {
		ParsedIRI uri = new ParsedIRI("jar:file:///some-file.jar!/another-file");
		assertEquals("jar:file:///some-file.jar!/another-file", uri.toString());
	}

	@Test
	public void resolvesAnAbsoluteUriRelativeToABaseJarUri() throws URISyntaxException {
		ParsedIRI uri = new ParsedIRI("jar:file:///some-file.jar!/some-nested-file");
		assertEquals("http://example.test/", uri.resolve("http://example.test/"));
	}

	@Test
	public void resolvesAPathRelativeUriRelativeToABaseJarUri() throws URISyntaxException {
		ParsedIRI uri = new ParsedIRI("jar:file:///some-file.jar!/some-nested-file");
		assertEquals("jar:file:///some-file.jar!/another-file", uri.resolve("another-file"));
	}

	@Test
	public void testDotSegments() throws URISyntaxException {
		assertEquals("http://example.org/up", new ParsedIRI("http://example.org/").resolve("../up"));
	}

	@Test
	public void testFileScheme() throws URISyntaxException {
		assertEquals("file:/file.txt", new ParsedIRI("file:///file.txt").normalize().toString());
		assertEquals("file:/file.txt", new ParsedIRI("file://localhost/file.txt").normalize().toString());
		assertEquals("file:/file.txt", new ParsedIRI("file:/file.txt").normalize().toString());
		assertEquals("file:/c:/path/to/file", new ParsedIRI("file:///c:/path/to/file").normalize().toString());
		assertEquals("file:/c:/path/to/file", new ParsedIRI("file:/c:/path/to/file").normalize().toString());
		assertEquals("file:/c:/path/to/file", new ParsedIRI("file:c:/path/to/file").normalize().toString());
		assertEquals("file:/c:/path/to/file", ParsedIRI.create("file:///c|/path/to/file").normalize().toString());
		assertEquals("file:/c:/path/to/file", ParsedIRI.create("file:/c|/path/to/file").normalize().toString());
		assertEquals("file:/c:/path/to/file", ParsedIRI.create("file:c|/path/to/file").normalize().toString());
		assertEquals("file:/c:/path/to/file", ParsedIRI.create("file:///c:\\path\\to\\file").normalize().toString());
		assertEquals("file:/c:/path/to/file", ParsedIRI.create("file:/c:\\path\\to\\file").normalize().toString());
		assertEquals("file:/c:/path/to/file", ParsedIRI.create("file:c:\\path\\to\\file").normalize().toString());
	}

	@Test
	public void testURN() throws URISyntaxException {
		assertEquals("urn:test:foo", new ParsedIRI("urn:test:foo").toString());
		assertEquals("urn", new ParsedIRI("urn:test:foo").getScheme());
		assertEquals("test:foo", new ParsedIRI("urn:test:foo").getPath());
		assertTrue(new ParsedIRI("urn:test:foo").isOpaque());
	}

	@Test
	public void testRoundTripQueryString() throws Exception {
		assertRoundTrip(
				"http://localhost:8080/pipelines/render-html.xpl?result&template=http%3A%2F%2Flocalhost%3A8080%2Fconcept-view.xhtml%3Ftemplate%26realm%3Dhttp%3A%2F%2Flocalhost%3A8080%2F&this=http%3A%2F%2Flocalhost%3A8080%2Fsun&query=view");
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
	public void testFragment2() throws URISyntaxException {
		assertResolves("#example", "http://example.com#", "http://example.com#example");
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

	@Test
	public void testNormalExamples() throws URISyntaxException {
		String base = "http://a/b/c/d;p?q";
		assertResolves("g:h", base, "g:h");
		assertResolves("g", base, "http://a/b/c/g");
		assertResolves("./g", base, "http://a/b/c/g");
		assertResolves("g/", base, "http://a/b/c/g/");
		assertResolves("/g", base, "http://a/g");
		assertResolves("//g", base, "http://g");
		assertResolves("?y", base, "http://a/b/c/d;p?y");
		assertResolves("g?y", base, "http://a/b/c/g?y");
		assertResolves("#s", base, "http://a/b/c/d;p?q#s");
		assertResolves("g#s", base, "http://a/b/c/g#s");
		assertResolves("g?y#s", base, "http://a/b/c/g?y#s");
		assertResolves(";x", base, "http://a/b/c/;x");
		assertResolves("g;x", base, "http://a/b/c/g;x");
		assertResolves("g;x?y#s", base, "http://a/b/c/g;x?y#s");
		assertResolves("", base, "http://a/b/c/d;p?q");
		assertResolves(".", base, "http://a/b/c/");
		assertResolves("./", base, "http://a/b/c/");
		assertResolves("..", base, "http://a/b/");
		assertResolves("../", base, "http://a/b/");
		assertResolves("../g", base, "http://a/b/g");
		assertResolves("../..", base, "http://a/");
		assertResolves("../../", base, "http://a/");
		assertResolves("../../g", base, "http://a/g");
	}

	@Test
	public void testAbnormalExamples() throws URISyntaxException {
		String base = "http://a/b/c/d;p?q";
		assertResolves("../../../g", base, "http://a/g");
		assertResolves("../../../../g", base, "http://a/g");
		assertResolves("/./g", base, "http://a/g");
		assertResolves("/../g", base, "http://a/g");
		assertResolves("g.", base, "http://a/b/c/g.");
		assertResolves(".g", base, "http://a/b/c/.g");
		assertResolves("g..", base, "http://a/b/c/g..");
		assertResolves("..g", base, "http://a/b/c/..g");
		assertResolves("./../g", base, "http://a/b/g");
		assertResolves("./g/.", base, "http://a/b/c/g/");
		assertResolves("g/./h", base, "http://a/b/c/g/h");
		assertResolves("g/../h", base, "http://a/b/c/h");
		assertResolves("g;x=1/./y", base, "http://a/b/c/g;x=1/y");
		assertResolves("g;x=1/../y", base, "http://a/b/c/y");
		assertResolves("g?y/./x", base, "http://a/b/c/g?y/./x");
		assertResolves("g?y/../x", base, "http://a/b/c/g?y/../x");
		assertResolves("g#s/./x", base, "http://a/b/c/g#s/./x");
		assertResolves("g#s/../x", base, "http://a/b/c/g#s/../x");
		assertResolves("http:g", base, "http:g");
	}

	@Test
	public void testResolveOpaque() throws URISyntaxException {
		assertResolves("", "urn:test:foo#bar", "urn:test:foo");
		assertResolves("", "urn:test:foo", "urn:test:foo");
		assertResolves("#bar", "urn:test:foo", "urn:test:foo#bar");
		assertResolves("#bat", "urn:test:foo#bar", "urn:test:foo#bat");
	}

	@Test
	public void testDoubleSlash() throws URISyntaxException {
		assertResolves("../xyz", "http://ab//de//ghi", "http://ab//de/xyz");
	}

	private void assertResolves(String relative, String base, String absolute) throws URISyntaxException {
		assertEquals(absolute, new ParsedIRI(base).resolve(relative));
	}

	@Test
	public void testUnreservedInPath() throws Exception {
		for (char g : UNRESERVED.toCharArray()) {
			assertEquals(BASE + g, new ParsedIRI(BASE + g).normalize().toString());
			assertEquals(BASE + g, new ParsedIRI(BASE + encode(g)).normalize().toString());
			assertEquals(BASE + g, new ParsedIRI(BASE + encode(g).toLowerCase()).normalize().toString());
		}
	}

	@Test
	public void testDigitInPath() throws Exception {
		for (char g : DIGIT.toCharArray()) {
			assertEquals(BASE + g, new ParsedIRI(BASE + g).normalize().toString());
			assertEquals(BASE + g, new ParsedIRI(BASE + encode(g)).normalize().toString());
			assertEquals(BASE + g, new ParsedIRI(BASE + encode(g).toLowerCase()).normalize().toString());
		}
	}

	@Test
	public void testUpperInPath() throws Exception {
		for (char g : UPPER.toCharArray()) {
			assertEquals(BASE + g, new ParsedIRI(BASE + g).normalize().toString());
			assertEquals(BASE + g, new ParsedIRI(BASE + encode(g)).normalize().toString());
			assertEquals(BASE + g, new ParsedIRI(BASE + encode(g).toLowerCase()).normalize().toString());
		}
	}

	@Test
	public void testLowerInPath() throws Exception {
		for (char g : LOWER.toCharArray()) {
			assertEquals(BASE + g, new ParsedIRI(BASE + g).normalize().toString());
			assertEquals(BASE + g, new ParsedIRI(BASE + encode(g)).normalize().toString());
			assertEquals(BASE + g, new ParsedIRI(BASE + encode(g).toLowerCase()).normalize().toString());
		}
	}

	@Test
	public void testGenDelimInPath() throws Exception {
		for (char g : ":/@".toCharArray()) {
			assertEquals(BASE + g, new ParsedIRI(BASE + g).normalize().toString());
			assertEquals(BASE + encode(g), new ParsedIRI(BASE + encode(g)).normalize().toString());
			assertEquals(BASE + encode(g), new ParsedIRI(BASE + encode(g).toLowerCase()).normalize().toString());
		}
	}

	@Test
	public void testSubDelimInPath() throws Exception {
		for (char g : SUB.toCharArray()) {
			assertEquals(BASE + g, new ParsedIRI(BASE + g).normalize().toString());
			assertEquals(BASE + encode(g), new ParsedIRI(BASE + encode(g)).normalize().toString());
			assertEquals(BASE + encode(g), new ParsedIRI(BASE + encode(g).toLowerCase()).normalize().toString());
		}
	}

	@Test
	public void testIllegalInPath() throws Exception {
		for (char g : ILLEGAL.toCharArray()) {
			try {
				new ParsedIRI(BASE + g);

			} catch (URISyntaxException e) {
				assertEquals(BASE.length(), e.getIndex());
			}
		}
	}

	@Test
	public void testGenDelimsInQuery() throws Exception {
		for (char g : ":/?@".toCharArray()) {
			assertEquals(QUERY + g, new ParsedIRI(QUERY + g).normalize().toString());
			assertEquals(QUERY + encode(g), new ParsedIRI(QUERY + encode(g)).normalize().toString());
		}
	}

	@Test
	public void testSubDelimsInQuery() throws Exception {
		for (char g : SUB.toCharArray()) {
			assertEquals(QUERY + g, new ParsedIRI(QUERY + g).normalize().toString());
			assertEquals(QUERY + encode(g), new ParsedIRI(QUERY + encode(g)).normalize().toString());
		}
	}

	@Test
	public void testIllegalInQuery() throws Exception {
		for (char g : ILLEGAL.toCharArray()) {
			try {
				new ParsedIRI(QUERY + g);

			} catch (URISyntaxException e) {
				assertEquals(QUERY.length(), e.getIndex());
			}
		}
	}

	@Test
	public void testGenDelimsInFragment() throws Exception {
		for (char g : ":/?@".toCharArray()) {
			assertEquals(FRAGMENT + g, new ParsedIRI(FRAGMENT + g).normalize().toString());
			assertEquals(FRAGMENT + encode(g), new ParsedIRI(FRAGMENT + encode(g)).normalize().toString());
		}
	}

	@Test
	public void testSubDelimsInFragment() throws Exception {
		for (char g : SUB.toCharArray()) {
			assertEquals(FRAGMENT + g, new ParsedIRI(FRAGMENT + g).normalize().toString());
			assertEquals(FRAGMENT + encode(g), new ParsedIRI(FRAGMENT + encode(g)).normalize().toString());
		}
	}

	@Test
	public void testIllegalInFragment() throws Exception {
		for (char g : ILLEGAL.toCharArray()) {
			try {
				new ParsedIRI(FRAGMENT + g);

			} catch (URISyntaxException e) {
				assertEquals(FRAGMENT.length(), e.getIndex());
			}
		}
	}

	private String encode(Character chr) throws UnsupportedEncodingException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] source = Character.toString(chr).getBytes(StandardCharsets.UTF_8);
		for (byte c : source) {
			out.write('%');
			char high = Character.forDigit((c >> 4) & 0xF, 16);
			char low = Character.forDigit(c & 0xF, 16);
			out.write(Character.toUpperCase(high));
			out.write(Character.toUpperCase(low));
		}
		return new String(out.toByteArray(), StandardCharsets.UTF_8);
	}

	@Test
	public void testPercentEncoding() throws URISyntaxException {
		assertEquals("http://example.org/~user", new ParsedIRI("http://example.org/~user").normalize().toString());
		assertEquals("http://example.org/~user", new ParsedIRI("http://example.org/%7euser").normalize().toString());
		assertEquals("http://example.org/~user", new ParsedIRI("http://example.org/%7Euser").normalize().toString());
	}

	@Test
	public void testPortNormalization() throws URISyntaxException {
		assertEquals("http://example.org/", new ParsedIRI("http://example.org").normalize().toString());
		assertEquals("http://example.org/", new ParsedIRI("http://example.org/").normalize().toString());
		assertEquals("http://example.org/", new ParsedIRI("http://example.org:/").normalize().toString());
		assertEquals("http://example.org/", new ParsedIRI("http://example.org:80/").normalize().toString());
	}

	@Test
	public void testToASCII() throws URISyntaxException {
		assertEquals("http://r\u00E9sum\u00E9.example.org/",
				new ParsedIRI("http://r\u00E9sum\u00E9.example.org/").toString());
		assertEquals("http://xn--rsum-bpad.example.org/",
				new ParsedIRI("http://r\u00E9sum\u00E9.example.org/").toASCIIString());
		assertEquals("http://r\u00E9sum\u00E9.example.org/",
				new ParsedIRI("http://xn--rsum-bpad.example.org/").normalize().toString());
		assertEquals("http://r%C3%A9sum%C3%A9.example.org/",
				new ParsedIRI("http://r%C3%A9sum%C3%A9.example.org/").toASCIIString());
		assertEquals("http://xn--rsum-bpad.example.org/",
				new ParsedIRI("http://r%C3%A9sum%C3%A9.example.org/").normalize().toASCIIString());
		assertEquals("http://r\u00E9sum\u00E9.example.org/",
				new ParsedIRI("http://r%C3%A9sum%C3%A9.example.org/").normalize().toString());
	}

	@Test
	public void testToASCIIQuery() throws URISyntaxException {
		assertEquals("http://validator.w3.org/check?uri=http%3A%2F%2Fr%C3%A9sum%C3%A9.example.org",
				new ParsedIRI("http://validator.w3.org/check?uri=http%3A%2F%2Frésumé.example.org").toASCIIString());
		assertEquals("uri=http%3A%2F%2Fr\u00E9sum\u00E9.example.org",
				new ParsedIRI("http://validator.w3.org/check?uri=http%3A%2F%2Fr%C3%A9sum%C3%A9.example.org").normalize()
						.getQuery());
	}

	@Test
	public void testVietnam() throws URISyntaxException {
		String correct = "/Vi\u1EC7t%20Nam";
		String incorrect = "/Vi\u00EA\u0323t%20Nam"; // containing a LATIN SMALL LETTER E WITH CIRCUMFLEX AND DOT BELOW
		assertFalse(correct.equals(incorrect));
		assertEquals("http://example.org" + correct,
				new ParsedIRI("http://example.org" + incorrect).normalize().toString());
		assertEquals("http://example.org/Vi%E1%BB%87t%20Nam",
				new ParsedIRI("http://example.org" + incorrect).normalize().toASCIIString());
		assertEquals(correct,
				new ParsedIRI(new ParsedIRI("http://example.org" + incorrect).toASCIIString()).normalize().getPath());
	}

	@Test
	public void testRedRose() throws URISyntaxException {
		assertEquals("http://www.example.org/red%09ros%C3%A9#red",
				new ParsedIRI("http://www.example.org/red%09ros\u00E9#red").toASCIIString());
	}

	@Test
	public void testWrongEacute() throws URISyntaxException {
		assertEquals("http://www.example.org/r%E9sum%E9.html",
				new ParsedIRI("http://www.example.org/r%E9sum%E9.html").toString());
		assertEquals("http://www.example.org/r%E9sum%E9.html",
				new ParsedIRI("http://www.example.org/r%E9sum%E9.html").normalize().toString());
		assertEquals("http://www.example.org/r%C3%A9sum%C3%A9.html",
				new ParsedIRI("http://www.example.org/r%C3%A9sum%C3%A9.html").toString());
		assertEquals("http://www.example.org/résumé.html",
				new ParsedIRI("http://www.example.org/r%C3%A9sum%C3%A9.html").normalize().toString());
	}

	@Test
	public void testDurst() throws URISyntaxException {
		assertURI2IRI("http://www.example.org/D%C3%BCrst", "http://www.example.org/D\u00FCrst");
		assertURI2IRI("http://www.example.org/D%FCrst", "http://www.example.org/D%FCrst");
	}

	@Test
	public void testDeseret() throws URISyntaxException {
		assertURI2IRI("http://www.example.org/U+10400/%F0%90%90%80", "http://www.example.org/U+10400/\uD801\uDC00");
	}

	@Test
	public void testPunycodeEncoding() throws URISyntaxException {
		assertEquals("http://xn--99zt52a.example.org/%E2%80%AE",
				new ParsedIRI("http://xn--99zt52a.example.org/%e2%80%ae").normalize().toASCIIString());
		assertEquals("http://\u7D0D\u8C46.example.org/\u202E",
				new ParsedIRI("http://\u7D0D\u8C46.example.org/%e2%80%ae").normalize().toString());
	}

	private void assertURI2IRI(String uri, String iri) throws URISyntaxException {
		assertEquals(uri, new ParsedIRI(iri).toASCIIString());
		assertEquals(iri, new ParsedIRI(uri).normalize().toString());
	}

	@Test
	public void testCreate() {
		assertEquals("http://example.org/poorly%20constructed",
				ParsedIRI.create("http://example.org/poorly constructed").toString());
		assertEquals("Just%20some%0AText!?", ParsedIRI.create("Just some\nText!?").toString());
		assertEquals("100%25", ParsedIRI.create("100%").toString());
	}

	@Test
	public void testCreateInvalid() {
		try {
			assertEquals("http://example.org:-80/", ParsedIRI.create("http://example.org:-80/").toString());
			fail();
		} catch (IllegalArgumentException e) {
		}
	}
}
