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
package org.eclipse.rdf4j.workbench.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Dale Visser
 */
public class CreateServletTest {

	@TempDir
	static File datadir;

	static CreateServlet servlet;

	@BeforeAll
	public static void setUpServlet() {
		servlet = new CreateServlet();
		servlet.setRepositoryManager(new LocalRepositoryManager(datadir));
	}

	private static final String[] EXPECTED_TEMPLATES = new String[] { "memory-customrule", "memory-rdfs-dt",
			"memory-rdfs", "memory",
			"native-customrule", "native-rdfs-dt", "native-rdfs", "native", "remote", "sparql", "memory-shacl",
			"native-shacl" };

	/**
	 * Regression test for SES-1907.
	 */
	@Test
	public final void testExpectedTemplatesCanBeResolved() {
		for (String template : EXPECTED_TEMPLATES) {
			String resource = template + ".ttl";
			assertThat(RepositoryConfig.class.getResourceAsStream(resource)).isNotNull().as(resource);
		}
	}

	@Test
	public final void testExpectedTemplatesCanBeLoaded() throws IOException {
		for (String template : EXPECTED_TEMPLATES) {
			assertThat(CreateServlet.getConfigTemplate(template).getTemplate()).isNotNull();
		}
	}

	@Test
	public void testUpdateRepositoryConfig_legacyConfig() {
		var servlet = new CreateServlet();
		servlet.setRepositoryManager(new LocalRepositoryManager(datadir));

		var in = getClass().getResourceAsStream("/configFiles/memory-legacy.ttl");

		try {
			String legacyConfigString = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			RepositoryConfig config = servlet.updateRepositoryConfig(legacyConfigString);

			assertThat(config.getID()).isEqualTo("legacy");
			assertThat(config.getTitle()).isEqualTo("Legacy Test Repository");
		} catch (RDF4JException | IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testUpdateRepositoryConfig_newConfig() {
		var servlet = new CreateServlet();
		servlet.setRepositoryManager(new LocalRepositoryManager(datadir));

		var in = getClass().getResourceAsStream("/configFiles/memory.ttl");

		try {
			String legacyConfigString = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			RepositoryConfig config = servlet.updateRepositoryConfig(legacyConfigString);

			assertThat(config.getID()).isEqualTo("test");
			assertThat(config.getTitle()).isEqualTo("test store");
		} catch (RDF4JException | IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testUpdateRepositoryConfig_useLegacy_legacyConfig() {
		var in = getClass().getResourceAsStream("/configFiles/memory-legacy.ttl");

		try {
			System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "true");
			String legacyConfigString = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			RepositoryConfig config = servlet.updateRepositoryConfig(legacyConfigString);
			System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "");

			assertThat(config.getID()).isEqualTo("legacy");
			assertThat(config.getTitle()).isEqualTo("Legacy Test Repository");
		} catch (RDF4JException | IOException e) {
			fail(e.getMessage());
		}
	}
}
