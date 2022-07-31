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

import static org.eclipse.rdf4j.model.util.Values.iri;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

/**
 * RDF Tutorial example 09: Reading a Turtle syntax file to create a Model
 *
 * In this example, we show how you can use the Rio Parser/writer toolkit to read files
 *
 * @author Jeen Broekstra
 */
public class Example09Filter {

	public static void main(String[] args) throws IOException {

		String filename = "example-data-artists.ttl";

		// read the file 'example-data-artists.ttl' as an InputStream.
		InputStream input = Example09Filter.class.getResourceAsStream("/" + filename);

		// Rio also accepts a java.io.Reader as input for the parser.
		Model model = Rio.parse(input, "", RDFFormat.TURTLE);

		// We want to find all information about the artist `ex:VanGogh`.
		IRI vanGogh = iri("http://example.org/VanGogh");

		// By filtering on a specific subject we zoom in on the data that is about that subject.
		// The filter method takes a subject, predicate, object (and optionally a named graph/context)
		// argument. The more arguments we set to a value, the more specific the filter becomes.
		Model aboutVanGogh = model.filter(vanGogh, null, null);

		// Iterate over the statements that are about Van Gogh
		for (Statement st : aboutVanGogh) {
			// the subject will always be `ex:VanGogh`, an IRI, so we can safely cast it
			IRI subject = (IRI) st.getSubject();
			// the property predicate can be anything, but it's always an IRI
			IRI predicate = st.getPredicate();

			// the property value could be an IRI, a BNode, a Literal, or an RDF-star Triple. In RDF4J, Value is
			// is the supertype of all possible kinds of RDF values.
			Value object = st.getObject();

			// let's print out the statement in a nice way. We ignore the namespaces and only print the
			// local name of each IRI
			System.out.print(subject.getLocalName() + " " + predicate.getLocalName() + " ");
			if (object.isLiteral()) {
				// it's a literal value. Let's print it out nicely, in quotes, and without any ugly
				// datatype stuff
				System.out.println("\"" + ((Literal) object).getLabel() + "\"");
			} else if (object.isIRI()) {
				// it's an IRI. Just print out the local part (without the namespace)
				System.out.println(((IRI) object).getLocalName());
			} else {
				// it's a blank node or an RDF-star Triple. Just print it out as-is.
				System.out.println(object);
			}
		}
	}
}
