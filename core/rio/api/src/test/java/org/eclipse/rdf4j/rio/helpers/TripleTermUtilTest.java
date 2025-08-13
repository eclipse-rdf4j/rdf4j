/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

/**
 * @author Pavel Mihaylov
 */
public class TripleTermUtilTest {
	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	public void testEncoding() {
		IRI iri = vf.createIRI("urn:a");
		assertSame(iri, TripleTermUtil.toRDFEncodedValue(iri));
		assertFalse(TripleTermUtil.isEncodedTriple(iri));

		Literal literal1 = vf.createLiteral("plain");
		assertSame(literal1, TripleTermUtil.toRDFEncodedValue(literal1));
		assertFalse(TripleTermUtil.isEncodedTriple(literal1));

		Literal literal2 = vf.createLiteral(1984L);
		assertSame(literal2, TripleTermUtil.toRDFEncodedValue(literal2));
		assertFalse(TripleTermUtil.isEncodedTriple(literal2));

		Literal literal3 = vf.createLiteral("einfach aber auf deutsch", "de");
		assertSame(literal3, TripleTermUtil.toRDFEncodedValue(literal3));
		assertFalse(TripleTermUtil.isEncodedTriple(literal3));

		BNode bNode = vf.createBNode("bnode1");
		assertSame(bNode, TripleTermUtil.toRDFEncodedValue(bNode));
		assertFalse(TripleTermUtil.isEncodedTriple(bNode));

		Triple triple = vf.createTriple(iri, RDF.TYPE, literal1);
		assertEquals(vf.createIRI(
				"urn:rdf4j:triple:PDwoIDx1cm46YT4gPGh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyN0eXBlPiAicGxhaW4iICk-Pg=="),
				TripleTermUtil.<Value>toRDFEncodedValue(triple));
		assertFalse(TripleTermUtil.isEncodedTriple(triple));
		assertTrue(TripleTermUtil.isEncodedTriple(TripleTermUtil.toRDFEncodedValue(triple)));
	}

	@Test
	public void testDecoding() {
		IRI iri = vf.createIRI("urn:a");
		assertSame(iri, TripleTermUtil.fromRDFEncodedValue(iri));

		Literal literal1 = vf.createLiteral("plain");
		assertSame(literal1, TripleTermUtil.fromRDFEncodedValue(literal1));
		assertFalse(TripleTermUtil.isEncodedTriple(literal1));

		Literal literal2 = vf.createLiteral(1984L);
		assertSame(literal2, TripleTermUtil.fromRDFEncodedValue(literal2));

		Literal literal3 = vf.createLiteral("einfach aber auf deutsch", "de");
		assertSame(literal3, TripleTermUtil.fromRDFEncodedValue(literal3));
		assertFalse(TripleTermUtil.isEncodedTriple(literal3));

		BNode bNode = vf.createBNode("bnode1");
		assertSame(bNode, TripleTermUtil.fromRDFEncodedValue(bNode));

		Value encoded = TripleTermUtil.toRDFEncodedValue(vf.createTriple(iri, RDF.TYPE, literal1));
		Value decoded = TripleTermUtil.fromRDFEncodedValue(encoded);
		assertTrue(decoded instanceof Triple);
		assertEquals(iri, ((Triple) decoded).getSubject());
		assertEquals(RDF.TYPE, ((Triple) decoded).getPredicate());
		assertEquals(literal1, ((Triple) decoded).getObject());
	}

	@Test
	public void testInvalidEncodedValue() {
		IRI[] invalidValues = {
				vf.createIRI("urn:rdf4j:triple:"),
				vf.createIRI("urn:rdf4j:triple:foo"),
				vf.createIRI("urn:rdf4j:triple:кирилица"),
				vf.createIRI("urn:rdf4j:triple:PDw8dXJuOmE-"),
				// Missing final -
				vf.createIRI("urn:rdf4j:triple:PDw8dXJuOmE-IDxodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1ze"
						+ "W50YXgtbnMjdHlwZT4gInBsYWluIj4"),
				// Extra x at the end
				vf.createIRI("urn:rdf4j:triple:PDw8dXJuOmE-IDxodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1ze"
						+ "W50YXgtbnMjdHlwZT4gInBsYWluIj4-x"),
		};

		for (IRI invalidValue : invalidValues) {
			assertTrue(TripleTermUtil.isEncodedTriple(invalidValue));
			try {
				TripleTermUtil.fromRDFEncodedValue(invalidValue);
				fail("Must fail because of invalid value");
			} catch (IllegalArgumentException e) {
				assertTrue(e.getMessage().startsWith("Invalid RDF 1.2 encoded triple"));
			}
		}
	}
}
