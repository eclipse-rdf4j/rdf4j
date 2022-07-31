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
 *
 * @author jeen
 *
 * @deprecated since 3.2. Use {@link VariableScopeChange} instead.
 */
@Deprecated
public interface GraphPatternGroupable {

	/**
	 * indicates if the node represents the root of a graph pattern group.
	 *
	 * @return true iff the node represents the node of a graph pattern group.
	 *
	 */
	@Deprecated
	boolean isGraphPatternGroup();

	/**
	 * Set the value of {@link #isGraphPatternGroup()} to true or false.
	 */
	@Deprecated
	void setGraphPatternGroup(boolean isGraphPatternGroup);

}
