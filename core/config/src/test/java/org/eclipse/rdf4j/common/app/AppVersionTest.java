/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.app;

import static org.junit.Assert.*;

import org.eclipse.rdf4j.common.app.AppVersion;
import org.junit.Test;


/**
 *
 * @author jeen
 */
public class AppVersionTest {

	@Test
	public void testCreateFromString() throws Exception {
		AppVersion v = AppVersion.parse("1.0.3");
		
		assertEquals(1, v.getMajor());
		assertEquals(0, v.getMinor());
		assertEquals(3, v.getMicro());
		assertNull(v.getModifier());
		
		v = AppVersion.parse("2.8.0-beta3-SNAPSHOT");
		assertEquals(2, v.getMajor());
		assertEquals(8, v.getMinor());
		assertEquals(0, v.getMicro());
		assertEquals("beta3-SNAPSHOT", v.getModifier());
	}

}
