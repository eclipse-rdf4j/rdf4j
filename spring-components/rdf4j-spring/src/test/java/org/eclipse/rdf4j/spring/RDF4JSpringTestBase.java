/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.spring;

import org.eclipse.rdf4j.spring.operationcache.OperationCacheConfig;
import org.eclipse.rdf4j.spring.operationlog.OperationLogConfig;
import org.eclipse.rdf4j.spring.operationlog.log.jmx.OperationLogJmxConfig;
import org.eclipse.rdf4j.spring.pool.PoolConfig;
import org.eclipse.rdf4j.spring.repository.inmemory.InMemoryRepositoryConfig;
import org.eclipse.rdf4j.spring.repository.remote.RemoteRepositoryConfig;
import org.eclipse.rdf4j.spring.resultcache.ResultCacheConfig;
import org.eclipse.rdf4j.spring.support.DataInserter;
import org.eclipse.rdf4j.spring.tx.TxConfig;
import org.eclipse.rdf4j.spring.uuidsource.noveltychecking.NoveltyCheckingUUIDSourceConfig;
import org.eclipse.rdf4j.spring.uuidsource.sequence.UUIDSequenceConfig;
import org.eclipse.rdf4j.spring.uuidsource.simple.SimpleRepositoryUUIDSourceConfig;
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
@ContextConfiguration(
		classes = {
				RDF4JConfig.class,
				TestConfig.class,
				InMemoryRepositoryConfig.class,
				RemoteRepositoryConfig.class,
				PoolConfig.class,
				ResultCacheConfig.class,
				OperationCacheConfig.class,
				OperationLogConfig.class,
				OperationLogJmxConfig.class,
				TxConfig.class,
				UUIDSequenceConfig.class,
				NoveltyCheckingUUIDSourceConfig.class,
				SimpleRepositoryUUIDSourceConfig.class
		})
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
