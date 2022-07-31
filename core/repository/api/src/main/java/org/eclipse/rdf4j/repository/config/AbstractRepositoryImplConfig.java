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

import static org.eclipse.rdf4j.repository.config.RepositoryConfigSchema.REPOSITORYTYPE;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;

/**
 * @author Herko ter Horst
 */
public class AbstractRepositoryImplConfig implements RepositoryImplConfig {

	private String type;

	/**
	 * Create a new RepositoryConfigImpl.
	 */
	public AbstractRepositoryImplConfig() {
	}

	/**
	 * Create a new RepositoryConfigImpl.
	 *
	 * @param type
	 */
	public AbstractRepositoryImplConfig(String type) {
		this();
		setType(type);
	}

	@Override
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public void validate() throws RepositoryConfigException {
		if (type == null) {
			throw new RepositoryConfigException("No type specified for repository implementation");
		}
	}

	@Override
	public Resource export(Model model) {
		BNode implNode = SimpleValueFactory.getInstance().createBNode();

		if (type != null) {
			model.add(implNode, REPOSITORYTYPE, SimpleValueFactory.getInstance().createLiteral(type));
		}

		return implNode;
	}

	@Override
	public void parse(Model model, Resource resource) throws RepositoryConfigException {
		Models.objectLiteral(model.getStatements(resource, REPOSITORYTYPE, null))
				.ifPresent(typeLit -> setType(typeLit.getLabel()));
	}

	/**
	 * Utility method to create a new {@link RepositoryImplConfig} by reading data from the supplied {@link Model}.
	 *
	 * @param model    the {@link Model} to read configuration data from.
	 * @param resource the subject {@link Resource} identifying the configuration data in the Model.
	 * @return a new {@link RepositoryImplConfig} initialized with the configuration from the input Model, or
	 *         {@code null} if no {@link RepositoryConfigSchema#REPOSITORYTYPE} property was found in the configuration
	 *         data..
	 * @throws RepositoryConfigException if an error occurred reading the configuration data from the model.
	 */
	public static RepositoryImplConfig create(Model model, Resource resource) throws RepositoryConfigException {
		try {
			// Literal typeLit = GraphUtil.getOptionalObjectLiteral(graph,
			// implNode, REPOSITORYTYPE);

			final Literal typeLit = Models.objectLiteral(model.getStatements(resource, REPOSITORYTYPE, null))
					.orElse(null);
			if (typeLit != null) {
				RepositoryFactory factory = RepositoryRegistry.getInstance()
						.get(typeLit.getLabel())
						.orElseThrow(() -> new RepositoryConfigException(
								"Unsupported repository type: " + typeLit.getLabel()));

				RepositoryImplConfig implConfig = factory.getConfig();
				implConfig.parse(model, resource);
				return implConfig;
			}

			return null;
		} catch (ModelException e) {
			throw new RepositoryConfigException(e.getMessage(), e);
		}
	}
}
