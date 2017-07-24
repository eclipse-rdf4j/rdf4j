/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.manager;

import org.eclipse.rdf4j.repository.config.AbstractRepositoryImplConfig;

/**
 * @author Arjohn Kampman
 */
@Deprecated
public class SystemRepositoryConfig extends AbstractRepositoryImplConfig {

	public SystemRepositoryConfig() {
		super(SystemRepository.REPOSITORY_TYPE);
	}
}
