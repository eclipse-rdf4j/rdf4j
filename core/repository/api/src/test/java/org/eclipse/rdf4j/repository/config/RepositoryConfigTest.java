/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.Values.iri;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
import org.junit.jupiter.api.Test;

public class RepositoryConfigTest {

	public static final String ID = "test";

	@Test
	public void testParse_newVocabulary() {

		var repoNode = iri("urn:repo1");
		Model m = new ModelBuilder()
				.subject(repoNode)
				.add(CONFIG.Rep.id, ID)
				.build();

		RepositoryConfig config = new RepositoryConfig(ID);

		config.parse(m, repoNode);

		assertThat(config.getID()).isEqualTo(ID);
	}

	@Test
	public void testParse_oldVocabulary() {
		var repoNode = iri("urn:repo1");
		Model m = new ModelBuilder()
				.subject(repoNode)
				.add(RepositoryConfigSchema.REPOSITORYID, ID)
				.build();

		RepositoryConfig config = new RepositoryConfig(ID);

		config.parse(m, repoNode);

		assertThat(config.getID()).isEqualTo(ID);
	}

}
