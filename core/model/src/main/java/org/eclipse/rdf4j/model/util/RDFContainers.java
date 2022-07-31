/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
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
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

/**
 * Utilities for working with RDF Containers and converting to/from Java {@link Collection} classes.
 * <P>
 * RDF Containers are represented using 3 different types of structures:
 *
 * 1. {@link RDF#BAG} : A Bag (a resource having type rdf:Bag) represents a group of resources or literals, possibly
 * including duplicate members, where there is no significance in the order of the members.
 *
 * 2. {@link RDF#SEQ} : A Sequence or Seq (a resource having type rdf:Seq) represents a group of resources or literals,
 * possibly including duplicate members, where the order of the members is significant.
 *
 * 3. {@link RDF#ALT} : An Alternative or Alt (a resource having type rdf:Alt) represents a group of resources or
 * literals that are alternatives (typically for a single value of a property).
 *
 * So, in each of the above types, the container starts with a first resource node, via the rdf:_1 relation. Similarly,
 * the next member is connected via the rdf:_2 relation and so on.
 *
 * For eg. Bag containing three literal values "A", "B", and "C" looks like this as an RDF Container:
 *
 * <pre>
 *   _:n1 -rdf:type--> rdf:Bag
 *     |
 *     +---rdf:_1--> "A"
 *     |
 *     +---rdf:_2--> "B"
 *     |
 *     +---rdf:_3--> "C"
 * </pre>
 *
 *
 * @see <a href="https://www.w3.org/TR/rdf-schema/#ch_container">RDF Schema 1.1 section on Collection vocabulary</a>
 */
public class RDFContainers {

	/**
	 * Converts the supplied {@link Iterable} to an <a href="https://www.w3.org/TR/rdf-schema/#ch_container">RDF
	 * Container</a>, using the supplied {@code head} resource as the starting resource of the RDF Containter. The
	 * statements making up the new RDF Containter will be added to the supplied statement collection.
	 *
	 * @param containerType defines the type of RDF Container
	 * @param values        an {@link Iterable} of objects (such as a Java {@link Collection} ), which will be converted
	 *                      to an RDF Containter. May not be {@code null}. The method attempts to convert each value
	 *                      that is not already an instance of {@link Value} to a {@link Literal}. This conversion will
	 *                      fail with a {@link LiteralUtilException} if the value's object type is not supported. See
	 *                      {@link Literals#createLiteralOrFail(ValueFactory, Object)} for an overview of supported
	 *                      types.
	 * @param container     a {@link Resource} which will be used as the head of the container, that is, the starting
	 *                      point of the created RDF Container. May be {@code null}, in which case a new resource is
	 *                      generated to represent the container head.
	 * @param sink          a {@link Collection} of {@link Statement} objects (for example a {@link Model}) to which the
	 *                      RDF Collection statements will be added. May not be {@code null}.
	 * @param contexts      the context(s) in which to add the RDF Containter. This argument is an optional vararg and
	 *                      can be left out.
	 * @return the supplied sink {@link Collection} of {@link Statement}s, with the new Statements forming the RDF
	 *         Collection added.
	 * @throws LiteralUtilException if one of the supplied values can not be converted to a Literal.
	 * @see <a href="https://www.w3.org/TR/rdf-schema/#ch_container">RDF Schema 1.1 section on Collection vocabulary</a>
	 */
	public static <C extends Collection<Statement>> C toRDF(IRI containerType, Iterable<?> values, Resource container,
			C sink,
			Resource... contexts) {

		Objects.requireNonNull(sink);
		consumeContainer(containerType, values, container, st -> sink.add(st), contexts);
		return sink;
	}

