/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

/**
 * A blank node (aka <em>bnode</em>, aka <em>anonymous node</em>). A blank node
 * has an identifier to be able to compare it to other blank nodes internally.
 * Please note that, conceptually, blank node equality can only be determined by
 * examining the statements that refer to them.
 */
public interface BNode extends Resource {

	/**
	 * retrieves this blank node's identifier.
	 *
	 * @return A blank node identifier.
	 */
	public String getID();
	
	/**
	 * Compares a blank node object to another object.
	 *
	 * @param o The object to compare this blank node to.
	 * @return <tt>true</tt> if the other object is an instance of {@link BNode}
	 * and their IDs are equal, <tt>false</tt> otherwise.
	 */
	public boolean equals(Object o);
	
	/**
	 * The hash code of a blank node is defined as the hash code of its
	 * identifier: <tt>id.hashCode()</tt>.
	 * 
	 * @return A hash code for the blank node.
	 */
	public int hashCode();
}
