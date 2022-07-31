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

package org.eclipse.rdf4j.sail.shacl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.junit.jupiter.api.Test;

/**
 * @author Håvard Ottestad
 */
public class TransactionValidationLimitTest {

	@Test
	public void testFailoverToBulkValidationSingleConnection() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shacl.trig");

		((ShaclSail) shaclRepository.getSail()).setTransactionalValidationLimit(3);

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();

			connection.begin();
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.commit();

			connection.begin(ShaclSail.TransactionSettings.PerformanceHint.CacheEnabled,
					ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation);
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);

			assertEquals(ShaclSail.TransactionSettings.ValidationApproach.Auto,
					shaclSailConnection.getTransactionSettings().getValidationApproach(),
					"Auto is the default validation approach so should still be the case since we haven't hit the transaction size limit yet.");

			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("4th statement"));

			assertEquals(ShaclSail.TransactionSettings.ValidationApproach.Bulk,
					shaclSailConnection.getTransactionSettings().getValidationApproach(),
					"We have added more than 3 statements so the validation approach should have switched to Bulk.");
			assertTrue(shaclSailConnection.getTransactionSettings().isCacheSelectNodes(),
					"Bulk validation should by default disable caching select nodes, but the local transaction settings should override this.");
			assertTrue(shaclSailConnection.getTransactionSettings().isParallelValidation(),
					"Bulk validation should by default disable parallel validation, but the local transaction settings should override this.");

			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("5th statement"));
			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}

	}

	@Test
	public void testFailoverToBulkValidationNewConnection() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shacl.trig");

		((ShaclSail) shaclRepository.getSail()).setTransactionalValidationLimit(3);

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.commit();
		}

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();

			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);

			assertEquals(ShaclSail.TransactionSettings.ValidationApproach.Auto,
					shaclSailConnection.getTransactionSettings().getValidationApproach(),
					"Auto is the default validation approach so should still be the case since we haven't hit the transaction size limit yet.");

			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("4th statement"));

			assertEquals(ShaclSail.TransactionSettings.ValidationApproach.Bulk,
					shaclSailConnection.getTransactionSettings().getValidationApproach(),
					"We have added more than 3 statements so the validation approach should have switched to Bulk.");

			assertFalse(shaclSailConnection.getTransactionSettings().isCacheSelectNodes(),
					"Bulk validation should by default disable caching select nodes.");

			assertFalse(shaclSailConnection.getTransactionSettings().isParallelValidation(),
					"Bulk validation should by default disable parallel validation.");

			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("5th statement"));
			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}

	}

	@Test
	public void testFailoverToBulkValidationTriggersValidation() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shacl.trig");

		((ShaclSail) shaclRepository.getSail()).setTransactionalValidationLimit(3);

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.add(RDFS.CLASS, RDFS.COMMENT, connection.getValueFactory().createLiteral("a"));
			connection.commit();

			connection.begin(ShaclSail.TransactionSettings.PerformanceHint.CacheEnabled,
					ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation);
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.CLASS, RDFS.COMMENT, connection.getValueFactory().createLiteral("4th statement"));
			connection.add(RDFS.CLASS, RDFS.COMMENT, connection.getValueFactory().createLiteral("5th statement"));

			assertThrows(ShaclSailValidationException.class, () -> {
				try {
					connection.commit();
				} catch (RepositoryException repositoryException) {
					throw repositoryException.getCause();
				}
			});

		} finally {
			shaclRepository.shutDown();
		}

	}

	@Test
	public void testBulkValidationForEmptySail() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shacl.trig");

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();

			connection.begin(ShaclSail.TransactionSettings.PerformanceHint.CacheEnabled);

			assertEquals(ShaclSail.TransactionSettings.ValidationApproach.Bulk,
					shaclSailConnection.getTransactionSettings().getValidationApproach(),
					"Empty sail should use bulk validation");
			assertTrue(shaclSailConnection.getTransactionSettings().isCacheSelectNodes(),
					"Bulk validation should by default disable caching select nodes, but the local transaction settings should override this.");
			assertFalse(shaclSailConnection.getTransactionSettings().isParallelValidation(),
					"Bulk validation should by default disable parallel validation.");

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}

	}

}
