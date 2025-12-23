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

package org.eclipse.rdf4j.http.server.readonly;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class MemoryBackedOnlySparqlApplicationTestConfig {

	@Bean(destroyMethod = "shutDown")
	public Repository getTestRepository() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		sailRepository.init();
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(sailRepository.getValueFactory().createStatement(RDF.ALT, RDF.BAG, RDF.FIRST));
		}

		return sailRepository;
	}
}
