/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class SetParametersTest {

	@Rule
	public MockitoRule rule = MockitoJUnit.rule();

	@Mock
	ConsoleIO consoleIo;

	@InjectMocks
	SetParameters setParameters;

	@After
	public void setLogLevelBackToDebug() {
		((Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.DEBUG);
	}

	@Test
	public void unknownParametersAreErrors() {
		setParameters.execute("set", "unknown");

		verify(consoleIo).writeError("unknown parameter: unknown");
		verifyNoMoreInteractions(consoleIo);
	}

	@Test
	public void noValueShowsCurrentLevel() {
		Logger logger = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.setLevel(Level.INFO);

		setParameters.execute("set", "log");

		verify(consoleIo).writeln("log: info");
		verifyNoMoreInteractions(consoleIo);
	}

	@Test
	public void settingLogChangesLevel() {
		Logger logger = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.setLevel(Level.DEBUG);

		setParameters.execute("set", "log=warning");

		assertEquals(Level.WARN, logger.getLevel());
	}

	@Test
	public void settingUnknownLevelIsLoggedAsError() {
		setParameters.execute("set", "log=chatty");

		verify(consoleIo).writeError("unknown logging level: chatty");
		verifyNoMoreInteractions(consoleIo);
	}

	@Test
	public void levelsThatDoNotMatchSlf4jAreMapped() {
		Logger logger = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.setLevel(Level.WARN);

		setParameters.execute("set", "log");

		verify(consoleIo).writeln("log: warning");
		verifyNoMoreInteractions(consoleIo);
	}

}
