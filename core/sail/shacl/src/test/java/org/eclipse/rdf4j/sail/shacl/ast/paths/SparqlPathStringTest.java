/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.Test;

public class SparqlPathStringTest {

	public static final IRI PREDICATE = Values.iri("http://example.com/predicate");
	public static final IRI PREDICATE1 = Values.iri("http://example.com/predicate1");
	public static final IRI PREDICATE2 = Values.iri("http://example.com/predicate2");
	public static final IRI PREDICATE3 = Values.iri("http://example.com/predicate3");

	@Test
	public void simplePath() {
		Path path = new SimplePath(PREDICATE);
		assertEquals("<http://example.com/predicate>", path.toSparqlPathString());
	}

	@Test
	public void inversePath() {
		Path path = new InversePath(null, new SimplePath(PREDICATE));
		assertEquals("^<http://example.com/predicate>", path.toSparqlPathString());
	}

	@Test
	public void oneOrMorePath() {
		Path path = new OneOrMorePath(null, new SimplePath(PREDICATE));
		assertEquals("<http://example.com/predicate>+", path.toSparqlPathString());
	}

	@Test
	public void zeroOrMore() {
		Path path = new ZeroOrMorePath(null, new SimplePath(PREDICATE));
		assertEquals("<http://example.com/predicate>*", path.toSparqlPathString());
	}

	@Test
	public void zeroOrOne() {
		Path path = new ZeroOrOnePath(null, new SimplePath(PREDICATE));
		assertEquals("<http://example.com/predicate>?", path.toSparqlPathString());
	}

	@Test
	public void sequencePath() {
		Path path = new SequencePath(null,
				List.of(new SimplePath(PREDICATE1), new SimplePath(PREDICATE2), new SimplePath(PREDICATE3)));
		assertEquals(
				"(<http://example.com/predicate1> / <http://example.com/predicate2> / <http://example.com/predicate3>)",
				path.toSparqlPathString());
	}

	@Test
	public void alternativePath() {
		Path path = new AlternativePath(null, null,
				List.of(new SimplePath(PREDICATE1), new SimplePath(PREDICATE2), new SimplePath(PREDICATE3)));
		assertEquals(
				"(<http://example.com/predicate1> | <http://example.com/predicate2> | <http://example.com/predicate3>)",
				path.toSparqlPathString());
	}

	@Test
	public void complexPath() {
		Path path = new AlternativePath(null, null,
				List.of(new SimplePath(PREDICATE1), new SimplePath(PREDICATE2), new SimplePath(PREDICATE3)));
		path = new SequencePath(null,
				List.of(path, new SimplePath(PREDICATE1), new InversePath(null, path), new SimplePath(PREDICATE3)));
		path = new OneOrMorePath(null, path);
		path = new AlternativePath(null, null, List.of(new SimplePath(PREDICATE1), new ZeroOrMorePath(null, path),
				new SimplePath(PREDICATE2), new SimplePath(PREDICATE3), path));
		path = new ZeroOrOnePath(null, new InversePath(null, path));

		assertEquals(
				"(^(<http://example.com/predicate1> | (((<http://example.com/predicate1> | <http://example.com/predicate2> | <http://example.com/predicate3>) / <http://example.com/predicate1> / ^(<http://example.com/predicate1> | <http://example.com/predicate2> | <http://example.com/predicate3>) / <http://example.com/predicate3>)+)* | <http://example.com/predicate2> | <http://example.com/predicate3> | ((<http://example.com/predicate1> | <http://example.com/predicate2> | <http://example.com/predicate3>) / <http://example.com/predicate1> / ^(<http://example.com/predicate1> | <http://example.com/predicate2> | <http://example.com/predicate3>) / <http://example.com/predicate3>)+))?",
				path.toSparqlPathString());
	}

}
