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
package org.eclipse.rdf4j.sparqlbuilder.core.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.var;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.junit.jupiter.api.Test;

class ConstructQueryFilterNotExistsTest {

	@Test
	void filterNotExistsGraphPatternIsWrappedInBraces() {
		Variable s = var("s");
		Variable p = var("p");
		Variable o = var("o");
		Iri sourceGraph = iri("http://example.com/source");
		Iri targetGraph = iri("http://example.com/target");

		ConstructQuery query = Queries.CONSTRUCT()
				.construct(GraphPatterns.tp(s, p, o))
				.where(GraphPatterns.and(s.has(p, o).from(sourceGraph))
						.filterNotExists(s.has(p, o).from(targetGraph)));

		assertThat(query.getQueryString())
				.contains("FILTER NOT EXISTS { GRAPH <http://example.com/target> { ?s ?p ?o . } }");
	}
}
