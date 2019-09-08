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
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Default {@link WriteStrategy} implementation for read only federations.
 * In case a user attempts to perform a write operation a {@link UnsupportedOperationException}
 * is thrown.
 * 
 * @author Andreas Schwarte
 *
 */
public class ReadOnlyWriteStrategy implements WriteStrategy {

	public static final ReadOnlyWriteStrategy INSTANCE = new ReadOnlyWriteStrategy();
	
	private ReadOnlyWriteStrategy() { }
	
	@Override
	public void begin() throws RepositoryException {
		throw new UnsupportedOperationException("Writing not supported to a federation: the federation is readonly.");		
	}

	@Override
	public void commit() throws RepositoryException {
		throw new UnsupportedOperationException("Writing not supported to a federation: the federation is readonly.");		
	}

	@Override
	public void rollback() throws RepositoryException {
		throw new UnsupportedOperationException("Writing not supported to a federation: the federation is readonly.");		
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
	public void initialize() throws RepositoryException {
	}

	@Override
	public void close() throws RepositoryException {
	}

	@Override
	public boolean isInitialized() {
		return true;
	}

}
