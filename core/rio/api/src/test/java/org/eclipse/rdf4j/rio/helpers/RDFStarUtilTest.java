/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.Test;

/**
 * @author Pavel Mihaylov
 */
public class RDFStarUtilTest {
	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	public void testEncoding() {
		IRI iri = vf.createIRI("urn:a");
		assertSame(iri, RDFStarUtil.toRDFEncodedValue(iri));
		assertFalse(RDFStarUtil.isEncodedTriple(iri));

		Literal literal1 = vf.createLiteral("plain");
		assertSame(literal1, RDFStarUtil.toRDFEncodedValue(literal1));
		assertFalse(RDFStarUtil.isEncodedTriple(literal1));

		Literal literal2 = vf.createLiteral(1984L);
		assertSame(literal2, RDFStarUtil.toRDFEncodedValue(literal2));
		assertFalse(RDFStarUtil.isEncodedTriple(literal2));

		Literal literal3 = vf.createLiteral("einfach aber auf deutsch", "de");
		assertSame(literal3, RDFStarUtil.toRDFEncodedValue(literal3));
		assertFalse(RDFStarUtil.isEncodedTriple(literal3));

		BNode bNode = vf.createBNode("bnode1");
		assertSame(bNode, RDFStarUtil.toRDFEncodedValue(bNode));
		assertFalse(RDFStarUtil.isEncodedTriple(bNode));

		Triple triple = vf.createTriple(iri, RDF.TYPE, literal1);
		assertEquals(vf.createIRI("urn:rdf4j:triple:PDw8dXJuOmE-IDxodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1ze"
				+ "W50YXgtbnMjdHlwZT4gInBsYWluIj4-"),
				RDFStarUtil.<Value>toRDFEncodedValue(triple));
		assertFalse(RDFStarUtil.isEncodedTriple(triple));
		assertTrue(RDFStarUtil.isEncodedTriple(RDFStarUtil.toRDFEncodedValue(triple)));
	}

	@Test
	public void testDecoding() {
		IRI iri = vf.createIRI("urn:a");
		assertSame(iri, RDFStarUtil.fromRDFEncodedValue(iri));

		Literal literal1 = vf.createLiteral("plain");
		assertSame(literal1, RDFStarUtil.fromRDFEncodedValue(literal1));
		assertFalse(RDFStarUtil.isEncodedTriple(literal1));

		Literal literal2 = vf.createLiteral(1984L);
		assertSame(literal2, RDFStarUtil.fromRDFEncodedValue(literal2));

		Literal literal3 = vf.createLiteral("einfach aber auf deutsch", "de");
		assertSame(literal3, RDFStarUtil.fromRDFEncodedValue(literal3));
		assertFalse(RDFStarUtil.isEncodedTriple(literal3));

		BNode bNode = vf.createBNode("bnode1");
		assertSame(bNode, RDFStarUtil.fromRDFEncodedValue(bNode));

		IRI encoded = vf.createIRI("urn:rdf4j:triple:PDw8dXJuOmE-IDxodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1ze"
				+ "W50YXgtbnMjdHlwZT4gInBsYWluIj4-");
		Value decoded = RDFStarUtil.fromRDFEncodedValue(encoded);
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
			assertTrue(RDFStarUtil.isEncodedTriple(invalidValue));
			try {
				RDFStarUtil.fromRDFEncodedValue(invalidValue);
				fail("Must fail because of invalid value");
			} catch (IllegalArgumentException e) {
				assertTrue(e.getMessage().startsWith("Invalid RDF-star encoded triple"));
			}
		}
	}
}
