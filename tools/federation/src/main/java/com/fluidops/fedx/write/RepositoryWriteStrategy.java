/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.write;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * A {@link WriteStrategy} to write to a designated {@link Repository}.
 * This write strategy opens a fresh {@link RepositoryConnection} and
 * keeps this until a call of {@link #close()}.
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
		return connection!=null;
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
