/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.setting;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.console.command.SetParameters;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Abstract class for settings
 *
 * @author Bart Hanssens
 */
public abstract class AbstractSettingTest {
	@Mock
	protected ConsoleIO mockConsoleIO;

	@Mock
	protected ConsoleState mockConsoleState;

	SetParameters setParameters;
	Map<String, ConsoleSetting> settings = new HashMap<>();

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		setParameters = new SetParameters(mockConsoleIO, mockConsoleState, settings);
	}
}
