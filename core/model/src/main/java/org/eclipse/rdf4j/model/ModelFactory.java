/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

/**
 * Factory to create empty {@link Model} implementations.
 *
 * @author James Leigh
 */
public interface ModelFactory {

	/**
	 * @return a newly created {@link Model}
	 */
	Model createEmptyModel();

}
