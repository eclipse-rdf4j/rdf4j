/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.protocol.transaction.operations;

import java.io.Serializable;

import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Encapsulation of a SPARQL 1.1 update operation executed as part of a transaction.
 *
 * @author Jeen Broekstra
 */
public class SPARQLUpdateOperation implements TransactionOperation, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 4432275498318918582L;

	private String updateString;

	private String baseURI;

	private boolean includeInferred;

	private Dataset dataset;

	private Binding[] bindings;

	public SPARQLUpdateOperation() {
		super();
	}

	public SPARQLUpdateOperation(String updateString, String baseURI, boolean includeInferred, Dataset dataset,
			Binding... bindings) {
		this.setUpdateString(updateString);
		this.setBaseURI(baseURI);
		this.setIncludeInferred(includeInferred);
		this.setDataset(dataset);
		this.setBindings(bindings);
	}

	@Override
	public void execute(RepositoryConnection con) throws RepositoryException {
		try {
			Update preparedUpdate = con.prepareUpdate(QueryLanguage.SPARQL, getUpdateString(), getBaseURI());
			preparedUpdate.setIncludeInferred(isIncludeInferred());
			preparedUpdate.setDataset(getDataset());

			if (getBindings() != null) {
				for (Binding binding : getBindings()) {
					preparedUpdate.setBinding(binding.getName(), binding.getValue());
				}
			}

			preparedUpdate.execute();
		} catch (MalformedQueryException | UpdateExecutionException e) {
			throw new RepositoryException(e);
		}

	}

	/**
	 * @return Returns the updateString.
	 */
	public String getUpdateString() {
		return updateString;
	}

	/**
	 * @param updateString The updateString to set.
	 */
	public void setUpdateString(String updateString) {
		this.updateString = updateString;
	}

	/**
	 * @return Returns the baseURI.
	 */
	public String getBaseURI() {
		return baseURI;
	}

	/**
	 * @param baseURI The baseURI to set.
	 */
	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
	}

	/**
	 * @return Returns the includeInferred.
	 */
	public boolean isIncludeInferred() {
		return includeInferred;
	}

	/**
	 * @param includeInferred The includeInferred to set.
	 */
	public void setIncludeInferred(boolean includeInferred) {
		this.includeInferred = includeInferred;
	}

	/**
	 * @return Returns the dataset.
	 */
	public Dataset getDataset() {
		return dataset;
	}

	/**
	 * @param dataset The dataset to set.
	 */
	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
	}

	/**
	 * @return Returns the bindings.
	 */
	public Binding[] getBindings() {
		return bindings;
	}

	/**
	 * @param bindings The bindings to set.
	 */
	public void setBindings(Binding[] bindings) {
		this.bindings = bindings;
	}

}
