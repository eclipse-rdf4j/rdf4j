/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.testsuite.repository.RepositoryConnectionTest;
import org.junit.jupiter.api.Test;

public class MemoryStoreConnectionTest extends RepositoryConnectionTest {
	@Override
	protected Repository createRepository(File dataDir) {
		return new SailRepository(new MemoryStore());
	}

	@Test
	public void reallyBigUpdateToTriggerPotentialStackOverflowTest() {
		setupTest(IsolationLevels.NONE);

		IRI g1 = vf.createIRI("urn:test:g1");
		testCon.begin();
		testCon.add(vf.createBNode(), RDF.TYPE, g1);
		testCon.commit();

		testCon.begin();

		StringBuilder bigUpdate = new StringBuilder();
		bigUpdate.append("DELETE { ?s ?p ?o } INSERT{\n");

		for (int i = 0; i < 20000; i++) {
			bigUpdate.append(" [] <urn:test:prop> \"").append(i).append("\".\n");
		}
		bigUpdate.append("\n} WHERE { ?s ?p ?o }");

		testCon.prepareUpdate(bigUpdate.toString()).execute();

		long size = testCon.size();
		assertThat(size).isEqualTo(20000);
		testCon.commit();
	}
}
