/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.builder;

/**
 * <p>
 * Interface for anything that supports having a collection of groups or sub-groups.
 * </p>
 * 
 * @author Michael Grove
 * @deprecated use {@link org.eclipse.rdf4j.sparqlbuilder} instead.
 */
@Deprecated
public interface SupportsGroups<T> {

	/**
	 * Add this group from the query
	 * 
	 * @param theGroup the group to add
	 * @return this builder
	 */
	public T addGroup(Group theGroup);

	/**
	 * Remove this group from the query
	 * 
	 * @param theGroup the group to remove
	 * @return this builder
	 */
	public T removeGroup(Group theGroup);
}
