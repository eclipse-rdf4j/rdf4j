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
import org.eclipse.rdf4j.sparqlbuilder.core.QueryElementCollection;

/**
 * An RDF predicate-object list collection
 *
 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#predObjLists"> Predicate-Object Lists</a>
 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#objLists"> Object Lists</a>
 */
public class RdfPredicateObjectListCollection extends QueryElementCollection<RdfPredicateObjectList> {
	private static final String DELIMITER = " ;\n    ";

	RdfPredicateObjectListCollection() {
		super(DELIMITER);
	}

	/**
	 * add predicate-object lists to this collection
	 *
	 * @param predicate the predicate of the predicate-object list to add
	 * @param objects   the object or objects to add
	 *
	 * @return this instance
	 */
	public RdfPredicateObjectListCollection andHas(RdfPredicate predicate, RdfObject... objects) {
		return andHas(Rdf.predicateObjectList(predicate, objects));
	}

	/**
	 * Add a predicate path with an object list to this collection.
	 *
	 * @param propertyPathConfigurer an object that configures the path
	 * @param objects                the objects to add
	 * @return
	 */
	public RdfPredicateObjectListCollection andHas(Consumer<EmptyPropertyPathBuilder> propertyPathConfigurer,
			RdfObject... objects) {
		EmptyPropertyPathBuilder pathBuilder = new EmptyPropertyPathBuilder();
		propertyPathConfigurer.accept(pathBuilder);
		return andHas(Rdf.predicateObjectList(pathBuilder.build(), objects));
	}

	/**
	 * add predicate-object lists to this collection
	 *
	 * @param predicate the predicate of the predicate-object list to add
	 * @param objects   the object or objects to add
	 *
	 * @return this instance
	 */
	public RdfPredicateObjectListCollection andHas(IRI predicate, RdfObject... objects) {
		return andHas(Rdf.predicateObjectList(Rdf.iri(predicate), objects));
	}

	/**
	 * add predicate-object lists to this collection
	 *
	 * @param lists the {@link RdfPredicateObjectList}'s to add to this collection
	 * @return this instance
	 */
	public RdfPredicateObjectListCollection andHas(RdfPredicateObjectList... lists) {
		addElements(lists);

		return this;
	}

	// TODO add suffix for if elements.size > 1; here or in triplessamesubject
}
