/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.model.util;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

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

		assert validType: "containerType should be one of ALT, BAG or SEQ";

		Statements.consume(vf, current, RDF.TYPE, containerType, consumer, contexts);

		Iterator<?> iter = values.iterator();
		int elementCounter = 1;
		while (iter.hasNext()) {
			Object o = iter.next();
			Value v = o instanceof Value ? (Value) o : Literals.createLiteralOrFail(vf, o);
			IRI elementCounterPredicate = vf.createIRI(RDF.NAMESPACE, "_" + elementCounter);
			elementCounter++;
			Statements.consume(vf, current, elementCounterPredicate, v, consumer, contexts);
			Statements.consume(vf, (Resource) v, RDFS.MEMBER, current, consumer, contexts);
		}
	}
}
