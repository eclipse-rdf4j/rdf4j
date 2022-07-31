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
package org.eclipse.rdf4j.http.protocol.transaction.operations;

import java.util.Arrays;
import java.util.Objects;

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
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

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
