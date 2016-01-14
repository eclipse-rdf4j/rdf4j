/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleIRI;
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

	public static final IRI QUERY_ENDPOINT = vf.createIRI(
			"http://www.openrdf.org/config/repository/sparql#query-endpoint");

	public static final IRI UPDATE_ENDPOINT = vf.createIRI(
			"http://www.openrdf.org/config/repository/sparql#update-endpoint");

	private String queryEndpointUrl;

	private String updateEndpointUrl;

	public SPARQLRepositoryConfig() {
		super(SPARQLRepositoryFactory.REPOSITORY_TYPE);
	}

	public SPARQLRepositoryConfig(String queryEndpointUrl) {
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
	public void validate()
		throws RepositoryConfigException
	{
		super.validate();
		if (getQueryEndpointUrl() == null) {
			throw new RepositoryConfigException("No endpoint URL specified for SPARQL repository");
		}
	}

	@Override
	public Resource export(Model m) {
		Resource implNode = super.export(m);

		if (getQueryEndpointUrl() != null) {
			m.add(implNode, QUERY_ENDPOINT, vf.createIRI(getQueryEndpointUrl()));
		}
		if (getUpdateEndpointUrl() != null) {
			m.add(implNode, UPDATE_ENDPOINT, vf.createIRI(getUpdateEndpointUrl()));
		}

		return implNode;
	}

	@Override
	public void parse(Model m, Resource implNode)
		throws RepositoryConfigException
	{
		super.parse(m, implNode);

		try {
			Models.objectIRI(m.filter(implNode, QUERY_ENDPOINT, null)).ifPresent(
					iri -> setQueryEndpointUrl(iri.stringValue()));
			Models.objectIRI(m.filter(implNode, UPDATE_ENDPOINT, null)).ifPresent(
					iri -> setUpdateEndpointUrl(iri.stringValue()));
		}
		catch (ModelException e) {
			throw new RepositoryConfigException(e.getMessage(), e);
		}
	}
}
