/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.rdf;

import java.util.function.Consumer;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.EmptyPropertyPathBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.StandardQueryElementCollection;

/**
 * A Predicate-Object List
 *
 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#predObjLists"> SPARQL Predicate-Object List</a>
 */
public class RdfPredicateObjectList extends StandardQueryElementCollection<RdfObject> {
	/**
	 * Build a predicate-object list.
	 *
	 * @param predicate
	 * @param objects
	 */
	RdfPredicateObjectList(RdfPredicate predicate, RdfObject... objects) {
		super(predicate.getQueryString(), ", ");
		printNameIfEmpty(false);
		and(objects);
	}

	/**
	 * Build a predicate-object list.
	 *
	 * @param predicate
	 * @param objects
	 */
	RdfPredicateObjectList(IRI predicate, RdfObject... objects) {
		this(Rdf.iri(predicate), objects);
	}

	/**
	 * Build a predicate path with an object list.
	 *
	 * @param propertyPathConfigurer
	 * @param objects
	 */
	RdfPredicateObjectList(Consumer<EmptyPropertyPathBuilder> propertyPathConfigurer, RdfObject... objects) {
		super(buildPath(propertyPathConfigurer), ", ");
		printNameIfEmpty(false);
		and(objects);
	}

	private static String buildPath(Consumer<EmptyPropertyPathBuilder> propertyPathConfigurer) {
		EmptyPropertyPathBuilder pathBuilder = new EmptyPropertyPathBuilder();
		propertyPathConfigurer.accept(pathBuilder);
		return pathBuilder.build().getQueryString();
	}

	/**
	 * Add {@link RdfObject} instances to this predicate-object list
	 *
	 * @param objects the objects to add to this list
	 *
	 * @return this {@link RdfPredicateObjectList} instance
	 */
	public RdfPredicateObjectList and(RdfObject... objects) {
		addElements(objects);

		return this;
	}
}
