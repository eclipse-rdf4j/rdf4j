/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.ntriples;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link NTriplesUtil}
 * 
 * @author Jeen Broekstra
 *
 */
public class NTriplesUtilTest {

	private StringBuilder appendable;
	private ValueFactory f = SimpleValueFactory.getInstance();

	@Before
	public void setUp() throws Exception {
		appendable = new StringBuilder();
	}

	@Test
	public void testAppendWithoutEncoding() throws Exception {
		Literal l = f.createLiteral("Äbc");
		NTriplesUtil.append(l, appendable, true, false);
		assertThat(appendable.toString()).isEqualTo("\"Äbc\"");
	}

	@Test
	public void testAppendWithEncoding() throws Exception {
		Literal l = f.createLiteral("Äbc");
		NTriplesUtil.append(l, appendable, true, true);
		assertThat(appendable.toString()).isEqualTo("\"\\u00C4bc\"");
	}
}
