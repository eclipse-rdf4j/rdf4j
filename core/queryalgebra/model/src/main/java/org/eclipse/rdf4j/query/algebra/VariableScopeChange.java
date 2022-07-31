/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

/**
 * {@link QueryModelNode}s that can constitute a variable scope change (such as group graph patterns, subselects, etc).
 *
 * @author Jeen Broekstra
 */
public interface VariableScopeChange {

	/**
	 * indicates if the node represents a variable scope change.
	 *
	 * @return true iff the node represents a variable scope change.
	 *
	 */
	boolean isVariableScopeChange();

	/**
	 * Set the value of {@link #isVariableScopeChange()} to true or false.
	 */
	void setVariableScopeChange(boolean isVariableScopeChange);

}
