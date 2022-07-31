/*******************************************************************************
 * Copyright (c) 2016, 2017 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.examples.model;

import java.io.IOException;

import org.eclipse.rdf4j.examples.model.vocabulary.EX;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

/**
 * RDF Tutorial example 12: Building a Model with named graphs
 *
 * In this example, we show how you can use the context mechanism to add statements separate named graphs within the
 * same Model.
 *
 * @author Jeen Broekstra
 */
public class Example12BuildModelWithNamedGraphs {

	public static void main(String[] args)
			throws IOException {
		// We'll use a ModelBuilder to create two named graphs, one containing data about
		// picasso, the other about Van Gogh.
		ModelBuilder builder = new ModelBuilder();
		builder.setNamespace("ex", "http://example.org/");

		// in named graph 1, we add info about Picasso
		builder.namedGraph("ex:namedGraph1")
				.subject("ex:Picasso")
				.add(RDF.TYPE, EX.ARTIST)
				.add(FOAF.FIRST_NAME, "Pablo");

		// in named graph2, we add info about Van Gogh.
		builder.namedGraph("ex:namedGraph2")
				.subject("ex:VanGogh")
				.add(RDF.TYPE, EX.ARTIST)
				.add(FOAF.FIRST_NAME, "Vincent");

		// We're done building, create our Model
		Model model = builder.build();

		// each named graph is stored as a separate context in our Model
		for (Resource context : model.contexts()) {
			System.out.println("Named graph " + context + " contains: ");

			// write _only_ the statements in the current named graph to the console, in N-Triples format
			Rio.write(model.filter(null, null, null, context), System.out, RDFFormat.NTRIPLES);
			System.out.println();
		}

	}
}
