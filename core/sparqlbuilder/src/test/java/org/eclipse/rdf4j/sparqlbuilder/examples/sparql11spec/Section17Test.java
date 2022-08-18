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
package org.eclipse.rdf4j.sparqlbuilder.examples.sparql11spec;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.Assert;
import org.junit.Test;

public class Section17Test extends BaseExamples {
	@Test
	public void example_17_4_1_9() {
		Prefix rdf = SparqlBuilder.prefix("rdf", Rdf.iri("http://example.com"));
		Variable attributeIRI = SparqlBuilder.var("attribute_iri");
		Iri type = rdf.iri("type");
		Expression in = Expressions.in(attributeIRI, type);
		Assert.assertThat(in.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"?attribute_iri IN ( rdf:type )"
		));
	}

	@Test
	public void example_17_4_1_10() {
		Prefix rdf = SparqlBuilder.prefix("rdf", Rdf.iri("http://example.com"));
		Variable attributeIRI = SparqlBuilder.var("attribute_iri");
		Iri type = rdf.iri("type");
		Expression notIn = Expressions.notIn(attributeIRI, type);
		Assert.assertThat(notIn.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"?attribute_iri NOT IN ( rdf:type )"
		));
	}
}
