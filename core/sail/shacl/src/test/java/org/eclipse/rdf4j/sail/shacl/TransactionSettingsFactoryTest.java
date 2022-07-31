/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.common.transaction.TransactionSettingFactory;
import org.eclipse.rdf4j.common.transaction.TransactionSettingRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests that all {@link ShaclSail.TransactionSettings} are registered in the TransactionSettingRegistry through a
 * TransactionSettingFactory
 *
 * @author Jeen Broekstra, Håvard M. Ottestad
 *
 */
public class TransactionSettingsFactoryTest {

	// this need static
	static Class<TransactionSetting>[] transactionSettingsProvider() {
		return (Class<TransactionSetting>[]) ShaclSail.TransactionSettings.class.getDeclaredClasses();
	}

	@ParameterizedTest
	@MethodSource("transactionSettingsProvider")
	public void testGetTransactionSetting(Class<? extends TransactionSetting> clazz) {

		TransactionSetting[] enumConstants = clazz.getEnumConstants();

		for (TransactionSetting expected : enumConstants) {

			Optional<TransactionSettingFactory> transactionSettingFactory = TransactionSettingRegistry.getInstance()
					.get(expected.getName());
			assertThat(transactionSettingFactory).isNotEmpty();

			Optional<TransactionSetting> actual = transactionSettingFactory.get()
					.getTransactionSetting(expected.getValue());
			assertThat(actual).isNotEmpty();

			assertThat(actual.get()).isEqualTo(expected);
			assertThat(transactionSettingFactory.get().getTransactionSetting("unrecognized value")).isEmpty();
		}

	}

	@Test
	public void testDistinctTransactionSettingsProviders() {
		assertThat(TransactionSettingRegistry.getInstance().getAll())
				.extracting("class")
				.doesNotHaveDuplicates();
	}

}
