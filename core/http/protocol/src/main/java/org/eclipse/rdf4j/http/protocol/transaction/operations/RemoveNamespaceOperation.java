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

import java.io.Serializable;
import java.util.Objects;

import org.eclipse.rdf4j.common.lang.ObjectUtil;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Operation that removes the namespace for a specific prefix.
 *
 * @author Arjohn Kampman
 * @author Leo Sauermann
 */
public class RemoveNamespaceOperation implements TransactionOperation, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 3227597422508894927L;

	private String prefix;

	public RemoveNamespaceOperation() {
	}

	public RemoveNamespaceOperation(String prefix) {
		setPrefix(prefix);
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public void execute(RepositoryConnection con) throws RepositoryException {
		con.removeNamespace(prefix);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof RemoveNamespaceOperation) {
			RemoveNamespaceOperation o = (RemoveNamespaceOperation) other;
			return Objects.equals(getPrefix(), o.getPrefix());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return ObjectUtil.nullHashCode(getPrefix());
	}
}
