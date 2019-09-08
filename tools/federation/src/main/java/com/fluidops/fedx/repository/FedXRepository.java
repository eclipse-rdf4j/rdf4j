/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package com.fluidops.fedx.repository;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A special {@link SailRepository} which performs the actions as
 * defined in {@link FedXRepositoryConnection}.
 * 
 * @author as
 */
public class FedXRepository extends SailRepository
{

	public FedXRepository(Sail sail)
	{
		super(sail);
	}

	@Override
	public FedXRepositoryConnection getConnection() throws RepositoryException {
		try {
			return new FedXRepositoryConnection(this, this.getSail().getConnection());
		}
		catch (SailException e) {
			throw new RepositoryException(e);
		}
	}	

}
