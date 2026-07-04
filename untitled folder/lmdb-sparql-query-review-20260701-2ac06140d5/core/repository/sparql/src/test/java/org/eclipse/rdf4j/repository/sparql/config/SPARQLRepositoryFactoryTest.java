/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
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

import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.junit.jupiter.api.Test;

public class SPARQLRepositoryFactoryTest {

	private final String queryEndpointUrl = "http://example.org/sparql";

	private final SPARQLRepositoryFactory factory = new SPARQLRepositoryFactory();

	@Test
	public void testGetRepository() {
		SPARQLRepositoryConfig config = new SPARQLRepositoryConfig(queryEndpointUrl);

		SPARQLRepository rep = factory.getRepository(config);
		assertThat(rep.getPassThroughEnabled()).isNull();

		config.setPassThroughEnabled(false);
		rep = factory.getRepository(config);
		assertThat(rep.getPassThroughEnabled()).isFalse();
	}
}
