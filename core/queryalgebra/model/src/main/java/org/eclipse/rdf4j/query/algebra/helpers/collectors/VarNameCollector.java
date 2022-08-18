/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.query.algebra.helpers.collectors;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;

/**
 * A QueryModelVisitor that collects the names of (non-constant) variables that are used in a query model.
 */
public class VarNameCollector extends AbstractSimpleQueryModelVisitor<RuntimeException> {

	private final List<String> varNames = new ArrayList<>();
	private Set<String> varNamesSet;

	public VarNameCollector() {
		super(true);
	}

	public static Set<String> process(QueryModelNode node) {
		VarNameCollector collector = new VarNameCollector();
		node.visit(collector);

		return collector.getVarNames();
	}

	public Set<String> getVarNames() {
		if (varNamesSet == null) {
			if (varNames.isEmpty()) {
				varNamesSet = Set.of();
			} else if (varNames.size() == 1) {
				varNamesSet = Set.of(varNames.get(0));
			} else {
				varNamesSet = new LinkedHashSet<>(varNames);
			}
		}
		return varNamesSet;
	}

	@Override
	public void meet(Var var) {
		if (!var.hasValue()) {
			varNames.add(var.getName());
		}
	}
}
