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

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
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
		Resource implNode = super.export(model);
		if (null != this.proxiedID) {
			model.setNamespace("proxy", ProxyRepositorySchema.NAMESPACE);
			model.add(implNode, ProxyRepositorySchema.PROXIED_ID,
					SimpleValueFactory.getInstance().createLiteral(this.proxiedID));
		}
		return implNode;
	}

	@Override
	public void parse(Model model, Resource implNode) throws RepositoryConfigException {
		super.parse(model, implNode);

		try {
			Models.objectLiteral(model.getStatements(implNode, ProxyRepositorySchema.PROXIED_ID, null))
					.ifPresent(lit -> setProxiedRepositoryID(lit.getLabel()));
		} catch (ModelException e) {
			throw new RepositoryConfigException(e.getMessage(), e);
		}
	}
}
