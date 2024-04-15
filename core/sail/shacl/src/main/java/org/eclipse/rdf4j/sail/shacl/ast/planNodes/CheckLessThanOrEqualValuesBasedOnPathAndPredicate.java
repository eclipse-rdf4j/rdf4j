/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;

public class CheckLessThanOrEqualValuesBasedOnPathAndPredicate extends AbstractPairwisePlanNode {

	public CheckLessThanOrEqualValuesBasedOnPathAndPredicate(SailConnection connection, Resource[] dataGraph,
			PlanNode parent, IRI predicate, StatementMatcher.Variable<Resource> subject,
			StatementMatcher.Variable<Value> object, SparqlFragment targetQueryFragment, Shape shape,
			ConstraintComponent constraintComponent, boolean produceValidationReports) {
		super(connection, dataGraph, parent, predicate, subject, object, targetQueryFragment, shape,
				constraintComponent, produceValidationReports);
	}

	Set<Value> getInvalidValues(Set<Value> valuesByPath, Set<Value> valuesByPredicate) {
		HashSet<Value> ret = new HashSet<>();

		for (Value value : valuesByPath) {
			for (Value value1 : valuesByPredicate) {
				boolean lessThanOrEquals = QueryEvaluationUtility.compare(value, value1, Compare.CompareOp.LE)
						.orElse(false);
				if (!lessThanOrEquals) {
					ret.add(value);
				}
			}
		}

		return ret;

	}

}
