/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.examples.model;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;

/**
 * RDF Tutorial example 01: Building a simple RDF Model using Eclipse RDF4J
 *
 * @author Jeen Broekstra
 */
public class Example01BuildModel {

	public static void main(String[] args) {
		// We want to reuse this namespace when creating several building blocks.
		String ex = "http://example.org/";

		// Create IRIs for the resources we want to add.
		IRI picasso = iri(ex, "Picasso");
		IRI artist = iri(ex, "Artist");

		// Create a new, empty Model object.
		Model model = new TreeModel();

		// add our first statement: Picasso is an Artist
		model.add(picasso, RDF.TYPE, artist);

		// second statement: Picasso's first name is "Pablo".
		model.add(picasso, FOAF.FIRST_NAME, literal("Pablo"));

		// to see what's in our model, let's just print it to the screen
		for (Statement st : model) {
			System.out.println(st);
		}
	}
}
