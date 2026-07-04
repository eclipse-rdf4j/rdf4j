/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.model.util.Values.literal;

import org.eclipse.rdf4j.common.transaction.QueryEvaluationMode;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.junit.jupiter.api.Test;

public class BaseSailConfigTest {

	@Test
	public void testParseQueryEvaluationMode() {
		var implNode = bnode();

		// capitalization wrong
		var incorrectEvalMode = literal("standard");
		var correctEvalMode = literal("STANDARD");

		{
			var config = new BaseSailConfig("stub") {
			};

			var model = new ModelBuilder()
					.add(implNode, CONFIG.Sail.defaultQueryEvaluationMode, incorrectEvalMode)
					.build();

			assertThatExceptionOfType(SailConfigException.class).isThrownBy(() -> config.parse(model, implNode));
		}

		{
			var config = new BaseSailConfig("stub") {
			};

			var model = new ModelBuilder()
					.add(implNode, CONFIG.Sail.defaultQueryEvaluationMode, correctEvalMode)
					.build();

			config.parse(model, implNode);
			assertThat(config.getDefaultQueryEvaluationMode()).hasValue(QueryEvaluationMode.STANDARD);
		}

		{
			var config = new BaseSailConfig("stub") {
			};
			var model = new ModelBuilder()
					.add(implNode, BaseSailSchema.DEFAULT_QUERY_EVALUATION_MODE, correctEvalMode)
					.build();

			config.parse(model, implNode);
			assertThat(config.getDefaultQueryEvaluationMode()).hasValue(QueryEvaluationMode.STANDARD);
		}

		{
			var config = new BaseSailConfig("stub") {
			};
			var model = new ModelBuilder()
					.add(implNode, BaseSailSchema.DEFAULT_QUERY_EVALUATION_MODE, correctEvalMode)
					.add(implNode, CONFIG.Sail.defaultQueryEvaluationMode, incorrectEvalMode)
					.build();

			assertThatExceptionOfType(SailConfigException.class).isThrownBy(() -> config.parse(model, implNode));
		}

		{
			var config = new BaseSailConfig("stub") {
			};
			var model = new ModelBuilder().build();

			config.parse(model, implNode);
			assertThat(config.getDefaultQueryEvaluationMode()).isEmpty();
		}
	}

	@Test
	public void testParseAndExportSlowQuerySettings() {
		var implNode = bnode();
		var config = new BaseSailConfig("stub") {
		};

		var model = new ModelBuilder()
				.add(implNode, CONFIG.Sail.slowQueryLogThresholdSeconds, literal(5L))
				.add(implNode, CONFIG.Sail.slowQueryLogFirstResultThresholdSeconds, literal(3L))
				.add(implNode, CONFIG.Sail.slowQueryLogFile, literal("logs/slow-query.log"))
				.build();

		config.parse(model, implNode);
		assertThat(config.getSlowQueryLogThresholdSeconds()).isEqualTo(5L);
		assertThat(config.getSlowQueryLogFirstResultThresholdSeconds()).isEqualTo(3L);
		assertThat(config.getSlowQueryLogFile()).isEqualTo("logs/slow-query.log");

		var exportedModel = new LinkedHashModel();
		var exportedNode = config.export(exportedModel);
		assertThat(exportedModel.contains(exportedNode, CONFIG.Sail.slowQueryLogThresholdSeconds, literal(5L)))
				.isTrue();
		assertThat(exportedModel.contains(exportedNode, CONFIG.Sail.slowQueryLogFirstResultThresholdSeconds,
				literal(3L))).isTrue();
		assertThat(exportedModel.contains(exportedNode, CONFIG.Sail.slowQueryLogFile,
				literal("logs/slow-query.log"))).isTrue();
	}

	@Test
	public void testParseAndExportLegacySlowQuerySettings() {
		String propertyName = "org.eclipse.rdf4j.model.vocabulary.useLegacyConfig";
		String previousValue = System.getProperty(propertyName);
		System.setProperty(propertyName, "true");
		try {
			var implNode = bnode();
			var config = new BaseSailConfig("stub") {
			};

			var model = new ModelBuilder()
					.add(implNode, BaseSailSchema.SLOW_QUERY_LOG_THRESHOLD_SECONDS, literal(7L))
					.add(implNode, BaseSailSchema.SLOW_QUERY_LOG_FIRST_RESULT_THRESHOLD_SECONDS, literal(2L))
					.add(implNode, BaseSailSchema.SLOW_QUERY_LOG_FILE, literal("legacy.log"))
					.build();

			config.parse(model, implNode);
			assertThat(config.getSlowQueryLogThresholdSeconds()).isEqualTo(7L);
			assertThat(config.getSlowQueryLogFirstResultThresholdSeconds()).isEqualTo(2L);
			assertThat(config.getSlowQueryLogFile()).isEqualTo("legacy.log");

			var exportedModel = new LinkedHashModel();
			var exportedNode = config.export(exportedModel);
			assertThat(exportedModel.contains(exportedNode, BaseSailSchema.SLOW_QUERY_LOG_THRESHOLD_SECONDS,
					literal(7L))).isTrue();
			assertThat(
					exportedModel.contains(exportedNode, BaseSailSchema.SLOW_QUERY_LOG_FIRST_RESULT_THRESHOLD_SECONDS,
							literal(2L))).isTrue();
			assertThat(exportedModel.contains(exportedNode, BaseSailSchema.SLOW_QUERY_LOG_FILE,
					literal("legacy.log"))).isTrue();
		} finally {
			if (previousValue == null) {
				System.clearProperty(propertyName);
			} else {
				System.setProperty(propertyName, previousValue);
			}
		}
	}
}
