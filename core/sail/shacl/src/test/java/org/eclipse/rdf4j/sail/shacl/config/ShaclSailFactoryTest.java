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
package org.eclipse.rdf4j.sail.shacl.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractNotifyingSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ShaclSailFactory}
 *
 * @author Jeen Broekstra
 */
public class ShaclSailFactoryTest {

	@Test
	public void getSailTypeReturnsCorrectValue() {
		ShaclSailFactory subject = new ShaclSailFactory();
		assertThat(subject.getSailType()).isEqualTo(ShaclSailFactory.SAIL_TYPE);
	}

	/**
	 * Verify that the created sail is configured according to the supplied default configuration.
	 */
	@Test
	public void getSailWithDefaultConfigSetsConfigurationCorrectly() {
		ShaclSailFactory subject = new ShaclSailFactory();

		ShaclSailConfig config = new ShaclSailConfig();

		ShaclSail sail = (ShaclSail) subject.getSail(config);
		sail.setBaseSail(new MemoryStore());

		assertMatchesConfig(sail, config);

		sail.shutDown();
	}

	/**
	 * Verify that the created sail is configured according to the supplied default configuration.
	 */
	@Test
	public void serializableValidationLimitedBySupportIsolationLevelsTest() {
		ShaclSailFactory subject = new ShaclSailFactory();

		ShaclSailConfig config = new ShaclSailConfig();
		config.setSerializableValidation(true);

		ShaclSail sail = (ShaclSail) subject.getSail(config);

		assertFalse(sail.isSerializableValidation());

		AbstractNotifyingSail sailThatDoesntSupportSnapshotIsolation = new AbstractNotifyingSail() {
			@Override
			protected void shutDownInternal() throws SailException {

			}

			@Override
			protected NotifyingSailConnection getConnectionInternal() throws SailException {
				return null;
			}

			@Override
			public boolean isWritable() throws SailException {
				return false;
			}

			@Override
			public ValueFactory getValueFactory() {
				return null;
			}

			@Override
			public List<IsolationLevel> getSupportedIsolationLevels() {
				return List.of(IsolationLevels.NONE, IsolationLevels.READ_COMMITTED, IsolationLevels.SNAPSHOT_READ,
						IsolationLevels.SERIALIZABLE);
			}
		};

		sail.setBaseSail(sailThatDoesntSupportSnapshotIsolation);
		assertFalse(sail.isSerializableValidation());

		sail.setBaseSail(new MemoryStore());
		assertTrue(sail.isSerializableValidation());

		sail.shutDown();

	}

	/**
	 * Verify that the created sail is configured according to the supplied customized configuration.
	 */
	@Test
	public void getSailWithCustomConfigSetsConfigurationCorrectly() {
		ShaclSailFactory subject = new ShaclSailFactory();

		ShaclSailConfig config = new ShaclSailConfig();

		// set everything to the opposite of its default
		config.setCacheSelectNodes(!config.isCacheSelectNodes());
		config.setGlobalLogValidationExecution(!config.isGlobalLogValidationExecution());
		config.setLogValidationPlans(!config.isLogValidationPlans());
		config.setLogValidationViolations(!config.isLogValidationViolations());
		config.setParallelValidation(!config.isParallelValidation());
		config.setValidationEnabled(!config.isValidationEnabled());
		config.setPerformanceLogging(!config.isPerformanceLogging());
		config.setSerializableValidation(!config.isSerializableValidation());
		config.setRdfsSubClassReasoning(!config.isRdfsSubClassReasoning());
		config.setEclipseRdf4jShaclExtensions(!config.isEclipseRdf4jShaclExtensions());
		config.setDashDataShapes(!config.isDashDataShapes());

		config.setValidationResultsLimitTotal(100);
		config.setValidationResultsLimitPerConstraint(3);

		ShaclSail sail = (ShaclSail) subject.getSail(config);
		assertMatchesConfig(sail, config);

	}

	private void assertMatchesConfig(ShaclSail sail, ShaclSailConfig config) {
		assertThat(sail.isCacheSelectNodes()).isEqualTo(config.isCacheSelectNodes());
		assertThat(sail.isGlobalLogValidationExecution()).isEqualTo(config.isGlobalLogValidationExecution());
		assertThat(sail.isLogValidationPlans()).isEqualTo(config.isLogValidationPlans());
		assertThat(sail.isLogValidationViolations()).isEqualTo(config.isLogValidationViolations());
		assertThat(sail.isParallelValidation()).isEqualTo(config.isParallelValidation());
		assertThat(sail.isValidationEnabled()).isEqualTo(config.isValidationEnabled());
		assertThat(sail.isPerformanceLogging()).isEqualTo(config.isPerformanceLogging());
		assertThat(sail.isSerializableValidation()).isEqualTo(config.isSerializableValidation());
		assertThat(sail.isRdfsSubClassReasoning()).isEqualTo(config.isRdfsSubClassReasoning());
		assertThat(sail.isEclipseRdf4jShaclExtensions()).isEqualTo(config.isEclipseRdf4jShaclExtensions());
		assertThat(sail.isDashDataShapes()).isEqualTo(config.isDashDataShapes());
		assertThat(sail.getValidationResultsLimitTotal()).isEqualTo(config.getValidationResultsLimitTotal());
		assertThat(sail.getValidationResultsLimitPerConstraint())
				.isEqualTo(config.getValidationResultsLimitPerConstraint());
	}

}
