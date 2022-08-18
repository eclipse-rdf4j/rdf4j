/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.sail.base.SailStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class MemoryOverflowToDiskTest {

	@Test
	@Timeout(5)
	public void testCleanerRemovesTempDirWhenMemoryOverflowModelGetsGCed() throws IOException, InterruptedException {
		File file = Files.createTempDirectory("model").toFile();

		Model model = createModel(file);
		assertTrue(file.exists());
		model = null;

		System.gc();
		while (file.exists()) {
			System.gc();
			Thread.sleep(10);
		}

		assertFalse(file.exists());

	}

	private Model createModel(File file) throws IOException {
		NativeStore.MemoryOverflowIntoNativeStore model = new NativeStore.MemoryOverflowIntoNativeStore();
		SailStore sailStore = model.createSailStore(file);
		assertNotNull(sailStore);
		assertNotNull(model);
		assertEquals(0, model.size());
		return model;
	}

}
