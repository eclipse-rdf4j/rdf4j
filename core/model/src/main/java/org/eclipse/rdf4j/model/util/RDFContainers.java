/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.model.util;

import org.eclipse.rdf4j.OpenRDFUtil;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * TODO add documentation
 */
public class RDFContainers {

	public static <C extends Collection<Statement>> C asRDF(IRI containerType, Iterable<?> values, Resource container, C sink,
                                                            Resource... contexts) {

		Objects.requireNonNull(sink);
		consumeContainer(containerType, values, container, st -> sink.add(st), contexts);
		return sink;
	}
	public static void consumeContainer(IRI containerType, Iterable<?> values, Resource container, Consumer<Statement> consumer,
			Resource... contexts) {
		consumeContainer(containerType, values, container, consumer, SimpleValueFactory.getInstance(), contexts);
	}
	public static void consumeContainer(IRI containerType, Iterable<?> values, Resource container, Consumer<Statement> consumer,
			ValueFactory vf,
			Resource... contexts) {
		Objects.requireNonNull(values, "input collection may not be null");
		Objects.requireNonNull(consumer, "consumer may not be null");
		Objects.requireNonNull(vf, "injected value factory may not be null");

		Resource current = container != null ? container : vf.createBNode();
		boolean validType = Objects.equals(containerType, RDF.ALT) ||
				Objects.equals(containerType, RDF.BAG) ||
				Objects.equals(containerType, RDF.SEQ);

		if (!validType) {
			throw new RuntimeException("containerType should be one of ALT, BAG or SEQ");
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
			Statements.consume(vf, (Resource) v, RDFS.MEMBER, current, consumer, contexts);
		}
	}

	private static IRI getAnnotatedMemberPredicate(ValueFactory vf, int elementCounter) {
		return vf.createIRI(RDF.NAMESPACE, "_" + elementCounter);
	}

	public static void consumeValues(final Model m, Resource container, IRI containerType, Consumer<Value> consumer, Resource... contexts)
			throws ModelException {
		Objects.requireNonNull(consumer, "consumer may not be null");
		Objects.requireNonNull(m, "input model may not be null");

		ValueFactory vf = SimpleValueFactory.getInstance();

		GetStatementOptional statementSupplier = (s, p, o, c) -> m.filter(s, p, o, c).stream().findAny();
		Function<String, Supplier<ModelException>> exceptionSupplier = Models::modelException;

		// TODO add proper documentation
		Pattern annotatedMembershipPredicatePattern = Pattern.compile("^" + vf.createIRI(RDF.NAMESPACE, "_") + "[1-9][0-9]*$");

		extract(containerType, statementSupplier, container, st -> {
			if (RDFS.MEMBER.equals(st.getPredicate()) ||
				annotatedMembershipPredicatePattern.matcher(st.getPredicate().toString()).matches()) {
				consumer.accept(st.getObject());
			}
		}, exceptionSupplier, contexts);
	}

	public static void extract(IRI containerType, Model sourceModel, Resource container, Consumer<Statement> consumer, Resource... contexts) {
	}

	public static <E extends RDF4JException> void extract(IRI containerType, GetStatementOptional statementSupplier, Resource container,
														  Consumer<Statement> collectionConsumer, Function<String, Supplier<E>> exceptionSupplier,
														  Resource... contexts) throws E {
	}
}
