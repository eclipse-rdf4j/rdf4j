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
package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import org.eclipse.rdf4j.sail.SailLockedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class NativeStoreDirLockTest {

	@TempDir
	File dataDir;

	@Test
	public void testLocking() {
		NativeStore sail = new NativeStore(dataDir, "spoc,posc");
		sail.init();

		try {
			NativeStore sail2 = new NativeStore(dataDir, "spoc,posc");
			sail2.init();
			try {
				fail("initialized a second native store with same dataDir");
			} finally {
				sail2.shutDown();
			}
		} catch (SailLockedException e) {
			// Expected: should not be able to open two native stores with the
			// same dataDir
			assertNotNull(e);
		} finally {
			sail.shutDown();
		}
	}
}
