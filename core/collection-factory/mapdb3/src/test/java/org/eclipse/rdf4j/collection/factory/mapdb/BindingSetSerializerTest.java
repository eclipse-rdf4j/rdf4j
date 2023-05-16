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
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.junit.jupiter.api.Test;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;

public class BindingSetSerializerTest {
	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	void noBinding() throws IOException {
		BindingSet toSerialize = new MapBindingSet();
		final BindingSetSerializer valueSerializer = createSerializer();
		final BindingSet deserialized = serializeDeserialize(toSerialize, valueSerializer);
		assertEquals(toSerialize, deserialized);
	}

	@Test
	void oneBinding() throws IOException {
		MapBindingSet toSerialize = new MapBindingSet();
		toSerialize.setBinding("a", vf.createLiteral(1));
		final BindingSetSerializer valueSerializer = createSerializer();
		final BindingSet deserialized = serializeDeserialize(toSerialize, valueSerializer);
		assertEquals(toSerialize, deserialized);
	}

	@Test
	void twoBinding() throws IOException {
		MapBindingSet toSerialize = new MapBindingSet();
		toSerialize.setBinding("a", vf.createLiteral(1));
		toSerialize.setBinding("v", vf.createLiteral(1));
		final BindingSetSerializer valueSerializer = createSerializer();
		final BindingSet deserialized = serializeDeserialize(toSerialize, valueSerializer);
		assertEquals(toSerialize, deserialized);
	}

	@Test
	void secondBindingIri() throws IOException {
		final BindingSetSerializer valueSerializer = createSerializer();

		MapBindingSet toSerialize = new MapBindingSet();
		toSerialize.setBinding("a", vf.createLiteral(1));
		toSerialize.setBinding("v", vf.createIRI("http://example.org/lala"));
		BindingSet deserialized = serializeDeserialize(toSerialize, valueSerializer);
		assertEquals(toSerialize, deserialized);
	}

	@Test
	void multipleNumbersOfBindingsIri() throws IOException {
		final BindingSetSerializer valueSerializer = createSerializer();

		MapBindingSet toSerialize = new MapBindingSet();
		toSerialize.setBinding("a", vf.createLiteral(1));
		toSerialize.setBinding("v", vf.createIRI("http://example.org/lala"));
		BindingSet deserialized = serializeDeserialize(toSerialize, valueSerializer);
		assertEquals(toSerialize, deserialized);

		toSerialize = new MapBindingSet();
		toSerialize.setBinding("a", vf.createLiteral(2));
		deserialized = serializeDeserialize(toSerialize, valueSerializer);
		assertEquals(toSerialize, deserialized);

		toSerialize = new MapBindingSet();
		toSerialize.setBinding("a", vf.createLiteral(1));
		toSerialize.setBinding("b", vf.createLiteral(4));
		toSerialize.setBinding("c", vf.createLiteral(1));
		deserialized = serializeDeserialize(toSerialize, valueSerializer);
		assertEquals(toSerialize, deserialized);
	}

	private BindingSet serializeDeserialize(BindingSet oneInt, BindingSetSerializer valueSerializer)
			throws IOException {

		final DataOutput2 dataOutput2 = new DataOutput2();
		valueSerializer.serialize(dataOutput2, oneInt);

		final DataInput2 input = new DataInput2.ByteArray(dataOutput2.copyBytes());
		BindingSet doneInt = valueSerializer.deserialize(input, 0);
		return doneInt;
	}

	private BindingSetSerializer createSerializer() {
		Function<String, Predicate<BindingSet>> getHas = (n) -> {
			return (b) -> b.hasBinding(n);
		};
		Function<String, Function<BindingSet, Value>> getGet = (n) -> {
			return (b) -> b.getValue(n);
		};
		Function<String, BiConsumer<Value, MutableBindingSet>> getSet = (n) -> {
			return (v, b) -> b.setBinding(n, v);
		};
		final BindingSetSerializer valueSerializer = new BindingSetSerializer(new ValueSerializer(vf),
				MapBindingSet::new, getHas, getGet, getSet);
		return valueSerializer;
	}
}
