/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.workbench.commands;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.junit.Test;

/**
 * @author Dale Visser
 */
public class TestCreateServlet {

	/**
	 * Regression test for SES-1907.
	 */
	@Test
	public final void testExpectedTemplatesCanBeResolved() {
		String[] expectedTemplates = {
				"memory-customrule",
				"memory-rdfs-dt",
				"memory-rdfs",
				"memory",
				"native-customrule",
				"native-rdfs-dt",
				"native-rdfs",
				"native",
				"remote",
				"sparql" };
		for (String template : expectedTemplates) {
			String resource = template + ".ttl";
			assertThat(resource, RepositoryConfig.class.getResourceAsStream(resource), is(notNullValue()));
		}
	}
}
