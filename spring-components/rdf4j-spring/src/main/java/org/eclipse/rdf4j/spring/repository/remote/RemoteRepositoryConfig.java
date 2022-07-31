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

package org.eclipse.rdf4j.spring.repository.remote;

import java.lang.invoke.MethodHandles;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.spring.support.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @since 4.0.0
 * @author Gabriel Pickl
 * @author Florian Kleedorfer
 */
@Configuration
@EnableConfigurationProperties(RemoteRepositoryProperties.class)
@ConditionalOnProperty("rdf4j.spring.repository.remote.manager-url")
public class RemoteRepositoryConfig {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Bean
	public Repository getRemoteRepository(
			@Autowired RemoteRepositoryProperties repositoryProperties) {
		Repository repository;
		logger.info("Using these repository properties: {}", repositoryProperties);
		try {
			RepositoryManager repositoryManager = new RemoteRepositoryManager(repositoryProperties.getManagerUrl());
			repositoryManager.init();
			repository = repositoryManager.getRepository(repositoryProperties.getName());
			logger.debug("Successfully initialized repository config: {}", repositoryProperties);
			return repository;
		} catch (Exception e) {
			throw new ConfigurationException(
					String.format(
							"Unable to retrieve repository for repository config %s",
							repositoryProperties),
					e);
		}
	}
}
