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

import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.model.util.Values.literal;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Configurations;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
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
		export(model, bnode());
	}

	/**
	 * Exports the configuration into RDF using the given repositoryNode
	 *
	 * @param model          target RDF collection
	 * @param repositoryNode
	 * @since 2.3
	 */
	public void export(Model model, Resource repositoryNode) {
		if (Configurations.useLegacyConfig()) {
			exportLegacy(model, repositoryNode);
			return;
		}

		model.setNamespace(RDFS.NS);
		model.setNamespace(XSD.NS);
		model.setNamespace(CONFIG.NS);
		model.add(repositoryNode, RDF.TYPE, CONFIG.Rep.Repository);

		if (id != null) {
			model.add(repositoryNode, CONFIG.Rep.id, literal(id));

		}
		if (title != null) {
			model.add(repositoryNode, RDFS.LABEL, literal(title));
		}
		if (implConfig != null) {
			Resource implNode = implConfig.export(model);
			model.add(repositoryNode, CONFIG.Rep.impl, implNode);

		}
	}

	private void exportLegacy(Model model, Resource repositoryNode) {
		model.setNamespace(RDFS.NS);
		model.setNamespace(XSD.NS);
		model.setNamespace("rep", RepositoryConfigSchema.NAMESPACE);
		model.add(repositoryNode, RDF.TYPE, RepositoryConfigSchema.REPOSITORY);

		if (id != null) {
			model.add(repositoryNode, RepositoryConfigSchema.REPOSITORYID, literal(id));

		}
		if (title != null) {
			model.add(repositoryNode, RDFS.LABEL, literal(title));
		}
		if (implConfig != null) {
			Resource implNode = implConfig.export(model);
			model.add(repositoryNode, RepositoryConfigSchema.REPOSITORYIMPL, implNode);

		}
	}

	public void parse(Model model, Resource repositoryNode) throws RepositoryConfigException {
		try {
			Configurations
					.getLiteralValue(model, repositoryNode, CONFIG.Rep.id, RepositoryConfigSchema.REPOSITORYID)
					.ifPresent(lit -> setID(lit.getLabel()));

			Models.objectLiteral(model.getStatements(repositoryNode, RDFS.LABEL, null))
					.ifPresent(lit -> setTitle(lit.getLabel()));

			Configurations
					.getResourceValue(model, repositoryNode, CONFIG.Rep.impl, RepositoryConfigSchema.REPOSITORYIMPL)
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
