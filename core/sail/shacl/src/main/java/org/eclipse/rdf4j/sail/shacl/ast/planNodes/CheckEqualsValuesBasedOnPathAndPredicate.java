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

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;

import com.google.common.collect.Sets;

/**
 * Used by sh:equals to return any targets and values where the target has values by path that are not values by the
 * predicate, or vice versa. It returns the targets and any symmetricDifference values when comparing the set of values
 * by path and by predicate.
 *
 * @author Håvard Ottestad
 */
public class CheckEqualsValuesBasedOnPathAndPredicate extends AbstractPairwisePlanNode {

	public CheckEqualsValuesBasedOnPathAndPredicate(SailConnection connection, Resource[] dataGraph, PlanNode parent,
			IRI predicate, StatementMatcher.Variable<Resource> subject, StatementMatcher.Variable<Value> object,
			SparqlFragment targetQueryFragment, Shape shape, ConstraintComponent constraintComponent,
			boolean produceValidationReports) {
		super(connection, dataGraph, parent, predicate, subject, object, targetQueryFragment, shape,
				constraintComponent, produceValidationReports);
	}

	Set<Value> getInvalidValues(Set<Value> valuesByPath, Set<Value> valuesByPredicate) {
		return Sets.symmetricDifference(valuesByPath, valuesByPredicate);
	}

}
