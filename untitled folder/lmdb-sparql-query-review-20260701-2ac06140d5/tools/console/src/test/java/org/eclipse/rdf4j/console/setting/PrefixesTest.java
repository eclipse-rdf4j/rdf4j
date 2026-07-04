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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Test namespace prefixes setting
 *
 * @author Bart Hanssens
 */
public class PrefixesTest extends AbstractSettingTest {
	@BeforeEach
	@Override
	public void setUp() {
		settings.put(Prefixes.NAME, new Prefixes());
		super.setUp();
	}

	@Test
	public void testClearPrefixes() {
		setParameters.execute("set", "prefixes=<none>");

		setParameters.execute("set", "prefixes");
		verify(mockConsoleIO).writeln("prefixes: ");

		verifyNoMoreInteractions(mockConsoleIO);
	}

	@Test
	public void testDefaultPrefixes() {
		setParameters.execute("set", "prefixes=<none>");
		setParameters.execute("set", "prefixes=<default>");

		setParameters.execute("set", "prefixes");
		ArgumentCaptor<String> s = ArgumentCaptor.forClass(String.class);
		verify(mockConsoleIO).writeln(s.capture());
		assertTrue(s.getValue().contains(DCTERMS.NAMESPACE), "Does not contain dcterms");

		verifyNoMoreInteractions(mockConsoleIO);
	}

	@Test
	public void testNewPrefix() {
		setParameters.execute("set", "prefixes=<none>");
		setParameters.execute("set", "prefixes=" + DCTERMS.PREFIX + " " + DCTERMS.NAMESPACE);

		setParameters.execute("set", "prefixes");
		ArgumentCaptor<String> s = ArgumentCaptor.forClass(String.class);
		verify(mockConsoleIO).writeln(s.capture());
		assertTrue(s.getValue().contains(DCTERMS.NAMESPACE), "Does not contain dcterms");

		verifyNoMoreInteractions(mockConsoleIO);
	}
}
