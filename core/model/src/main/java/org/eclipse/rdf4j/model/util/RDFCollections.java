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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;

/**
 * Utilities for working with RDF Collections and converting to/from Java {@link Collection} classes.
 * <P>
 * RDF Collections are represented using a Lisp-like structure: the list starts with a head resource (typically a blank
 * node), which is connected to the first collection member via the {@link RDF#FIRST} relation. The head resource is
 * then connected to the rest of the list via an {@link RDF#REST} relation. The last resource in the list is marked
 * using the {@link RDF#NIL} node.
 * <p>
 * As an example, a list containing three literal values "A", "B", and "C" looks like this as an RDF Collection:
 *
 * <pre>
 *   _:n1 -rdf:type--> rdf:List
 *     |
 *     +---rdf:first--> "A"
 *     |
 *     +---rdf:rest --> _:n2 -rdf:first--> "B"
 *                        |
 *                        +---rdf:rest--> _:n3 -rdf:first--> "C"
 *                                          |
 *                                          +---rdf:rest--> rdf:nil
 * </pre>
 *
 * Here, {@code _:n1} is the head resource of the list. Note that in this example it is declared an instance of
 * {@link RDF#LIST}, however this is not required for the collection to be considered well-formed.
 *
 * @author Jeen Broekstra
 * @see <a href="http://www.w3.org/TR/rdf-schema/#ch_collectionvocab">RDF Schema 1.1 section on Collection
 *      vocabulary</a>
 */
public class RDFCollections {

	/**
	 * Converts the supplied {@link Iterable} to an <a href="http://www.w3.org/TR/rdf-schema/#ch_collectionvocab">RDF
	 * Collection</a>, using the supplied {@code head} resource as the starting resource of the RDF Collection. The
	 * statements making up the new RDF Collection will be added to the supplied statement collection.
	 *
	 * @param values   an {@link Iterable} of objects (such as a Java {@link Collection} ), which will be converted to
	 *                 an RDF Collection. May not be {@code null}. The method attempts to convert each value that is not
	 *                 already an instance of {@link Value} to a {@link Literal}. This conversion will fail with a
	 *                 {@link LiteralUtilException} if the value's object type is not supported. See
	 *                 {@link Literals#createLiteralOrFail(ValueFactory, Object)} for an overview of supported types.
	 * @param head     a {@link Resource} which will be used as the head of the list, that is, the starting point of the
	 *                 created RDF Collection. May be {@code null}, in which case a new resource is generated to
	 *                 represent the list head.
	 * @param sink     a {@link Collection} of {@link Statement} objects (for example a {@link Model}) to which the RDF
	 *                 Collection statements will be added. May not be {@code null}.
	 * @param contexts the context(s) in which to add the RDF Collection. This argument is an optional vararg and can be
	 *                 left out.
	 * @return the supplied sink {@link Collection} of {@link Statement}s, with the new Statements forming the RDF
	 *         Collection added.
	 * @throws LiteralUtilException if one of the supplied values can not be converted to a Literal.
	 * @see <a href="http://www.w3.org/TR/rdf-schema/#ch_collectionvocab">RDF Schema 1.1 section on Collection
	 *      vocabulary</a>
	 */
	public static <C extends Collection<Statement>> C asRDF(Iterable<?> values, Resource head, C sink,
			Resource... contexts) {
		Objects.requireNonNull(sink);
		consumeCollection(values, head, sink::add, contexts);
		return sink;
	}

	/**
	 * Converts the supplied {@link Iterable} to an <a href="http://www.w3.org/TR/rdf-schema/#ch_collectionvocab">RDF
	 * Collection</a>, using the supplied {@code head} resource as the starting resource of the RDF Collection. The
	 * statements making up the new RDF Collection will be added to the supplied statement collection.
	 *
	 * @param values       an {@link Iterable} of objects (such as a Java {@link Collection} ), which will be converted
	 *                     to an RDF Collection. May not be {@code null}. The method attempts to convert each value that
	 *                     is not already an instance of {@link Value} to a {@link Literal}. This conversion will fail
	 *                     with a {@link LiteralUtilException} if the value's object type is not supported. See
	 *                     {@link Literals#createLiteralOrFail(ValueFactory, Object)} for an overview of supported
	 *                     types.
	 * @param head         a {@link Resource} which will be used as the head of the list, that is, the starting point of
	 *                     the created RDF Collection. May be {@code null}, in which case a new resource is generated to
	 *                     represent the list head.
	 * @param sink         a {@link Collection} of {@link Statement} objects (for example a {@link Model}) to which the
	 *                     RDF Collection statements will be added. May not be {@code null}.
	 * @param valueFactory the {@link ValueFactory} to be used for creation of RDF model objects. May not be
	 *                     {@code null}.
	 * @param contexts     the context(s) in which to add the RDF Collection. This argument is an optional vararg and
	 *                     can be left out.
	 * @return the supplied sink {@link Collection} of {@link Statement}s, with the new Statements forming the RDF
	 *         Collection added.
	 * @throws LiteralUtilException if one of the supplied values can not be converted to a Literal.
	 * @see <a href="http://www.w3.org/TR/rdf-schema/#ch_collectionvocab">RDF Schema 1.1 section on Collection
	 *      vocabulary</a>
	 *
	 * @since 3.0
	 */
	public static <C extends Collection<Statement>> C asRDF(Iterable<?> values, Resource head, C sink,
			ValueFactory valueFactory, Resource... contexts) {
		Objects.requireNonNull(sink);
		consumeCollection(values, head, st -> sink.add(st), valueFactory, contexts);
		return sink;
	}

