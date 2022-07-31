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
package org.eclipse.rdf4j.query.impl;

import org.eclipse.rdf4j.query.Query;

/**
 * Abstract super class of all query types.
 */
public abstract class AbstractQuery extends AbstractOperation implements Query {

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new query object.
	 */
	protected AbstractQuery() {
		super();
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Deprecated
	@Override
	public void setMaxQueryTime(int maxQueryTimeSeconds) {
		setMaxExecutionTime(maxQueryTimeSeconds);

	}

	@Deprecated
	@Override
	public int getMaxQueryTime() {
		return getMaxExecutionTime();
	}

}
