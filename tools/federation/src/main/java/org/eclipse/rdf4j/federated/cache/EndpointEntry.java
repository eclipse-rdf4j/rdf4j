/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.cache;

import java.io.Serializable;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Statement;

public class EndpointEntry implements Serializable {

	private static final long serialVersionUID = -5572059274543728740L;

	protected final String endpointID;
	protected boolean doesProvideStatements;
	protected boolean hasLocalStatements = false;

	public EndpointEntry(String endpointID, boolean canProvideStatements) {
		super();
		this.endpointID = endpointID;
		this.doesProvideStatements = canProvideStatements;
	}

	public boolean doesProvideStatements() {
		return doesProvideStatements;
	}

	public CloseableIteration<? extends Statement, Exception> getStatements() {
		throw new UnsupportedOperationException("This operation is not yet supported.");
	}

	public boolean hasLocalStatements() {
		return hasLocalStatements;
	}

	public void setCanProvideStatements(boolean canProvideStatements) {
		this.doesProvideStatements = canProvideStatements;
	}

	public String getEndpointID() {
		return endpointID;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " {endpointID=" + endpointID + ", doesProvideStatements="
				+ doesProvideStatements + ", hasLocalStatements=" + hasLocalStatements + "}";
	}

}
