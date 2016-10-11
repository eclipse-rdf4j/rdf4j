/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.ProxyRepositoryConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Dale Visser
 */
public class DropTest extends AbstractCommandTest {

	private static final String MEMORY_MEMBER_ID1 = "alien";

	private static final String PROXY_ID = "proxyID";

	private Drop drop;

	@Rule
	public final TemporaryFolder LOCATION = new TemporaryFolder();

	@Before
	public void prepareManager()
		throws UnsupportedEncodingException, IOException, RDF4JException
	{
		manager = new LocalRepositoryManager(LOCATION.getRoot());
		manager.initialize();
		addRepositories(MEMORY_MEMBER_ID1);
		manager.addRepositoryConfig(
				new RepositoryConfig(PROXY_ID, new ProxyRepositoryConfig(MEMORY_MEMBER_ID1)));
		ConsoleState state = mock(ConsoleState.class);
		when(state.getManager()).thenReturn(manager);
		drop = new Drop(mockConsoleIO, state, new Close(mockConsoleIO, state),
				new LockRemover(mockConsoleIO));
	}

	private void setUserDropConfirm(boolean confirm)
		throws IOException
	{
		when(mockConsoleIO.askProceed(startsWith("WARNING: you are about to drop repository '"),
				anyBoolean())).thenReturn(confirm);
	}

	@After
	public void tearDown()
		throws RDF4JException
	{
		manager.shutDown();
	}

	@Test
	public final void testSafeDrop()
		throws RepositoryException, IOException
	{
		setUserDropConfirm(true);
		assertThat(manager.isSafeToRemove(PROXY_ID), is(equalTo(true)));
		drop.execute("drop", PROXY_ID);
		verify(mockConsoleIO).writeln("Dropped repository '" + PROXY_ID + "'");
		assertThat(manager.isSafeToRemove(MEMORY_MEMBER_ID1), is(equalTo(true)));
		drop.execute("drop", MEMORY_MEMBER_ID1);
		verify(mockConsoleIO).writeln("Dropped repository '" + MEMORY_MEMBER_ID1 + "'");
	}

	@Test
	public final void testUnsafeDropCancel()
		throws RepositoryException, IOException
	{
		setUserDropConfirm(true);
		assertThat(manager.isSafeToRemove(MEMORY_MEMBER_ID1), is(equalTo(false)));
		when(mockConsoleIO.askProceed(startsWith("WARNING: dropping this repository may break"),
				anyBoolean())).thenReturn(false);
		drop.execute("drop", MEMORY_MEMBER_ID1);
		verify(mockConsoleIO).writeln("Drop aborted");
	}

	@Test
	public final void testUserAbortedUnsafeDropBeforeWarning()
		throws IOException
	{
		setUserDropConfirm(false);
		drop.execute("drop", MEMORY_MEMBER_ID1);
		verify(mockConsoleIO, never()).askProceed(startsWith("WARNING: dropping this repository may break"),
				anyBoolean());
		verify(mockConsoleIO).writeln("Drop aborted");
	}
}
