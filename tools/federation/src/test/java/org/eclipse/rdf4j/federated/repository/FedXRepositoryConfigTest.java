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
package org.eclipse.rdf4j.federated.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.Models.subject;

import java.io.InputStream;

import org.eclipse.rdf4j.federated.util.Vocabulary.FEDX;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FedXRepositoryConfigTest {

	@Test
	public void testParseConfig() throws Exception {
		Model model = readConfig("/tests/rdf4jserver/config.ttl");

		FedXRepositoryConfig config = new FedXRepositoryConfig();
		config.parse(model, implNode(model));

		Assertions.assertNull(config.getDataConfig());

		Model members = config.getMembers();
		assertThat(members.filter(null, FEDX.STORE, null).size()).isEqualTo(2);

		assertThat(members.filter(null, FEDX.REPOSITORY_NAME, null).objects().stream().map(Value::stringValue))
				.containsExactly("endpoint1", "endpoint2");
	}

	@Test
	public void testParseConfig_DataConfig() throws Exception {
		Model model = readConfig("/tests/rdf4jserver/config-withDataConfig.ttl");

		FedXRepositoryConfig config = new FedXRepositoryConfig();
		config.parse(model, implNode(model));

		Assertions.assertEquals("dataConfig.ttl", config.getDataConfig());

		Assertions.assertNull(config.getMembers());

	}

	@Test
	public void testExport() throws Exception {
		Model model = readConfig("/tests/rdf4jserver/config.ttl");

		FedXRepositoryConfig config = new FedXRepositoryConfig();
		config.parse(model, implNode(model));

		// export into model
		Model export = new TreeModel();
		Resource implNode = config.export(export);

		assertThat(export.filter(implNode, FedXRepositoryConfig.MEMBER, null).size()).isEqualTo(2);

		assertThat(export.filter(null, FEDX.REPOSITORY_NAME, null).objects().stream().map(Value::stringValue))
				.containsExactly("endpoint1", "endpoint2");
	}

	protected Model readConfig(String configResource) throws Exception {
		try (InputStream in = FedXRepositoryConfigTest.class.getResourceAsStream(configResource)) {
			return Rio.parse(in, "http://example.org/", RDFFormat.TURTLE);
		}
	}

	protected Resource implNode(Model model) {
		return subject(model.filter(null, RepositoryConfigSchema.REPOSITORYTYPE, null)).get();
	}

}
