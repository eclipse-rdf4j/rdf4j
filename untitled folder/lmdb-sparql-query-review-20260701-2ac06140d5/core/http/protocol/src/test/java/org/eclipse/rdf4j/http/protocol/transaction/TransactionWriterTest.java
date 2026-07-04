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
package org.eclipse.rdf4j.http.protocol.transaction;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.StringWriter;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.xml.XMLWriter;
import org.eclipse.rdf4j.model.Literal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TransactionWriterTest {
	private static Stream<Arguments> serializedLiterals() {
		return Stream.of(
				arguments(literal("Hello World"),
						"<literal datatype='http://www.w3.org/2001/XMLSchema#string'>Hello World</literal>"),
				arguments(literal(42), "<literal datatype='http://www.w3.org/2001/XMLSchema#int'>42</literal>"),
				arguments(literal("with special <> char"),
						"<literal datatype='http://www.w3.org/2001/XMLSchema#string'>with special &lt;&gt; char</literal>"),
				arguments(literal("non valid char" + '\u0001'),
						"<literal datatype='http://www.w3.org/2001/XMLSchema#string' encoding='base64'>bm9uIHZhbGlkIGNoYXIB</literal>")
		);
	}

	@ParameterizedTest
	@MethodSource("serializedLiterals")
	public void testSerializeLiteral(Literal literal, String expectedString) throws Exception {
		TransactionWriter writer = new TransactionWriter();

		StringWriter sw = new StringWriter();
		XMLWriter xmlWriter = new XMLWriter(sw);
		writer.serialize(literal, xmlWriter);

		assertThat(sw.toString())
				.isEqualTo(expectedString);
	}
}
