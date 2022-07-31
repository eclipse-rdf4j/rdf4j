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
package org.eclipse.rdf4j.sail.config;

import static org.eclipse.rdf4j.sail.config.SailConfigSchema.DELEGATE;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;

/**
 * @author Herko ter Horst
 */
public abstract class AbstractDelegatingSailImplConfig extends AbstractSailImplConfig
		implements DelegatingSailImplConfig {

	private SailImplConfig delegate;

	/**
	 * Create a new RepositoryConfigImpl.
	 */
	public AbstractDelegatingSailImplConfig() {
		super();
	}

	/**
	 * Create a new RepositoryConfigImpl.
	 */
	public AbstractDelegatingSailImplConfig(String type) {
		super(type);
	}

	/**
	 * Create a new RepositoryConfigImpl.
	 */
	public AbstractDelegatingSailImplConfig(String type, SailImplConfig delegate) {
		this(type);
		setDelegate(delegate);
	}

	@Override
	public SailImplConfig getDelegate() {
		return delegate;
	}

	public void setDelegate(SailImplConfig delegate) {
		this.delegate = delegate;
	}

	@Override
	public void validate() throws SailConfigException {
		super.validate();
		if (delegate == null) {
			throw new SailConfigException("No delegate specified for " + getType() + " Sail");
		}
		delegate.validate();
	}

	@Override
	public Resource export(Model m) {
		Resource implNode = super.export(m);

		if (delegate != null) {
			Resource delegateNode = delegate.export(m);
			m.add(implNode, DELEGATE, delegateNode);
		}

		return implNode;
	}

	@Override
	public void parse(Model m, Resource implNode) throws SailConfigException {
		super.parse(m, implNode);

		try {
			Models.objectResource(m.getStatements(implNode, DELEGATE, null))
					.ifPresent(delegate -> setDelegate(SailConfigUtil.parseRepositoryImpl(m, delegate)));
		} catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}
}
