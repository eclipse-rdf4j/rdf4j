/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * @author jeen
 */
public class AppVersionTest {

	@Test
	public void testCreateFromString()
		throws Exception
	{
		AppVersion v = AppVersion.parse("1.0.3");

		assertEquals(1, v.getMajor());
		assertEquals(0, v.getMinor());
		assertEquals(3, v.getPatch());
		assertNull(v.getModifier());

		v = AppVersion.parse("2.8.0-beta3-SNAPSHOT");
		assertEquals(2, v.getMajor());
		assertEquals(8, v.getMinor());
		assertEquals(0, v.getPatch());
		assertEquals("beta3-SNAPSHOT", v.getModifier());

		v = AppVersion.parse("1.0M1");
		assertEquals(1, v.getMajor());
		assertEquals(0, v.getMinor());
		assertEquals(-1, v.getPatch());
		assertEquals(1, v.getMilestone());
		assertNull(v.getModifier());
	}

	@Test
	public void testCompare1()
		throws Exception
	{
		AppVersion v1 = AppVersion.parse("1.0M1");
		AppVersion v2 = AppVersion.parse("1.0");

		assertEquals(-1, v1.compareTo(v2));
	}

	@Test
	public void testCompare2()
		throws Exception
	{
		AppVersion v1 = AppVersion.parse("1.0M1-SNAPSHOT");
		AppVersion v2 = AppVersion.parse("1.0M1");

		assertEquals(-1, v1.compareTo(v2));
	}

}
