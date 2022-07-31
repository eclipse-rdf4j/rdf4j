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

import static org.eclipse.rdf4j.repository.config.RepositoryConfigSchema.DELEGATE;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;

/**
 * @author Herko ter Horst
 */
public abstract class AbstractDelegatingRepositoryImplConfig extends AbstractRepositoryImplConfig
		implements DelegatingRepositoryImplConfig {

	private RepositoryImplConfig delegate;

	/**
	 * Create a new DelegatingRepositoryImplConfigBase.
	 */
	protected AbstractDelegatingRepositoryImplConfig() {
		super();
	}

	/**
	 * Create a new DelegatingRepositoryImplConfigBase.
	 */
	protected AbstractDelegatingRepositoryImplConfig(String type) {
		super(type);
	}

	/**
	 * Create a new DelegatingRepositoryImplConfigBase.
	 */
	protected AbstractDelegatingRepositoryImplConfig(String type, RepositoryImplConfig delegate) {
		this(type);
		setDelegate(delegate);
	}

	@Override
	public RepositoryImplConfig getDelegate() {
		return delegate;
	}

	public void setDelegate(RepositoryImplConfig delegate) {
		this.delegate = delegate;
	}

	@Override
	public void validate() throws RepositoryConfigException {
		super.validate();
		if (delegate == null) {
			throw new RepositoryConfigException("No delegate specified for " + getType() + " repository");
		}
		delegate.validate();
	}

	@Override
	public Resource export(Model model) {
		Resource resource = super.export(model);

		if (delegate != null) {
			Resource delegateNode = delegate.export(model);
			model.add(resource, DELEGATE, delegateNode);
		}

		return resource;
	}

	@Override
	public void parse(Model model, Resource resource) throws RepositoryConfigException {
		super.parse(model, resource);

		Models.objectResource(model.getStatements(resource, DELEGATE, null))
				.ifPresent(delegate -> setDelegate(create(model, delegate)));
	}
}
