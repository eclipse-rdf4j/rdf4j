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
package org.eclipse.rdf4j.console.setting;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test console width
 *
 * @author Bart Hanssens
 */
public class ConsoleWidthTest extends AbstractSettingTest {
	@BeforeEach
	@Override
	public void setUp() {
		settings.put(ConsoleWidth.NAME, new ConsoleWidth());
		super.setUp();
	}

	@Test
	public void testShowWidth() {
		setParameters.execute("set", "width=42");

		setParameters.execute("set", "width");
		verify(mockConsoleIO).writeln("width: 42");

		verifyNoMoreInteractions(mockConsoleIO);
	}
}
