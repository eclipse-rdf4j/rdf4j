/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.config;

/**
 * @deprecated since 4.0. Use {@link AbstractRepositoryImplConfig} instead.
 * @author Jeen Broekstra
 */
@Deprecated
public class RepositoryImplConfigBase extends AbstractRepositoryImplConfig {

	/**
	 * Create a new RepositoryConfigImpl.
	 */
	public RepositoryImplConfigBase() {
		super();
	}

	/**
	 * Create a new RepositoryConfigImpl.
	 */
	public RepositoryImplConfigBase(String type) {
		super(type);
	}

}
