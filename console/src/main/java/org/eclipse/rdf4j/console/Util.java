/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;

/**
 * Helper class
 * 
 * @author Bart Hanssens
 */
public class Util {
	/**
	 * Get context IRI from string representation
	 * 
	 * @param repository repository
	 * @param ctxID context as string
	 * @return context IRI
	 */
	protected static Resource getContext(Repository repository, String ctxID) {
		if (ctxID.equalsIgnoreCase("null")) {
			return null;
		}
		if (ctxID.startsWith("_:")) {
			return repository.getValueFactory().createBNode(ctxID.substring(2));
		}
		return repository.getValueFactory().createIRI(ctxID);
	}
	
	/**
	 * Get context IRIs from a series of tokens, starting from (zero-based) position within the series.
	 * 
	 * @param tokens command as series of tokens
	 * @param pos position to start from
	 * @param repository repository
	 * @return array of contexts or null for default context
	 * @throws IllegalArgumentException
	 */
	protected static Resource[] getContexts(String[] tokens, int pos, Repository repository)
									throws IllegalArgumentException {	
		Resource[] contexts = new Resource[]{};

		if (tokens.length > pos) {
			contexts = new Resource[tokens.length - pos];
			for (int i = pos; i < tokens.length; i++) {
				contexts[i - pos] = getContext(repository, tokens[i]);
			}
		}
		return contexts;
	}
}
