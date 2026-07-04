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
package org.eclipse.rdf4j.federated.write;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RepositoryWriteStrategyTest {

	@InjectMocks
	private RepositoryWriteStrategy strategy;
	@Mock
	private Repository writeRepository;
	@Mock
	private RepositoryConnection connection;

	@BeforeEach
	public void setUp() {
		when(writeRepository.getConnection()).thenReturn(connection);
	}

	@Test
	public void testBegin() {
		strategy.begin();
		verify(connection).begin();
	}

	@Test
	public void testSetTransactionSettings() {
		TransactionSetting setting = mock(TransactionSetting.class);
		strategy.setTransactionSettings(setting);

		strategy.begin();
		// check that the setting is passed when begin is called
		verify(connection).begin(setting);

		strategy.setTransactionSettings((TransactionSetting[]) null);
		strategy.begin();
		verify(connection).begin();

	}

}
