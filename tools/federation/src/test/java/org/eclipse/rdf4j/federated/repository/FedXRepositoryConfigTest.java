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

import org.eclipse.rdf4j.federated.FedXConfig;
import org.eclipse.rdf4j.federated.util.Vocabulary.FEDX;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class FedXRepositoryConfigTest {

	@Test
	public void testParseConfig() throws Exception {
		Model model = readConfig("/tests/rdf4jserver/config.ttl");

		FedXRepositoryConfig config = new FedXRepositoryConfig();
		config.parse(model, implNode(model));

		Assertions.assertNull(config.getDataConfig());
		Assertions.assertNull(config.getConfig());

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

		assertThat(export.filter(implNode, FedXRepositoryConfig.FEDX_CONFIG, null)).isEmpty();

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

	@Nested
	class FedXConfigParsing {

		@Test
		public void testParse() throws Exception {
			Model model = readConfig("/tests/rdf4jserver/config-withFedXConfig.ttl");
			FedXRepositoryConfig repoConfig = new FedXRepositoryConfig();

			repoConfig.parse(model, implNode(model));
			FedXConfig config = repoConfig.getConfig();

			assertThat(config.getEnforceMaxQueryTime()).isEqualTo(105);
			assertThat(config.isEnableMonitoring()).isTrue();
			assertThat(config.isLogQueryPlan()).isTrue();
			assertThat(config.isDebugQueryPlan()).isTrue();
			assertThat(config.isLogQueries()).isTrue();
			assertThat(config.getSourceSelectionCacheSpec()).isEqualTo("spec-goes-here");
		}

		@Test
		public void testParseConfigOverridesExistingConfig() throws Exception {
			Model model = readConfig("/tests/rdf4jserver/config-withFedXConfig.ttl");
			FedXRepositoryConfig repoConfig = new FedXRepositoryConfig();
			repoConfig.setConfig(new FedXConfig().withEnforceMaxQueryTime(33));

			repoConfig.parse(model, implNode(model));
			FedXConfig config = repoConfig.getConfig();

			assertThat(config.getEnforceMaxQueryTime()).isEqualTo(105);
			assertThat(config.isEnableMonitoring()).isTrue();
			assertThat(config.isLogQueryPlan()).isTrue();
			assertThat(config.isDebugQueryPlan()).isTrue();
			assertThat(config.isLogQueries()).isTrue();
			assertThat(config.getSourceSelectionCacheSpec()).isEqualTo("spec-goes-here");
		}

		@Test
		public void testParseWithEmptyConfig() throws Exception {
			Model model = readConfig("/tests/rdf4jserver/config.ttl");
			FedXRepositoryConfig repoConfig = new FedXRepositoryConfig();
			repoConfig.setConfig(new FedXConfig());

			repoConfig.parse(model, implNode(model));
			FedXConfig config = repoConfig.getConfig();

			// expecting defaults
			assertThat(config.getEnforceMaxQueryTime()).isEqualTo(30);
			assertThat(config.isEnableMonitoring()).isFalse();
			assertThat(config.isLogQueryPlan()).isFalse();
			assertThat(config.isDebugQueryPlan()).isFalse();
			assertThat(config.isLogQueries()).isFalse();
			assertThat(config.getSourceSelectionCacheSpec()).isNull();
		}

		@Test
		public void testExport() throws Exception {
			Model model = readConfig("/tests/rdf4jserver/config-withFedXConfig.ttl");

			FedXRepositoryConfig repoConfig = new FedXRepositoryConfig();
			repoConfig.parse(model, implNode(model));

			// export into model
			Model export = new TreeModel();
			Resource implNode = repoConfig.export(export);
			Resource configNode = Models
					.objectResource(export.getStatements(implNode, FedXRepositoryConfig.FEDX_CONFIG, null))
					.orElse(null);
			assertThat(configNode).isNotNull();

			assertThat(export.filter(configNode, null, null)).hasSize(15);

			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_JOIN_WORKER_THREADS, null)))
					.hasValueSatisfying(v -> assertThat(v.intValue()).isEqualTo(101));
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_UNION_WORKER_THREADS, null)))
					.hasValueSatisfying(v -> assertThat(v.intValue()).isEqualTo(102));
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_LEFT_JOIN_WORKER_THREADS,
									null)))
					.hasValueSatisfying(v -> assertThat(v.intValue()).isEqualTo(103));
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_BOUND_JOIN_BLOCK_SIZE, null)))
					.hasValueSatisfying(v -> assertThat(v.intValue()).isEqualTo(104));
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_ENFORCE_MAX_QUERY_TIME, null)))
					.hasValueSatisfying(v -> assertThat(v.intValue()).isEqualTo(105));
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_ENABLE_SERVICE_AS_BOUND_JOIN,
									null)))
					.hasValueSatisfying(v -> assertThat(v.booleanValue()).isFalse());
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_ENABLE_OPTIONAL_AS_BIND_JOIN,
									null)))
					.hasValueSatisfying(v -> assertThat(v.booleanValue()).isFalse());
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_ENABLE_MONITORING, null)))
					.hasValueSatisfying(v -> assertThat(v.booleanValue()).isTrue());
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_LOG_QUERY_PLAN, null)))
					.hasValueSatisfying(v -> assertThat(v.booleanValue()).isTrue());
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_LOG_QUERIES, null)))
					.hasValueSatisfying(v -> assertThat(v.booleanValue()).isTrue());
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_DEBUG_QUERY_PLAN, null)))
					.hasValueSatisfying(v -> assertThat(v.booleanValue()).isTrue());
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_INCLUDE_INFERRED_DEFAULT,
									null)))
					.hasValueSatisfying(v -> assertThat(v.booleanValue()).isFalse());
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_SOURCE_SELECTION_CACHE_SPEC,
									null)))
					.hasValueSatisfying(v -> assertThat(v.stringValue()).isEqualTo("spec-goes-here"));
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_PREFIX_DECLARATIONS,
									null)))
					.hasValueSatisfying(v -> assertThat(v.stringValue()).isEqualTo("prefixes-go-here"));

			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_CONSUMING_ITERATION_MAX,
									null)))
					.hasValueSatisfying(v -> assertThat(v.intValue()).isEqualTo(106));
		}

		@Test
		public void testExportWithEmptyConfig() throws Exception {
			FedXRepositoryConfig repoConfig = new FedXRepositoryConfig();
			// Set to force export of defaults
			repoConfig.setConfig(new FedXConfig());

			// export into model
			Model export = new TreeModel();
			Resource implNode = repoConfig.export(export);
			Resource configNode = Models
					.objectResource(export.getStatements(implNode, FedXRepositoryConfig.FEDX_CONFIG, null))
					.orElse(null);
			assertThat(configNode).isNotNull();

			// Note: 13 instead of 15 since CONFIG_SOURCE_SELECTION_CACHE_SPEC & CONFIG_PREFIX_DECLARATIONS are null
			// and thus should not be populated
			assertThat(export.filter(configNode, null, null)).hasSize(13);

			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_JOIN_WORKER_THREADS, null)))
					.hasValueSatisfying(v -> assertThat(v.intValue()).isEqualTo(20));
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_UNION_WORKER_THREADS, null)))
					.hasValueSatisfying(v -> assertThat(v.intValue()).isEqualTo(20));
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_LEFT_JOIN_WORKER_THREADS,
									null)))
					.hasValueSatisfying(v -> assertThat(v.intValue()).isEqualTo(10));
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_BOUND_JOIN_BLOCK_SIZE, null)))
					.hasValueSatisfying(v -> assertThat(v.intValue()).isEqualTo(25));
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_ENFORCE_MAX_QUERY_TIME, null)))
					.hasValueSatisfying(v -> assertThat(v.intValue()).isEqualTo(30));
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_ENABLE_SERVICE_AS_BOUND_JOIN,
									null)))
					.hasValueSatisfying(v -> assertThat(v.booleanValue()).isTrue());
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_ENABLE_OPTIONAL_AS_BIND_JOIN,
									null)))
					.hasValueSatisfying(v -> assertThat(v.booleanValue()).isTrue());
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_ENABLE_MONITORING, null)))
					.hasValueSatisfying(v -> assertThat(v.booleanValue()).isFalse());
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_LOG_QUERY_PLAN, null)))
					.hasValueSatisfying(v -> assertThat(v.booleanValue()).isFalse());
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_LOG_QUERIES, null)))
					.hasValueSatisfying(v -> assertThat(v.booleanValue()).isFalse());
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_DEBUG_QUERY_PLAN, null)))
					.hasValueSatisfying(v -> assertThat(v.booleanValue()).isFalse());
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_INCLUDE_INFERRED_DEFAULT,
									null)))
					.hasValueSatisfying(v -> assertThat(v.booleanValue()).isTrue());
			assertThat(
					Models.objectLiteral(
							export.getStatements(configNode, FedXRepositoryConfig.CONFIG_CONSUMING_ITERATION_MAX,
									null)))
					.hasValueSatisfying(v -> assertThat(v.intValue()).isEqualTo(1000));
		}
	}
}
