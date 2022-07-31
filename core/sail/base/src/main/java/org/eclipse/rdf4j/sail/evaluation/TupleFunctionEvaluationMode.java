/**
 * Copyright (c) 2015 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.sail.evaluation;

import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;

public enum TupleFunctionEvaluationMode {
	/**
	 * Uses the base SAIL along with an embedded SERVICE to perform query evaluation. The SERVICE is used to evaluate
	 * extended query algebra nodes such as {@link TupleFunction}s. (Default).
	 */
	SERVICE,
	/**
	 * Assumes the base SAIL supports an extended query algebra (e.g. {@link TupleFunction}s) and use it to perform all
	 * query evaluation.
	 */
	NATIVE,
	/**
	 * Treats the base SAIL as a simple triple source and all the query evaluation is performed by this SAIL.
	 */
	TRIPLE_SOURCE
}
