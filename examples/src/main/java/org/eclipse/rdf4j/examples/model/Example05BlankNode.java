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

import static org.eclipse.rdf4j.model.util.Values.bnode;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;

/**
 * RDF Tutorial example 05: Adding blank nodes to an RDF Model.
 *
 * In this example, we show how you can use a blank node for representing composite objects - in this case, an address.
 *
 * @author Jeen Broekstra
 */
public class Example05BlankNode {

	public static void main(String[] args) {

		// Create a bnode for the address
		BNode address = bnode();

		// First we do the same thing we did in example 02: create a new ModelBuilder, and add
		// two statements about Picasso.
		ModelBuilder builder = new ModelBuilder();
		builder
				.setNamespace("ex", "http://example.org/")
				.subject("ex:Picasso")
				.add(RDF.TYPE, "ex:Artist")
				.add(FOAF.FIRST_NAME, "Pablo")
				// this is where it becomes new: we add the address by linking the blank node
				// to picasso via the `ex:homeAddress` property, and then adding facts _about_ the address
				.add("ex:homeAddress", address) // link the blank node
				.subject(address) // switch the subject
				.add("ex:street", "31 Art Gallery")
				.add("ex:city", "Madrid")
				.add("ex:country", "Spain");

		Model model = builder.build();

		// To see what's in our model, let's just print it to the screen
		for (Statement st : model) {
			System.out.println(st);
		}
	}
}
