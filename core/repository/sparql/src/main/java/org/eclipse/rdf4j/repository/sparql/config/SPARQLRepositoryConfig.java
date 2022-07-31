/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql.config;

import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.repository.config.AbstractRepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;

/**
 * Configuration for a SPARQL endpoint.
 *
 * @author James Leigh
 */
public class SPARQLRepositoryConfig extends AbstractRepositoryImplConfig {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	public static final String NAMESPACE = "http://www.openrdf.org/config/repository/sparql#";

	/**
	 * Configuration setting for the SPARQL query endpoint. Required.
	 */
	public static final IRI QUERY_ENDPOINT = vf
			.createIRI("http://www.openrdf.org/config/repository/sparql#query-endpoint");

	/**
	 * Configuration setting for the SPARQL update endpoint. Optional.
	 */
	public static final IRI UPDATE_ENDPOINT = vf
			.createIRI("http://www.openrdf.org/config/repository/sparql#update-endpoint");

	/**
	 * Configuration setting for enabling/disabling direct result pass-through. Optional.
	 *
	 * @see SPARQLProtocolSession#isPassThroughEnabled()
	 */
	public static final IRI PASS_THROUGH_ENABLED = vf
			.createIRI("http://www.openrdf.org/config/repository/sparql#pass-through-enabled");

	private String queryEndpointUrl;

	private String updateEndpointUrl;

	private Boolean passThroughEnabled;

	public SPARQLRepositoryConfig() {
		super(SPARQLRepositoryFactory.REPOSITORY_TYPE);
	}

	public SPARQLRepositoryConfig(String queryEndpointUrl) {
		this();
		setQueryEndpointUrl(queryEndpointUrl);
	}

	public SPARQLRepositoryConfig(String queryEndpointUrl, String updateEndpointUrl) {
		this(queryEndpointUrl);
		setUpdateEndpointUrl(updateEndpointUrl);
	}

	public String getQueryEndpointUrl() {
		return queryEndpointUrl;
	}

	public void setQueryEndpointUrl(String url) {
		this.queryEndpointUrl = url;
	}

	public String getUpdateEndpointUrl() {
		return updateEndpointUrl;
	}

	public void setUpdateEndpointUrl(String url) {
		this.updateEndpointUrl = url;
	}

	@Override
	public void validate() throws RepositoryConfigException {
		super.validate();
		if (getQueryEndpointUrl() == null) {
			throw new RepositoryConfigException("No endpoint URL specified for SPARQL repository");
		}
	}

	@Override
	public Resource export(Model m) {
		Resource implNode = super.export(m);

		m.setNamespace("sparql", NAMESPACE);
		if (getQueryEndpointUrl() != null) {
			m.add(implNode, QUERY_ENDPOINT, vf.createIRI(getQueryEndpointUrl()));
		}
		if (getUpdateEndpointUrl() != null) {
			m.add(implNode, UPDATE_ENDPOINT, vf.createIRI(getUpdateEndpointUrl()));
		}
		if (getPassThroughEnabled() != null) {
			m.add(implNode, PASS_THROUGH_ENABLED, BooleanLiteral.valueOf(getPassThroughEnabled()));
		}

		return implNode;
	}

	@Override
	public void parse(Model m, Resource implNode) throws RepositoryConfigException {
		super.parse(m, implNode);

		try {
			Models.objectIRI(m.getStatements(implNode, QUERY_ENDPOINT, null))
					.ifPresent(iri -> setQueryEndpointUrl(iri.stringValue()));
			Models.objectIRI(m.getStatements(implNode, UPDATE_ENDPOINT, null))
					.ifPresent(iri -> setUpdateEndpointUrl(iri.stringValue()));
			Models.objectLiteral(m.getStatements(implNode, PASS_THROUGH_ENABLED, null))
					.ifPresent(lit -> setPassThroughEnabled(lit.booleanValue()));
		} catch (ModelException e) {
			throw new RepositoryConfigException(e.getMessage(), e);
		}
	}

	/**
	 * @return the passThroughEnabled
	 */
	public Boolean getPassThroughEnabled() {
		return passThroughEnabled;
	}

	/**
	 * @param passThroughEnabled the passThroughEnabled to set
	 */
	public void setPassThroughEnabled(Boolean passThroughEnabled) {
		this.passThroughEnabled = passThroughEnabled;
	}
}
