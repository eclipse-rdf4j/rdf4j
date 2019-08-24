/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

/**
 * {@link QueryModelNode}s that can represent a full graph pattern group.
 * 
 * Although the notion of a graph pattern group is strictly not relevant at the algebra level, it gives an indication to
 * evaluation strategy implementations on how they can optimize join patterns wrt variable scope.
 * 
 * @author Jeen Broekstra
 *
 */
public interface GraphPatternGroupable {

	/**
	 * indicates if the node represents the root of a graph pattern group.
	 * 
	 * @return true iff the node represents the node of a graph pattern group.
	 *
	 */
	public boolean isGraphPatternGroup();

	/**
	 * Set the value of {@link #isGraphPatternGroup()} to true or false.
	 */
	public void setGraphPatternGroup(boolean isGraphPatternGroup);

}
