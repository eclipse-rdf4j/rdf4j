/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.examples.model;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.IOException;
import java.io.InputStream;

/**
 * RDF Tutorial example 08: Reading a Turtle syntax file to create a Model
 *
 * In this example, we show how you can use the Rio Parser/writer toolkit to read files
 *
 * @author Jeen Broekstra
 * @see
 */
public class Example08ReadTurtle {

	public static void main(String[] args) throws IOException {

		String filename = "example-data-artists.ttl";

		// read the file 'example-data-artists.ttl' as an InputStream.
		InputStream input = Example08ReadTurtle.class.getResourceAsStream("/" + filename);

		// Rio also accepts a java.io.Reader as input for the parser.
		Model model = Rio.parse(input, "", RDFFormat.TURTLE);

		// To check that we have correctly read the file, let's print out the model to the screen again
		for (Statement statement: model) {
			System.out.println(statement);
		}
	}
}