	/**
	 * Converts the supplied {@link Iterable} to an <a href="https://www.w3.org/TR/rdf-schema/#ch_container">RDF
	 * Container</a>, using the supplied {@code head} resource as the starting resource of the RDF Containter. The
	 * statements making up the new RDF Containter will be added to the supplied statement collection.
	 *
	 * @param containerType defines the type of RDF Container
	 * @param values        an {@link Iterable} of objects (such as a Java {@link Collection} ), which will be converted
	 *                      to an RDF Containter. May not be {@code null}. The method attempts to convert each value
	 *                      that is not already an instance of {@link Value} to a {@link Literal}. This conversion will
	 *                      fail with a {@link LiteralUtilException} if the value's object type is not supported. See
	 *                      {@link Literals#createLiteralOrFail(ValueFactory, Object)} for an overview of supported
	 *                      types.
	 * @param container     a {@link Resource} which will be used as the head of the container, that is, the starting
	 *                      point of the created RDF Container. May be {@code null}, in which case a new resource is
	 *                      generated to represent the container head.
	 * @param sink          a {@link Collection} of {@link Statement} objects (for example a {@link Model}) to which the
	 *                      RDF Collection statements will be added. May not be {@code null}.
	 * @param vf            the {@link ValueFactory} to be used for creation of RDF model objects. May not be
	 *                      {@code null}.
	 * @param contexts      the context(s) in which to add the RDF Containter. This argument is an optional vararg and
	 *                      can be left out.
	 * @return the supplied sink {@link Collection} of {@link Statement}s, with the new Statements forming the RDF
	 *         Collection added.
	 * @throws LiteralUtilException if one of the supplied values can not be converted to a Literal.
	 * @see <a href="https://www.w3.org/TR/rdf-schema/#ch_container">RDF Schema 1.1 section on Collection vocabulary</a>
	 */
	public static <C extends Collection<Statement>> C toRDF(IRI containerType, Iterable<?> values, Resource container,
			C sink,
			ValueFactory vf, Resource... contexts) {

		Objects.requireNonNull(sink);
		consumeContainer(containerType, values, container, st -> sink.add(st), vf, contexts);
		return sink;
	}

	/**
	 * Converts an RDF Containter to a Java {@link Collection} of {@link Value} objects. The RDF Containter is given by
	 * the supplied {@link Model} and {@code container}. This method expects the RDF Containter to be well-formed. If
	 * the collection is not well-formed the method may return part of the collection, or may throw a
	 * {@link ModelException}.
	 *
	 * @param containerType defines the type of RDF Container
	 * @param m             the Model containing the collection to read.
	 * @param container     the {@link Resource} that represents the container head, that is the start resource of the
	 *                      RDF Container to be read. May not be {@code null}.
	 * @param collection    the Java {@link Collection} to add the collection items to.
	 * @param contexts      the context(s) from which to read the RDF Containter. This argument is an optional vararg
	 *                      and can be left out.
	 * @return the supplied Java {@link Collection}, filled with the items from the RDF Containter (if any).
	 * @throws ModelException if a problem occurs reading the RDF Containter, for example if the Collection is not
	 *                        well-formed.
	 * @see <a href="https://www.w3.org/TR/rdf-schema/#ch_container">RDF Schema 1.1 section on Collection vocabulary</a>
	 */
	public static <C extends Collection<Value>> C toValues(IRI containerType, final Model m, Resource container,
			C collection,
			Resource... contexts) throws ModelException {
		Objects.requireNonNull(collection, "collection may not be null");

		consumeValues(m, container, containerType, v -> collection.add(v), contexts);

		return collection;
	}

