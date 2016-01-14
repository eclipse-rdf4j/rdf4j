/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation;

import java.util.List;
import java.util.Random;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.SailException;

/**
 * Statements are only written to a single member. Statements that have a
 * {@link IllegalStatementException} throw when added to a member are tried
 * against all other members until it is accepted. If no members accept a
 * statement the original exception is re-thrown.
 * 
 * @author James Leigh
 */
class WritableConnection extends AbstractEchoWriteConnection {

	private int addIndex;

	public WritableConnection(Federation federation, List<RepositoryConnection> members)
		throws SailException
	{
		super(federation, members);
		int size = members.size();
		int rnd = (new Random().nextInt() % size + size) % size;
		// use round-robin to distribute the load
		for (int i = rnd, n = rnd + size; i < n; i++) {
			try {
				if (members.get(i % size).getRepository().isWritable()) {
					addIndex = i % size;
				}
			}
			catch (RepositoryException e) {
				throw new SailException(e);
			}
		}
	}

	@Override
	public void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts)
		throws SailException
	{
		add(members.get(addIndex), subj, pred, obj, contexts);
	}

	private void add(RepositoryConnection member, Resource subj, IRI pred, Value obj, Resource... contexts)
		throws SailException
	{
		try {
			member.add(subj, pred, obj, contexts);
		}
		catch (RepositoryException e) {
			throw new SailException(e);
		}
	}

	@Override
	protected void clearInternal(Resource... contexts)
		throws SailException
	{
		removeStatementsInternal(null, null, null, contexts);
	}

}
