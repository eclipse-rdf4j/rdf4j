/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
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

	private int maxExecutionTime = 0;


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

	public void setBinding(String name, Value value) {
		bindings.addBinding(name, value);
	}

	public void removeBinding(String name) {
		bindings.removeBinding(name);
	}

	public void clearBindings() {
		bindings.clear();
	}

	public BindingSet getBindings() {
		return bindings;
	}

	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
	}

	public Dataset getDataset() {
		return dataset;
	}

	public void setIncludeInferred(boolean includeInferred) {
		this.includeInferred = includeInferred;
	}

	public boolean getIncludeInferred() {
		return includeInferred;
	}

	@Override
	public void setMaxExecutionTime(int maxExecutionTime) {
		this.maxExecutionTime = maxExecutionTime;
	}

	@Override
	public int getMaxExecutionTime() {
		return maxExecutionTime;
	}
}
