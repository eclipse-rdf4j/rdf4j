/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LmdbTempDirCleanupExtensionTest {

	private static Path methodTempDir;
	private static Path fieldTempDir;

	@TempDir
	Path instanceTempDir;

	@Test
	void deletesTempDirMethodArguments(@TempDir Path tempDir) throws IOException {
		methodTempDir = tempDir;
		Files.writeString(tempDir.resolve("marker.txt"), "method temp dir");
		assertTrue(Files.exists(tempDir));
	}

	@Test
	void deletesTempDirFields() throws IOException {
		fieldTempDir = instanceTempDir;
		Files.writeString(instanceTempDir.resolve("marker.txt"), "field temp dir");
		assertTrue(Files.exists(instanceTempDir));
	}

	@AfterAll
	static void tempDirsWereDeletedExplicitly() {
		assertFalse(Files.exists(methodTempDir));
		assertFalse(Files.exists(fieldTempDir));
	}
}
