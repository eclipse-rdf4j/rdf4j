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
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.junit.jupiter.api.Test;

public class BaseSailConfigTest {

	@Test
	public void testParseQueryEvaluationMode() throws Exception {
		var implNode = bnode();

		{
			var config = new BaseSailConfig("stub") {
			};

			// capitalization wrong
			var incorrectEvalMode = literal("standard");
			var model = new ModelBuilder()
					.add(implNode, BaseSailSchema.DEFAULT_QUERY_EVALUATION_MODE, incorrectEvalMode)
					.build();

			assertThatExceptionOfType(SailConfigException.class).isThrownBy(() -> config.parse(model, implNode));
		}

		{
			var config = new BaseSailConfig("stub") {
			};
			var correctEvalMode = literal("STANDARD");
			var model = new ModelBuilder()
					.add(implNode, BaseSailSchema.DEFAULT_QUERY_EVALUATION_MODE, correctEvalMode)
					.build();

			config.parse(model, implNode);
			assertThat(config.getDefaultQueryEvaluationMode()).hasValue(QueryEvaluationMode.STANDARD);
		}

		{
			var config = new BaseSailConfig("stub") {
			};
			var model = new ModelBuilder().build();

			config.parse(model, implNode);
			assertThat(config.getDefaultQueryEvaluationMode()).isEmpty();
		}
	}
}
