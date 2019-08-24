/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.protocol.transaction.operations;

import java.util.Arrays;

import org.eclipse.rdf4j.OpenRDFUtil;
import org.eclipse.rdf4j.model.Resource;

/**
 * A TransactionOperation that operates on a specific (set of) contexts.
 * 
 * @author Arjohn Kampman
 * @author Leo Sauermann
 */
public abstract class ContextOperation implements TransactionOperation {

	protected Resource[] contexts;

	protected ContextOperation(Resource... contexts) {
		setContexts(contexts);
	}

	public Resource[] getContexts() {
		return contexts;
	}

	public void setContexts(Resource... contexts) {
		OpenRDFUtil.verifyContextNotNull(contexts);

		this.contexts = contexts;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ContextOperation) {
			ContextOperation o = (ContextOperation) other;
			return Arrays.deepEquals(getContexts(), o.getContexts());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return Arrays.deepHashCode(getContexts());
	}
}
