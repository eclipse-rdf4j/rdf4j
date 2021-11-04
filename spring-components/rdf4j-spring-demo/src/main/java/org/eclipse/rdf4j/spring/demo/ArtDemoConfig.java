/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.demo;

import org.eclipse.rdf4j.spring.RDF4JConfig;
import org.eclipse.rdf4j.spring.dao.RDF4JDao;
import org.eclipse.rdf4j.spring.demo.support.InitialDataInserter;
import org.eclipse.rdf4j.spring.support.DataInserter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.io.Resource;

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
