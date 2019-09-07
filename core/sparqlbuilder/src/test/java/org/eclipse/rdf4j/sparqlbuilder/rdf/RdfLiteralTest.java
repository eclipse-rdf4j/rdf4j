/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sparqlbuilder.rdf;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfLiteral.StringLiteral;
import org.junit.Test;

public class RdfLiteralTest {

	@Test
	public void emptyStringLiteralIsNotPadded() {
		StringLiteral literal = new StringLiteral("");
		assertThat(literal.getQueryString()).isEqualTo("\"\"");
	}

	@Test
	public void simpleStringLiteralIsNotPadded() {
		StringLiteral literal = new StringLiteral("foo");
		assertThat(literal.getQueryString()).isEqualTo("\"foo\"");
	}
}
