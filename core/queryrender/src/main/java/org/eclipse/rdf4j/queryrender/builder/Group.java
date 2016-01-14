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
 * Abstract representation of a group of atoms in a query
 * </p>
 * 
 * @author Michael Grove
 * @since 2.7.0
 */
public interface Group extends SupportsExpr {

	public boolean isOptional();

	public void addChild(Group theGroup);

	public int size();
}
