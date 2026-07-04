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
package org.eclipse.rdf4j.sail;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleDataset;

/**
 * Provided with add and remove operation to give them context within a {@link UpdateExpr} operation.
 *
 * @author James Leigh
 */
public class UpdateContext {

	private final UpdateExpr updateExpr;

	private final Dataset dataset;

	private final BindingSet bindings;

	private final boolean includeInferred;

	public UpdateContext(UpdateExpr updateExpr, Dataset dataset, BindingSet bindings, boolean includeInferred) {
		assert updateExpr != null;
		this.updateExpr = updateExpr;
		if (dataset == null) {
			this.dataset = new SimpleDataset();
		} else {
			this.dataset = dataset;
		}
		if (bindings == null) {
			this.bindings = EmptyBindingSet.getInstance();
		} else {
			this.bindings = bindings;
		}
		this.includeInferred = includeInferred;
	}

	@Override
	public String toString() {
		return updateExpr.toString();
	}

	/**
	 * @return Returns the updateExpr.
	 */
	public UpdateExpr getUpdateExpr() {
		return updateExpr;
	}

	/**
	 * @return Returns the dataset.
	 */
	public Dataset getDataset() {
		return dataset;
	}

	/**
	 * @return Returns the bindings.
	 */
	public BindingSet getBindingSet() {
		return bindings;
	}

	/**
	 * @return Returns the includeInferred.
	 */
	public boolean isIncludeInferred() {
		return includeInferred;
	}
}