	/**
	 * Converts the supplied {@link Iterable} to an <a href="https://www.w3.org/TR/rdf-schema/#ch_container">RDF
	 * Container</a>, using the supplied {@code head} resource as the starting resource of the RDF Containter. The
	 * statements making up the new RDF Containter will be reported to the supplied {@link Consumer} function.
	 *
	 * @param containerType defines the type of RDF Container
	 * @param values        an {@link Iterable} of objects (such as a Java {@link Collection} ), which will be converted
	 *                      to an RDF Containter. May not be {@code null}. The method attempts to convert each value
	 *                      that is not already an instance of {@link Value} to a {@link Literal}. This conversion will
	 *                      fail with a {@link LiteralUtilException} if the value's object type is not supported. See
	 *                      {@link Literals#createLiteralOrFail(ValueFactory, Object)} for an overview of supported
	 *                      types.
	 * @param container     a {@link Resource} which will be used as the head of the container, that is, the starting
	 *                      point of the created RDF Containter. May be {@code null}, in which case a new resource is
	 *                      generated to represent the containter head.
	 * @param consumer      the {@link Consumer} function for the Statements of the RDF Containter. May not be
	 *                      {@code null}.
	 * @param contexts      the context(s) in which to add the RDF Containter. This argument is an optional vararg and
	 *                      can be left out.
	 * @throws LiteralUtilException if one of the supplied values can not be converted to a Literal.
	 * @see <a href="https://www.w3.org/TR/rdf-schema/#ch_container">RDF Schema 1.1 section on Collection vocabulary</a>
	 * @see Literals#createLiteralOrFail(ValueFactory, Object)
	 */
	public static void consumeContainer(IRI containerType, Iterable<?> values, Resource container,
			Consumer<Statement> consumer,
			Resource... contexts) {
		consumeContainer(containerType, values, container, consumer, SimpleValueFactory.getInstance(), contexts);
	}

	/**
	 * Converts the supplied {@link Iterable} to an <a href="https://www.w3.org/TR/rdf-schema/#ch_container">RDF
	 * Container</a>, using the supplied {@code head} resource as the starting resource of the RDF Container. The
	 * statements making up the new RDF Container will be reported to the supplied {@link Consumer} function.
	 *
	 * @param containerType defines the type of RDF Container
	 * @param values        an {@link Iterable} of objects (such as a Java {@link Collection} ), which will be converted
	 *                      to an RDF Container. May not be {@code null}. The method attempts to convert each value that
	 *                      is not already an instance of {@link Value} to a {@link Literal}. This conversion will fail
	 *                      with a {@link LiteralUtilException} if the value's object type is not supported. See
	 *                      {@link Literals#createLiteralOrFail(ValueFactory, Object)} for an overview of supported
	 *                      types.
	 * @param container     a {@link Resource} which will be used as the head of the container, that is, the starting
	 *                      point of the created RDF Container. May be {@code null}, in which case a new resource is
	 *                      generated to represent the containter head.
	 * @param consumer      the {@link Consumer} function for the Statements of the RDF Container. May not be
	 *                      {@code null}.
	 * @param vf            the {@link ValueFactory} to use for creation of new model objects. May not be {@code null}
	 * @param contexts      the context(s) in which to add the RDF Container. This argument is an optional vararg and
	 *                      can be left out.
	 * @throws LiteralUtilException if one of the supplied values can not be converted to a Literal.
	 * @see <a href="https://www.w3.org/TR/rdf-schema/#ch_container">RDF Schema 1.1 section on Collection vocabulary</a>
	 * @see Literals#createLiteralOrFail(ValueFactory, Object)
	 *
	 * @since 3.3.0
	 */
	public static void consumeContainer(IRI containerType, Iterable<?> values, Resource container,
			Consumer<Statement> consumer,
			ValueFactory vf, Resource... contexts) {
		Objects.requireNonNull(values, "input collection may not be null");
		Objects.requireNonNull(consumer, "consumer may not be null");
		Objects.requireNonNull(vf, "injected value factory may not be null");

		Resource current = container != null ? container : vf.createBNode();
		boolean validType = Objects.equals(containerType, RDF.ALT) ||
				Objects.equals(containerType, RDF.BAG) ||
				Objects.equals(containerType, RDF.SEQ);

		if (!validType) {
			throw new ModelException("containerType should be one of ALT, BAG or SEQ");
		}

		Statements.consume(vf, current, RDF.TYPE, containerType, consumer, contexts);

		Iterator<?> iter = values.iterator();
		int elementCounter = 1;
		while (iter.hasNext()) {
			Object o = iter.next();
			Value v = o instanceof Value ? (Value) o : Literals.createLiteralOrFail(vf, o);
			IRI elementCounterPredicate = getAnnotatedMemberPredicate(vf, elementCounter);
			elementCounter++;
			Statements.consume(vf, current, elementCounterPredicate, v, consumer, contexts);
			Statements.consume(vf, current, RDFS.MEMBER, v, consumer, contexts);
		}
	}

