/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.workbench.commands;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.junit.jupiter.api.Test;

/**
 * @author Dale Visser
 */
public class TestCreateServlet {

	private static final String[] EXPECTED_TEMPLATES = new String[] { "memory-customrule", "memory-rdfs-dt",
			"memory-rdfs", "memory", "native-customrule", "native-rdfs-dt", "native-rdfs", "native", "remote", "sparql",
			"memory-shacl", "native-shacl" };

	/**
	 * Regression test for SES-1907.
	 * 
	 * @throws IOException
	 */
	@Test
	public final void testExpectedTemplatesCanBeResolved() throws IOException {
		for (String template : EXPECTED_TEMPLATES) {
			System.out.println(template);
			String resource = template + ".ttl";
			try (InputStream resourceAsStream = RepositoryConfig.class.getResourceAsStream(resource)) {
				assertNotNull(resourceAsStream);
				try (BufferedInputStream bis = new BufferedInputStream(resourceAsStream)) {
					String createdTemplate = new String(bis.readAllBytes());
					assertTrue(createdTemplate.contains("rep:repositoryType"));
				}
			}
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
