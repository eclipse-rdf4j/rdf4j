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
 * Operation that sets the namespace for a specific prefix.
 *
 * @author Arjohn Kampman
 * @author Leo Sauermann
 */
public class SetNamespaceOperation implements TransactionOperation, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 7197096029612751574L;

	private String prefix;

	private String name;

	public SetNamespaceOperation() {
	}

	public SetNamespaceOperation(String prefix, String name) {
		setPrefix(prefix);
		setName(name);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public void execute(RepositoryConnection con) throws RepositoryException {
		con.setNamespace(prefix, name);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof SetNamespaceOperation) {
			SetNamespaceOperation o = (SetNamespaceOperation) other;
			return Objects.equals(getPrefix(), o.getPrefix()) && Objects.equals(getName(), o.getName());
		}

		return false;
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtil.nullHashCode(getPrefix());
		hashCode = 31 * hashCode + ObjectUtil.nullHashCode(getName());
		return hashCode;
	}
}
