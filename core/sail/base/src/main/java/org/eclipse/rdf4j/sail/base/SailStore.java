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
package org.eclipse.rdf4j.sail.base;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;

/**
 * A high level interface used by {@link SailSourceConnection} to access {@link SailSource}.
 *
 * @author James Leigh
 */
public interface SailStore extends SailClosable {

	/**
	 * The {@link ValueFactory} that should be used in association with this.
	 *
	 * @return this object's {@link ValueFactory}
	 */
	ValueFactory getValueFactory();

	/**
	 * Used by {@link SailSourceConnection} to determine query join order.
	 *
	 * @return a {@link EvaluationStatistics} that is aware of the data distribution of this {@link SailStore} .
	 */
	EvaluationStatistics getEvaluationStatistics();

	/**
	 * @return {@link SailSource} of only explicit statements
	 */
	SailSource getExplicitSailSource();

	/**
	 * @return {@link SailSource} of only inferred statements
	 */
	SailSource getInferredSailSource();

}