	/**
	 * Converts an RDF Collection to a Java {@link Collection} of {@link Value} objects. The RDF Collection is given by
	 * the supplied {@link Model} and {@code head}. This method expects the RDF Collection to be well-formed. If the
	 * collection is not well-formed the method may return part of the collection, or may throw a
	 * {@link ModelException}.
	 *
	 * @param m          the Model containing the collection to read.
	 * @param head       the {@link Resource} that represents the list head, that is the start resource of the RDF
	 *                   Collection to be read. May not be {@code null}.
	 * @param collection the Java {@link Collection} to add the collection items to.
	 * @param contexts   the context(s) from which to read the RDF Collection. This argument is an optional vararg and
	 *                   can be left out.
	 * @return the supplied Java {@link Collection}, filled with the items from the RDF Collection (if any).
	 * @throws ModelException if a problem occurs reading the RDF Collection, for example if the Collection is not
	 *                        well-formed.
	 * @see <a href="http://www.w3.org/TR/rdf-schema/#ch_collectionvocab">RDF Schema 1.1 section on Collection
	 *      vocabulary</a>
	 */
	public static <C extends Collection<Value>> C asValues(final Model m, Resource head, C collection,
			Resource... contexts) throws ModelException {
		Objects.requireNonNull(collection, "collection may not be null");

		consumeValues(m, head, v -> collection.add(v), contexts);

		return collection;
	}

	/**
	 * Converts the supplied {@link Iterable} to an <a href="http://www.w3.org/TR/rdf-schema/#ch_collectionvocab">RDF
	 * Collection</a>, using the supplied {@code head} resource as the starting resource of the RDF Collection. The
	 * statements making up the new RDF Collection will be reported to the supplied {@link Consumer} function.
	 *
	 * @param values   an {@link Iterable} of objects (such as a Java {@link Collection} ), which will be converted to
	 *                 an RDF Collection. May not be {@code null}. The method attempts to convert each value that is not
	 *                 already an instance of {@link Value} to a {@link Literal}. This conversion will fail with a
	 *                 {@link LiteralUtilException} if the value's object type is not supported. See
	 *                 {@link Literals#createLiteralOrFail(ValueFactory, Object)} for an overview of supported types.
	 * @param head     a {@link Resource} which will be used as the head of the list, that is, the starting point of the
	 *                 created RDF Collection. May be {@code null}, in which case a new resource is generated to
	 *                 represent the list head.
	 * @param consumer the {@link Consumer} function for the Statements of the RDF Collection. May not be {@code null}.
	 * @param contexts the context(s) in which to add the RDF Collection. This argument is an optional vararg and can be
	 *                 left out.
	 * @throws LiteralUtilException if one of the supplied values can not be converted to a Literal.
	 * @see <a href="http://www.w3.org/TR/rdf-schema/#ch_collectionvocab">RDF Schema 1.1 section on Collection
	 *      vocabulary</a>
	 * @see Literals#createLiteralOrFail(ValueFactory, Object)
	 */
	public static void consumeCollection(Iterable<?> values, Resource head, Consumer<Statement> consumer,
			Resource... contexts) {
		consumeCollection(values, head, consumer, SimpleValueFactory.getInstance(), contexts);
	}

