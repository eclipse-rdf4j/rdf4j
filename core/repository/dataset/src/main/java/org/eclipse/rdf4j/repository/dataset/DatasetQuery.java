/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.dataset;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.repository.sail.SailQuery;

/**
 * @author Arjohn Kampman
 */
abstract class DatasetQuery implements Query {

	protected final DatasetRepositoryConnection con;

	protected final SailQuery sailQuery;

	protected DatasetQuery(DatasetRepositoryConnection con, SailQuery sailQuery) {
		this.con = con;
		this.sailQuery = sailQuery;
	}

	public final void setBinding(String name, Value value) {
		sailQuery.setBinding(name, value);
	}

	public final void removeBinding(String name) {
		sailQuery.removeBinding(name);
	}

	public final void clearBindings() {
		sailQuery.clearBindings();
	}

	public final BindingSet getBindings() {
		return sailQuery.getBindings();
	}

	public final void setDataset(Dataset dataset) {
		sailQuery.setDataset(dataset);
	}

	public final Dataset getDataset() {
		return sailQuery.getDataset();
	}

	public final void setIncludeInferred(boolean includeInferred) {
		sailQuery.setIncludeInferred(includeInferred);
	}

	public final boolean getIncludeInferred() {
		return sailQuery.getIncludeInferred();
	}

	@Override
	public void setMaxExecutionTime(int maxExecTime) {
		sailQuery.setMaxExecutionTime(maxExecTime);
	}
	
	@Override
	public int getMaxExecutionTime() {
		return sailQuery.getMaxExecutionTime();
	}
	
	@Deprecated
	public void setMaxQueryTime(int maxQueryTime) {
		setMaxExecutionTime(maxQueryTime);
	}

	@Deprecated
	public int getMaxQueryTime() {
		return getMaxExecutionTime();
	}

	@Override
	public String toString() {
		return sailQuery.toString();
	}
}
