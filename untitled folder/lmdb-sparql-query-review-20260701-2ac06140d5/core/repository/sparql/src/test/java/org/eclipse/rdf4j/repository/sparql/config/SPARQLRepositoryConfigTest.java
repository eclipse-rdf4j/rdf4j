/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.junit.jupiter.api.Test;

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
		assertThat(config.getQueryEndpointUrl()).isEqualTo(QUERY_ENDPOINT_URL);
		assertThat(config.getUpdateEndpointUrl()).isNull();
		assertThat(config.getType()).isEqualTo(SPARQLRepositoryFactory.REPOSITORY_TYPE);
	}

	@Test
	public void testConstructorWithQueryAndUpdateEndpoints() {
		SPARQLRepositoryConfig config = new SPARQLRepositoryConfig(QUERY_ENDPOINT_URL, UPDATE_ENDPOINT_URL);

		assertThat(config.getQueryEndpointUrl()).isEqualTo(QUERY_ENDPOINT_URL);
		assertThat(config.getUpdateEndpointUrl()).isEqualTo(UPDATE_ENDPOINT_URL);
		assertThat(config.getType()).isEqualTo(SPARQLRepositoryFactory.REPOSITORY_TYPE);
	}

	@Test
	public void testPassThroughEnabled() {
		SPARQLRepositoryConfig config = new SPARQLRepositoryConfig(QUERY_ENDPOINT_URL);

		assertThat(config.getPassThroughEnabled()).isNull();
		config.setPassThroughEnabled(true);
		assertThat(config.getPassThroughEnabled()).isTrue();
		config.setPassThroughEnabled(false);
		assertThat(config.getPassThroughEnabled()).isFalse();
	}

	@Test
	public void testParse() {
		Model m = new LinkedHashModel();
		BNode implNode = bnode();
		m.add(implNode, RepositoryConfigSchema.REPOSITORYTYPE, literal(SPARQLRepositoryFactory.REPOSITORY_TYPE));
		m.add(implNode, SPARQLRepositoryConfig.QUERY_ENDPOINT, iri(QUERY_ENDPOINT_URL));
		m.add(implNode, SPARQLRepositoryConfig.UPDATE_ENDPOINT, iri(UPDATE_ENDPOINT_URL));

		SPARQLRepositoryConfig config = new SPARQLRepositoryConfig();
		config.parse(m, implNode);

		assertThat(config.getQueryEndpointUrl()).isEqualTo(QUERY_ENDPOINT_URL);
		assertThat(config.getUpdateEndpointUrl()).isEqualTo(UPDATE_ENDPOINT_URL);
		assertThat(config.getPassThroughEnabled()).isNull();

		m.add(implNode, SPARQLRepositoryConfig.PASS_THROUGH_ENABLED, BooleanLiteral.FALSE);

		config.parse(m, implNode);
		assertThat(config.getPassThroughEnabled()).isFalse();
	}
}
