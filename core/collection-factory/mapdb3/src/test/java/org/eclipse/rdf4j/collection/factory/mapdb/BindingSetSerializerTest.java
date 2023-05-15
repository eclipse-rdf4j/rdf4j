/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.collection.factory.mapdb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.junit.jupiter.api.Test;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;

public class BindingSetSerializerTest {
	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	void noBinding() throws IOException {
		BindingSet toSerialize = new MapBindingSet();
		final BindingSet deserialized = serializeDeserialize(toSerialize);
		assertEquals(toSerialize, deserialized);
	}

	@Test
	void oneBinding() throws IOException {
		MapBindingSet toSerialize = new MapBindingSet();
		toSerialize.setBinding("a", vf.createLiteral(1));
		final BindingSet deserialized = serializeDeserialize(toSerialize);
		assertEquals(toSerialize, deserialized);
	}

	@Test
	void twoBinding() throws IOException {
		MapBindingSet toSerialize = new MapBindingSet();
		toSerialize.setBinding("a", vf.createLiteral(1));
		toSerialize.setBinding("v", vf.createLiteral(1));
		final BindingSet deserialized = serializeDeserialize(toSerialize);
		assertEquals(toSerialize, deserialized);
	}

	@Test
	void secondBindingIri() throws IOException {
		MapBindingSet toSerialize = new MapBindingSet();
		toSerialize.setBinding("a", vf.createLiteral(1));
		toSerialize.setBinding("v", vf.createIRI("http://example.org/lala"));
		final BindingSet deserialized = serializeDeserialize(toSerialize);
		assertEquals(toSerialize, deserialized);
	}

	private BindingSet serializeDeserialize(BindingSet oneInt) throws IOException {
		final BindingSetSerializer valueSerializer = new BindingSetSerializer(new ValueSerializer(vf));
		final DataOutput2 dataOutput2 = new DataOutput2();
		valueSerializer.serialize(dataOutput2, oneInt);

		final DataInput2 input = new DataInput2.ByteArray(dataOutput2.copyBytes());
		BindingSet doneInt = valueSerializer.deserialize(input, 0);
		return doneInt;
	}
}
