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
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.SailException;

/**
 * Echos all write operations to all members.
 * 
 * @author James Leigh
 */
abstract class AbstractEchoWriteConnection extends AbstractFederationConnection {

	public AbstractEchoWriteConnection(Federation federation, List<RepositoryConnection> members) {
		super(federation, members);
	}

	@Override
	public void startTransactionInternal()
		throws SailException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection con)
				throws RepositoryException
			{
				con.begin();
			}
		});
	}

	@Override
	public void rollbackInternal()
		throws SailException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection con)
				throws RepositoryException
			{
				con.rollback();
			}
		});
	}

	@Override
	public void commitInternal()
		throws SailException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection con)
				throws RepositoryException
			{
				con.commit();
			}
		});
	}

	public void setNamespaceInternal(final String prefix, final String name)
		throws SailException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection con)
				throws RepositoryException
			{
				con.setNamespace(prefix, name);
			}
		});
	}

	@Override
	public void clearNamespacesInternal()
		throws SailException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection con)
				throws RepositoryException
			{
				con.clearNamespaces();
			}
		});
	}

	@Override
	public void removeNamespaceInternal(final String prefix)
		throws SailException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection con)
				throws RepositoryException
			{
				con.removeNamespace(prefix);
			}
		});
	}

	@Override
	public void removeStatementsInternal(final Resource subj, final IRI pred, final Value obj,
			final Resource... contexts)
		throws SailException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection con)
				throws RepositoryException
			{
				con.remove(subj, pred, obj, contexts);
			}
		});
	}
}
