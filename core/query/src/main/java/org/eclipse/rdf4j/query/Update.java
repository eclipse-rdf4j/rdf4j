/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query;

/**
 * An update operation on a repository that can be formulated in one of the supported query languages (for example
 * SPARQL).
 *
 * @author Jeen
 */
public interface Update extends Operation {

	/**
	 * Execute this update on the repository.
	 *
	 * @throws UpdateExecutionException if the update could not be successfully completed.
	 */
	void execute() throws UpdateExecutionException;

}
