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
import java.io.InputStream;

import org.eclipse.rdf4j.examples.model.vocabulary.EX;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

/**
 * RDF Tutorial example 11: Getting a single property value for a particular subject. In this example, we show how you
 * can get retrieve each artist's first name and print it.
 *
 * @author Jeen Broekstra
 */
public class Example11SinglePropertyValue {

	public static void main(String[] args)
			throws IOException {

		String filename = "example-data-artists.ttl";

		// read the file 'example-data-artists.ttl' as an InputStream.
		InputStream input = Example11SinglePropertyValue.class.getResourceAsStream("/" + filename);
		Model model = Rio.parse(input, "", RDFFormat.TURTLE);

		// iterate over all resources that are of type 'ex:Artist'
		for (Resource artist : model.filter(null, RDF.TYPE, EX.ARTIST).subjects()) {
			// get all RDF triples that denote values for the `foaf:firstName` property
			// of the current artist
			Model firstNameTriples = model.filter(artist, FOAF.FIRST_NAME, null);

			// Get the actual first name by just selecting any property value. If no value can be found,
			// set the first name to '(unknown)'.
			String firstName = Models.objectString(firstNameTriples).orElse("(unknown)");

			System.out.println(artist + " has first name '" + firstName + "'");
		}

	}
}