	/**
	 * Converts the supplied {@link Iterable} to an <a href="http://www.w3.org/TR/rdf-schema/#ch_collectionvocab">RDF
	 * Collection</a>, using the supplied {@code head} resource as the starting resource of the RDF Collection. The
	 * statements making up the new RDF Collection will be reported to the supplied {@link Consumer} function.
	 *
	 * @param values   an {@link Iterable} of objects (such as a Java {@link Collection} ), which will be converted to
	 *                 an RDF Collection. May not be {@code null}. The method attempts to convert each value that is not
	 *                 already an instance of {@link Value} to a {@link Literal}. This conversion will fail with a
	 *                 {@link LiteralUtilException} if the value's object type is not supported. See
	 *                 {@link Literals#createLiteralOrFail(ValueFactory, Object)} for an overview of supported types.
	 * @param head     a {@link Resource} which will be used as the head of the list, that is, the starting point of the
	 *                 created RDF Collection. May be {@code null}, in which case a new resource is generated to
	 *                 represent the list head.
	 * @param consumer the {@link Consumer} function for the Statements of the RDF Collection. May not be {@code null}.
	 * @param vf       the {@link ValueFactory} to use for creation of new model objects. May not be {@code null}
	 * @param contexts the context(s) in which to add the RDF Collection. This argument is an optional vararg and can be
	 *                 left out.
	 * @throws LiteralUtilException if one of the supplied values can not be converted to a Literal.
	 * @see <a href="http://www.w3.org/TR/rdf-schema/#ch_collectionvocab">RDF Schema 1.1 section on Collection
	 *      vocabulary</a>
	 * @see Literals#createLiteralOrFail(ValueFactory, Object)
	 *
	 * @since 3.0
	 */
	public static void consumeCollection(Iterable<?> values, Resource head, Consumer<Statement> consumer,
			ValueFactory vf,
			Resource... contexts) {
		Objects.requireNonNull(values, "input collection may not be null");
		Objects.requireNonNull(consumer, "consumer may not be null");
		Objects.requireNonNull(vf, "injected value factory may not be null");

		Resource current = head != null ? head : vf.createBNode();

		Statements.consume(vf, current, RDF.TYPE, RDF.LIST, consumer, contexts);

		Iterator<?> iter = values.iterator();
		while (iter.hasNext()) {
			Object o = iter.next();
			Value v = o instanceof Value ? (Value) o : Literals.createLiteralOrFail(vf, o);
			Statements.consume(vf, current, RDF.FIRST, v, consumer, contexts);
			if (iter.hasNext()) {
				Resource next = vf.createBNode();
				Statements.consume(vf, current, RDF.REST, next, consumer, contexts);
				current = next;
			} else {
				Statements.consume(vf, current, RDF.REST, RDF.NIL, consumer, contexts);
			}
		}
	}

	/**
	 * Reads an RDF Collection starting with the supplied list head from the supplied {@link Model} and sends each
	 * collection member {@link Value} to the supplied {@link Consumer} function. This method expects the RDF Collection
	 * to be well-formed. If the collection is not well-formed the method may report only part of the collection, or may
	 * throw a {@link ModelException}.
	 *
	 * @param m        the Model containing the collection to read.
	 * @param head     the {@link Resource} that represents the list head, that is the start resource of the RDF
	 *                 Collection to be read. May not be {@code null}.
	 * @param consumer the Java {@link Consumer} function to which the collection items are reported.
	 * @param contexts the context(s) from which to read the RDF Collection. This argument is an optional vararg and can
	 *                 be left out.
	 * @throws ModelException if a problem occurs reading the RDF Collection, for example if the Collection is not
	 *                        well-formed.
	 * @see <a href="http://www.w3.org/TR/rdf-schema/#ch_collectionvocab">RDF Schema 1.1 section on Collection
	 *      vocabulary</a>
	 */
	public static void consumeValues(final Model m, Resource head, Consumer<Value> consumer, Resource... contexts)
			throws ModelException {
		Objects.requireNonNull(consumer, "consumer may not be null");
		Objects.requireNonNull(m, "input model may not be null");

		GetStatementOptional statementSupplier = (s, p, o, c) -> m.filter(s, p, o, c).stream().findAny();
		Function<String, Supplier<ModelException>> exceptionSupplier = Models::modelException;

		extract(statementSupplier, head, st -> {
			if (RDF.FIRST.equals(st.getPredicate())) {
				consumer.accept(st.getObject());
			}
		}, exceptionSupplier, contexts);
	}

