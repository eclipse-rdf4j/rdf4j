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
package org.eclipse.rdf4j.federated.repository;

import java.util.Set;

import org.eclipse.rdf4j.federated.FedXConfig;
import org.eclipse.rdf4j.federated.util.Vocabulary.FEDX;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.config.AbstractRepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;

/**
 * A {@link RepositoryImplConfig} to configure FedX for the use in the RDF4J workbench.
 *
 * <p>
 * Federation member repositories (e.g. NativeStore or SPARQL endpoints) can be managed in the RDF4J Workbench, and
 * referenced as members in the federation. Alternatively, FedX can manage repositories, please refer to the
 * documentation for <i>data configuration</i>.
 * </p>
 * <p>
 * Example configuration file:
 * </p>
 *
 * <pre>
 * # RDF4J configuration template for a FedX Repository
 *
 * &#64;prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
 * &#64;prefix rep: <http://www.openrdf.org/config/repository#>.
 * &#64;prefix fedx: <http://www.fluidops.com/config/fedx#>.
 *
 * [] a rep:Repository ;
 * rep:repositoryImpl [
 *   rep:repositoryType "fedx:FedXRepository" ;
 *   fedx:member [
 *      fedx:store "ResolvableRepository" ;
 *      fedx:repositoryName "endpoint1"
 *   ],
 *   [
 *      fedx:store "ResolvableRepository" ;
 *      fedx:repositoryName "endpoint2"
 *   ]
 *   # optionally define data config
 *   #fedx:fedxConfig "fedxConfig.prop" ;
 *   fedx:dataConfig "dataConfig.ttl" ;
 *
 *   # optionally define FedXConfig overrides
 *   fedx:config [
 *      fedx:sourceSelectionCacheSpec "maximumSize=0" ;
 *      fedx:enforceMaxQueryTime 30 ;
 *   ]
 * ];
 * rep:repositoryID "fedx" ;
 * rdfs:label "FedX Federation" .
 * </pre>
 *
 * <p>
 * Note that the location of the fedx config and the data config is relative to the repository's data dir (as managed by
 * the RDF4J repository manager)
 * </p>
 *
 * @author Andreas Schwarte
 *
 */
public class FedXRepositoryConfig extends AbstractRepositoryImplConfig {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	/**
	 * FedX schema namespace (<var>http://rdf4j.org/config/federation#</var>).
	 */
	public static final String NAMESPACE = FEDX.NAMESPACE;

	/**
	 * IRI of the property pointing to the FedX data config
	 */
	public static final IRI DATA_CONFIG = vf.createIRI(NAMESPACE, "dataConfig");

	/**
	 * IRI of the property pointing to the {@link FedXConfig}
	 */
	public static final IRI FEDX_CONFIG = vf.createIRI(NAMESPACE, "config");

	/**
	 * IRI of the property pointing to a federation member node
	 */
	public static final IRI MEMBER = vf.createIRI(NAMESPACE, "member");

	/**
	 * IRI of the property populating {@link FedXConfig#getJoinWorkerThreads()}
	 */
	public static final IRI CONFIG_JOIN_WORKER_THREADS = vf.createIRI(NAMESPACE, "joinWorkerThreads");

	/**
	 * IRI of the property populating {@link FedXConfig#getUnionWorkerThreads()}
	 */
	public static final IRI CONFIG_UNION_WORKER_THREADS = vf.createIRI(NAMESPACE, "unionWorkerThreads");

	/**
	 * IRI of the property populating {@link FedXConfig#getLeftJoinWorkerThreads()}
	 */
	public static final IRI CONFIG_LEFT_JOIN_WORKER_THREADS = vf.createIRI(NAMESPACE, "leftJoinWorkerThreads");

	/**
	 * IRI of the property populating {@link FedXConfig#getBoundJoinBlockSize()}
	 */
	public static final IRI CONFIG_BOUND_JOIN_BLOCK_SIZE = vf.createIRI(NAMESPACE, "boundJoinBlockSize");

