/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;

/**
 * Utility methods for working with {@link Statement} objects, including conversion to/from {@link Triple RDF 1.2 triple
 * objects}.
 *
 * @author Jeen Broekstra
 */
public class Statements {

	/**
	 * A {@link Function} that maps {@link Triple} to {@link org.eclipse.rdf4j.model.BNode} consistently. Multiple
	 * invocations for the same {@link Triple} will return the same {@link org.eclipse.rdf4j.model.BNode}.
	 * <p>
	 * The current implementation creates a {@link org.eclipse.rdf4j.model.BNode} by encoding the string representation
	 * of the {@link Triple} using base64 URL-safe encoding.
	 */
	@Experimental
	public static Function<Triple, Resource> TRIPLE_BNODE_MAPPER = (t) -> SimpleValueFactory.getInstance()
			.createBNode(Base64.getUrlEncoder().encodeToString(t.stringValue().getBytes(StandardCharsets.UTF_8)));

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
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

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
	 * @param contexts   the context(s) for which to produce statements. This argument is an optional vararg: leave it
	 *                   out completely to produce a single statement without context.
	 * @return the input collection of Statements, with the newly created Statements added.
	 */
	public static <C extends Collection<Statement>> C create(ValueFactory vf, Resource subject, IRI predicate,
			Value object, C collection, Resource... contexts) {
		Objects.requireNonNull(collection);
		consume(vf, subject, predicate, object, st -> collection.add(st), contexts);
		return collection;
	}

	/**
	 * Strips the context (if any) from the supplied statement and returns a statement with the same subject, predicate
	 * and object, but with no assigned context.
	 *
	 * @param statement the statement to strip the context from
	 * @return a statement without context
	 * @since 3.1.0
	 */
	public static Statement stripContext(Statement statement) {
		return stripContext(SimpleValueFactory.getInstance(), statement);
	}

	/**
	 * Strips the context (if any) from the supplied statement and returns a statement with the same subject, predicate
	 * and object, but with no assigned context.
	 *
	 * @param vf        the {@link ValueFactory} to use for creating a new {@link Statement}.
	 * @param statement the statement to strip the context from.
	 * @return a statement without context
	 * @since 3.1.0
	 */
	public static Statement stripContext(ValueFactory vf, Statement statement) {
		if (statement.getContext() == null) {
			return statement;
		}
		return vf.createStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
	}

	/**
	 * Create an {@link Triple RDF 1.2 triple} from the supplied {@link Statement}
	 *
	 * @param statement a statement to convert to an RDF 1.2 triple
	 * @return an {@link Triple RDF 1.2 triple} with the same subject, predicate and object as the input statement.
	 * @since 3.4.0
	 * @deprecated Use {@link Values#triple(Statement)} instead
	 */
	@Deprecated(since = "3.5.0")
	public static Triple toTriple(Statement statement) {
		return toTriple(SimpleValueFactory.getInstance(), statement);
	}

	/**
	 * Create an {@link Triple RDF 1.2 triple} from the supplied {@link Statement}
	 *
	 * @param vf        the {@link ValueFactory} to use for creating the {@link Triple} object.
	 * @param statement a statement to convert to an RDF 1.2 triple
	 * @return an {@link Triple RDF 1.2 triple} with the same subject, predicate and object as the input statement.
	 * @since 3.4.0
	 * @deprecated Use {@link Values#triple(ValueFactory, Statement)} instead
	 */
	@Deprecated(since = "3.5.0")
	public static Triple toTriple(ValueFactory vf, Statement statement) {
		return vf.createTriple(statement.getSubject(), statement.getPredicate(), statement.getObject());
	}

	/**
	 * Create a {@link Statement} from the supplied {@link Triple RDF 1.2 triple}
	 *
	 * @param triple an RDF 1.2 triple to convert to a {@link Statement}.
	 * @return an {@link Statement} with the same subject, predicate and object as the input triple, and no context.
	 * @since 3.4.0
	 * @deprecated Use {@link #statement(Triple)} instead
	 */
	public static Statement toStatement(Triple triple) {
		return statement(triple);
	}

	/**
	 * Create a {@link Statement} from the supplied {@link Triple RDF 1.2 triple}
	 *
	 * @param triple an RDF 1.2 triple to convert to a {@link Statement}.
	 * @return an {@link Statement} with the same subject, predicate and object as the input triple, and no context.
	 * @since 3.4.0
	 */
	public static Statement statement(Triple triple) {
		return toStatement(triple, null);
	}

	/**
	 * Create a {@link Statement} from the supplied {@link Triple RDF 1.2 triple} and context.
	 *
	 * @param triple  an RDF 1.2 triple to convert to a {@link Statement}.
	 * @param context the context to assign to the {@link Statement}.
	 * @return an {@link Statement} with the same subject, predicate and object as the input triple, and having the
	 *         supplied context.
	 * @since 3.7.0
	 */
	public static Statement statement(Triple triple, Resource context) {
		return statement(SimpleValueFactory.getInstance(), triple, context);
	}

	/**
	 * Create a {@link Statement} from the supplied {@link Triple RDF 1.2 triple} and context.
	 *
	 * @param triple  an RDF 1.2 triple to convert to a {@link Statement}.
	 * @param context the context to assign to the {@link Statement}.
	 * @return an {@link Statement} with the same subject, predicate and object as the input triple, and having the
	 *         supplied context.
	 * @since 3.4.0
	 * @deprecated since 3.7.0 - use {@link #statement(Triple, Resource)} instead
	 */
	public static Statement toStatement(Triple triple, Resource context) {
		return statement(SimpleValueFactory.getInstance(), triple, context);
	}