	/**
	 * Extracts the <a href="http://www.w3.org/TR/rdf-schema/#ch_collectionvocab">RDF Collection</a> starting with the
	 * supplied {@code head} resource from the supplied source {@link Model}. The statements making up the RDF
	 * Collection will be added to the supplied statement collection, which will also be returned.
	 *
	 * @param sourceModel the source model, containing the RDF Collection to be read.
	 * @param head        the {@link Resource} that represents the list head, that is the start resource of the RDF
	 *                    Collection to be read. May not be {@code null}. a {@link Collection} of {@link Statement}
	 *                    objects (for example a {@link Model}) to which the RDF Collection statements will be added.
	 *                    May not be {@code null}.
	 * @param sink        a {@link Collection} of {@link Statement} objects (for example a {@link Model}) to which the
	 *                    RDF Collection statements will be added. May not be {@code null}.
	 * @param contexts    the context(s) from which to read the RDF Collection. This argument is an optional vararg and
	 *                    can be left out.
	 * @return the supplied sink {@link Collection} of {@link Statement}s, with the Statements of the RDF Collection
	 *         added.
	 */
	public static <C extends Collection<Statement>> C getCollection(Model sourceModel, Resource head, C sink,
			Resource... contexts) {
		Objects.requireNonNull(sourceModel, "input model may not be null");
		extract(sourceModel, head, st -> sink.add(st), contexts);
		return sink;
	}

	/**
	 * Extracts the <a href="http://www.w3.org/TR/rdf-schema/#ch_collectionvocab">RDF Collection</a> starting with
	 * supplied {@code head} resource from the supplied source {@link Model} and sends the statements that make up the
	 * collection to the supplied {@link Consumer}.
	 *
	 * @param sourceModel the source model, containing the RDF Collection to be read.
	 * @param head        the {@link Resource} that represents the list head, that is the start resource of the RDF
	 *                    Collection to be read. May not be {@code null}. a {@link Collection} of {@link Statement}
	 *                    objects (for example a {@link Model}) to which the RDF Collection statements will be added.
	 *                    May not be {@code null}.
	 * @param consumer    the {@link Consumer} function for the Statements of the RDF Collection. May not be
	 *                    {@code null}.
	 * @param contexts    the context(s) from which to read the RDF Collection. This argument is an optional vararg and
	 *                    can be left out.
	 */
	public static void extract(Model sourceModel, Resource head, Consumer<Statement> consumer, Resource... contexts) {
		Objects.requireNonNull(sourceModel, "source model may not be null");
		GetStatementOptional statementSupplier = (s, p, o,
				c) -> ((Model) sourceModel).filter(s, p, o, c).stream().findAny();
		extract(statementSupplier, head, consumer, Models::modelException, contexts);
	}

	/**
	 * Extracts an RDF Collection starting with the supplied list head from the statement supplier and sends all
	 * statements that make up the collection to the supplied {@link Consumer} function. This method expects the RDF
	 * Collection to be well-formed. If the collection is not well-formed the method may report only part of the
	 * collection, or may throw an exception.
	 *
	 * @param statementSupplier  the source of the statements from which the RDF collection is to be read, specified as
	 *                           a functional interface.
	 * @param head               the {@link Resource} that represents the list head, that is the start resource of the
	 *                           RDF Collection to be read. May not be {@code null}.
	 * @param collectionConsumer the Java {@link Consumer} function to which the collection statements are reported.
	 * @param exceptionSupplier  a functional interface that produces the exception type this method will throw when an
	 *                           error occurs.
	 * @param contexts           the context(s) from which to read the RDF Collection. This argument is an optional
	 *                           vararg and can be left out.
	 * @throws E if a problem occurs reading the RDF Collection, for example if it is not well-formed.
	 */
	public static <E extends RDF4JException> void extract(GetStatementOptional statementSupplier, Resource head,
			Consumer<Statement> collectionConsumer, Function<String, Supplier<E>> exceptionSupplier,
			Resource... contexts) throws E {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");
		Objects.requireNonNull(head, "list head may not be null");
		Objects.requireNonNull(collectionConsumer, "collection consumer may not be null");

		Resource current = head;
		final Set<Resource> visited = new HashSet<>();
		while (!RDF.NIL.equals(current)) {
			if (visited.contains(current)) {
				throw exceptionSupplier.apply("list not well-formed: cycle detected").get();
			}

			statementSupplier.get(current, RDF.TYPE, RDF.LIST, contexts).ifPresent(collectionConsumer);

			collectionConsumer.accept(statementSupplier.get(current, RDF.FIRST, null, contexts)
					.orElseThrow(exceptionSupplier.apply("list not wellformed: rdf:first statement missing.")));

			Statement next = statementSupplier.get(current, RDF.REST, null, contexts)
					.orElseThrow(exceptionSupplier.apply("list not well-formed: rdf:rest statement missing."));

			collectionConsumer.accept(next);

			if (!(next.getObject() instanceof Resource)) {
				throw exceptionSupplier.apply("list not well-formed: value of rdf:rest should be one of (IRI, BNode).")
						.get();
			}
			visited.add(current);
			current = (Resource) next.getObject();
		}
	}

}
