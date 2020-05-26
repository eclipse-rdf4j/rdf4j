/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.setting;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Before;
import org.junit.Test;

/**
 * Test query prefix setting
 *
 * @author Bart Hanssens
 */
public class QueryPrefixTest extends AbstractSettingTest {
	@Before
	@Override
	public void setUp() {
		settings.put(QueryPrefix.NAME, new QueryPrefix());
		super.setUp();
	}

	@Test
	public void testQueryPrefix() {
		setParameters.execute("set", "queryPrefix=false");

		setParameters.execute("set", "queryPrefix");
		verify(mockConsoleIO).writeln("queryPrefix: false");

		verifyNoMoreInteractions(mockConsoleIO);
	}
}
