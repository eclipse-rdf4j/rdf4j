/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.inlined;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype.XSD;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

class StringsTest {

	@Test
	void testPackStringWithinMaxLength() {
		ValueFactory valueFactory = SimpleValueFactory.getInstance();
		Literal literal = valueFactory.createLiteral("test", XSD.STRING);
		long packed = Strings.packString(literal);

		// Assert that the packed value is not 0
		assertNotEquals(0L, packed, "Packed value should not be 0 for valid input.");
	}

	@Test
	void testPackStringExceedsMaxLength() {
		ValueFactory valueFactory = SimpleValueFactory.getInstance();
		// Create a string longer than MAX_LENGTH - 1, one byte is used to encode string length
		String longString = "a".repeat(Values.MAX_LENGTH);
		Literal literal = valueFactory.createLiteral(longString, XSD.STRING);
		long packed = Strings.packString(literal);

		// Assert that the packed value is 0
		assertEquals(0L, packed, "Packed value should be 0 for input exceeding max length.");
	}

	@Test
	void testUnpackString() {
		ValueFactory valueFactory = SimpleValueFactory.getInstance();
		Literal literal = valueFactory.createLiteral("test", XSD.STRING);
		long packed = Strings.packString(literal);

		Literal unpackedLiteral = Strings.unpackString(packed, valueFactory);

		// Assert that the unpacked value matches the original
		assertEquals(literal.getLabel(), unpackedLiteral.getLabel(), "Unpacked label should match original.");
	}
}