	/**
	 * Create a {@link Statement} from the supplied {@link Triple RDF 1.2 triple} and context.
	 *
	 * @param vf      the {@link ValueFactory} to use for creating the {@link Statement} object.
	 * @param triple  an RDF 1.2 triple to convert to a {@link Statement}.
	 * @param context the context to assign to the {@link Statement}. May be null to indicate no context.
	 * @return an {@link Statement} with the same subject, predicate and object as the input triple, and having the
	 *         supplied context.
	 * @since 3.4.0
	 * @deprecated Use {@link #statement(ValueFactory, Triple, Resource)} instead
	 */
	public static Statement toStatement(ValueFactory vf, Triple triple, Resource context) {
		return statement(vf, triple, context);
	}

	/**
	 * Create a {@link Statement} from the supplied {@link Triple RDF 1.2 triple} and context.
	 *
	 * @param vf      the {@link ValueFactory} to use for creating the {@link Statement} object.
	 * @param triple  an RDF 1.2 triple to convert to a {@link Statement}.
	 * @param context the context to assign to the {@link Statement}. May be null to indicate no context.
	 * @return an {@link Statement} with the same subject, predicate and object as the input triple, and having the
	 *         supplied context.
	 * @since 3.7.0
	 */
	public static Statement statement(ValueFactory vf, Triple triple, Resource context) {
		return vf.createStatement(triple.getSubject(), triple.getPredicate(), triple.getObject(), context);
	}

	/**
	 * Create a {@link Statement} from the supplied subject, predicate, object and context.
	 *
	 * @param subject   the statement subject
	 * @param predicate the statement predicate
	 * @param object    the statement object
	 * @param context   the context to assign to the {@link Statement}. May be null to indicate no context.
	 * @return an {@link Statement} with the same subject, predicate and object as the input triple, and having the
	 *         supplied context.
	 * @throws NullPointerException if any of subject, predicate, or object are <code>null</code>.
	 * @since 3.5.0
	 */
	public static Statement statement(Resource subject, IRI predicate, Value object,
			Resource context) {
		return statement(SimpleValueFactory.getInstance(), subject, predicate, object, context);
	}

	/**
	 * Create a {@link Statement} from the supplied subject, predicate, object and context.
	 *
	 * @param vf        the {@link ValueFactory} to use for creating the {@link Statement} object.
	 * @param subject   the statement subject
	 * @param predicate the statement predicate
	 * @param object    the statement object
	 * @param context   the context to assign to the {@link Statement}. May be null to indicate no context.
	 * @return an {@link Statement} with the same subject, predicate and object as the input triple, and having the
	 *         supplied context.
	 * @throws NullPointerException if any of vf, subject, predicate, or object are <code>null</code>.
	 * @since 3.5.0
	 */
	public static Statement statement(ValueFactory vf, Resource subject, IRI predicate, Value object,
			Resource context) {
		return vf.createStatement(subject, predicate, object, context);
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

	/**
	 * Converts the supplied RDF 1.2 statement to RDF reification statements, and sends the resultant statements to the
	 * supplied consumer. If the supplied statement is not RDF 1.2 it will be sent to the consumer as is.
	 * <p>
	 * The statements needed to represent reification will use blank nodes.
	 *
	 * @param st       the {@link Statement} to convert.
	 * @param consumer the {@link Consumer} function for the produced statements.
	 */
	@Experimental
	public static void convertRDF12ReificationToRDF11(Statement st, Consumer<Statement> consumer) {
		convertRDF12ReificationToRDF11(SimpleValueFactory.getInstance(), st, consumer);
	}

	/**
	 * Converts the supplied RDF 1.2 statement to RDF 1.1 reification statements, and sends the resultant statements to
	 * the supplied consumer. If the supplied statement does not contain a triple term it will be sent to the consumer
	 * as is.
	 * <p>
	 * The supplied value factory is used to create all new statements.
	 *
	 * @param vf       the {@link ValueFactory} to use for creating statements.
	 * @param st       the {@link Statement} to convert,
	 * @param consumer the {@link Consumer} function for the produced statements.
	 */
	@Experimental
	public static void convertRDF12ReificationToRDF11(ValueFactory vf, Statement st, Consumer<Statement> consumer) {
		Resource subject = st.getSubject();
		IRI predicate = st.getPredicate();
		Value object = st.getObject();
		Resource context = st.getContext();

		if (object.isTriple()) {
			if (!predicate.equals(RDF.REIFIES)) {
				throw new IllegalArgumentException(
						"Cannot convert triple term statement with predicate other than rdf:reifies");
			}
			Triple triple = (Triple) object;
			if (triple.getObject().isTriple()) {
				throw new IllegalArgumentException("Nested triples cannot be converted to RDF 1.1 reification");
			}
			consumer.accept(vf.createStatement(subject, RDF.TYPE, RDF.STATEMENT, context));
			consumer.accept(vf.createStatement(subject, RDF.SUBJECT, triple.getSubject(), context));
			consumer.accept(vf.createStatement(subject, RDF.PREDICATE, triple.getPredicate(), context));
			consumer.accept(vf.createStatement(subject, RDF.OBJECT, triple.getObject(), context));
		} else {
			consumer.accept(st);
		}
	}
}
