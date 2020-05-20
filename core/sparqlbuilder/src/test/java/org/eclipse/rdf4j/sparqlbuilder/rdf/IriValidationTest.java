package org.eclipse.rdf4j.sparqlbuilder.rdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.junit.Test;

public class IriValidationTest {

	@Test
	public void testURN() throws URISyntaxException {
		assertEquals("urn:test:foobar", new ParsedIRI("urn:test:foobar").toString());
		assertEquals("urn", new ParsedIRI("urn:test:foobar").getScheme());
		assertEquals("test:foobar", new ParsedIRI("urn:test:foobar").getPath());
		assertTrue(new ParsedIRI("urn:test:foobar").isOpaque());
	}

	@Test
	public void testUnknownSchemeHostProcessing() throws URISyntaxException {
		ParsedIRI uri = new ParsedIRI("bundleresource://385.fwk19480900/test.ttl");
		assertThat(uri.isAbsolute());
		assertThat(uri.getScheme()).isEqualTo("bundleresource");
		assertThat(uri.isOpaque());
		assertThat(uri.getHost()).isEqualTo("385.fwk19480900");
	}

	@Test
	public void validateIRI() throws URISyntaxException {
		ParsedIRI iri = new ParsedIRI("urn://example.org/ros&#xE9");
	}

}
