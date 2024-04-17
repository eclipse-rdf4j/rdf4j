/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
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

import java.io.InputStream;

import org.eclipse.rdf4j.federated.FedXConfig;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;

public class FedXConfigParserTest {

	@Test
	public void testParse() throws Exception {
		Model model = readConfig("/tests/rdf4jserver/config-fedXConfig-only.ttl");

		FedXConfig config = FedXConfigParser.parse(new FedXConfig(), model, Values.iri("http://example.org/conf"));

		assertThat(config.getEnforceMaxQueryTime()).isEqualTo(1234);
		assertThat(config.isEnableMonitoring()).isTrue();
		assertThat(config.isLogQueryPlan()).isTrue();
		assertThat(config.isDebugQueryPlan()).isTrue();
		assertThat(config.isLogQueries()).isTrue();
		assertThat(config.getSourceSelectionCacheSpec()).isEqualTo("spec-goes-here");
	}

	@Test
	public void testParseWithEmptyConfig() throws Exception {
		Model model = new TreeModel();

		FedXConfig config = FedXConfigParser.parse(new FedXConfig(), model, Values.iri("http://example.org/conf"));

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
		Model model = readConfig("/tests/rdf4jserver/config-fedXConfig-only.ttl");

		FedXConfig config = FedXConfigParser.parse(new FedXConfig(), model, Values.iri("http://example.org/conf"));

		Model export = new TreeModel();
		Resource configNode = FedXConfigParser.export(config, export);

		assertThat(export.filter(configNode, null, null)).hasSize(6);

		assertThat(
				Models.objectLiteral(
						export.getStatements(configNode, FedXConfigParser.CONFIG_ENFORCE_MAX_QUERY_TIME, null)))
				.hasValueSatisfying(v -> assertThat(v.intValue()).isEqualTo(1234));
		assertThat(
				Models.objectLiteral(
						export.getStatements(configNode, FedXConfigParser.CONFIG_ENABLE_MONITORING, null)))
				.hasValueSatisfying(v -> assertThat(v.booleanValue()).isTrue());
		assertThat(
				Models.objectLiteral(
						export.getStatements(configNode, FedXConfigParser.CONFIG_LOG_QUERY_PLAN, null)))
				.hasValueSatisfying(v -> assertThat(v.booleanValue()).isTrue());
		assertThat(
				Models.objectLiteral(
						export.getStatements(configNode, FedXConfigParser.CONFIG_DEBUG_QUERY_PLAN, null)))
				.hasValueSatisfying(v -> assertThat(v.booleanValue()).isTrue());
		assertThat(
				Models.objectLiteral(
						export.getStatements(configNode, FedXConfigParser.CONFIG_LOG_QUERIES, null)))
				.hasValueSatisfying(v -> assertThat(v.booleanValue()).isTrue());
		assertThat(
				Models.objectLiteral(
						export.getStatements(configNode, FedXConfigParser.CONFIG_SOURCE_SELECTION_CACHE_SPEC, null)))
				.hasValueSatisfying(v -> assertThat(v.stringValue()).isEqualTo("spec-goes-here"));
	}

	@Test
	public void testExportWithEmptyConfig() throws Exception {
		Model export = new TreeModel();
		Resource configNode = FedXConfigParser.export(new FedXConfig(), export);

		// Note: 5 instead of 6 since CONFIG_SOURCE_SELECTION_CACHE_SPEC is null and thus should not be populated
		assertThat(export.filter(configNode, null, null)).hasSize(5);

		assertThat(
				Models.objectLiteral(
						export.getStatements(configNode, FedXConfigParser.CONFIG_ENFORCE_MAX_QUERY_TIME, null)))
				.hasValueSatisfying(v -> assertThat(v.intValue()).isEqualTo(30));
		assertThat(
				Models.objectLiteral(
						export.getStatements(configNode, FedXConfigParser.CONFIG_ENABLE_MONITORING, null)))
				.hasValueSatisfying(v -> assertThat(v.booleanValue()).isFalse());
		assertThat(
				Models.objectLiteral(
						export.getStatements(configNode, FedXConfigParser.CONFIG_LOG_QUERY_PLAN, null)))
				.hasValueSatisfying(v -> assertThat(v.booleanValue()).isFalse());
		assertThat(
				Models.objectLiteral(
						export.getStatements(configNode, FedXConfigParser.CONFIG_DEBUG_QUERY_PLAN, null)))
				.hasValueSatisfying(v -> assertThat(v.booleanValue()).isFalse());
		assertThat(
				Models.objectLiteral(
						export.getStatements(configNode, FedXConfigParser.CONFIG_LOG_QUERIES, null)))
				.hasValueSatisfying(v -> assertThat(v.booleanValue()).isFalse());
	}

	protected Model readConfig(String configResource) throws Exception {
		try (InputStream in = FedXRepositoryConfigTest.class.getResourceAsStream(configResource)) {
			return Rio.parse(in, "http://example.org/", RDFFormat.TURTLE);
		}
	}
}
