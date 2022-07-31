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
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
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
	 * IRI of the property pointing to a federation member node
	 */
	public static final IRI MEMBER = vf.createIRI(NAMESPACE, "member");

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
}
