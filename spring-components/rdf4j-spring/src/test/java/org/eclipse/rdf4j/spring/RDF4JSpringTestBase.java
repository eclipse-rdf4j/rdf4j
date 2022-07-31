/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring;

import org.eclipse.rdf4j.spring.support.DataInserter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration(classes = { TestConfig.class })
@TestPropertySource("classpath:application.properties")
@TestPropertySource(
		properties = {
				"rdf4j.spring.repository.inmemory.enabled=true",
				"rdf4j.spring.repository.inmemory.use-shacl-sail=true",
				"rdf4j.spring.tx.enabled=true",
				"rdf4j.spring.resultcache.enabled=false",
				"rdf4j.spring.operationcache.enabled=false",
				"rdf4j.spring.pool.enabled=true",
				"rdf4j.spring.pool.max-connections=2"

		})
@DirtiesContext
public class RDF4JSpringTestBase {
	@BeforeAll
	public static void insertTestData(
			@Autowired DataInserter dataInserter,
			@Value("classpath:/data/example-data-artists-copy.ttl") Resource dataFile) {
		dataInserter.insertData(dataFile);
	}
}
