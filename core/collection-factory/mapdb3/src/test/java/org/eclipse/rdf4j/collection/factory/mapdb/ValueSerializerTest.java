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
import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;

public class ValueSerializerTest {
	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	void literalOneInt() throws IOException {
		Literal oneInt = vf.createLiteral(1);
		Value doneInt = serializeDeserialize(oneInt);
		assertEquals(oneInt, doneInt);
	}

	@Test
	void literalOneIntCoreDatatype() throws IOException {
		Literal oneInt = vf.createLiteral("1", CoreDatatype.XSD.INT);
		Value doneInt = serializeDeserialize(oneInt);
		assertEquals(oneInt, doneInt);
	}

	@Test
	void literalTwoInt() throws IOException {
		Literal twoInt = vf.createLiteral(2);
		Value dTwoInt = serializeDeserialize(twoInt);
		assertEquals(twoInt, dTwoInt);
	}

	@Test
	void literalTwoFloat() throws IOException {
		Literal toSerialize = vf.createLiteral(2.0f);
		Value deserialized = serializeDeserialize(toSerialize);
		assertEquals(toSerialize, deserialized);
	}

	@Test
	void literalTwoDouble() throws IOException {
		Literal toSerialize = vf.createLiteral(2.0d);
		Value deserialized = serializeDeserialize(toSerialize);
		assertEquals(toSerialize, deserialized);
	}

	@Test
	void literalTwoBigInteger() throws IOException {
		Literal toSerialize = vf.createLiteral(BigInteger.valueOf(20000000));
		Value deserialized = serializeDeserialize(toSerialize);
		assertEquals(toSerialize, deserialized);
	}

	@Test
	void literalTwoBigDecimal() throws IOException {
		Literal toSerialize = vf.createLiteral(BigDecimal.valueOf(20000030.3443));
		Value deserialized = serializeDeserialize(toSerialize);
		assertEquals(toSerialize, deserialized);
	}

	@Test
	void iriExampleOrg() throws IOException {
		Value toSerialize = vf.createIRI("https://example.org/lala");
		Value deserialized = serializeDeserialize(toSerialize);
		assertEquals(toSerialize, deserialized);
	}

	@Test
	void tripleOfExampleOrg() throws IOException {
		final IRI iri = vf.createIRI("https://example.org/lala");
		Value toSerialize = vf.createTriple(iri, iri, iri);
		Value deserialized = serializeDeserialize(toSerialize);
		assertEquals(toSerialize, deserialized);
	}

	@Test
	void bnode() throws IOException {
		Value toSerialize = vf.createBNode("111");
		Value deserialized = serializeDeserialize(toSerialize);
		assertEquals(toSerialize, deserialized);
	}

	@Test
	void newCoreDatatype() throws IOException {
		CoreDatatype n = new CoreDatatype() {

			@Override
			public IRI getIri() {
				return vf.createIRI("https://example.org/lala");
			}

		};
		Value toSerialize = vf.createLiteral("111", n);
		Value deserialized = serializeDeserialize(toSerialize);
		assertEquals(toSerialize, deserialized);
	}

	private Value serializeDeserialize(Value oneInt) throws IOException {
		final ValueSerializer valueSerializer = new ValueSerializer(vf);
		final DataOutput2 dataOutput2 = new DataOutput2();
		valueSerializer.serialize(dataOutput2, oneInt);

		final DataInput2 input = new DataInput2.ByteArray(dataOutput2.copyBytes());
		Value doneInt = valueSerializer.deserialize(input, 0);
		return doneInt;
	}
}
