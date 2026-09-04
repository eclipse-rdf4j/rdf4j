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

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.lmdb.ValueIds;
import org.junit.jupiter.api.Test;

class BooleansTest {

	private final ValueFactory valueFactory = SimpleValueFactory.getInstance();

	@Test
	void testPackBooleanTrue() {
		Literal trueLiteral = valueFactory.createLiteral(true);
		long expectedId = ValueIds.createId(ValueIds.T_BOOLEAN, 1L);
		long actualId = Booleans.packBoolean(trueLiteral);

		assertEquals(expectedId, actualId, "Packing true literal should return the correct ID");
	}

	@Test
	void testPackBooleanFalse() {
		Literal falseLiteral = valueFactory.createLiteral(false);
		long expectedId = ValueIds.createId(ValueIds.T_BOOLEAN, 0L);
		long actualId = Booleans.packBoolean(falseLiteral);

		assertEquals(expectedId, actualId, "Packing false literal should return the correct ID");
	}

	@Test
	void testUnpackBooleanTrue() {
		long trueId = ValueIds.createId(ValueIds.T_BOOLEAN, 1L);
		Literal expectedLiteral = valueFactory.createLiteral(true);
		Literal actualLiteral = Booleans.unpackBoolean(trueId, valueFactory);

		assertEquals(expectedLiteral, actualLiteral, "Unpacking ID for true should return true literal");
	}

	@Test
	void testUnpackBooleanFalse() {
		long falseId = ValueIds.createId(ValueIds.T_BOOLEAN, 0L);
		Literal expectedLiteral = valueFactory.createLiteral(false);
		Literal actualLiteral = Booleans.unpackBoolean(falseId, valueFactory);

		assertEquals(expectedLiteral, actualLiteral, "Unpacking ID for false should return false literal");
	}
}
