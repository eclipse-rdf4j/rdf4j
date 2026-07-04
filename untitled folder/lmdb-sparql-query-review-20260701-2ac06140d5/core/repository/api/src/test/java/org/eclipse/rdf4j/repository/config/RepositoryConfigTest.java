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
import static org.eclipse.rdf4j.model.util.Values.literal;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
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
	public void testParse_useLegacy_newVocabulary() {
		var repoNode = iri("urn:repo1");
		Model m = new ModelBuilder()
				.subject(repoNode)
				.add(CONFIG.Rep.id, ID)
				.build();

		RepositoryConfig config = new RepositoryConfig();

		System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "true");
		config.parse(m, repoNode);
		System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "");

		assertThat(config.getID()).isEqualTo(ID);
	}

	@Test
	public void testParse_oldVocabulary() {
		var repoNode = iri("urn:repo1");
		Model m = new ModelBuilder()
				.subject(repoNode)
				.add(RepositoryConfigSchema.REPOSITORYID, ID)
				.build();

		RepositoryConfig config = new RepositoryConfig();

		config.parse(m, repoNode);

		assertThat(config.getID()).isEqualTo(ID);
	}

	@Test
	public void testParse_mixedVocabulary() {
		var repoNode = iri("urn:repo1");

		var newId = "new_test";
		Model m = new ModelBuilder()
				.subject(repoNode)
				.add(RepositoryConfigSchema.REPOSITORYID, ID)
				.add(CONFIG.Rep.id, newId)
				.build();

		RepositoryConfig config = new RepositoryConfig();

		config.parse(m, repoNode);

		assertThat(config.getID()).isEqualTo(newId);
	}

	@Test
	public void testParse_useLegacy_mixedVocabulary() {
		var repoNode = iri("urn:repo1");
		var newId = "new_test";
		Model m = new ModelBuilder()
				.subject(repoNode)
				.add(RepositoryConfigSchema.REPOSITORYID, ID)
				.add(CONFIG.Rep.id, newId)
				.build();

		RepositoryConfig config = new RepositoryConfig();

		System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "true");
		config.parse(m, repoNode);
		System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "");

		assertThat(config.getID()).isEqualTo(ID);
	}

	@Test
	public void testExport() {
		var repoNode = iri("urn:repo1");
		var config = new RepositoryConfig(ID);
		var m = new DynamicModelFactory().createEmptyModel();
		config.export(m, repoNode);

		assertThat(m.filter(repoNode, CONFIG.Rep.id, null).objects()).containsExactly(literal(ID));
		assertThat(m.filter(repoNode, RepositoryConfigSchema.REPOSITORYID, null).objects()).isEmpty();
	}

	@Test
	public void testExport_useLegacy() {
		var repoNode = iri("urn:repo1");
		var config = new RepositoryConfig(ID);
		var m = new DynamicModelFactory().createEmptyModel();
		System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "true");
		config.export(m, repoNode);
		System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "");

		assertThat(m.filter(repoNode, RepositoryConfigSchema.REPOSITORYID, null).objects())
				.containsExactly(literal(ID));
		assertThat(m.filter(repoNode, CONFIG.Rep.id, null).objects()).isEmpty();
	}
}
