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

package org.eclipse.rdf4j.spring.test;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.spring.RDF4JConfig;
import org.eclipse.rdf4j.spring.operationcache.OperationCacheConfig;
import org.eclipse.rdf4j.spring.operationlog.OperationLogConfig;
import org.eclipse.rdf4j.spring.operationlog.log.jmx.OperationLogJmxConfig;
import org.eclipse.rdf4j.spring.pool.PoolConfig;
import org.eclipse.rdf4j.spring.repository.inmemory.InMemoryRepositoryConfig;
import org.eclipse.rdf4j.spring.repository.remote.RemoteRepositoryConfig;
import org.eclipse.rdf4j.spring.resultcache.ResultCacheConfig;
import org.eclipse.rdf4j.spring.tx.TxConfig;
import org.eclipse.rdf4j.spring.uuidsource.noveltychecking.NoveltyCheckingUUIDSourceConfig;
import org.eclipse.rdf4j.spring.uuidsource.predictable.PredictableUUIDSourceConfig;
import org.eclipse.rdf4j.spring.uuidsource.sequence.UUIDSequenceConfig;
import org.eclipse.rdf4j.spring.uuidsource.simple.SimpleRepositoryUUIDSourceConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Spring configuration for use in unit tests. Imports the configurations of all subsystems that are autoconfigured when
 * used outside of tests. Test configurations should import this configuration:
 *
 * <pre>
 * 	&#64TestConfiguration
 * 	&#64Import(RDF4JTestConfig.class)
 * 	&#64ComponentScan(basePackages = "com.example.myapp.*")
 *  	public class TestConfig {
 * 			// application-specific configuration
 *   	}
 * </pre>
 *
 * @author Florian Kleedorfer
 * @since 4.0.0
 */
@Experimental
@Configuration
@Import({
		RDF4JConfig.class,
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
		SimpleRepositoryUUIDSourceConfig.class,
		PredictableUUIDSourceConfig.class
})
public class RDF4JTestConfig {
}