	/**
	 * IRI of the property populating {@link FedXConfig#getEnforceMaxQueryTime()}
	 */
	public static final IRI CONFIG_ENFORCE_MAX_QUERY_TIME = vf.createIRI(NAMESPACE, "enforceMaxQueryTime");

	/**
	 * IRI of the property populating {@link FedXConfig#getEnableServiceAsBoundJoin()}
	 */
	public static final IRI CONFIG_ENABLE_SERVICE_AS_BOUND_JOIN = vf.createIRI(NAMESPACE, "enableServiceAsBoundJoin");

	/**
	 * IRI of the property populating {@link FedXConfig#isEnableOptionalAsBindJoin()}
	 */
	public static final IRI CONFIG_ENABLE_OPTIONAL_AS_BIND_JOIN = vf.createIRI(NAMESPACE, "enableOptionalAsBindJoin");

	/**
	 * IRI of the property populating {@link FedXConfig#isEnableMonitoring()}
	 */
	public static final IRI CONFIG_ENABLE_MONITORING = vf.createIRI(NAMESPACE, "enableMonitoring");

	/**
	 * IRI of the property populating {@link FedXConfig#isLogQueryPlan()}
	 */
	public static final IRI CONFIG_LOG_QUERY_PLAN = vf.createIRI(NAMESPACE, "logQueryPlan");

	/**
	 * IRI of the property populating {@link FedXConfig#isLogQueries()}
	 */
	public static final IRI CONFIG_LOG_QUERIES = vf.createIRI(NAMESPACE, "logQueries");

	/**
	 * IRI of the property populating {@link FedXConfig#isDebugQueryPlan()}
	 */
	public static final IRI CONFIG_DEBUG_QUERY_PLAN = vf.createIRI(NAMESPACE, "debugQueryPlan");

	/**
	 * IRI of the property populating {@link FedXConfig#getIncludeInferredDefault()}
	 */
	public static final IRI CONFIG_INCLUDE_INFERRED_DEFAULT = vf.createIRI(NAMESPACE, "includeInferredDefault");

	/**
	 * IRI of the property populating {@link FedXConfig#getSourceSelectionCacheSpec()}
	 */
	public static final IRI CONFIG_SOURCE_SELECTION_CACHE_SPEC = vf.createIRI(NAMESPACE, "sourceSelectionCacheSpec");

	/**
	 * IRI of the property populating {@link FedXConfig#getPrefixDeclarations()}
	 */
	public static final IRI CONFIG_PREFIX_DECLARATIONS = vf.createIRI(NAMESPACE, "prefixDeclarations");

	/**
	 * IRI of the property populating {@link FedXConfig#getConsumingIterationMax()}
	 */
	public static final IRI CONFIG_CONSUMING_ITERATION_MAX = vf.createIRI(NAMESPACE, "consumingIterationMax");

	/**
	 * the location of the data configuration
	 */
	private String dataConfig;

	/**
	 * the model representing the members
	 *
	 * <pre>
	 * :member1 fedx:store "ResolvableRepository" ;
	 * 		fedx:repositoryName "endpoint1" .
	 * :member2 fedx:store "ResolvableRepository" ;
	 * 		fedx:repositoryName "endpoint2" .
	 * </pre>
	 */
	private Model members;

	/**
	 * Initialized {@link FedXConfig}
	 */
	private FedXConfig config;

	public FedXRepositoryConfig() {
		super(FedXRepositoryFactory.REPOSITORY_TYPE);
	}

	public String getDataConfig() {
		return dataConfig;
	}

	public void setDataConfig(String dataConfig) {
		this.dataConfig = dataConfig;
	}

	public Model getMembers() {
		return this.members;
	}

	public void setMembers(Model members) {
		this.members = members;
	}

	public FedXConfig getConfig() {
		return config;
	}

	public void setConfig(FedXConfig config) {
		this.config = config;
	}

