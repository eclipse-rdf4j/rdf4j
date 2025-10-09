/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearch.config;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.AbstractSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.junit.jupiter.api.Test;

class ElasticsearchSailFactoryTest {

	@Test
	void getSailType() {
		ElasticsearchSailFactory f = new ElasticsearchSailFactory();
		assertEquals(ElasticsearchSailFactory.SAIL_TYPE, f.getSailType());
	}

	@Test
	void getSailFromConfig() throws Exception {
		ElasticsearchSailFactory f = new ElasticsearchSailFactory();
		ElasticsearchSailConfig cfg = new ElasticsearchSailConfig("./idx");
		assertEquals(ElasticsearchSailFactory.SAIL_TYPE, cfg.getType());

		Sail sail = f.getSail(cfg);
		assertNotNull(sail);
		assertTrue(sail instanceof LuceneSail);
	}

	@Test
	void wrongTypeThrows() {
		ElasticsearchSailFactory f = new ElasticsearchSailFactory();
		AbstractSailImplConfig wrong = new AbstractSailImplConfig("wrong:type") {
		};
		assertThrows(SailConfigException.class, () -> f.getSail(wrong));
	}
}
