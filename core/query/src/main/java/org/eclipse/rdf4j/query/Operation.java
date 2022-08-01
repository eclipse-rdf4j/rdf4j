/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query;

import org.eclipse.rdf4j.model.Value;

/**
 * An operation (e.g. a query or an update) on a repository that can be formulated in one of the supported query
 * languages (for example SPARQL). It allows one to predefine bindings in the operation to be able to reuse the same
 * operation with different bindings.
 *
 * @author Jeen
 */
public interface Operation {

	/**
	 * Binds the specified variable to the supplied value. Any value that was previously bound to the specified value
	 * will be overwritten.
	 *
	 * @param name  The name of the variable that should be bound.
	 * @param value The (new) value for the specified variable.
	 */
	void setBinding(String name, Value value);

	/**
	 * Removes a previously set binding on the supplied variable. Calling this method with an unbound variable name has
	 * no effect.
	 *
	 * @param name The name of the variable from which the binding is to be removed.
	 */
	void removeBinding(String name);

	/**
	 * Removes all previously set bindings.
	 */
	void clearBindings();

	/**
	 * Retrieves the bindings that have been set on this operation.
	 *
	 * @return A (possibly empty) set of operation variable bindings.
	 * @see #setBinding(String, Value)
	 */
	BindingSet getBindings();

	/**
	 * Specifies the dataset against which to execute an operation, overriding any dataset that is specified in the
	 * operation itself.
	 */
	void setDataset(Dataset dataset);

	/**
	 * Gets the dataset that has been set using {@link #setDataset(Dataset)}, if any.
	 */
	Dataset getDataset();

	/**
	 * Determine whether evaluation results of this operation should include inferred statements (if any inferred
	 * statements are present in the repository). The default setting is 'true'.
	 *
	 * @param includeInferred indicates whether inferred statements should be included in the result.
	 */
	void setIncludeInferred(boolean includeInferred);

	/**
	 * Returns whether or not this operation will return inferred statements (if any are present in the repository).
	 *
	 * @return <var>true</var> if inferred statements will be returned, <var>false</var> otherwise.
	 */
	boolean getIncludeInferred();

	/**
	 * Specifies the maximum time that an operation is allowed to run. The operation will be interrupted when it exceeds
	 * the time limit. Any consecutive requests to fetch query results will result in {@link QueryInterruptedException}s
	 * or {@link UpdateExecutionException}s (depending on whether the operation is a query or an update).
	 *
	 * @param maxExecutionTimeSeconds The maximum query time, measured in seconds. A negative or zero value indicates an
	 *                                unlimited execution time (which is the default).
	 */
	void setMaxExecutionTime(int maxExecutionTimeSeconds);

	/**
	 * Returns the maximum operation execution time.
	 *
	 * @return The maximum operation execution time, measured in seconds.
	 * @see #setMaxExecutionTime(int)
	 */
	int getMaxExecutionTime();

}
