/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.memory.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.Values.bnode;

import org.eclipse.rdf4j.common.transaction.QueryEvaluationMode;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.sail.base.config.BaseSailSchema;
import org.junit.jupiter.api.Test;

/**
 * @author Jeen Broekstra
 *
 */
public class MemoryStoreConfigTest {

	@Test
	void testParse() {
		MemoryStoreConfig config = new MemoryStoreConfig();

		BNode implNode = bnode();
		Model serializedConfig = new ModelBuilder()
				.subject(implNode)
				.add(MemoryStoreSchema.PERSIST, true)
				.add(MemoryStoreSchema.SYNC_DELAY, 1000l)
				.add(BaseSailSchema.DEFAULT_QUERY_EVALUATION_MODE, "STANDARD")
				.build();

		config.parse(serializedConfig, implNode);

		assertThat(config.getDefaultQueryEvaluationMode()).hasValue(QueryEvaluationMode.STANDARD);
		assertThat(config.getPersist()).isTrue();
		assertThat(config.getSyncDelay()).isEqualTo(1000);
	}
}
