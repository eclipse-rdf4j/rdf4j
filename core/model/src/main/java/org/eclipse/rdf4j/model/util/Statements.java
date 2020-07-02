/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.rdf4j.OpenRDFUtil;
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
 * Utility methods for {@link Statement} objects.
 *
 * @author Jeen Broekstra
 */
public class Statements {

	/**
	 * A {@link Function} that maps {@link Triple} to {@link org.eclipse.rdf4j.model.BNode} consistently. Multiple
	 * invocations for the same {@link Triple} will return the same {@link org.eclipse.rdf4j.model.BNode}.
	 *
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
	 * Strips the context (if any) from the supplied statement and returns a statement with the same subject, predicate
	 * and object, but with no assigned context.
	 *
	 * @param statement the statement to strip the context from
	 * @return a statement without context
	 *
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
	 *
	 * @since 3.1.0
	 */
	public static Statement stripContext(ValueFactory vf, Statement statement) {
		if (statement.getContext() == null) {
			return statement;
		}
		return vf.createStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
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
	 * Converts the supplied RDF* statement to RDF reification statements, and sends the resultant statements to the
	 * supplied consumer. If the supplied statement is not RDF* it will be sent to the consumer as is.
	 * <p>
	 * The statements needed to represent reification will use blank nodes.
	 *
	 * @param st       the {@link Statement} to convert.
	 * @param consumer the {@link Consumer} function for the produced statements.
	 */
	@Experimental
	public static void convertRDFStarToReification(Statement st, Consumer<Statement> consumer) {
		convertRDFStarToReification(SimpleValueFactory.getInstance(), st, consumer);
	}

	/**
	 * Converts the supplied RDF* statement to RDF reification statements, and sends the resultant statements to the
	 * supplied consumer. If the supplied statement is not RDF* it will be sent to the consumer as is.
	 * <p>
	 * The statements needed to represent reification will use blank nodes.
	 * <p>
	 * The supplied value factory is used to create all new statements and blank nodes.
	 *
	 * @param vf       the {@link ValueFactory} to use for creating statements.
	 * @param st       the {@link Statement} to convert.
	 * @param consumer the {@link Consumer} function for the produced statements.
	 */
	@Experimental
	public static void convertRDFStarToReification(ValueFactory vf, Statement st, Consumer<Statement> consumer) {
		convertRDFStarToReification(vf, TRIPLE_BNODE_MAPPER, st, consumer);
	}

	/**
	 * Converts the supplied RDF* statement to RDF reification statements, and sends the resultant statements to the
	 * supplied consumer. If the supplied statement is not RDF* it will be sent to the consumer as is.
	 * <p>
	 * The supplied value factory is used to create all new statements.
	 * <p>
	 * The supplied mapper function maps a {@link Triple} to a {@link Resource} and is used to create the ID of the RDF
	 * reification statement corresponding to the converted triple. The function must return the same value for
	 * identical triples in order to produce consistent results between invocations. See {@link #TRIPLE_BNODE_MAPPER}.
	 *
	 * @param vf              the {@link ValueFactory} to use for creating statements.
	 * @param reifiedIdMapper the mapper {@link Function} from {@link Triple} to {@link Resource}.
	 * @param st              the {@link Statement} to convert,
	 * @param consumer        the {@link Consumer} function for the produced statements.
	 */
	@Experimental
	public static void convertRDFStarToReification(ValueFactory vf, Function<Triple, Resource> reifiedIdMapper,
			Statement st, Consumer<Statement> consumer) {
		Resource subject = st.getSubject();
		Value object = st.getObject();
		if (subject instanceof Triple || object instanceof Triple) {
			if (subject instanceof Triple) {
				subject = createReifiedStatement(vf, reifiedIdMapper, (Triple) subject, st.getContext(), consumer);
			}
			if (object instanceof Triple) {
				object = createReifiedStatement(vf, reifiedIdMapper, (Triple) object, st.getContext(), consumer);
			}
			st = vf.createStatement(subject, st.getPredicate(), object, st.getContext());
		}
		consumer.accept(st);
	}

	/**
	 * Converts the supplied RDF* triple to a series of RDF reification statements and sends the statements to the
	 * supplied consumer. The subject of the created statements is returned.
	 * <p>
	 * The supplied value factory is used to create all new statements.
	 * <p>
	 * The supplied mapper function maps a {@link Triple} to a {@link Resource} and is used to create the ID of the RDF
	 * reification statement corresponding to the converted triple.
	 *
	 * @param vf              the {@link ValueFactory} to use for creating statements.
	 * @param reifiedIdMapper the mapper {@link Function} from {@link Triple} to {@link Resource}.
	 * @param triple          the {@link Triple} to convert.
	 * @param consumer        the {@link Consumer} function for the produced statements.
	 * @return the {@link Resource} that was used as the subject of the created RDF reification statements.
	 */
	private static Resource createReifiedStatement(ValueFactory vf, Function<Triple, Resource> reifiedIdMapper,
			Triple triple, Resource context, Consumer<Statement> consumer) {
		Resource stId = reifiedIdMapper.apply(triple);
		Statement reifiedSt = vf.createStatement(stId, RDF.TYPE, RDF.STATEMENT, context);
		consumer.accept(reifiedSt);
		Statement reifiedStSubject = vf.createStatement(stId, RDF.SUBJECT, triple.getSubject(), context);
		convertRDFStarToReification(vf, reifiedIdMapper, reifiedStSubject, consumer);
		Statement reifiedStPredicate = vf.createStatement(stId, RDF.PREDICATE, triple.getPredicate(), context);
		consumer.accept(reifiedStPredicate);
		Statement reifiedStObject = vf.createStatement(stId, RDF.OBJECT, triple.getObject(), context);
		convertRDFStarToReification(vf, reifiedIdMapper, reifiedStObject, consumer);
		return stId;
	}
}
