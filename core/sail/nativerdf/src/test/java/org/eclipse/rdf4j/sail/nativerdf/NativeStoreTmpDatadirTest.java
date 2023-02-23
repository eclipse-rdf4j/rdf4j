/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors,.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class NativeStoreTmpDatadirTest {

	@TempDir
	File dataDir;

	@Test
	public void testNoTmpDatadir() {
		NativeStore store = new NativeStore(dataDir);

		store.init();
		assertTrue(dataDir.equals(store.getDataDir()), "Data dir not set correctly");

		store.shutDown();
		assertTrue(dataDir.exists(), "Data dir does not exist anymore");
	}

	@Test
	public void testTmpDatadir() {
		NativeStore store = new NativeStore();
		store.init();
		File dataDir = store.getDataDir();
		assertTrue(dataDir != null && dataDir.exists(), "Temp data dir not created");

		store.shutDown();
		assertFalse(dataDir.exists(), "Temp data dir still exists");
	}

	@Test
	public void testTmpDatadirReinit() {
		NativeStore store = new NativeStore();
		store.init();
		File dataDir1 = store.getDataDir();
		store.shutDown();

		store.init();
		File dataDir2 = store.getDataDir();
		store.shutDown();
		assertFalse(dataDir1.equals(dataDir2), "Temp data dirs are the same");
	}

	@Test
	public void testDatadirMix() {
		NativeStore store = new NativeStore(dataDir);

		store.init();
		store.shutDown();

		store.setDataDir(null);
		store.init();
		File tmpDataDir = store.getDataDir();
		store.shutDown();

		assertFalse(tmpDataDir.exists(), "Temp data dir still exists");
		assertTrue(dataDir.exists(), "Data dir does not exist anymore");
	}
}