	@Override
	public Resource export(Model m) {

		Resource implNode = super.export(m);

		m.setNamespace("fedx", NAMESPACE);
		if (getDataConfig() != null) {
			m.add(implNode, DATA_CONFIG, vf.createLiteral(getDataConfig()));
		}

		exportFedXConfig(m, implNode);

		if (getMembers() != null) {

			Model members = getMembers();
			Set<Resource> memberNodes = members.subjects();
			for (Resource memberNode : memberNodes) {
				m.add(implNode, MEMBER, memberNode);
				m.addAll(members.filter(memberNode, null, null));
			}
		}

		return implNode;
	}

	@Override
	public void validate() throws RepositoryConfigException {
		super.validate();

		if (getMembers() == null) {
			if (getDataConfig() == null) {
				throw new RepositoryConfigException(
						"DataConfig needs to be "
								+ "provided to initialize the federation, if no explicit members are defined");
			}
		}

	}

	@Override
	public void parse(Model m, Resource implNode) throws RepositoryConfigException {
		super.parse(m, implNode);

		try {
			Models.objectLiteral(m.getStatements(implNode, DATA_CONFIG, null))
					.ifPresent(value -> setDataConfig(value.stringValue()));

			parseFedXConfig(m, implNode);

			Set<Value> memberNodes = m.filter(implNode, MEMBER, null).objects();
			if (!memberNodes.isEmpty()) {
				Model members = new TreeModel();

				// add all statements for the given member node
				for (Value memberNode : memberNodes) {
					if (!(memberNode instanceof Resource)) {
						throw new RepositoryConfigException("Member nodes must be of type resource, was " + memberNode);
					}
					members.addAll(m.filter((Resource) memberNode, null, null));
				}

				this.members = members;
			}
		} catch (ModelException e) {
			throw new RepositoryConfigException(e.getMessage(), e);
		}
	}

	/**
	 * Updates the container {@link FedXConfig} instance with properties from the supplied model. It is up to the caller
	 * to retrieve configuration from {@link #FEDX_CONFIG} as well as to initialise the parsed configuration (via
	 * {@link #setConfig(FedXConfig)}) since it can be null.
	 *
	 * @param m        the model from which to read configuration properties
	 * @param implNode the subject against which to expect the {@link #FEDX_CONFIG} property.
	 *
	 * @throws RepositoryConfigException if any of the overridden fields are deemed to be invalid
	 */
	protected void parseFedXConfig(Model m, Resource implNode) throws RepositoryConfigException {
		Models.objectResource(m.getStatements(implNode, FEDX_CONFIG, null))
				.ifPresent(res -> parseFedXConfigInternal(m, res));
	}

	private void parseFedXConfigInternal(Model m, Resource confNode) throws RepositoryConfigException {
		if (getConfig() == null) {
			setConfig(new FedXConfig());
		}

		Models.objectLiteral(m.getStatements(confNode, CONFIG_JOIN_WORKER_THREADS, null))
				.ifPresent(value -> config.withJoinWorkerThreads(value.intValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_UNION_WORKER_THREADS, null))
				.ifPresent(value -> config.withUnionWorkerThreads(value.intValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_LEFT_JOIN_WORKER_THREADS, null))
				.ifPresent(value -> config.withLeftJoinWorkerThreads(value.intValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_BOUND_JOIN_BLOCK_SIZE, null))
				.ifPresent(value -> config.withBoundJoinBlockSize(value.intValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_ENFORCE_MAX_QUERY_TIME, null))
				.ifPresent(value -> config.withEnforceMaxQueryTime(value.intValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_ENABLE_SERVICE_AS_BOUND_JOIN, null))
				.ifPresent(value -> config.withEnableServiceAsBoundJoin(value.booleanValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_ENABLE_OPTIONAL_AS_BIND_JOIN, null))
				.ifPresent(value -> config.withEnableOptionalAsBindJoin(value.booleanValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_ENABLE_MONITORING, null))
				.ifPresent(value -> config.withEnableMonitoring(value.booleanValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_LOG_QUERY_PLAN, null))
				.ifPresent(value -> config.withLogQueryPlan(value.booleanValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_LOG_QUERIES, null))
				.ifPresent(value -> config.withLogQueries(value.booleanValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_DEBUG_QUERY_PLAN, null))
				.ifPresent(value -> config.withDebugQueryPlan(value.booleanValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_INCLUDE_INFERRED_DEFAULT, null))
				.ifPresent(value -> config.withIncludeInferredDefault(value.booleanValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_SOURCE_SELECTION_CACHE_SPEC, null))
				.ifPresent(value -> config.withSourceSelectionCacheSpec(value.stringValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_PREFIX_DECLARATIONS, null))
				.ifPresent(value -> config.withPrefixDeclarations(value.stringValue()));

		Models.objectLiteral(m.getStatements(confNode, CONFIG_CONSUMING_ITERATION_MAX, null))
				.ifPresent(value -> config.withConsumingIterationMax(value.intValue()));

	}

