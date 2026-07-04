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
package org.eclipse.rdf4j.repository.http.config;

import static org.eclipse.rdf4j.repository.http.config.HTTPRepositorySchema.REPOSITORYURL;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Configurations;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
import org.eclipse.rdf4j.repository.config.AbstractRepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;

/**
 * @author Arjohn Kampman
 */
public class HTTPRepositoryConfig extends AbstractRepositoryImplConfig {

	private String url;

	private String username;

	private String password;

	public HTTPRepositoryConfig() {
		super(HTTPRepositoryFactory.REPOSITORY_TYPE);
	}

	public HTTPRepositoryConfig(String url) {
		this();
		setURL(url);
	}

	public String getURL() {
		return url;
	}

	public void setURL(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public void validate() throws RepositoryConfigException {
		super.validate();
		if (url == null) {
			throw new RepositoryConfigException("No URL specified for HTTP repository");
		}
	}

	@Override
	public Resource export(Model graph) {
		if (Configurations.useLegacyConfig()) {
			return exportLegacy(graph);
		}

		Resource implNode = super.export(graph);

		if (url != null) {
			graph.setNamespace(CONFIG.NS);
			graph.add(implNode, CONFIG.Http.url, SimpleValueFactory.getInstance().createIRI(url));

		}
		return implNode;
	}

	private Resource exportLegacy(Model graph) {
		Resource implNode = super.export(graph);

		if (url != null) {
			graph.setNamespace("http", HTTPRepositorySchema.NAMESPACE);
			graph.add(implNode, REPOSITORYURL, SimpleValueFactory.getInstance().createIRI(url));

		}
		return implNode;
	}

	@Override
	public void parse(Model model, Resource implNode) throws RepositoryConfigException {
		super.parse(model, implNode);

		try {

			Configurations
					.getIRIValue(model, implNode, CONFIG.Http.url, REPOSITORYURL)
					.ifPresent(iri -> setURL(iri.stringValue()));

			Configurations
					.getLiteralValue(model, implNode, CONFIG.Http.username, HTTPRepositorySchema.USERNAME)
					.ifPresent(username -> setUsername(username.getLabel()));

			Configurations
					.getLiteralValue(model, implNode, CONFIG.Http.password, HTTPRepositorySchema.PASSWORD)
					.ifPresent(password -> setPassword(password.getLabel()));

		} catch (ModelException e) {
			throw new RepositoryConfigException(e.getMessage(), e);
		}
	}
}
