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
package com.fluidops.fedx.cache;

import java.io.Serializable;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Statement;


public class EndpointEntry implements Serializable {


	private static final long serialVersionUID = -5572059274543728740L;
	
	protected final String endpointID;
	protected boolean doesProvideStatements = false;
	protected boolean hasLocalStatements = false;
	
	
	public EndpointEntry(String endpointID, boolean canProvideStatements) {
		super();
		this.endpointID = endpointID;
		this.doesProvideStatements = canProvideStatements;
	}

	public boolean doesProvideStatements() {
		return doesProvideStatements;
	}

	public CloseableIteration<? extends Statement, Exception> getStatements() {
		throw new UnsupportedOperationException("This operation is not yet supported.");
	}


	public boolean hasLocalStatements() {
		return hasLocalStatements;
	}

	public void setCanProvideStatements(boolean canProvideStatements) {
		this.doesProvideStatements = canProvideStatements;
	}


	public String getEndpointID() {
		return endpointID;
	}
	
	public String toString() {
		return getClass().getSimpleName() + " {endpointID=" + endpointID + ", doesProvideStatements=" + doesProvideStatements + ", hasLocalStatements=" + hasLocalStatements + "}";
	}
	
}
