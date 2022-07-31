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
package org.eclipse.rdf4j.console.command;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.ProxyRepositoryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Bart Hanssens
 */
public class LoadTest extends AbstractCommandTest {

	private static final String MEMORY_MEMBER_ID1 = "alien";

	private static final String PROXY_ID = "proxyID";

	private Load cmd;

	@BeforeEach
	public void setUp() throws UnsupportedEncodingException, IOException, RDF4JException {
		manager = new LocalRepositoryManager(locationFile);

		addRepositories("load", MEMORY_MEMBER_ID1);
		manager.addRepositoryConfig(new RepositoryConfig(PROXY_ID, new ProxyRepositoryConfig(MEMORY_MEMBER_ID1)));

		ConsoleState state = mock(ConsoleState.class);
		when(state.getManager()).thenReturn(manager);
		cmd = new Load(mockConsoleIO, state, defaultSettings);
	}

	@Test
	public final void testLoad() throws RepositoryException, IOException {
		File f = new File(locationFile, "alien.ttl");
		copyFromResource("load/alien.ttl", f);

		cmd.execute("load", f.getAbsolutePath());
		verify(mockConsoleIO, never()).writeError(anyString());
	}

	@Test
	public final void testLoadWorkDir() throws RepositoryException, IOException {
		setWorkingDir(cmd);

		File f = new File(locationFile, "alien.ttl");
		copyFromResource("load/alien.ttl", f);

		cmd.execute("load", f.getName());
		verify(mockConsoleIO, never()).writeError(anyString());
	}
}
