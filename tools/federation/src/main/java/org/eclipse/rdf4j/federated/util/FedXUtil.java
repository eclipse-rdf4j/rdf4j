/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.util;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.federated.FedXConfig;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.repository.FedXRepositoryConnection;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.sail.SailQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * General utility functions
 *
 * @author Andreas Schwarte
 * @since 5.0
 */
public class FedXUtil {

	private static final Logger log = LoggerFactory.getLogger(FedXUtil.class);

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

	/**
	 * Convert the given contexts to a {@link Dataset} representation.
	 *
	 * @param contexts
	 * @return
	 */
	public static Dataset toDataset(Resource[] contexts) {
		SimpleDataset dataset = new SimpleDataset();
		for (Resource context : contexts) {
			if (!(context instanceof IRI)) {
				log.warn("FedX does not support to use non-IRIs as context identifier. Ignoring {}", context);
				continue;
			}
			dataset.addDefaultGraph((IRI) context);
		}
		return dataset;
	}

	/**
	 * Convert the given {@link Dataset} to an array of contexts
	 *
	 * @param ds
	 * @return
	 */
	public static Resource[] toContexts(Dataset ds) {
		if (ds == null) {
			return new Resource[0];
		}
		return ds.getDefaultGraphs().toArray(new Resource[0]);
	}

	/**
	 * Retrieve the contexts from the {@link StatementPattern} and {@link Dataset}.
	 *
	 * @param stmt
	 * @param dataset
	 * @return
	 */
	public static Resource[] toContexts(StatementPattern stmt, Dataset dataset) {
		if (dataset == null && (stmt.getContextVar() == null || !stmt.getContextVar().hasValue())) {
			return new Resource[0];
		}

		Set<Resource> contexts = Sets.newHashSet();

		if (dataset != null) {
			contexts.addAll(dataset.getDefaultGraphs());
		}

		if (stmt.getScope().equals(Scope.NAMED_CONTEXTS)) {
			if (stmt.getContextVar().hasValue()) {
				contexts.add((Resource) stmt.getContextVar().getValue());
			}
		}

		return contexts.toArray(new Resource[contexts.size()]);
	}

	/**
	 * Returns a {@link Dataset} representation of the given {@link StatementPattern} and {@link Dataset}.
	 * <p>
	 * If the {@link StatementPattern} does not have a context value, the {@link Dataset} is returned as-is, which may
	 * also be <code>null</code>.
	 * </p>
	 *
	 * <p>
	 * Otherwise the newly constructed {@link Dataset} contains all information from the original one plus the context
	 * from the statement.
	 * </p>
	 *
	 * @param stmt
	 * @param dataset
	 * @return
	 */
	public static Dataset toDataset(StatementPattern stmt, Dataset dataset) {
		if (stmt.getContextVar() == null || !stmt.getContextVar().hasValue()) {
			return dataset;
		}
		SimpleDataset res = new SimpleDataset();
		if (dataset != null) {
			dataset.getDefaultGraphs().forEach(iri -> res.addDefaultGraph(iri));
			dataset.getNamedGraphs().forEach(iri -> res.addNamedGraph(iri));
			dataset.getDefaultRemoveGraphs().forEach(iri -> res.addDefaultRemoveGraph(iri));
			res.setDefaultInsertGraph(dataset.getDefaultInsertGraph());
		}
		Value stmtContext = stmt.getContextVar().getValue();
		if (stmtContext instanceof IRI) {
			res.addDefaultGraph((IRI) stmtContext);
		} else {
			log.warn("FedX named graph handling does not support non-IRIs: " + stmtContext);
		}
		return res;
	}
}