	/**
	 * Creates the IRI of the element counter predicate in the {@link RDF} namespace, rdf:_nnn
	 *
	 * @param vf             the {@link ValueFactory} to use for creation of new model objects. May not be {@code null}
	 * @param elementCounter the counter varialbe for which IRI has to be created
	 * @return {@link IRI} of the rdf:_nnn
	 */
	private static IRI getAnnotatedMemberPredicate(ValueFactory vf, int elementCounter) {
		return vf.createIRI(RDF.NAMESPACE, "_" + elementCounter);
	}

	/**
	 * Reads an RDF Container starting with the supplied containter head from the supplied {@link Model} and sends each
	 * collection member {@link Value} to the supplied {@link Consumer} function. This method expects the RDF Container
	 * to be well-formed. If the collection is not well-formed the method may report only part of the collection, or may
	 * throw a {@link ModelException}.
	 *
	 * @param m             the Model containing the collection to read.
	 * @param container     the {@link Resource} that represents the containter head, that is the start resource of the
	 *                      RDF Container to be read. May not be {@code null}.
	 * @param containerType defines the type of RDF Container
	 * @param consumer      the Java {@link Consumer} function to which the collection items are reported.
	 * @param contexts      the context(s) from which to read the RDF Container. This argument is an optional vararg and
	 *                      can be left out.
	 * @throws ModelException if a problem occurs reading the RDF Container, for example if the Collection is not
	 *                        well-formed.
	 * @see <a href="https://www.w3.org/TR/rdf-schema/#ch_container">RDF Schema 1.1 section on Collection vocabulary</a>
	 */

	public static void consumeValues(final Model m, Resource container, IRI containerType, Consumer<Value> consumer,
			Resource... contexts)
			throws ModelException {
		Objects.requireNonNull(consumer, "consumer may not be null");
		Objects.requireNonNull(m, "input model may not be null");

		ValueFactory vf = SimpleValueFactory.getInstance();

		GetStatementOptional statementSupplier = (s, p, o, c) -> m.filter(s, p, o, c).stream().findAny();
		Function<String, Supplier<ModelException>> exceptionSupplier = Models::modelException;

		// TODO add proper documentation
		Pattern annotatedMembershipPredicatePattern = Pattern
				.compile("^" + vf.createIRI(RDF.NAMESPACE, "_") + "[1-9][0-9]*$");

		extract(containerType, statementSupplier, container, st -> {
			if (RDFS.MEMBER.equals(st.getPredicate()) ||
					annotatedMembershipPredicatePattern.matcher(st.getPredicate().toString()).matches()) {
				consumer.accept(st.getObject());
			}
		}, exceptionSupplier, contexts);
	}

	/**
	 * Extracts the <a href="https://www.w3.org/TR/rdf-schema/#ch_container">RDF Container</a> starting with the
	 * supplied {@code head} resource from the supplied source {@link Model}. The statements making up the RDF Container
	 * will be added to the supplied statement collection, which will also be returned.
	 *
	 * @param containerType defines the type of RDF Container
	 * @param sourceModel   the source model, containing the RDF Container to be read.
	 * @param container     the {@link Resource} that represents the container head, that is the start resource of the
	 *                      RDF Container to be read. May not be {@code null}. a {@link Collection} of {@link Statement}
	 *                      objects (for example a {@link Model}) to which the RDF Container statements will be added.
	 *                      May not be {@code null}.
	 * @param sink          a {@link Collection} of {@link Statement} objects (for example a {@link Model}) to which the
	 *                      RDF Container statements will be added. May not be {@code null}.
	 * @param contexts      the context(s) from which to read the RDF Container. This argument is an optional vararg and
	 *                      can be left out.
	 * @return the supplied sink {@link Collection} of {@link Statement}s, with the Statements of the RDF Container
	 *         added.
	 */
	public static <C extends Collection<Statement>> C getContainer(IRI containerType, Model sourceModel,
			Resource container, C sink,
			Resource... contexts) {
		Objects.requireNonNull(sourceModel, "input model may not be null");
		extract(containerType, sourceModel, container, st -> sink.add(st), contexts);
		return sink;
	}

