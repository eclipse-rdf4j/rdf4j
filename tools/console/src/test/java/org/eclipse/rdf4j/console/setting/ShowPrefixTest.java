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
 * Test show prefix setting
 *
 * @author Bart Hanssens
 */
public class ShowPrefixTest extends AbstractSettingTest {
	@BeforeEach
	@Override
	public void setUp() {
		settings.put(ShowPrefix.NAME, new ShowPrefix());
		super.setUp();
	}

	@Test
	public void testShowQueryPrefix() {
		setParameters.execute("set", "showPrefix=true");

		setParameters.execute("set", "showPrefix");
		verify(mockConsoleIO).writeln("showPrefix: true");

		verifyNoMoreInteractions(mockConsoleIO);
	}
}
