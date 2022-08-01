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
package org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics;

import java.util.stream.Stream;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleSailStore;

/**
 * ExtensibleDirectEvaluationStatistics provides evaluation statistics by directly querying the underlying data source.
 */
@Experimental
public class ExtensibleDirectEvaluationStatistics extends ExtensibleEvaluationStatistics {
	public ExtensibleDirectEvaluationStatistics(ExtensibleSailStore extensibleSailStore) {
		super(extensibleSailStore);
	}

	@Override
	protected CardinalityCalculator createCardinalityCalculator() {
		return cardinalityCalculator;
	}

	CardinalityCalculator cardinalityCalculator = new CardinalityCalculator() {
		@Override
		protected double getCardinality(StatementPattern sp) {

			SailDataset dataset = extensibleSailStore.getExplicitSailSource().dataset(IsolationLevels.NONE);

			Resource subject = (Resource) sp.getSubjectVar().getValue();
			IRI predicate = (IRI) sp.getPredicateVar().getValue();
			Value object = sp.getObjectVar().getValue();

			if (sp.getScope() == StatementPattern.Scope.DEFAULT_CONTEXTS) {
				try (Stream<? extends Statement> stream = Iterations
						.stream(dataset.getStatements(subject, predicate, object))) {
					return stream.count();
				}
			} else {
				Resource[] context = new Resource[] { (Resource) sp.getContextVar().getValue() };
				try (Stream<? extends Statement> stream = Iterations
						.stream(dataset.getStatements(subject, predicate, object, context))) {
					return stream.count();
				}
			}

		}
	};

}
