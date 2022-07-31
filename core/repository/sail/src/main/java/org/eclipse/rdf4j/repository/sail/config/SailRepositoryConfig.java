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
package org.eclipse.rdf4j.repository.sail.config;

import static org.eclipse.rdf4j.repository.sail.config.SailRepositorySchema.NAMESPACE;
import static org.eclipse.rdf4j.repository.sail.config.SailRepositorySchema.SAILIMPL;
import static org.eclipse.rdf4j.sail.config.SailConfigSchema.SAILTYPE;

import java.util.Optional;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.repository.config.AbstractRepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.config.SailRegistry;

/**
 * @author Arjohn Kampman
 */
public class SailRepositoryConfig extends AbstractRepositoryImplConfig {

	private SailImplConfig sailImplConfig;

	public SailRepositoryConfig() {
		super(SailRepositoryFactory.REPOSITORY_TYPE);
	}

	public SailRepositoryConfig(SailImplConfig sailImplConfig) {
		this();
		setSailImplConfig(sailImplConfig);
	}

	public SailImplConfig getSailImplConfig() {
		return sailImplConfig;
	}

	public void setSailImplConfig(SailImplConfig sailImplConfig) {
		this.sailImplConfig = sailImplConfig;
	}

	@Override
	public void validate() throws RepositoryConfigException {
		super.validate();
		if (sailImplConfig == null) {
			throw new RepositoryConfigException("No Sail implementation specified for Sail repository");
		}

		try {
			sailImplConfig.validate();
		} catch (SailConfigException e) {
			throw new RepositoryConfigException(e.getMessage(), e);
		}
	}

	@Override
	public Resource export(Model model) {
		Resource repImplNode = super.export(model);

		if (sailImplConfig != null) {
			model.setNamespace("sr", NAMESPACE);
			Resource sailImplNode = sailImplConfig.export(model);
			model.add(repImplNode, SAILIMPL, sailImplNode);
		}

		return repImplNode;
	}

	@Override
	public void parse(Model model, Resource repImplNode) throws RepositoryConfigException {
		try {
			Optional<Resource> sailImplNode = Models.objectResource(model.getStatements(repImplNode, SAILIMPL, null));
			if (sailImplNode.isPresent()) {
				Models.objectLiteral(model.getStatements(sailImplNode.get(), SAILTYPE, null)).ifPresent(typeLit -> {
					SailFactory factory = SailRegistry.getInstance()
							.get(typeLit.getLabel())
							.orElseThrow(() -> new RepositoryConfigException(
									"Unsupported Sail type: " + typeLit.getLabel()));

					sailImplConfig = factory.getConfig();
					sailImplConfig.parse(model, sailImplNode.get());
				});
			}
		} catch (ModelException | SailConfigException e) {
			throw new RepositoryConfigException(e.getMessage(), e);
		}
	}
}
