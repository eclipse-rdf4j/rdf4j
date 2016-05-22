/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import java.util.Collection;
import java.util.Objects;

import org.eclipse.rdf4j.OpenRDFUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;

/**
 * Utility methods for {@link Statement} objects.
 *
 * @author Jeen Broekstra
 */
public class Statements {

	/**
	 * Creates one or more {@link Statement} objects with the given subject, predicate and object, one for
	 * each given context. If no context is supplied, only a single statement (without any assigned context)
	 * is created.
	 * 
	 * @param vf
	 *        the {@link ValueFactory} to use for creating statements.
	 * @param subject
	 *        the subject of each statement. May not be null.
	 * @param predicate
	 *        the predicate of each statement. May not be null.
	 * @param object
	 *        the object of each statement. May not be null.
	 * @param collection
	 *        the collection of Statements to which the newly created Statements will be added. May not be
	 *        null.
	 * @return the input collection of Statements, with the newly created Statements added.
	 * @param contexts
	 *        the context(s) for which to produce statements. This argument is an optional vararg: leave it
	 *        out completely to produce a single statement without context.
	 */
	public static <C extends Collection<Statement>> C create(ValueFactory vf, Resource subject, IRI predicate,
			Value object, C collection, Resource... contexts)
	{
		Objects.requireNonNull(collection);
		OpenRDFUtil.verifyContextNotNull(contexts);

		if (contexts.length > 0) {
			for (Resource context : contexts) {
				collection.add(vf.createStatement(subject, predicate, object, context));
			}
		}
		else {
			collection.add(vf.createStatement(subject, predicate, object));
		}
		return collection;
	}
}
