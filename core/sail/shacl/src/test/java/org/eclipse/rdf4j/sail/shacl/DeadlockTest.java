/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import java.io.File;
import java.io.IOException;

import org.assertj.core.util.Files;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Isolated
public class DeadlockTest {

	@Test
	public void test() throws IOException {

		for (int i = 0; i < 10; i++) {

			String shaclPath = "complexBenchmark/";

			File dataDir = Files.newTemporaryFolder();
			dataDir.deleteOnExit();

			ShaclSail shaclSail = new ShaclSail(new NativeStore(dataDir));
			SailRepository shaclRepository = new SailRepository(shaclSail);
			shaclRepository.init();

			shaclSail.setParallelValidation(true);

			Utils.loadShapeData(shaclRepository, shaclPath + "shacl.trig");

			try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

				connection.begin(IsolationLevels.SNAPSHOT);
				connection
						.prepareUpdate(IOUtil.readString(
								DeadlockTest.class.getClassLoader().getResourceAsStream(shaclPath + "transaction1.qr")))
						.execute();
				connection.commit();

				connection.begin(IsolationLevels.SNAPSHOT);
				connection
						.prepareUpdate(IOUtil.readString(
								DeadlockTest.class.getClassLoader().getResourceAsStream(shaclPath + "transaction2.qr")))
						.execute();
				connection.commit();
			} finally {
				shaclRepository.shutDown();
			}
		}
	}

}
