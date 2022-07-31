/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.explanation;

import org.eclipse.rdf4j.common.annotation.Experimental;

/**
 * This is an experimental feature. The interface may be changed, moved or potentially removed in a future release.
 *
 * The interface is used to implement query explanations (query plan)
 *
 * @since 3.2.0
 */
@Experimental
public interface Explanation {

	/**
	 * The different levels that the query explanation can be at.
	 *
	 * @since 3.2.0
	 */
	@Experimental
	enum Level {
		Unoptimized, // simple parsed
		Optimized, // parsed and optimized, which includes cost estimated
		Executed, // plan as it was executed, which includes resultSizeActual
		Timed, // plan as it was executed, including resultSizeActual and where each node has been timed
	}

	// location in maven hierarchy prevents us from using TupleExpr here
	// TupleExpr asTupleExpr();

	GenericPlanNode toGenericPlanNode();

	String toJson();

	String toDot();

}
