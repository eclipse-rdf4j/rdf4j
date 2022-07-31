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

import java.time.LocalDate;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelBuilder;

/**
 * RDF Tutorial example 03: Datatyped literals
 *
 * @author Jeen Broekstra
 */
public class Example03LiteralDatatypes {

	public static void main(String[] args) {

		// Create a new RDF model containing information about the painting "The Potato Eaters"
		ModelBuilder builder = new ModelBuilder();
		Model model = builder
				.setNamespace("ex", "http://example.org/")
				.subject("ex:PotatoEaters")
				// this painting was created on April 1, 1885
				.add("ex:creationDate", LocalDate.parse("1885-04-01"))
				// instead of a java.time value, you can directly create a date-typed literal as well
				// .add("ex:creationDate", literal("1885-04-01", XSD.DATE))

				// the painting shows 5 people
				.add("ex:peopleDepicted", 5)
				.build();

		// To see what's in our model, let's just print stuff to the screen
		for (Statement st : model) {
			// we want to see the object values of each property
			IRI property = st.getPredicate();
			Value value = st.getObject();
			if (value instanceof Literal) {
				Literal literal = (Literal) value;
				System.out.println("datatype: " + literal.getDatatype());

				// get the value of the literal directly as a Java primitive.
				if (property.getLocalName().equals("peopleDepicted")) {
					int peopleDepicted = literal.intValue();
					System.out.println(peopleDepicted + " people are depicted in this painting");
				} else if (property.getLocalName().equals("creationDate")) {
					LocalDate date = LocalDate.from(literal.temporalAccessorValue());
					System.out.println("The painting was created on " + date);
				}

				// you can also just get the lexical value (a string) without worrying about the datatype
				System.out.println("Lexical value: '" + literal.getLabel() + "'");
			}
		}
	}
}
