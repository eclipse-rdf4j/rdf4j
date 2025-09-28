/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CopilotWorkflowSetupTest {

	@Test
	void copilotSetupWorkflowShouldExist() {
		Path moduleRoot = Path.of("").toAbsolutePath();
		Path projectRoot = moduleRoot.getParent().getParent();
		Path workflow = projectRoot.resolve(".github/workflows/copilot-setup-steps.yml");

		assertThat(Files.exists(workflow))
				.as("Expected workflow file %s to exist", workflow)
				.isTrue();
	}
}
