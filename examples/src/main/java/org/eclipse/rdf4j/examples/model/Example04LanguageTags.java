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

import static org.eclipse.rdf4j.model.util.Values.literal;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.DC;

/**
 * RDF Tutorial example 04: Language tags
 *
 * @author Jeen Broekstra
 */
public class Example04LanguageTags {

	public static void main(String[] args) {

		// Create a new RDF model containing some information about the painting "The Potato Eaters"
		ModelBuilder builder = new ModelBuilder();
		Model model = builder
				.setNamespace("ex", "http://example.org/")
				.subject("ex:PotatoEaters")
				// In English, this painting is called "The Potato Eaters"
				.add(DC.TITLE, literal("The Potato Eaters", "en"))
				// In Dutch, it's called "De Aardappeleters"
				.add(DC.TITLE, literal("De Aardappeleters", "nl"))
				.build();

		// To see what's in our model, let's just print it to the screen
		for (Statement st : model) {
			// we want to see the object values of each statement
			Value value = st.getObject();
			if (value instanceof Literal) {
				Literal title = (Literal) value;
				System.out.println("language: " + title.getLanguage().orElse("unknown"));
				System.out.println(" title: " + title.getLabel());
			}
		}
	}
}
