/*******************************************************************************
 * Copyright (c) 2016, 2017 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.examples.model;

import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

/**
 * RDF Tutorial example 03: Datatyped literals
 *
 * @author Jeen Broekstra
 */
public class Example03LiteralDatatypes {

	public static void main(String[] args) {

		ValueFactory vf = SimpleValueFactory.getInstance();
		
		// Create a new RDF model containing information about the painting "The Potato Eaters"
		ModelBuilder builder = new ModelBuilder();
		Model model = builder
				.setNamespace("ex", "http://example.org/")
				.subject("ex:PotatoEaters")
					// this painting was created on April 1, 1885
					.add("ex:creationDate", vf.createLiteral("1885-04-01T00:00:00Z", XMLSchema.DATETIME))
					// You can also pass in a Java Date object directly: 
				    //.add("ex:creationDate", new GregorianCalendar(1885, Calendar.APRIL, 1).getTime())
					
					// the painting shows 5 people
					.add("ex:peopleDepicted", 5)
				.build();

		// To see what's in our model, let's just print stuff to the screen
		for(Statement st: model) {
			// we want to see the object values of each property
			IRI property = st.getPredicate();
			Value value = st.getObject();
			if (value instanceof Literal) {
				Literal literal = (Literal)value;
				System.out.println("datatype: " + literal.getDatatype());
				
				// get the value of the literal directly as a Java primitive.
				if (property.getLocalName().equals("peopleDepicted")) {
					int peopleDepicted = literal.intValue();
					System.out.println(peopleDepicted + " people are depicted in this painting");
				}
				else if (property.getLocalName().equals("creationDate")) {
					XMLGregorianCalendar calendar = literal.calendarValue();
					Date date = calendar.toGregorianCalendar().getTime();
					System.out.println("The painting was created on " + date);
				}
				
				// you can also just get the lexical value (a string) without worrying about the datatype
				System.out.println("Lexical value: '" + literal.getLabel() + "'");
			}
		}
	}
}
