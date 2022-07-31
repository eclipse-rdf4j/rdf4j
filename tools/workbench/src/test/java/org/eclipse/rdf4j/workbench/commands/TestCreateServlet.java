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
package org.eclipse.rdf4j.workbench.commands;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.junit.Test;

/**
 * @author Dale Visser
 */
public class TestCreateServlet {

	private static final String[] EXPECTED_TEMPLATES = new String[] { "memory-customrule", "memory-rdfs-dt",
			"memory-rdfs", "memory",
			"native-customrule", "native-rdfs-dt", "native-rdfs", "native", "remote", "sparql", "memory-shacl",
			"native-shacl" };

	/**
	 * Regression test for SES-1907.
	 */
	@Test
	public final void testExpectedTemplatesCanBeResolved() {
		for (String template : EXPECTED_TEMPLATES) {
			System.out.println(template);
			String resource = template + ".ttl";
			assertThat(RepositoryConfig.class.getResourceAsStream(resource)).isNotNull().as(resource);
		}
	}

	@Test
	public final void testExpectedTemplatesCanBeLoaded() throws IOException {
		for (String template : EXPECTED_TEMPLATES) {
			System.out.println(template);
			CreateServlet.getConfigTemplate(template);
		}
	}

}
