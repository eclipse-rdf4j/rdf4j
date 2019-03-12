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
import java.util.function.Consumer;

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
	 * Creates one or more {@link Statement} objects with the given subject, predicate and object, one for each given
	 * context, and sends each created statement to the supplied {@link Consumer}. If no context is supplied, only a
	 * single statement (without any assigned context) is created.
	 * 
	 * @param vf        the {@link ValueFactory} to use for creating statements.
	 * @param subject   the subject of each statement. May not be null.
	 * @param predicate the predicate of each statement. May not be null.
	 * @param object    the object of each statement. May not be null.
	 * @param consumer  the {@link Consumer} function for the produced statements.
	 * @param contexts  the context(s) for which to produce statements. This argument is an optional vararg: leave it
	 *                  out completely to produce a single statement without context.
	 */
	public static void consume(ValueFactory vf, Resource subject, IRI predicate, Value object,
			Consumer<Statement> consumer, Resource... contexts) {
		OpenRDFUtil.verifyContextNotNull(contexts);
		Objects.requireNonNull(consumer);

		if (contexts.length > 0) {
			for (Resource context : contexts) {
				consumer.accept(vf.createStatement(subject, predicate, object, context));
			}
		} else {
			consumer.accept(vf.createStatement(subject, predicate, object));
		}
	}

	/**
	 * Creates one or more {@link Statement} objects with the given subject, predicate and object, one for each given
	 * context. If no context is supplied, only a single statement (without any assigned context) is created.
	 * 
	 * @param vf         the {@link ValueFactory} to use for creating statements.
	 * @param subject    the subject of each statement. May not be null.
	 * @param predicate  the predicate of each statement. May not be null.
	 * @param object     the object of each statement. May not be null.
	 * @param collection the collection of Statements to which the newly created Statements will be added. May not be
	 *                   null.
	 * @return the input collection of Statements, with the newly created Statements added.
	 * @param contexts the context(s) for which to produce statements. This argument is an optional vararg: leave it out
	 *                 completely to produce a single statement without context.
	 */
	public static <C extends Collection<Statement>> C create(ValueFactory vf, Resource subject, IRI predicate,
			Value object, C collection, Resource... contexts) {
		Objects.requireNonNull(collection);
		consume(vf, subject, predicate, object, st -> collection.add(st), contexts);
		return collection;
	}

	/**
	 * Checks if the two statements represent the same triple (that is, they have equal subject, predicate, and object).
	 * Context information is disregarded.
	 * 
	 * @param st1 the first statement to compare. May not be null.
	 * @param st2 the second statement to compare. May not be null.
	 * @return {@code true} iff the subject, predicate and object of {@code st1} and {@code st2} are equal,
	 *         {@code false} otherwise.
	 * @see Statement#equals(Object)
	 * @since 2.0
	 */
	public static boolean isSameTriple(Statement st1, Statement st2) {
		Objects.requireNonNull(st1);
		Objects.requireNonNull(st2);
		return st1.getPredicate().equals(st2.getPredicate()) && st1.getSubject().equals(st2.getSubject())
				&& st1.getObject().equals(st2.getObject());
	}
}
