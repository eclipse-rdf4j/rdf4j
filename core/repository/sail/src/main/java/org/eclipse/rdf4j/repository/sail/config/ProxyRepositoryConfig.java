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

import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.eclipse.rdf4j.repository.sail.config.ProxyRepositorySchema.PROXIED_ID;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Configurations;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
import org.eclipse.rdf4j.repository.config.AbstractRepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;

public class ProxyRepositoryConfig extends AbstractRepositoryImplConfig {

	private String proxiedID;

	public ProxyRepositoryConfig() {
		super(ProxyRepositoryFactory.REPOSITORY_TYPE);
	}

	public ProxyRepositoryConfig(String proxiedID) {
		this();
		this.setProxiedRepositoryID(proxiedID);
	}

	public final void setProxiedRepositoryID(String value) {
		this.proxiedID = value;
	}

	public String getProxiedRepositoryID() {
		return this.proxiedID;
	}

	@Override
	public void validate() throws RepositoryConfigException {
		super.validate();
		if (null == this.proxiedID) {
			throw new RepositoryConfigException("No id specified for proxied repository");
		}
	}

	@Override
	public Resource export(Model model) {
		if (Configurations.useLegacyConfig()) {
			return exportLegacy(model);
		}

		Resource implNode = super.export(model);
		if (null != this.proxiedID) {
			model.setNamespace(CONFIG.NS);
			model.add(implNode, CONFIG.Proxy.proxiedID, literal(this.proxiedID));
		}
		return implNode;
	}

	private Resource exportLegacy(Model model) {
		Resource implNode = super.export(model);
		if (null != this.proxiedID) {
			model.add(implNode, PROXIED_ID, literal(this.proxiedID));
		}
		return implNode;
	}

	@Override
	public void parse(Model model, Resource implNode) throws RepositoryConfigException {
		super.parse(model, implNode);

		try {
			Configurations
					.getLiteralValue(model, implNode, CONFIG.Proxy.proxiedID, PROXIED_ID)
					.ifPresent(lit -> setProxiedRepositoryID(lit.getLabel()));
		} catch (ModelException e) {
			throw new RepositoryConfigException(e.getMessage(), e);
		}
	}
}
