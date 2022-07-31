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
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RepositoryWriteStrategyTest {

	private RepositoryWriteStrategy strategy;
	private Repository writeRepository;
	private RepositoryConnection connection;

	@BeforeEach
	public void setUp() {
		writeRepository = mock(Repository.class);
		connection = mock(RepositoryConnection.class);
		when(writeRepository.getConnection()).thenReturn(connection);

		strategy = new RepositoryWriteStrategy(writeRepository);
	}

	@Test
	public void testBegin() throws Exception {
		strategy.begin();
		verify(connection).begin();
	}

	@Test
	public void testSetTransactionSettings() throws Exception {
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
