/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.cache;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.optimizer.SourceSelection;
import org.eclipse.rdf4j.federated.structures.SubQuery;

/**
 * Describes a cache that can be used for {@link SourceSelection} to reduce the number of remote requests.
 *
 * @author Andreas Schwarte
 */
public interface SourceSelectionCache {

	enum StatementSourceAssurance {
		/**
		 * The endpoint does <b>not</b> provide any information
		 */
		NONE,
		/**
		 * The endpoint has data for a given query pattern
		 */
		HAS_REMOTE_STATEMENTS,
		/**
		 * No local information available: a remote check needs to be performed to check if a data source can provide
		 * statements
		 */
		POSSIBLY_HAS_STATEMENTS
	}

	/**
	 * Ask the cache if a given endpoint can provide results for a {@link SubQuery}.
	 * <p>
	 * Implementations may infer information by applying logical rules, e.g. if a cache knows that an endpoint can
	 * provide statements {s, foaf:name, "Alan"}, it can also provide results for {s, foaf:name, ?name}.
	 * </p>
	 *
	 * <p>
	 * If a cache cannot provide information for the given arguments, it must return
	 * {@link StatementSourceAssurance#POSSIBLY_HAS_STATEMENTS} in order to trigger a remote check.</p
	 *
	 * @param subQuery
	 * @param endpoint
	 * @return the {@link StatementSourceAssurance}
	 */
	StatementSourceAssurance getAssurance(SubQuery subQuery, Endpoint endpoint);

	/**
	 * Update the information for a given {@link SubQuery} and {@link Endpoint}.
	 *
	 * <p>
	 * Implementations must make sure that any operations are thread-safe
	 * </p>
	 *
	 * @param subQuery
	 * @param endpoint
	 * @param hasStatements
	 */
	void updateInformation(SubQuery subQuery, Endpoint endpoint, boolean hasStatements);
}
