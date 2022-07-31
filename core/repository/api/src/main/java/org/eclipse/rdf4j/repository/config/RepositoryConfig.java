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
package org.eclipse.rdf4j.repository.config;

import static org.eclipse.rdf4j.repository.config.RepositoryConfigSchema.NAMESPACE;
import static org.eclipse.rdf4j.repository.config.RepositoryConfigSchema.REPOSITORY;
import static org.eclipse.rdf4j.repository.config.RepositoryConfigSchema.REPOSITORYID;
import static org.eclipse.rdf4j.repository.config.RepositoryConfigSchema.REPOSITORYIMPL;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * @author Arjohn Kampman
 */
public class RepositoryConfig {

	private String id;

	private String title;

	private RepositoryImplConfig implConfig;

	/**
	 * Create a new RepositoryConfig.
	 */
	public RepositoryConfig() {
	}

	/**
	 * Create a new RepositoryConfigImpl.
	 */
	public RepositoryConfig(String id) {
		this();
		setID(id);
	}

	/**
	 * Create a new RepositoryConfigImpl.
	 */
	public RepositoryConfig(String id, RepositoryImplConfig implConfig) {
		this(id);
		setRepositoryImplConfig(implConfig);
	}

	/**
	 * Create a new RepositoryConfigImpl.
	 */
	public RepositoryConfig(String id, String title) {
		this(id);
		setTitle(title);
	}

	/**
	 * Create a new RepositoryConfigImpl.
	 */
	public RepositoryConfig(String id, String title, RepositoryImplConfig implConfig) {
		this(id, title);
		setRepositoryImplConfig(implConfig);
	}

	public String getID() {
		return id;
	}

	public void setID(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public RepositoryImplConfig getRepositoryImplConfig() {
		return implConfig;
	}

	public void setRepositoryImplConfig(RepositoryImplConfig implConfig) {
		this.implConfig = implConfig;
	}

	/**
	 * Validates this configuration. A {@link RepositoryConfigException} is thrown when the configuration is invalid.
	 * The exception should contain an error message that indicates why the configuration is invalid.
	 *
	 * @throws RepositoryConfigException If the configuration is invalid.
	 */
	public void validate() throws RepositoryConfigException {
		if (id == null) {
			throw new RepositoryConfigException("Repository ID missing");
		}
		if (implConfig == null) {
			throw new RepositoryConfigException("Repository implementation for repository missing");
		}
		implConfig.validate();
	}

	/**
	 * @deprecated use {@link #export(Model, Resource)}
	 */
	@Deprecated
	public void export(Model model) {
		ValueFactory vf = SimpleValueFactory.getInstance();
		export(model, vf.createBNode());
	}

	/**
	 * Exports the configuration into RDF using the given repositoryNode
	 *
	 * @param model          target RDF collection
	 * @param repositoryNode
	 * @since 2.3
	 */
	public void export(Model model, Resource repositoryNode) {
		ValueFactory vf = SimpleValueFactory.getInstance();
		model.setNamespace(RDFS.NS);
		model.setNamespace(XSD.NS);
		model.setNamespace("rep", NAMESPACE);
		model.add(repositoryNode, RDF.TYPE, REPOSITORY);

		if (id != null) {
			model.add(repositoryNode, REPOSITORYID, vf.createLiteral(id));
		}
		if (title != null) {
			model.add(repositoryNode, RDFS.LABEL, vf.createLiteral(title));
		}
		if (implConfig != null) {
			Resource implNode = implConfig.export(model);
			model.add(repositoryNode, REPOSITORYIMPL, implNode);
		}
	}

	public void parse(Model model, Resource repositoryNode) throws RepositoryConfigException {
		try {

			Models.objectLiteral(model.getStatements(repositoryNode, REPOSITORYID, null))
					.ifPresent(lit -> setID(lit.getLabel()));
			Models.objectLiteral(model.getStatements(repositoryNode, RDFS.LABEL, null))
					.ifPresent(lit -> setTitle(lit.getLabel()));
			Models.objectResource(model.getStatements(repositoryNode, REPOSITORYIMPL, null))
					.ifPresent(res -> setRepositoryImplConfig(AbstractRepositoryImplConfig.create(model, res)));
		} catch (ModelException e) {
			throw new RepositoryConfigException(e.getMessage(), e);
		}
	}

	/**
	 * Creates a new {@link RepositoryConfig} object and initializes it by supplying the {@code model} and
	 * {@code repositoryNode} to its {@link #parse(Model, Resource) parse} method.
	 *
	 * @param model          the {@link Model} to read initialization data from.
	 * @param repositoryNode the subject {@link Resource} that identifies the {@link RepositoryConfig} in the supplied
	 *                       Model.
	 */
	public static RepositoryConfig create(Model model, Resource repositoryNode) throws RepositoryConfigException {
		RepositoryConfig config = new RepositoryConfig();
		config.parse(model, repositoryNode);
		return config;
	}
}
