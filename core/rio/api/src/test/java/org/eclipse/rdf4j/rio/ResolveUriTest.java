/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResolveUriTest {

	private TestParser parser;

	@BeforeEach
	void setUp() {
		parser = new TestParser();
		parser.setBaseUri("http://example.org/dir/doc");
	}

	@Test
	void absoluteIriIsNotResolved() {
		IRI iri = parser.resolve("http://other.org/x");
		assertEquals("http://other.org/x", iri.stringValue());
	}

	@Test
	void urnIsNotResolved() {
		IRI iri = parser.resolve("urn:isbn:0451450523");
		assertEquals("urn:isbn:0451450523", iri.stringValue());
	}

	@Test
	void singleLetterSchemeIsNotResolved() {
		IRI iri = parser.resolve("a:b");
		assertEquals("a:b", iri.stringValue());
	}

	@Test
	void plainRelativePathIsResolved() {
		IRI iri = parser.resolve("other");
		assertEquals("http://example.org/dir/other", iri.stringValue());
	}

	@Test
	void colonInLaterPathSegmentIsResolved() {
		IRI iri = parser.resolve("path/foo:bar");
		assertEquals("http://example.org/dir/path/foo:bar", iri.stringValue());
	}

	@Test
	void dotSegmentColonIsResolved() {
		// RFC 3986 explicitly recommends "./a:b" to express this reference
		IRI iri = parser.resolve("./a:b");
		assertEquals("http://example.org/dir/a:b", iri.stringValue());
	}

	@Test
	void colonInQueryIsResolved() {
		IRI iri = parser.resolve("?q=a:b");
		assertEquals("http://example.org/dir/doc?q=a:b", iri.stringValue());
	}

	@Test
	void colonInFragmentIsResolved() {
		IRI iri = parser.resolve("#a:b");
		assertEquals("http://example.org/dir/doc#a:b", iri.stringValue());
	}

	@Test
	void networkPathReferenceUsesBaseScheme() {
		IRI iri = parser.resolve("//other.org/x");
		assertEquals("http://other.org/x", iri.stringValue());
	}

	@Test
	void emptyStringResolvesToBase() {
		IRI iri = parser.resolve("");
		assertEquals("http://example.org/dir/doc", iri.stringValue());
	}

	@Test
	void fragmentOnlyResolvesAgainstBase() {
		IRI iri = parser.resolve("#frag");
		assertEquals("http://example.org/dir/doc#frag", iri.stringValue());
	}

	@Test
	void relativeIriWithoutBaseFails() {
		TestParser noBase = new TestParser(); // no base URI set
		assertThrows(RDFParseException.class, () -> noBase.resolve("relative/path"));
	}

	@Test
	void colonInPathWithoutBaseFails() {
		TestParser noBase = new TestParser();
		assertThrows(RDFParseException.class, () -> noBase.resolve("path/foo:bar"));
	}

	@Test
	void leadingColonResolvesAsRelativePathWithEncodedColon() {
		// ":foo" has no valid scheme, so it is not absolute. Per RFC 3986 a
		// relative reference may not contain ':' in its first path segment
		// (the unambiguous spelling is "./:foo"); ParsedIRI resolves it
		// leniently and percent-encodes the colon to keep the result
		// unambiguous.
		IRI iri = parser.resolve(":foo");
		assertEquals("http://example.org/dir/%3Afoo", iri.stringValue());
	}

	/**
	 * Minimal concrete parser exposing resolveURI for testing.
	 */
	private static class TestParser extends AbstractRDFParser {

		IRI resolve(String uriSpec) {
			return resolveURI(uriSpec);
		}

		void setBaseUri(String base) {
			setBaseURI(base);
		}

		// --- boilerplate to satisfy the abstract contract ---

		@Override
		public org.eclipse.rdf4j.rio.RDFFormat getRDFFormat() {
			return org.eclipse.rdf4j.rio.RDFFormat.TURTLE;
		}

		@Override
		public void parse(java.io.InputStream in, String baseURI) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void parse(java.io.Reader reader, String baseURI) {
			throw new UnsupportedOperationException();
		}
	}
}
