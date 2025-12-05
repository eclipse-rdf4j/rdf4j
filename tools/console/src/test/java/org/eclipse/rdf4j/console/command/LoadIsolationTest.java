/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class LoadIsolationTest extends AbstractCommandTest {

	@Mock
	private Repository mockRepository;

	@Mock
	private RepositoryConnection mockConnection;

	private Load cmd;

	@BeforeEach
	public void setUp() throws Exception {
		cmd = new Load(mockConsoleIO, mockConsoleState, defaultSettings);

		when(mockConsoleState.getRepository()).thenReturn(mockRepository);
		when(mockRepository.getConnection()).thenReturn(mockConnection);
		doNothing().when(mockConnection).add(any(File.class), isNull(), isNull(), any());
	}

	@Test
        public void promptBeforeUsingDefaultIsolation() throws Exception {
		when(mockConsoleIO.askProceed(contains("isolation level NONE"), eq(false))).thenReturn(false);

		cmd.execute("load", "data.ttl");

		verify(mockConsoleIO).askProceed(contains("isolation level NONE"), eq(false));
		verify(mockConnection, never()).add(any(File.class), isNull(), isNull(), any());
	}

	@Test
	public void allowsIsolationArgumentWithoutPrompt() throws Exception {
		cmd.execute("load", "data.ttl", "isolation", IsolationLevels.SNAPSHOT.name());

		verify(mockConsoleIO, never()).askProceed(anyString(), eq(false));
		verify(mockConnection).begin(IsolationLevels.SNAPSHOT);
	}
}
