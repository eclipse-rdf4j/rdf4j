/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.TripleTerm;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

class LmdbBackupDeltaCodecTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	void roundTripsNestedTripleTermsAndStatementKinds() throws IOException {
		Statement addition1 = vf.createStatement(vf.createIRI("urn:add:s1"), vf.createIRI("urn:add:p1"),
				vf.createLiteral("plain"));
		TripleTerm nested = vf.createTripleTerm(vf.createBNode("outer"), vf.createIRI("urn:mid:p"),
				vf.createTripleTerm(vf.createIRI("urn:inner:s"), vf.createIRI("urn:inner:p"),
						vf.createLiteral("nested", vf.createIRI("urn:datatype"))));
		Statement addition2 = vf.createStatement(vf.createIRI("urn:add:s2"), vf.createIRI("urn:add:p2"), nested);
		Statement removal = vf.createStatement(vf.createBNode("remove"), vf.createIRI("urn:remove:p"),
				vf.createLiteral("bonjour", "fr"), vf.createIRI("urn:ctx"));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		LmdbBackupDeltaCodec.write(out, List.of(addition1, addition2), List.of(removal));

		List<LmdbBackupDeltaCodec.Record> records = LmdbBackupDeltaCodec
				.read(new ByteArrayInputStream(out.toByteArray()));

		assertEquals(3, records.size());
		assertTrue(records.get(0).isAddition());
		assertTrue(records.get(1).isAddition());
		assertFalse(records.get(2).isAddition());
		assertIterableEquals(List.of(addition1, addition2, removal),
				records.stream().map(LmdbBackupDeltaCodec.Record::getStatement).toList());
	}

	@Test
	void rejectsUnknownHeader() {
		assertThrows(IOException.class,
				() -> LmdbBackupDeltaCodec.read(new ByteArrayInputStream(new byte[] { 0, 0, 0, 1, 7 })));
	}
}
