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
package org.eclipse.rdf4j.federated.algebra;

import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * Interface marking known FedX algebra nodes.
 *
 * @author Andreas Schwarte
 * @see StatementTupleExpr
 * @see ExclusiveTupleExpr
 */
public interface FedXTupleExpr extends TupleExpr, VariableExpr, QueryRef {

	/**
	 * @return the number of free (i.e. unbound) variables in this expression
	 */
	default int getFreeVarCount() {
		return getFreeVars().size();
	}
}
