/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.write;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * A {@link WriteStrategy} to write to a designated {@link Repository}. This write strategy opens a fresh
 * {@link RepositoryConnection} and keeps this until a call of {@link #close()}.
 * 
 * 
 * <p>
 * <b>Note:</b> this is an experimental feature which is subject to change in a future version.
 * </p>
 * 
 * @author Andreas Schwarte
 * @see WriteStrategy
 */
public class RepositoryWriteStrategy implements WriteStrategy {

	private final Repository writeRepository;
	private RepositoryConnection connection = null;

	public RepositoryWriteStrategy(Repository writeRepository) {
		super();
		this.writeRepository = writeRepository;
	}

	@Override
	public void initialize() throws RepositoryException {
		connection = writeRepository.getConnection();
	}

	@Override
	public boolean isInitialized() {
		return connection != null;
	}

	@Override
	public void close() throws RepositoryException {
		connection.close();
	}

	@Override
	public void begin() throws RepositoryException {
		connection.begin();
	}

	@Override
	public void commit() throws RepositoryException {
		connection.commit();
	}

	@Override
	public void rollback() throws RepositoryException {
		connection.rollback();
	}

	@Override
	public void addStatement(Resource subj, IRI pred, Value obj,
			Resource... contexts) throws RepositoryException {
		connection.add(subj, pred, obj, contexts);

	}

	@Override
	public void removeStatement(Resource subj, IRI pred, Value obj,
			Resource... contexts) throws RepositoryException {
		connection.remove(subj, pred, obj, contexts);
	}
}
