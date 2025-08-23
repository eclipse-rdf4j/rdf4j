/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.ir;

import java.util.ArrayList;
import java.util.List;

/**
 * Textual IR node representing a UNION with multiple branches.
 */
public class IrUnion extends IrNode {
	private final List<IrWhere> branches = new ArrayList<>();

	public List<IrWhere> getBranches() {
		return branches;
	}

	public void addBranch(IrWhere w) {
		if (w != null) {
			branches.add(w);
		}
	}
}
