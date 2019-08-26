package org.eclipse.rdf4j.repository.sparql.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link SPARQLRepositoryConfig}.
 *
 * @author infgeoax
 */
public class SPARQLRepositoryConfigTest {

	private static final String QUERY_ENDPOINT_URL = "http://example.com/sparql";

	private static final String UPDATE_ENDPOINT_URL = "http://example.com/update";

	@Test
	public void testConstructorWithSPARQLEndpoint() {
		SPARQLRepositoryConfig config = new SPARQLRepositoryConfig(QUERY_ENDPOINT_URL);
		assertEquals(QUERY_ENDPOINT_URL, config.getQueryEndpointUrl());
		assertNull(config.getUpdateEndpointUrl());
		assertEquals(SPARQLRepositoryFactory.REPOSITORY_TYPE, config.getType());
	}

	@Test
	public void testConstructorWithQueryAndUpdateEndpoints() {
		SPARQLRepositoryConfig config = new SPARQLRepositoryConfig(QUERY_ENDPOINT_URL, UPDATE_ENDPOINT_URL);
		assertEquals(QUERY_ENDPOINT_URL, config.getQueryEndpointUrl());
		assertEquals(UPDATE_ENDPOINT_URL, config.getUpdateEndpointUrl());
		assertEquals(SPARQLRepositoryFactory.REPOSITORY_TYPE, config.getType());
	}
}
