/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.buildtools.license;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NewFileCopyrightCheckerTest {

	@TempDir
	Path tempDir;

	@Test
	void passesWhenAllHeadersMatch() throws Exception {
		String contents = String.join("\n",
				"/*******************************************************************************",
				" * Copyright (c) 2024 Eclipse RDF4J contributors, Aduna, and others.",
				" *",
				" * SPDX-License-Identifier: BSD-3-Clause",
				" ******************************************************************************/",
				"package example;",
				"");
		Path file = createFile("src/main/java/Test.java", contents);

		GitService git = () -> Map.of(tempDir.relativize(file), 2024);
		NewFileCopyrightChecker checker = new NewFileCopyrightChecker(tempDir, git, List.of("**/*.java"), List.of());

		assertDoesNotThrow(checker::check);
	}

	@Test
	void failsWhenHeaderMissing() throws Exception {
		Path file = createFile("src/main/java/TestMissing.java", "package example;\n");

		GitService git = () -> Map.of(tempDir.relativize(file), 2024);
		NewFileCopyrightChecker checker = new NewFileCopyrightChecker(tempDir, git, List.of("**/*.java"), List.of());

		assertThrows(CopyrightCheckException.class, checker::check);
	}

	@Test
	void failsWhenYearDoesNotMatch() throws Exception {
		String contents = String.join("\n",
				"/*******************************************************************************",
				" * Copyright (c) 2022 Eclipse RDF4J contributors, Aduna, and others.",
				" ******************************************************************************/",
				"package example;",
				"");
		Path file = createFile("src/main/java/TestWrongYear.java", contents);

		GitService git = () -> Map.of(tempDir.relativize(file), 2024);
		NewFileCopyrightChecker checker = new NewFileCopyrightChecker(tempDir, git, List.of("**/*.java"), List.of());

		assertThrows(CopyrightCheckException.class, checker::check);
	}

	private Path createFile(String relative, String contents) throws IOException {
		Path file = tempDir.resolve(relative);
		Files.createDirectories(file.getParent());
		Files.writeString(file, contents);
		return file;
	}
}
