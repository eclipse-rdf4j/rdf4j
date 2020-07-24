/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.common.transaction.TransactionSettingRegistry;
import org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.ValidationApproach;
import org.junit.Test;

/**
 * Unit tests for the {@link ValidationApproachFactory} class
 *
 * @author Jeen Broekstra
 *
 */
public class ValidationApproachFactoryTest {

	@Test
	public void testGetTransactionSetting() {
		ValidationApproachFactory factory = new ValidationApproachFactory();

		assertThat(factory.getTransactionSetting(ValidationApproach.Auto.getValue())).isNotEmpty();
		assertThat(factory.getTransactionSetting("unrecognized value")).isEmpty();
	}

	@Test
	public void testRegistry() {
		assertThat(TransactionSettingRegistry.getInstance().getAll())
				.extracting("class")
				.containsOnlyOnce(ValidationApproachFactory.class);
	}
}
