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

import java.lang.invoke.MethodHandles;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.spring.operationcache.CachingOperationInstantiator;
import org.eclipse.rdf4j.spring.operationcache.OperationCacheProperties;
import org.eclipse.rdf4j.spring.operationlog.LoggingRepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.operationlog.log.OperationLog;
import org.eclipse.rdf4j.spring.pool.PoolProperties;
import org.eclipse.rdf4j.spring.pool.PooledRepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.resultcache.CachingRepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.resultcache.ResultCacheProperties;
import org.eclipse.rdf4j.spring.support.DirectOperationInstantiator;
import org.eclipse.rdf4j.spring.support.OperationInstantiator;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;
import org.eclipse.rdf4j.spring.support.UUIDSource;
import org.eclipse.rdf4j.spring.support.connectionfactory.DirectRepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.support.connectionfactory.RepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.tx.TransactionalRepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.tx.TxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
@Experimental
@Configuration
@EnableTransactionManagement
public class RDF4JConfig {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Bean
	RDF4JTemplate getRdf4JTemplate(@Autowired RepositoryConnectionFactory repositoryConnectionFactory,
			@Autowired(required = false) OperationCacheProperties operationCacheProperties,
			@Autowired ResourceLoader resourceLoader,
			@Autowired(required = false) UUIDSource uuidSource) {
		OperationInstantiator operationInstantiator;
		if (operationCacheProperties != null && operationCacheProperties.isEnabled()) {
			logger.debug("Operation caching is enabled");
			operationInstantiator = new CachingOperationInstantiator();
		} else {
			logger.debug("Operation caching is not enabled");
			operationInstantiator = new DirectOperationInstantiator();
		}
		return new RDF4JTemplate(repositoryConnectionFactory, operationInstantiator, resourceLoader, uuidSource);
	}

	@Bean
	RepositoryConnectionFactory getRepositoryConnectionFactory(
			@Autowired Repository repository,
			@Autowired(required = false) PoolProperties poolProperties,
			@Autowired(required = false) ResultCacheProperties resultCacheProperties,
			@Autowired(required = false) OperationLog operationLog,
			@Autowired(required = false) TxProperties txProperties) {
		RepositoryConnectionFactory factory = getDirectRepositoryConnectionFactory(repository);

		if (poolProperties != null && poolProperties.isEnabled()) {
			logger.debug("Connection pooling is enabled");
			factory = wrapWithPooledRepositoryConnectionFactory(factory, poolProperties);
		} else {
			logger.debug("Connection pooling is not enabled");
		}
		if (resultCacheProperties != null && resultCacheProperties.isEnabled()) {
			factory = wrapWithCachingRepositoryConnectionFactory(factory, resultCacheProperties);
			logger.debug("Result caching is enabled");
		} else {
			logger.debug("Result caching is not enabled");
		}
		if (operationLog != null) {
			factory = wrapWithLoggingRepositoryConnectionFactory(factory, operationLog);
			logger.debug("Query logging is enabled");
		} else {
			logger.debug("Query logging is not enabled");
		}
		if (txProperties != null && txProperties.isEnabled()) {
			factory = wrapWithTxRepositoryConnectionFactory(factory);
			logger.debug("Spring transaction integration is enabled");
		} else {
			logger.debug("Spring transaction integration is not enabled");
		}
		return factory;
	}

	RepositoryConnectionFactory getDirectRepositoryConnectionFactory(Repository repository) {
		return new DirectRepositoryConnectionFactory(repository);
	}

	RepositoryConnectionFactory wrapWithPooledRepositoryConnectionFactory(
			RepositoryConnectionFactory delegate, PoolProperties poolProperties) {
		GenericObjectPoolConfig<RepositoryConnection> config = new GenericObjectPoolConfig<>();
		config.setMaxTotal(poolProperties.getMaxConnections());
		config.setMinIdle(poolProperties.getMinIdleConnections());
		config.setTimeBetweenEvictionRunsMillis(
				poolProperties.getTimeBetweenEvictionRuns().toMillis());
		config.setTestWhileIdle(poolProperties.isTestWhileIdle());
		return new PooledRepositoryConnectionFactory(delegate, config);
	}

	RepositoryConnectionFactory wrapWithLoggingRepositoryConnectionFactory(
			RepositoryConnectionFactory delegate, OperationLog operationLog) {
		return new LoggingRepositoryConnectionFactory(delegate, operationLog);
	}

	RepositoryConnectionFactory wrapWithCachingRepositoryConnectionFactory(
			RepositoryConnectionFactory delegate, ResultCacheProperties resultCacheProperties) {
		return new CachingRepositoryConnectionFactory(delegate, resultCacheProperties);
	}

	TransactionalRepositoryConnectionFactory wrapWithTxRepositoryConnectionFactory(
			RepositoryConnectionFactory delegate) {
		return new TransactionalRepositoryConnectionFactory(delegate);
	}
}
