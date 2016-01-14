/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.SailReadOnlyException;

/**
 * Finishes the {@link AbstractFederationConnection} by throwing
 * {@link SailReadOnlyException}s for all write operations.
 * 
 * @author James Leigh
 */
class ReadOnlyConnection extends AbstractFederationConnection {

	public ReadOnlyConnection(Federation federation, List<RepositoryConnection> members) {
		super(federation, members);
	}

	@Override
	public void setNamespaceInternal(String prefix, String name)
		throws SailException
	{
		throw new SailReadOnlyException("");
	}

	@Override
	public void clearNamespacesInternal()
		throws SailException
	{
		throw new SailReadOnlyException("");
	}

	@Override
	public void removeNamespaceInternal(String prefix)
		throws SailException
	{
		throw new SailReadOnlyException("");
	}

	@Override
	public void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts)
		throws SailException
	{
		throw new SailReadOnlyException("");
	}

	@Override
	public void removeStatementsInternal(Resource subj, IRI pred, Value obj, Resource... context)
		throws SailException
	{
		throw new SailReadOnlyException("");
	}

	@Override
	protected void clearInternal(Resource... contexts)
		throws SailException
	{
		throw new SailReadOnlyException("");
	}

	@Override
	protected void commitInternal()
		throws SailException
	{
		// no-op
	}

	@Override
	protected void rollbackInternal()
		throws SailException
	{
		// no-op
	}

	@Override
	protected void startTransactionInternal()
		throws SailException
	{
		// no-op
	}
}
