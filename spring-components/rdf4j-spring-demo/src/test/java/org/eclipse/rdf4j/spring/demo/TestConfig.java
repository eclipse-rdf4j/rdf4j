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

package org.eclipse.rdf4j.spring.demo;

import org.eclipse.rdf4j.spring.support.DataInserter;
import org.eclipse.rdf4j.spring.test.RDF4JTestConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@TestConfiguration
@EnableTransactionManagement
@Import(RDF4JTestConfig.class)
@ComponentScan("org.eclipse.rdf4j.spring.demo.*")
public class TestConfig {

	@Bean
	DataInserter getDataInserter() {
		return new DataInserter();
	}

}
