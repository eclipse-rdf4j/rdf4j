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
package org.eclipse.rdf4j.federated.optimizer;

import java.util.Comparator;

import org.eclipse.rdf4j.federated.algebra.ExclusiveGroup;
import org.eclipse.rdf4j.federated.algebra.ExclusiveStatement;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * Comparator:
 *
 * partial order: OwnedStatementSourcePatternGroup -> OwnedStatementSourcePattern -> StatementSourcePattern
 *
 * @author Andreas
 *
 */
public class NaryJoinArgumentsComparator implements Comparator<TupleExpr> {

	@Override
	public int compare(TupleExpr a, TupleExpr b) {

		if (a instanceof ExclusiveGroup) {
			if (b instanceof ExclusiveGroup) {
				return 0;
			} else {
				return -1;
			}
		} else if (b instanceof ExclusiveGroup) {
			return 1;
		} else if (a instanceof ExclusiveStatement) {
			if (b instanceof ExclusiveStatement) {
				return 0; // 0
			} else {
				return -1; // -1
			}
		} else if (b instanceof ExclusiveStatement) {
			return 1; // 1
		}

		// XXX compare number of free variables

		return 0;
	}

}
