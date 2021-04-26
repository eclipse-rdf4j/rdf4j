/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.queryrender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.Values.literal;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;

public class RenderUtilsTest {

	@Test
	public void tosPARQL_StringTypedLiteralRendersPlain() {
		Value val = literal("test", XSD.STRING);

		assertThat(RenderUtils.toSPARQL(val)).isEqualTo("\"test\"");
	}

	@Test
	public void toSPARQl_LiteralSerialisesLanguageTag() {
		Value val = literal("test", "en");

		assertThat(RenderUtils.toSPARQL(val)).isEqualTo("\"test\"@en");
	}

	@Test
	public void tosPARQL_LiteralWithNewlines() {
		Value val = literal("literal with\nnew lines\nin it");

		assertThat(RenderUtils.toSPARQL(val)).isEqualTo("\"\"\"literal with\nnew lines\nin it\"\"\"");
	}

	@Test
	public void tosPARQL_LiteralWithEscapedNewlines() {
		Value val = literal("literal with\\nnew lines\\nin it");

		assertThat(RenderUtils.toSPARQL(val)).isEqualTo("\"literal with\\nnew lines\\nin it\"");
	}

}
