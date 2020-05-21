/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

import org.eclipse.rdf4j.common.annotation.Experimental;

/**
 * An RDF* triple. Triples have a subject, predicate and object. Unlike {@link Statement}, a triple never has an
 * associated context.
 *
 * @author Pavel Mihaylov
 */
@Experimental
public interface Triple extends Resource {

	/**
	 * Gets the subject of this triple.
	 *
	 * @return The triple's subject.
	 */
	Resource getSubject();

	/**
	 * Gets the predicate of this triple.
	 *
	 * @return The triple's predicate.
	 */
	IRI getPredicate();

	/**
	 * Gets the object of this triple.
	 *
	 * @return The triple's object.
	 */
	Value getObject();
}
