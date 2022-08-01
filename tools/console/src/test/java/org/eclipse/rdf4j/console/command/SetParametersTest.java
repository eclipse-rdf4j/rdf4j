/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.console.setting.ConsoleSetting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test setting parameters
 *
 * @author Bart Hanssens
 */
public class SetParametersTest extends AbstractCommandTest {
	SetParameters setParameters;

	@BeforeEach
	public void setUp() {
		Map<String, ConsoleSetting> settings = new HashMap<>();
		setParameters = new SetParameters(mockConsoleIO, mockConsoleState, settings);
	}

	@Test
	public void testUnknownParametersAreErrors() {
		setParameters.execute("set", "unknown");

		verify(mockConsoleIO).writeError("Unknown parameter: unknown");
		verifyNoMoreInteractions(mockConsoleIO);
	}
}
