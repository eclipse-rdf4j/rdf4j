/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sparqlbuilder.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SparqlBuilderUtilsTest {

	@Test
	public void getBracedStringReturnsBracedWithPadding() {
		assertThat(SparqlBuilderUtils.getBracedString("string")).isEqualTo("{ string }");
	}

	@Test
	public void getBracketedStringReturnsBracketedWithPadding() {
		assertThat(SparqlBuilderUtils.getBracketedString("string")).isEqualTo("[ string ]");
	}

	@Test
	public void getQuotedStringReturnsQuotedNoPadding() {
		assertThat(SparqlBuilderUtils.getQuotedString("string")).isEqualTo("\"string\"");
	}

	@Test
	public void getQuotedStringOnEmptyAddsNoPadding() {
		assertThat(SparqlBuilderUtils.getQuotedString("")).isEqualTo("\"\"");
	}

	@Test
	public void getLongQuotedStringReturnsTripleSingleQuotes() {
		assertThat(SparqlBuilderUtils.getLongQuotedString("string")).isEqualTo("'''string'''");
	}

}
