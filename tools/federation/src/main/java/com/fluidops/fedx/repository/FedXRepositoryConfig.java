/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.repository;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.repository.config.AbstractRepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;

/**
 * A {@link RepositoryImplConfig} to configure FedX for the use in the RDF4J
 * workbench.
 * 
 * Example configuration file:
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
 *   fedx:fedxConfig "fedxConfig.prop" ;
 *   fedx:dataConfig "dataConfig.ttl" ;
 * ];
 * rep:repositoryID "fedx" ;
 * rdfs:label "FedX Federation" .
 * </pre>
 * 
 * <p>
 * Note that the location of the fedx config and the data config is relative to
 * the repository's data dir (as managed by the RDF4J repository manager)
 * </p>
 * 
 * @author Andreas Schwarte
 *
 */
public class FedXRepositoryConfig extends AbstractRepositoryImplConfig {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	/**
	 * FedX schema namespace (<tt>http://www.fluidops.com/config/fedx#</tt>).
	 */
	public static final String NAMESPACE = "http://www.fluidops.com/config/fedx#";

	/**
	 * IRI of the property pointing to the FedX configuration
	 */
	public static final IRI FEDX_CONFIG = vf.createIRI(NAMESPACE, "fedxConfig");

	/**
	 * IRI of the property pointing to the FedX data config
	 */
	public static final IRI DATA_CONFIG = vf.createIRI(NAMESPACE, "dataConfig");

	/**
	 * the location of the fedx configuration
	 */
	private String fedxConfig;

	/**
	 * the location of the data configuration
	 */
	private String dataConfig;

	public FedXRepositoryConfig() {
		super(FedXRepositoryFactory.REPOSITORY_TYPE);
	}

	/**
	 * @return the location of the FedX configuration
	 */
	public String getFedxConfig() {
		return fedxConfig;
	}

	/**
	 * Set the location of the FedX configuration
	 */
	public void setFedxConfig(String fedxConfig) {
		this.fedxConfig = fedxConfig;
	}

	public String getDataConfig() {
		return dataConfig;
	}

	public void setDataConfig(String dataConfig) {
		this.dataConfig = dataConfig;
	}

	@Override
	public Resource export(Model m) {

		Resource implNode = super.export(m);
		
		m.setNamespace("fedx", NAMESPACE);
		if (getFedxConfig() != null) {
			m.add(implNode, FEDX_CONFIG, vf.createLiteral(getFedxConfig()));
		}
		if (getDataConfig() != null) {
			m.add(implNode, DATA_CONFIG, vf.createLiteral(getDataConfig()));
		}

		return implNode;
	}

	@Override
	public void validate() throws RepositoryConfigException {
		super.validate();

		if (getDataConfig() == null && getFedxConfig() == null) {
			throw new RepositoryConfigException(
					"At least one of fedxConfig or dataConfig needs to be "
					+ "provided to initialize the federation");
		}
	}

	@Override
	public void parse(Model m, Resource implNode) throws RepositoryConfigException {
		super.parse(m, implNode);

		try {
			Models.objectLiteral(m.filter(implNode, FEDX_CONFIG, null))
					.ifPresent(value -> setFedxConfig(value.stringValue()));
			Models.objectLiteral(m.filter(implNode, DATA_CONFIG, null))
					.ifPresent(value -> setDataConfig(value.stringValue()));
		} catch (ModelException e) {
			throw new RepositoryConfigException(e.getMessage(), e);
		}
	}
}
