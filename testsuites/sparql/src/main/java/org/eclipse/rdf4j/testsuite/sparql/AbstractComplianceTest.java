/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.sparql;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.testsuite.sparql.vocabulary.EX;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for tests included in the {@link RepositorySPARQLComplianceTestSuite}.
 *
 * @author Jeen Broekstra
 *
 */
public abstract class AbstractComplianceTest {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	protected Repository repo;
	protected RepositoryConnection conn;

	@Before
	public void setUp() throws Exception {
		repo = RepositorySPARQLComplianceTestSuite.getEmptyInitializedRepository(this.getClass());
		conn = repo.getConnection();
	}

	@After
	public void tearDown() throws Exception {
		try {
			conn.close();
		} finally {
			repo.shutDown();
		}
	}

	protected void loadTestData(String dataFile, Resource... contexts)
			throws RDFParseException, RepositoryException, IOException {
		logger.debug("loading dataset {}", dataFile);
		try (InputStream dataset = this.getClass().getResourceAsStream(dataFile)) {
			conn.add(dataset, "", Rio.getParserFormatForFileName(dataFile).orElseThrow(Rio.unsupportedFormat(dataFile)),
					contexts);
		}
		logger.debug("dataset loaded.");
	}

	/**
	 * Get a set of useful namespace prefix declarations.
	 *
	 * @return namespace prefix declarations for dc, foaf and ex.
	 */
	protected String getNamespaceDeclarations() {
		return "PREFIX dc: <" + DCTERMS.NAMESPACE + "> \n" +
				"PREFIX foaf: <" + FOAF.NAMESPACE + "> \n" +
				"PREFIX ex: <" + EX.NAMESPACE + "> \n" +
				"\n";
	}
}
