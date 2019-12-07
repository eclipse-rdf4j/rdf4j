/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.util;

import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.federated.FedXConfig;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.repository.FedXRepositoryConnection;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.repository.sail.SailQuery;

/**
 * General utility functions
 * 
 * @author Andreas Schwarte
 * @since 5.0
 */
public class FedXUtil {

	private static final AtomicLong count = new AtomicLong(0L);

	/**
	 * @param iri
	 * @return the IRI for the full URI string
	 */
	public static IRI iri(String iri) {
		return valueFactory().createIRI(iri);
	}

	/**
	 * 
	 * @param literal
	 * @return the string literal
	 */
	public static Literal literal(String literal) {
		return valueFactory().createLiteral(literal);
	}

	/**
	 * 
	 * @return a {@link SimpleValueFactory} instance
	 */
	public static ValueFactory valueFactory() {
		return SimpleValueFactory.getInstance();
	}

	/**
	 * Apply query bindings to transfer information from the query into the evaluation routine, e.g. the query execution
	 * time.
	 * 
	 * @param query
	 */
	public static void applyQueryBindings(SailQuery query) {
		query.setBinding(FedXRepositoryConnection.BINDING_ORIGINAL_MAX_EXECUTION_TIME,
				FedXUtil.valueFactory().createLiteral(query.getMaxExecutionTime()));
	}

	/**
	 * Hexadecimal representation of an incremental integer.
	 * 
	 * @return an incremental hex UUID
	 */
	public static String getIncrementalUUID() {
		long id = count.incrementAndGet();
		return Long.toHexString(id);
	}

	/**
	 * Set a maximum execution time corresponding to {@link FedXConfig#getEnforceMaxQueryTime()} to this operation.
	 * 
	 * Note that this is an upper bound only as FedX applies other means for evaluation the maximum query execution
	 * time.
	 * 
	 * @param operation         the {@link Operation}
	 * @param federationContext the {@link FederationContext}
	 */
	public static void applyMaxQueryExecutionTime(Operation operation, FederationContext federationContext) {
		int maxExecutionTime = federationContext.getConfig().getEnforceMaxQueryTime();
		if (maxExecutionTime <= 0) {
			return;
		}
		operation.setMaxExecutionTime(maxExecutionTime);
	}
}
