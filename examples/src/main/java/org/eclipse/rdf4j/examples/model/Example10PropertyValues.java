/*******************************************************************************
 * Copyright (c) 2016, 2017 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.examples.model;

import org.eclipse.rdf4j.examples.model.vocabulary.EX;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * RDF Tutorial example 10: Getting all values of a property for a particular subject.
 *
 * In this example, we show how you can get retrieve all paintings that Van Gogh created.
 *
 * @author Jeen Broekstra
 */
public class Example10PropertyValues {

	public static void main(String[] args) throws IOException {

		String filename = "example-data-artists.ttl";

		// read the file 'example-data-artists.ttl' as an InputStream.
		InputStream input = Example10PropertyValues.class.getResourceAsStream("/" + filename);
		Model model = Rio.parse(input, "", RDFFormat.TURTLE);

		ValueFactory vf = SimpleValueFactory.getInstance();

		// We want to find all information about the artist `ex:VanGogh`.
		IRI vanGogh = vf.createIRI(EX.NAMESPACE, "VanGogh");

		// Retrieve all values of the `ex:creatorOf` property for Van Gogh. These will be
		// resources that identify paintings by Van Gogh.
		Set<Value> paintings = model.filter(vanGogh, EX.CREATOR_OF, null).objects();

		for (Value painting: paintings) {
			if (painting instanceof Resource) {
				// our value is either an IRI or a blank node. Retrieve its properties and print.
				Model paintingProperties = model.filter((Resource)painting, null, null);

				// write the info about this painting to the console in Turtle format
				System.out.println("--- information about painting: " + painting);
				Rio.write(paintingProperties, System.out, RDFFormat.TURTLE);
				System.out.println();
			}
		}

	}
}
