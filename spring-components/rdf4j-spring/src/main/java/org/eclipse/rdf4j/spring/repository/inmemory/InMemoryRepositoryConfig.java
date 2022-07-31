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

package org.eclipse.rdf4j.spring.repository.inmemory;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
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
@EnableConfigurationProperties(InMemoryRepositoryProperties.class)
@ConditionalOnProperty("rdf4j.spring.repository.inmemory.enabled")
public class InMemoryRepositoryConfig {
	@Bean
	public Repository getInMemoryRepository(
			@Autowired InMemoryRepositoryProperties repositoryProperties) {
		if (repositoryProperties.isUseShaclSail()) {
			return new SailRepository(new ShaclSail(new MemoryStore()));
		} else {
			return new SailRepository(new MemoryStore());
		}
	}
}