	/**
	 * Export the provided {@link FedXConfig} to its RDF representation. Note that {@link #getConfig()} could be null if
	 * configuration has not been set yet.
	 *
	 * @param config   the configuration to export
	 * @param implNode the node to which to write the config reference (i.e. {@link #FEDX_CONFIG}) to
	 */
	protected void exportFedXConfig(Model model, Resource implNode) {
		if (getConfig() == null) {
			return;
		}

		BNode confNode = Values.bnode();

		model.add(confNode, CONFIG_JOIN_WORKER_THREADS, vf.createLiteral(config.getJoinWorkerThreads()));

		model.add(confNode, CONFIG_UNION_WORKER_THREADS, vf.createLiteral(config.getUnionWorkerThreads()));

		model.add(confNode, CONFIG_LEFT_JOIN_WORKER_THREADS, vf.createLiteral(config.getLeftJoinWorkerThreads()));

		model.add(confNode, CONFIG_BOUND_JOIN_BLOCK_SIZE, vf.createLiteral(config.getBoundJoinBlockSize()));

		model.add(confNode, CONFIG_ENFORCE_MAX_QUERY_TIME, vf.createLiteral(config.getEnforceMaxQueryTime()));

		model.add(confNode, CONFIG_ENABLE_SERVICE_AS_BOUND_JOIN,
				vf.createLiteral(config.getEnableServiceAsBoundJoin()));

		model.add(confNode, CONFIG_ENABLE_OPTIONAL_AS_BIND_JOIN,
				vf.createLiteral(config.isEnableOptionalAsBindJoin()));

		model.add(confNode, CONFIG_ENABLE_MONITORING, vf.createLiteral(config.isEnableMonitoring()));

		model.add(confNode, CONFIG_LOG_QUERY_PLAN, vf.createLiteral(config.isLogQueryPlan()));

		model.add(confNode, CONFIG_LOG_QUERIES, vf.createLiteral(config.isLogQueries()));

		model.add(confNode, CONFIG_DEBUG_QUERY_PLAN, vf.createLiteral(config.isDebugQueryPlan()));

		model.add(confNode, CONFIG_INCLUDE_INFERRED_DEFAULT, vf.createLiteral(config.getIncludeInferredDefault()));

		if (config.getSourceSelectionCacheSpec() != null) {
			model.add(confNode, CONFIG_SOURCE_SELECTION_CACHE_SPEC,
					vf.createLiteral(config.getSourceSelectionCacheSpec()));
		}

		if (config.getPrefixDeclarations() != null) {
			model.add(confNode, CONFIG_PREFIX_DECLARATIONS,
					vf.createLiteral(config.getPrefixDeclarations()));
		}

		model.add(confNode, CONFIG_CONSUMING_ITERATION_MAX, vf.createLiteral(config.getConsumingIterationMax()));

		model.add(implNode, FEDX_CONFIG, confNode);
	}
}
