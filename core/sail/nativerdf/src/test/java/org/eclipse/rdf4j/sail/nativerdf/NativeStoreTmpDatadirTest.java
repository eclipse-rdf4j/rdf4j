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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class NativeStoreTmpDatadirTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testNoTmpDatadir() throws IOException {
		File dataDir = tempFolder.newFolder();
		NativeStore store = new NativeStore(dataDir);

		store.init();
		assertTrue("Data dir not set correctly", dataDir.equals(store.getDataDir()));

		store.shutDown();
		assertTrue("Data dir does not exist anymore", dataDir.exists());
	}

	@Test
	public void testTmpDatadir() throws IOException {
		NativeStore store = new NativeStore();
		store.init();
		File dataDir = store.getDataDir();
		assertTrue("Temp data dir not created", dataDir != null && dataDir.exists());

		store.shutDown();
		assertFalse("Temp data dir still exists", dataDir.exists());
	}

	@Test
	public void testTmpDatadirReinit() throws IOException {
		NativeStore store = new NativeStore();
		store.init();
		File dataDir1 = store.getDataDir();
		store.shutDown();

		store.init();
		File dataDir2 = store.getDataDir();
		store.shutDown();
		assertFalse("Temp data dirs are the same", dataDir1.equals(dataDir2));
	}

	@Test
	public void testDatadirMix() throws IOException {
		File dataDir = tempFolder.newFolder();
		NativeStore store = new NativeStore(dataDir);

		store.init();
		store.shutDown();

		store.setDataDir(null);
		store.init();
		File tmpDataDir = store.getDataDir();
		store.shutDown();

		assertFalse("Temp data dir still exists", tmpDataDir.exists());
		assertTrue("Data dir does not exist anymore", dataDir.exists());
	}
}
