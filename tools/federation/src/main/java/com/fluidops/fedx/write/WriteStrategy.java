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
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.fluidops.fedx.FedX;


/**
 * Interface for the {@link WriteStrategy} that is used for writing data
 * to the federation. The implementation can decided upon how is data 
 * written to the underlying federation members (e.g. to a designated
 * federation member)
 * 
 * @author Andreas Schwarte
 * @since 4.0
 * @see RepositoryWriteStrategy
 * @see ReadOnlyWriteStrategy
 * @see FedX#getWriteStrategy()
 */
public interface WriteStrategy {

	/**
	 * Initialize the write strategy (e.g. open a shared
	 * {@link RepositoryConnection}. 
	 * 
	 * @throws RepositoryException
	 */
	public void initialize() throws RepositoryException;
	
	/**
	 * Returns true if this instance is initialized
	 * 
	 * @return flag indicating the initialization resources
	 */
	public boolean isInitialized();
	
	/**
	 * Close this write strategy (e.g. close a shared
	 * {@link RepositoryException}).
	 * 
	 * @throws RepositoryException
	 */
	public void close() throws RepositoryException;
	
	/**
	 * Begin a transaction.
	 * 
	 * @throws RepositoryException
	 */
	public void begin()	throws RepositoryException;
	
	/**
	 * Commit a transaction.
	 * 
	 * @throws RepositoryException
	 */
	public void commit() throws RepositoryException;
	
	/**
	 * Rollback a transaction.
	 * 
	 * @throws RepositoryException
	 */
	public void rollback() throws RepositoryException;
	
	/**
	 * Add a statement 
	 * 
	 * @param subj
	 * @param pred
	 * @param obj
	 * @param contexts
	 * @throws RepositoryException
	 */
	public void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws RepositoryException;
	
	/**
	 * Remove a statement
	 * 
	 * @param subj
	 * @param pred
	 * @param obj
	 * @param contexts
	 * @throws RepositoryException
	 */
	public void removeStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws RepositoryException;
}
