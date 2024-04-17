/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.repository;

import static org.eclipse.rdf4j.federated.repository.FedXRepositoryConfig.NAMESPACE;

import org.eclipse.rdf4j.federated.FedXConfig;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;

/**
 * A parser & exporter of {@link FedXConfig} to fine-tune FedX repositories when configured via
 * {@link FedXRepositoryConfig}.
 *
 * @author Iotic Labs
 */
public class FedXConfigParser {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	/**
	 * IRI of the property populating {@link FedXConfig#getEnforceMaxQueryTime()}
	 */
	public static final IRI CONFIG_ENFORCE_MAX_QUERY_TIME = vf.createIRI(NAMESPACE, "enforceMaxQueryTime");

	/**
	 * IRI of the property populating {@link FedXConfig#isEnableMonitoring()}
	 */
	public static final IRI CONFIG_ENABLE_MONITORING = vf.createIRI(NAMESPACE, "enableMonitoring");

	/**
	 * IRI of the property populating {@link FedXConfig#isLogQueryPlan()}
	 */
	public static final IRI CONFIG_LOG_QUERY_PLAN = vf.createIRI(NAMESPACE, "logQueryPlan");

	/**
	 * IRI of the property populating {@link FedXConfig#isDebugQueryPlan()}
	 */
	public static final IRI CONFIG_DEBUG_QUERY_PLAN = vf.createIRI(NAMESPACE, "debugQueryPlan");

	/**
	 * IRI of the property populating {@link FedXConfig#isLogQueries()}
	 */
	public static final IRI CONFIG_LOG_QUERIES = vf.createIRI(NAMESPACE, "logQueries");

	/**
	 * IRI of the property populating {@link FedXConfig#getSourceSelectionCacheSpec()}
	 */
	public static final IRI CONFIG_SOURCE_SELECTION_CACHE_SPEC = vf.createIRI(NAMESPACE, "sourceSelectionCacheSpec");

	private FedXConfigParser() {
	}

	/**
	 * Updates the provided {@link FedXConfig} with properties from the supplied model.
	 *
	 * @param config   the configuration to be amended.
	 * @param m        the model from which to read configuration properties
	 * @param confNode the subject against which to expect {@link FedXConfig} overrides.
	 *
	 * @return The updated {@link FedXConfig}
	 *
	 * @throws RepositoryConfigException if any of the overridden fields are deemed to be invalid
	 */
	public static FedXConfig parse(FedXConfig config, Model m, Resource confNode) throws RepositoryConfigException {
		Models.objectLiteral(m.getStatements(confNode, CONFIG_ENFORCE_MAX_QUERY_TIME, null))
				.ifPresent(value -> config.withEnforceMaxQueryTime(value.intValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_ENABLE_MONITORING, null))
				.ifPresent(value -> config.withEnableMonitoring(value.booleanValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_LOG_QUERY_PLAN, null))
				.ifPresent(value -> config.withLogQueryPlan(value.booleanValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_DEBUG_QUERY_PLAN, null))
				.ifPresent(value -> config.withDebugQueryPlan(value.booleanValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_LOG_QUERIES, null))
				.ifPresent(value -> config.withLogQueries(value.booleanValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_SOURCE_SELECTION_CACHE_SPEC, null))
				.ifPresent(value -> config.withSourceSelectionCacheSpec(value.stringValue()));

		return config;
	}

	/**
	 * Export the provided {@link FedXConfig} to its RDF representation.
	 *
	 * @param config the configuration to export
	 * @param m      the model to which to write configuration properties
	 *
	 * @return the node against which the configuration has been written
	 */
	public static Resource export(FedXConfig config, Model m) {
		BNode confNode = Values.bnode();

		m.add(confNode, CONFIG_ENFORCE_MAX_QUERY_TIME, vf.createLiteral(config.getEnforceMaxQueryTime()));

		m.add(confNode, CONFIG_ENABLE_MONITORING, vf.createLiteral(config.isEnableMonitoring()));

		m.add(confNode, CONFIG_LOG_QUERY_PLAN, vf.createLiteral(config.isLogQueryPlan()));

		m.add(confNode, CONFIG_DEBUG_QUERY_PLAN, vf.createLiteral(config.isDebugQueryPlan()));

		m.add(confNode, CONFIG_LOG_QUERIES, vf.createLiteral(config.isLogQueries()));

		if (config.getSourceSelectionCacheSpec() != null) {
			m.add(confNode, CONFIG_SOURCE_SELECTION_CACHE_SPEC, vf.createLiteral(config.getSourceSelectionCacheSpec()));
		}

		return confNode;
	}
}
