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
package org.eclipse.rdf4j.common.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.rdf4j.RDF4J;
import org.junit.jupiter.api.Test;

/**
 * @author jeen
 */
public class AppVersionTest {

	@Test
	public void testCreateFromString() {
		AppVersion v = AppVersion.parse("1.0.3");

		assertEquals(1, v.getMajor());
		assertEquals(0, v.getMinor());
		assertEquals(3, v.getPatch());
		assertNull(v.getModifier());
	}

	@Test
	public void testCreateFromStringSnapshot() {
		AppVersion v;
		v = AppVersion.parse("2.8.0-beta3-SNAPSHOT");
		assertEquals(2, v.getMajor());
		assertEquals(8, v.getMinor());
		assertEquals(0, v.getPatch());
		assertEquals("beta3-SNAPSHOT", v.getModifier());

	}

	@Test
	public void testCreateFromStringMilestone() {
		AppVersion v;
		v = AppVersion.parse("1.0M1");
		assertEquals(1, v.getMajor());
		assertEquals(0, v.getMinor());
		assertEquals(-1, v.getPatch());
		assertEquals(1, v.getMilestone());
		assertNull(v.getModifier());
	}

	@Test
	public void testCreateFromStringBeta() {
		AppVersion v;
		v = AppVersion.parse("1.0.0-beta3");
		assertEquals(1, v.getMajor());
		assertEquals(0, v.getMinor());
		assertEquals(0, v.getPatch());
		assertEquals("beta3", v.getModifier());
	}

	@Test
	public void testCreateFromStringModifier() {
		AppVersion v;
		v = AppVersion.parse("1.0.0-M1");
		assertEquals(1, v.getMajor());
		assertEquals(0, v.getMinor());
		assertEquals(0, v.getPatch());
		assertEquals("M1", v.getModifier());
	}

	@Test
	public void testCreateFromStringModifierMilestone() {
		AppVersion v;
		v = AppVersion.parse("1.0.0-GAMMA");
		assertEquals(1, v.getMajor());
		assertEquals(0, v.getMinor());
		assertEquals(0, v.getPatch());
		assertEquals("GAMMA", v.getModifier());
	}

	@Test
	public void testCurrentVersion() {
		AppVersion v;
		v = AppVersion.parse(RDF4J.getVersion());
		assertEquals(RDF4J.getVersion(), v.toString());
	}

	@Test
	public void testCompare1() {
		AppVersion v1 = AppVersion.parse("1.0M1");
		AppVersion v2 = AppVersion.parse("1.0");

		assertEquals(-1, v1.compareTo(v2));
	}

	@Test
	public void testCompare2() {
		AppVersion v1 = AppVersion.parse("1.0M1-SNAPSHOT");
		AppVersion v2 = AppVersion.parse("1.0M1");

		assertEquals(-1, v1.compareTo(v2));
	}

}
