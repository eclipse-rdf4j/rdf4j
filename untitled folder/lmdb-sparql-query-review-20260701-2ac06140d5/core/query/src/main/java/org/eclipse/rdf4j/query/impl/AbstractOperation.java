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

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.Operation;

/**
 * Abstract super class of all operation types.
 *
 * @author Jeen Broekstra
 */
public abstract class AbstractOperation implements Operation {

	/*------------*
	 * Attributes *
	 *------------*/

	protected final MapBindingSet bindings = new MapBindingSet();

	protected Dataset dataset = null;

	protected boolean includeInferred = true;

	private int maxExecutionTimeSeconds = 0;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new operation object.
	 */
	protected AbstractOperation() {
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void setBinding(String name, Value value) {
		bindings.setBinding(name, value);
	}

	@Override
	public void removeBinding(String name) {
		bindings.removeBinding(name);
	}

	@Override
	public void clearBindings() {
		bindings.clear();
	}

	@Override
	public BindingSet getBindings() {
		return bindings;
	}

	@Override
	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
	}

	@Override
	public Dataset getDataset() {
		return dataset;
	}

	@Override
	public void setIncludeInferred(boolean includeInferred) {
		this.includeInferred = includeInferred;
	}

	@Override
	public boolean getIncludeInferred() {
		return includeInferred;
	}

	@Override
	public void setMaxExecutionTime(int maxExecutionTimeSeconds) {
		this.maxExecutionTimeSeconds = maxExecutionTimeSeconds;
	}

	@Override
	public int getMaxExecutionTime() {
		return maxExecutionTimeSeconds;
	}
}
