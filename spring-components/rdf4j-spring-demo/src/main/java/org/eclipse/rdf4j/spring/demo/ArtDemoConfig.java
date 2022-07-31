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

import org.eclipse.rdf4j.spring.RDF4JConfig;
import org.eclipse.rdf4j.spring.dao.RDF4JDao;
import org.eclipse.rdf4j.spring.demo.support.InitialDataInserter;
import org.eclipse.rdf4j.spring.support.DataInserter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;

/**
 * Spring config for the demo.
 *
 * Here is what it does:
 *
 * <ul>
 * <li>it imports {@link RDF4JConfig} which interprets the config properties (in our example, they are in
 * <code>application.properties</code>) and registers a number of beans.</li>
 * <li>it scans the <code>org.eclipse.rdf4j.spring.demo.dao</code> package, finds the DAOs, registers them as beans and
 * injects their dependencies</li>
 * <li>it configures the 'data inserter' beans, which read data from the 'artists.ttl' file and adds them to the
 * repository at startup</li>
 * </ul>
 *
 * See {@link org.eclipse.rdf4j.spring Rdf4J-Spring} for an overview and more pointers.
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
@Configuration
@Import(RDF4JConfig.class)
@ComponentScan(
		value = "org.eclipse.rdf4j.spring.demo.dao", includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RDF4JDao.class))
public class ArtDemoConfig {
	@Bean
	public DataInserter getDataInserter() {
		return new DataInserter();
	}

	@Bean
	public InitialDataInserter getInitialDataInserter(
			@Autowired DataInserter dataInserter,
			@Value("classpath:/artists.ttl") Resource ttlFile) {
		return new InitialDataInserter(dataInserter, ttlFile);
	}
}
