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

import static org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.PerformanceHint.CacheDisabled;
import static org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.PerformanceHint.CacheEnabled;
import static org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation;
import static org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.PerformanceHint.SerialValidation;
import static org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.ValidationApproach.Auto;
import static org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.ValidationApproach.Bulk;
import static org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.ValidationApproach.Disabled;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TransactionSettingsTest {

	@Test
	public void testBulk() {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.setParallelValidation(true);
		shaclSail.setCacheSelectNodes(true);

		SailRepository sailRepository = new SailRepository(shaclSail);
		addDummyData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(Bulk);

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();
			ShaclSailConnection.Settings transactionSettings = sailConnection.getTransactionSettings();

			Assertions.assertSame(transactionSettings.getValidationApproach(), Bulk);
			Assertions.assertFalse(transactionSettings.isCacheSelectNodes());
			Assertions.assertFalse(transactionSettings.isParallelValidation());

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
		addDummyData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(Bulk, ParallelValidation);

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();
			ShaclSailConnection.Settings transactionSettings = sailConnection.getTransactionSettings();

			Assertions.assertSame(transactionSettings.getValidationApproach(), Bulk);
			Assertions.assertFalse(transactionSettings.isCacheSelectNodes());
			Assertions.assertTrue(transactionSettings.isParallelValidation());

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
		addDummyData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(Bulk, ParallelValidation, CacheEnabled);

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();
			ShaclSailConnection.Settings transactionSettings = sailConnection.getTransactionSettings();

			Assertions.assertSame(transactionSettings.getValidationApproach(), Bulk);
			Assertions.assertTrue(transactionSettings.isCacheSelectNodes());
			Assertions.assertTrue(transactionSettings.isParallelValidation());

			connection.commit();

		} finally {
			sailRepository.shutDown();
		}
	}

	@Test
	public void testParallelCacheEmptyRepo() {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.setParallelValidation(true);
		shaclSail.setCacheSelectNodes(true);

		SailRepository sailRepository = new SailRepository(shaclSail);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(ParallelValidation, CacheEnabled);

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();
			ShaclSailConnection.Settings transactionSettings = sailConnection.getTransactionSettings();

			Assertions.assertSame(transactionSettings.getValidationApproach(), Bulk);
			Assertions.assertTrue(transactionSettings.isCacheSelectNodes());
			Assertions.assertTrue(transactionSettings.isParallelValidation());

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
		addDummyData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin();

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();
			ShaclSailConnection.Settings transactionSettings = sailConnection.getTransactionSettings();

			Assertions.assertSame(transactionSettings.getValidationApproach(), Auto);
			Assertions.assertTrue(transactionSettings.isCacheSelectNodes());
			Assertions.assertTrue(transactionSettings.isParallelValidation());

			connection.commit();

		} finally {
			sailRepository.shutDown();
		}
	}

	@Test
	public void testNulls() {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		SailRepository sailRepository = new SailRepository(shaclSail);
		addDummyData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin();

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();
			ShaclSailConnection.Settings transactionSettings = sailConnection.getTransactionSettings();

			Assertions.assertNotNull(transactionSettings.getValidationApproach());
			Assertions.assertTrue(transactionSettings.isCacheSelectNodes());
			Assertions.assertTrue(transactionSettings.isParallelValidation());

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

		addDummyData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(CacheDisabled, SerialValidation);

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();
			ShaclSailConnection.Settings transactionSettings = sailConnection.getTransactionSettings();

			Assertions.assertSame(transactionSettings.getValidationApproach(), Auto);
			Assertions.assertFalse(transactionSettings.isCacheSelectNodes());
			Assertions.assertFalse(transactionSettings.isParallelValidation());
			Assertions.assertSame(transactionSettings.getIsolationLevel(), IsolationLevels.SNAPSHOT_READ);

			connection.commit();

		}

		sailRepository.shutDown();

	}

	private void addDummyData(SailRepository sailRepository) {
		try (SailRepositoryConnection connection1 = sailRepository.getConnection()) {
			connection1.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
		}
	}

	@Test
	public void testSerializableParallelValidation() {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.setParallelValidation(true);

		SailRepository sailRepository = new SailRepository(shaclSail);
		addDummyData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(IsolationLevels.SERIALIZABLE, ParallelValidation);

			ShaclSailConnection sailConnection = (ShaclSailConnection) connection.getSailConnection();
			ShaclSailConnection.Settings transactionSettings = sailConnection.getTransactionSettings();

			Assertions.assertSame(transactionSettings.getValidationApproach(), Auto);
			Assertions.assertFalse(transactionSettings.isParallelValidation());

			connection.commit();

		} finally {
			sailRepository.shutDown();
		}
	}

	@Test
	public void testPriority() {
		// test default behaviour
		Assertions.assertEquals(Auto, ShaclSailConnection.Settings.getMostSignificantValidationApproach(null, null));

		// test single null
		Assertions.assertEquals(Bulk, ShaclSailConnection.Settings.getMostSignificantValidationApproach(Bulk, null));
		Assertions.assertEquals(Bulk, ShaclSailConnection.Settings.getMostSignificantValidationApproach(null, Bulk));

		// test base overrides transaction
		Assertions.assertEquals(Bulk, ShaclSailConnection.Settings.getMostSignificantValidationApproach(Bulk, Auto));
		Assertions.assertEquals(Disabled,
				ShaclSailConnection.Settings.getMostSignificantValidationApproach(Disabled, Auto));
		Assertions.assertEquals(Disabled,
				ShaclSailConnection.Settings.getMostSignificantValidationApproach(Disabled, Bulk));

		// test transaction overrides base
		Assertions.assertEquals(Bulk, ShaclSailConnection.Settings.getMostSignificantValidationApproach(Auto, Bulk));
		Assertions.assertEquals(Disabled,
				ShaclSailConnection.Settings.getMostSignificantValidationApproach(Auto, Disabled));
		Assertions.assertEquals(Disabled,
				ShaclSailConnection.Settings.getMostSignificantValidationApproach(Bulk, Disabled));

	}

	@Test
	public void testValid() throws Exception {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));
		addDummyData(repository);

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(Bulk, IsolationLevels.NONE);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.trig")) {
				connection.add(shapesData, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.commit();

		} finally {
			repository.shutDown();
		}

	}

	@Test
	public void testInvalid() throws Throwable {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));
		addDummyData(repository);

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(Bulk, IsolationLevels.NONE);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.trig")) {
				connection.add(shapesData, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			assertThrows(ShaclSailValidationException.class, () -> {
				try {
					connection.commit();
				} catch (RepositoryException e) {
					throw e.getCause();
				}
			});

		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void testInvalidSnapshot() throws Throwable {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));
		addDummyData(repository);

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(Bulk, IsolationLevels.SNAPSHOT);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.trig")) {
				connection.add(shapesData, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			assertThrows(ShaclSailValidationException.class, () -> {
				try {
					connection.commit();
				} catch (RepositoryException e) {
					throw e.getCause();
				}
			});

		} finally {
			repository.shutDown();
		}

	}

	@Test
	public void testInvalidRollsBackCorrectly() {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));
		addDummyData(repository);

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(Bulk, IsolationLevels.NONE);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.trig")) {
				connection.add(shapesData, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.commit();

		} catch (Exception ignored) {

		}

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.NONE);
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

		} finally {
			repository.shutDown();
		}

	}

	@Test
	public void testValidationDisabled() throws Throwable {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));
		addDummyData(repository);

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(Disabled);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.trig")) {
				connection.add(shapesData, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.commit();

			connection.begin(Bulk);
			try (SailRepositoryConnection connection1 = repository.getConnection()) {

				assertThrows(ShaclSailValidationException.class, () -> {
					try {
						connection.commit();
					} catch (RepositoryException e) {
						throw e.getCause();
					}
				});
			}

		} finally {
			repository.shutDown();
		}

	}

	@Test
	public void testValidationDisabledSnapshotSerializableValidation() throws Throwable {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));
		addDummyData(repository);

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(Disabled, IsolationLevels.SNAPSHOT);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.trig")) {
				connection.add(shapesData, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.commit();

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.CLASS);

			connection.begin(Disabled, IsolationLevels.SNAPSHOT);

			try (SailRepositoryConnection connection1 = repository.getConnection()) {

				connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

				connection.commit();

			}

		} finally {
			repository.shutDown();
		}

	}

	@Test
	public void testDisabledValidationBulk() throws Throwable {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));
		addDummyData(repository);

		((ShaclSail) repository.getSail()).disableValidation();

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(Bulk);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.trig")) {
				connection.add(shapesData, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.commit();

		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void testDisabledValidationAuto() throws Throwable {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));
		addDummyData(repository);

		((ShaclSail) repository.getSail()).disableValidation();

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(Auto);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.trig")) {
				connection.add(shapesData, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.commit();

			connection.begin(Auto);

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.commit();

		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void testDisabledValidationAutoEmptyRepo() throws Throwable {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));
		((ShaclSail) repository.getSail()).disableValidation();

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(Auto);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.trig")) {
				connection.add(shapesData, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.commit();

			connection.begin(Auto);

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.commit();

		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void testSerialOverrideWithModifiedShapes() throws Throwable {

		ShaclSail sail = new ShaclSail(new MemoryStore());
		ShaclSail spy = Mockito.spy(sail);
		SailRepository repository = new SailRepository(spy);
		addDummyData(repository);

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin();

			connection.add(RDFS.RESOURCE, RDF.TYPE, FOAF.PERSON);
			connection.add(RDFS.RESOURCE, FOAF.AGE, connection.getValueFactory().createLiteral(1));
			connection.add(RDFS.RESOURCE, FOAF.AGE, connection.getValueFactory().createLiteral(2));

			connection.commit();

			connection.begin(SerialValidation);

			try (InputStream shapesData = Utils.class.getClassLoader()
					.getResourceAsStream("shaclDatatypeAndMinCount.trig")) {
				connection.add(shapesData, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.commit();

			Mockito.verify(spy, Mockito.never()).submitToExecutorService(Mockito.any());

			connection.clear(RDF4J.SHACL_SHAPE_GRAPH);

			Mockito.verify(spy, Mockito.never()).submitToExecutorService(Mockito.any());

			connection.begin(ParallelValidation);

			try (InputStream shapesData = Utils.class.getClassLoader()
					.getResourceAsStream("shaclDatatypeAndMinCount.trig")) {
				connection.add(shapesData, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.commit();

			Mockito.verify(spy, Mockito.atLeastOnce()).submitToExecutorService(Mockito.any());

		} finally {
			repository.shutDown();
		}
	}

}
