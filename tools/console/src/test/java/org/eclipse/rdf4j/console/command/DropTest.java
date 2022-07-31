/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
 * @author Dale Visser
 */
public class DropTest extends AbstractCommandTest {

	private static final String MEMORY_MEMBER_ID1 = "alien";

	private static final String PROXY_ID = "proxyID";

	private Drop drop;

	@BeforeEach
	public void setUp() throws UnsupportedEncodingException, IOException, RDF4JException {
		manager = new LocalRepositoryManager(locationFile);

		addRepositories("drop", MEMORY_MEMBER_ID1);
		manager.addRepositoryConfig(new RepositoryConfig(PROXY_ID, new ProxyRepositoryConfig(MEMORY_MEMBER_ID1)));

		ConsoleState state = mock(ConsoleState.class);
		when(state.getManager()).thenReturn(manager);
		drop = new Drop(mockConsoleIO, state, new Close(mockConsoleIO, state));
	}

	private void setUserDropConfirm(boolean confirm) throws IOException {
		when(mockConsoleIO.askProceed(startsWith("WARNING: you are about to drop repository '"), anyBoolean()))
				.thenReturn(confirm);
	}

	@Test
	public final void testSafeDrop() throws RepositoryException, IOException {
		setUserDropConfirm(true);
		assertThat(manager.isSafeToRemove(PROXY_ID)).isTrue();
		drop.execute("drop", PROXY_ID);
		verify(mockConsoleIO).writeln("Dropped repository '" + PROXY_ID + "'");
		assertThat(manager.isSafeToRemove(MEMORY_MEMBER_ID1)).isTrue();
		drop.execute("drop", MEMORY_MEMBER_ID1);
		verify(mockConsoleIO).writeln("Dropped repository '" + MEMORY_MEMBER_ID1 + "'");
	}

	@Test
	public final void testUnsafeDropCancel() throws RepositoryException, IOException {
		setUserDropConfirm(true);
		assertThat(manager.isSafeToRemove(MEMORY_MEMBER_ID1)).isFalse();
		when(mockConsoleIO.askProceed(startsWith("WARNING: dropping this repository may break"), anyBoolean()))
				.thenReturn(false);
		drop.execute("drop", MEMORY_MEMBER_ID1);
		verify(mockConsoleIO).writeln("Drop aborted");
	}

	@Test
	public final void testUserAbortedUnsafeDropBeforeWarning() throws IOException {
		setUserDropConfirm(false);
		drop.execute("drop", MEMORY_MEMBER_ID1);
		verify(mockConsoleIO, never()).askProceed(startsWith("WARNING: dropping this repository may break"),
				anyBoolean());
		verify(mockConsoleIO).writeln("Drop aborted");
	}
}
