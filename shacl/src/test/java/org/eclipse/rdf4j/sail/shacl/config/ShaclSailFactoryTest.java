/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.config;

import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ShaclSailFactory}
 * 
 * @author Jeen Broekstra
 */
public class ShaclSailFactoryTest {

	private ShaclSailFactory subject;

	@Before
	public void setUp() throws Exception {
		subject = new ShaclSailFactory();
	}

	@Test
	public void getSailTypeReturnsCorrectValue() {
		assertThat(subject.getSailType()).isEqualTo(ShaclSailFactory.SAIL_TYPE);
	}

	/**
	 * Verify that the created sail is configured according to the supplied default configuration.
	 */
	@Test
	public void getSailWithDefaultConfigSetsConfigurationCorrectly() {
		ShaclSailConfig config = new ShaclSailConfig();
		ShaclSail sail = (ShaclSail) subject.getSail(config);
		assertMatchesConfig(sail, config);
	}

	/**
	 * Verify that the created sail is configured according to the supplied customized configuration.
	 */
	@Test
	public void getSailWithCustomConfigSetsConfigurationCorrectly() {
		ShaclSailConfig config = new ShaclSailConfig();

		// set everything to the opposite of its default
		config.setCacheSelectNodes(!config.isCacheSelectNodes());
		config.setGlobalLogValidationExecution(!config.isGlobalLogValidationExecution());
		config.setIgnoreNoShapesLoadedException(!config.isIgnoreNoShapesLoadedException());
		config.setLogValidationPlans(!config.isLogValidationPlans());
		config.setLogValidationViolations(!config.isLogValidationViolations());
		config.setParallelValidation(!config.isParallelValidation());
		config.setUndefinedTargetValidatesAllSubjects(!config.isUndefinedTargetValidatesAllSubjects());
		config.setValidationEnabled(!config.isValidationEnabled());

		ShaclSail sail = (ShaclSail) subject.getSail(config);
		assertMatchesConfig(sail, config);
	}

	private void assertMatchesConfig(ShaclSail sail, ShaclSailConfig config) {
		assertThat(sail.isCacheSelectNodes()).isEqualTo(config.isCacheSelectNodes());
		assertThat(sail.isGlobalLogValidationExecution()).isEqualTo(config.isGlobalLogValidationExecution());
		assertThat(sail.isIgnoreNoShapesLoadedException()).isEqualTo(config.isIgnoreNoShapesLoadedException());
		assertThat(sail.isLogValidationPlans()).isEqualTo(config.isLogValidationPlans());
		assertThat(sail.isLogValidationViolations()).isEqualTo(config.isLogValidationViolations());
		assertThat(sail.isParallelValidation()).isEqualTo(config.isParallelValidation());
		assertThat(sail.isUndefinedTargetValidatesAllSubjects())
				.isEqualTo(config.isUndefinedTargetValidatesAllSubjects());
		assertThat(sail.isValidationEnabled()).isEqualTo(config.isValidationEnabled());
	}

}
