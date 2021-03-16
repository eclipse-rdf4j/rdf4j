/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;

public class TransactionSettingsTest {

	@Test
	public void testBulk() {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.setParallelValidation(true);
		shaclSail.setCacheSelectNodes(true);

		SailRepository sailRepository = new SailRepository(shaclSail);
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk);

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();
			ShaclSailConnection.Settings transactionSettings = sailConnection.getTransactionSettings();

			assertSame(transactionSettings.getValidationApproach(),
					ShaclSail.TransactionSettings.ValidationApproach.Bulk);
			assertFalse(transactionSettings.isCacheSelectNodes());
			assertFalse(transactionSettings.isParallelValidation());

			connection.commit();

		} finally {
			sailRepository.shutDown();
		}

	}

	@Test
	public void testBulkParallel() {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.setParallelValidation(true);
		shaclSail.setCacheSelectNodes(true);

		SailRepository sailRepository = new SailRepository(shaclSail);
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk,
					ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation);

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();
			ShaclSailConnection.Settings transactionSettings = sailConnection.getTransactionSettings();

			assertSame(transactionSettings.getValidationApproach(),
					ShaclSail.TransactionSettings.ValidationApproach.Bulk);
			assertFalse(transactionSettings.isCacheSelectNodes());
			assertTrue(transactionSettings.isParallelValidation());

			connection.commit();

		} finally {
			sailRepository.shutDown();
		}
	}

	@Test
	public void testBulkParallelCache() {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.setParallelValidation(true);
		shaclSail.setCacheSelectNodes(true);

		SailRepository sailRepository = new SailRepository(shaclSail);
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk,
					ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation,
					ShaclSail.TransactionSettings.PerformanceHint.CacheEnabled);

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();
			ShaclSailConnection.Settings transactionSettings = sailConnection.getTransactionSettings();

			assertSame(transactionSettings.getValidationApproach(),
					ShaclSail.TransactionSettings.ValidationApproach.Bulk);
			assertTrue(transactionSettings.isCacheSelectNodes());
			assertTrue(transactionSettings.isParallelValidation());

			connection.commit();

		} finally {
			sailRepository.shutDown();
		}
	}

	@Test
	public void testDefault() {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.setParallelValidation(true);
		shaclSail.setCacheSelectNodes(true);

		SailRepository sailRepository = new SailRepository(shaclSail);
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin();

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();
			ShaclSailConnection.Settings transactionSettings = sailConnection.getTransactionSettings();

			assertSame(transactionSettings.getValidationApproach(),
					ShaclSail.TransactionSettings.ValidationApproach.Auto);
			assertTrue(transactionSettings.isCacheSelectNodes());
			assertTrue(transactionSettings.isParallelValidation());

			connection.commit();

		} finally {
			sailRepository.shutDown();
		}
	}

	@Test
	public void testNulls() {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		SailRepository sailRepository = new SailRepository(shaclSail);
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin();

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();
			ShaclSailConnection.Settings transactionSettings = sailConnection.getTransactionSettings();

			assertNotNull(transactionSettings.getValidationApproach());
			assertTrue(transactionSettings.isCacheSelectNodes());
			assertTrue(transactionSettings.isParallelValidation());

			connection.commit();

		} finally {
			sailRepository.shutDown();
		}
	}

	@Test
	public void testDefaultOverride() {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.setParallelValidation(true);
		shaclSail.setCacheSelectNodes(true);

		SailRepository sailRepository = new SailRepository(shaclSail);
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(ShaclSail.TransactionSettings.PerformanceHint.CacheDisabled,
					ShaclSail.TransactionSettings.PerformanceHint.SerialValidation);

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();
			ShaclSailConnection.Settings transactionSettings = sailConnection.getTransactionSettings();

			assertSame(transactionSettings.getValidationApproach(),
					ShaclSail.TransactionSettings.ValidationApproach.Auto);
			assertFalse(transactionSettings.isCacheSelectNodes());
			assertFalse(transactionSettings.isParallelValidation());
			assertSame(transactionSettings.getIsolationLevel(), IsolationLevels.SNAPSHOT_READ);

			connection.commit();

		}

		sailRepository.shutDown();

	}

	@Test
	public void testSerializableParallelValidation() {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.setParallelValidation(true);

		SailRepository sailRepository = new SailRepository(shaclSail);
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(IsolationLevels.SERIALIZABLE,
					ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation);

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();
			ShaclSailConnection.Settings transactionSettings = sailConnection.getTransactionSettings();

			assertSame(transactionSettings.getValidationApproach(),
					ShaclSail.TransactionSettings.ValidationApproach.Auto);
			assertFalse(transactionSettings.isParallelValidation());

			connection.commit();

		} finally {
			sailRepository.shutDown();
		}
	}

}
