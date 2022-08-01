/**
 * Copyright (c) 2017 Eclipse RDF4J contributors, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.model.util;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;

/**
 * Utility functions for working with vocabularies.
 *
 * @author Bart Hanssens
 */
public class Vocabularies {
	/**
	 * Get all the {@link IRI IRIs} of the classes and properties of a vocabulary.
	 *
	 * @param vocabulary RDF vocabulary
	 * @return set of IRIs
	 */
	public static Set<IRI> getIRIs(Class vocabulary) {
		Set<IRI> iris = new LinkedHashSet<>();

		for (Field f : vocabulary.getFields()) {
			if (f.getType().equals(IRI.class)) {
				try {
					iris.add((IRI) f.get(vocabulary));
				} catch (IllegalAccessException ex) {
					// should not happen
					throw new RuntimeException("Cannot access vocabulary field", ex);
				}
			}
		}
		return iris;
	}
}