	/**
	 * Extracts the <a href="https://www.w3.org/TR/rdf-schema/#ch_container">RDF Container</a> starting with supplied
	 * {@code head} resource from the supplied source {@link Model} and sends the statements that make up the collection
	 * to the supplied {@link Consumer}.
	 *
	 * @param containerType defines the type of RDF Container
	 * @param sourceModel   the source model, containing the RDF Container to be read.
	 * @param container     the {@link Resource} that represents the container head, that is the start resource of the
	 *                      RDF Container to be read. May not be {@code null}. a {@link Collection} of {@link Statement}
	 *                      objects (for example a {@link Model}) to which the RDF Container statements will be added.
	 *                      May not be {@code null}.
	 * @param consumer      the {@link Consumer} function for the Statements of the RDF Container. May not be
	 *                      {@code null}.
	 * @param contexts      the context(s) from which to read the RDF Container. This argument is an optional vararg and
	 *                      can be left out.
	 */
	public static void extract(IRI containerType, Model sourceModel, Resource container, Consumer<Statement> consumer,
			Resource... contexts) {
		Objects.requireNonNull(sourceModel, "source model may not be null");
		GetStatementOptional statementSupplier = (s, p, o,
				c) -> ((Model) sourceModel).filter(s, p, o, c).stream().findAny();
		extract(containerType, statementSupplier, container, consumer, Models::modelException, contexts);
	}

	/**
	 * Extracts an RDF Container starting with the supplied container head from the statement supplier and sends all
	 * statements that make up the collection to the supplied {@link Consumer} function. This method expects the RDF
	 * Container to be well-formed. If the collection is not well-formed the method may report only part of the
	 * collection, or may throw an exception.
	 *
	 * @param containerType      defines the type of RDF Container
	 * @param statementSupplier  the source of the statements from which the RDF Container is to be read, specified as a
	 *                           functional interface.
	 * @param container          the {@link Resource} that represents the container head, that is the start resource of
	 *                           the RDF Container to be read. May not be {@code null}.
	 * @param collectionConsumer the Java {@link Consumer} function to which the collection statements are reported.
	 * @param exceptionSupplier  a functional interface that produces the exception type this method will throw when an
	 *                           error occurs.
	 * @param contexts           the context(s) from which to read the RDF Container. This argument is an optional
	 *                           vararg and can be left out.
	 * @throws E if a problem occurs reading the RDF Container, for example if it is not well-formed.
	 */
	public static <E extends RDF4JException> void extract(IRI containerType, GetStatementOptional statementSupplier,
			Resource container,
			Consumer<Statement> collectionConsumer, Function<String, Supplier<E>> exceptionSupplier,
			Resource... contexts) throws E {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");
		Objects.requireNonNull(container, "container head may not be null");
		Objects.requireNonNull(collectionConsumer, "collection consumer may not be null");

		ValueFactory vf = SimpleValueFactory.getInstance();

		Resource current = container;

		for (int annotatedMembershipPropertyCounter = 1; true; annotatedMembershipPropertyCounter++) {

			IRI annotatedMembershipPredicate = getAnnotatedMemberPredicate(vf, annotatedMembershipPropertyCounter);
			if (statementSupplier.get(container, annotatedMembershipPredicate, null, contexts)
					.equals(Optional.empty())) {
				break;
			}
			Statement statement = statementSupplier.get(container, annotatedMembershipPredicate, null, contexts).get();

			collectionConsumer.accept(statement);
		}
	}
}
