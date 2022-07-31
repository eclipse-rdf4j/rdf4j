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
package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.impl.SimpleDataset;

public class PlanNodeHelper {

	private final static SimpleDataset rdf4jNilDataset;

	static {
		rdf4jNilDataset = new SimpleDataset();
		rdf4jNilDataset.addDefaultGraph(RDF4J.NIL);
	}

	public static PlanNode handleSorting(PlanNode child, PlanNode parent) {
		return handleSorting(child.requiresSorted(), parent);
	}

	public static PlanNode handleSorting(boolean requiresSorted, PlanNode parent) {
		if (requiresSorted) {
			if (!parent.producesSorted()) {
				parent = new Sort(parent);
			}
		}
		return parent;
	}

	public static Dataset asDefaultGraphDataset(Resource[] dataGraph) {
		if (dataGraph.length == 0) {
			return null;
		} else if (dataGraph.length == 1 && dataGraph[0] == null) {
			return rdf4jNilDataset;
		}

		SimpleDataset dataGraphDataset = new SimpleDataset();
		for (Resource context : dataGraph) {
			if (context == null) {
				dataGraphDataset.addDefaultGraph(RDF4J.NIL);
			} else if (context.isIRI()) {
				dataGraphDataset.addDefaultGraph(((IRI) context));
			} else {
				throw new IllegalArgumentException("Trying to validate a data graph that is not an IRI.");
			}
		}
		return dataGraphDataset;
	}

}
