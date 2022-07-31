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
package org.eclipse.rdf4j.federated.write;

import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Default {@link WriteStrategy} implementation for read only federations. In case a user attempts to perform a write
 * operation a {@link UnsupportedOperationException} is thrown.
 *
 * @author Andreas Schwarte
 *
 */
public class ReadOnlyWriteStrategy implements WriteStrategy {

	public static final ReadOnlyWriteStrategy INSTANCE = new ReadOnlyWriteStrategy();

	private ReadOnlyWriteStrategy() {
	}

	@Override
	public void begin() throws RepositoryException {
		// no-op
	}

	@Override
	public void commit() throws RepositoryException {
		// no-op
	}

	@Override
	public void rollback() throws RepositoryException {
		// no-op
	}

	@Override
	public void setTransactionSettings(TransactionSetting... transactionSettings) throws RepositoryException {
		// no-op
	}

	@Override
	public void addStatement(Resource subj, IRI pred, Value obj,
			Resource... contexts) {
		throw new UnsupportedOperationException("Writing not supported to a federation: the federation is readonly.");
	}

	@Override
	public void removeStatement(Resource subj, IRI pred, Value obj,
			Resource... contexts) throws RepositoryException {
		throw new UnsupportedOperationException("Writing not supported to a federation: the federation is readonly.");
	}

	@Override
	public void clear(Resource... contexts) throws RepositoryException {
		throw new UnsupportedOperationException("Writing not supported to a federation: the federation is readonly.");
	}

	@Override
	public void close() throws RepositoryException {
	}

	@Override
	public void clearNamespaces() throws RepositoryException {
		throw new UnsupportedOperationException("Writing not supported to a federation: the federation is readonly.");
	}

}
