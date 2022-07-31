/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.tx;

import static org.springframework.transaction.TransactionDefinition.ISOLATION_DEFAULT;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRED;
import static org.springframework.transaction.TransactionDefinition.TIMEOUT_DEFAULT;

import java.util.function.Function;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.transaction.support.SmartTransactionObject;

/**
 * @since 4.0.0
 * @author ameingast@gmail.com
 * @author Florian Kleedorfer
 */
@Experimental
public class TransactionObject {

	private RepositoryConnection connection;

	private boolean existing;

	private String name = "";

	private boolean rollbackOnly = false;

	private int timeout = TIMEOUT_DEFAULT;

	private int isolationLevel = ISOLATION_DEFAULT;

	private int propagationBehavior = PROPAGATION_REQUIRED;

	private boolean readOnly = false;

	public TransactionObject(RepositoryConnection connection) {
		this.connection = connection;
	}

	public RepositoryConnection getConnection() {
		return connection;
	}

	public void wrapConnection(Function<RepositoryConnection, RepositoryConnection> wrapper) {
		this.connection = wrapper.apply(connection);
	}

	public void setExisting(boolean existing) {
		this.existing = existing;
	}

	public boolean isExisting() {
		return existing;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isRollbackOnly() {
		return rollbackOnly;
	}

	public void setRollbackOnly(boolean rollbackOnly) {
		this.rollbackOnly = rollbackOnly;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getIsolationLevel() {
		return isolationLevel;
	}

	public void setIsolationLevel(int isolationLevel) {
		this.isolationLevel = isolationLevel;
	}

	public int getPropagationBehavior() {
		return propagationBehavior;
	}

	public void setPropagationBehavior(int propagationBehavior) {
		this.propagationBehavior = propagationBehavior;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	@Override
	public String toString() {
		return "TransactionData{"
				+ "connection="
				+ connection
				+ ", existing="
				+ existing
				+ ", name='"
				+ name
				+ '\''
				+ ", rollbackOnly="
				+ rollbackOnly
				+ ", timeout="
				+ timeout
				+ ", isolationLevel="
				+ isolationLevel
				+ ", propagationBehavior="
				+ propagationBehavior
				+ ", readOnly="
				+ readOnly
				+ '}';
	}
}
