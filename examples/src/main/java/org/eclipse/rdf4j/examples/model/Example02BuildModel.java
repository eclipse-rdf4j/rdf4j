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

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;

/**
 * RDF Tutorial example 02: Building a simple RDF Model using the RDF4J ModelBuilder
 *
 * @author Jeen Broekstra
 */
public class Example02BuildModel {

	public static void main(String[] args) {

		// Create a new RDF model containing two statements by using a ModelBuilder
		ModelBuilder builder = new ModelBuilder();
		Model model = builder.setNamespace("ex", "http://example.org/")
				.subject("ex:Picasso")
				.add(RDF.TYPE, "ex:Artist") // Picasso is an Artist
				.add(FOAF.FIRST_NAME, "Pablo") // his first name is "Pablo"
				.build();

		// To see what's in our model, let's just print it to the screen
		for (Statement st : model) {
			System.out.println(st);
		}
	}
}
