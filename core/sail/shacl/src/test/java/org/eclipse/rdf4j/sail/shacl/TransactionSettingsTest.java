/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

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

			assert transactionSettings.getValidationApproach() == ShaclSail.TransactionSettings.ValidationApproach.Bulk;
			assert !transactionSettings.isCacheSelectNodes();
			assert !transactionSettings.isParallelValidation();

			connection.commit();

		}

		sailRepository.shutDown();

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

			assert transactionSettings.getValidationApproach() == ShaclSail.TransactionSettings.ValidationApproach.Bulk;
			assert !transactionSettings.isCacheSelectNodes();
			assert transactionSettings.isParallelValidation();

			connection.commit();

		}

		sailRepository.shutDown();

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

			assert transactionSettings.getValidationApproach() == ShaclSail.TransactionSettings.ValidationApproach.Bulk;
			assert transactionSettings.isCacheSelectNodes();
			assert transactionSettings.isParallelValidation();

			connection.commit();

		}

		sailRepository.shutDown();

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

			assert transactionSettings.getValidationApproach() == ShaclSail.TransactionSettings.ValidationApproach.Auto;
			assert transactionSettings.isCacheSelectNodes();
			assert transactionSettings.isParallelValidation();

			connection.commit();

		}

		sailRepository.shutDown();

	}

	@Test
	public void testNulls() {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		SailRepository sailRepository = new SailRepository(shaclSail);
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin();

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();
			ShaclSailConnection.Settings transactionSettings = sailConnection.getTransactionSettings();

			assert transactionSettings.getValidationApproach() != null;
			assert transactionSettings.isCacheSelectNodes();
			assert transactionSettings.isParallelValidation();

			connection.commit();

		}

		sailRepository.shutDown();

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

			assert transactionSettings.getValidationApproach() == ShaclSail.TransactionSettings.ValidationApproach.Auto;
			assert !transactionSettings.isCacheSelectNodes();
			assert !transactionSettings.isParallelValidation();

			connection.commit();

		}

		sailRepository.shutDown();

	}

}
