/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.memory.model.MemIRI;
import org.eclipse.rdf4j.sail.memory.model.MemResource;
import org.eclipse.rdf4j.sail.memory.model.MemStatementList;
import org.eclipse.rdf4j.sail.memory.model.MemValue;
import org.eclipse.rdf4j.sail.memory.model.MemValueFactory;

/**
 * Uses the MemoryStore's statement sizes to give cost estimates based on the size of the expected results. This process
 * could be improved with repository statistics about size and distribution of statements.
 *
 * @author Arjohn Kampman
 * @author James Leigh
 */
class MemEvaluationStatistics extends EvaluationStatistics {

	private final MemValueFactory valueFactory;
	private final MemStatementList memStatementList;

	MemEvaluationStatistics(MemValueFactory valueFactory, MemStatementList memStatementList) {
		this.valueFactory = valueFactory;
		this.memStatementList = memStatementList;
	}

	@Override
	protected CardinalityCalculator createCardinalityCalculator() {
		return new MemCardinalityCalculator();
	}

	protected class MemCardinalityCalculator extends CardinalityCalculator {

		@Override
		public double getCardinality(StatementPattern sp) {

			Value subj = getConstantValue(sp.getSubjectVar());
			if (!(subj != null && subj.isResource())) {
				// can happen when a previous optimizer has inlined a comparison
				// operator.
				// this can cause, for example, the subject variable to be
				// equated to a literal value.
				// See SES-970 / SES-998
				subj = null;
			}
			Value pred = getConstantValue(sp.getPredicateVar());
			if (!(pred != null && pred.isIRI())) {
				// can happen when a previous optimizer has inlined a comparison
				// operator. See SES-970 / SES-998
				pred = null;
			}
			Value obj = getConstantValue(sp.getObjectVar());
			Value context = getConstantValue(sp.getContextVar());
			if (!(context != null && context.isResource())) {
				// can happen when a previous optimizer has inlined a comparison
				// operator. See SES-970 / SES-998
				context = null;
			}

			// Perform look-ups for value-equivalents of the specified values
			MemResource memSubj = valueFactory.getMemResource((Resource) subj);
			MemIRI memPred = valueFactory.getMemURI((IRI) pred);
			MemValue memObj = valueFactory.getMemValue(obj);
			MemResource memContext = valueFactory.getMemResource((Resource) context);

			if (subj != null && memSubj == null || pred != null && memPred == null || obj != null && memObj == null
					|| context != null && memContext == null) {
				// non-existent subject, predicate, object or context
				return 0.0;
			}

			// Search for the smallest list that can be used by the iterator
			int minListSizes = Integer.MAX_VALUE;
			if (memSubj != null) {
				minListSizes = Math.min(minListSizes, memSubj.getSubjectStatementCount());
			}
			if (memPred != null) {
				minListSizes = Math.min(minListSizes, memPred.getPredicateStatementCount());
			}
			if (memObj != null) {
				minListSizes = Math.min(minListSizes, memObj.getObjectStatementCount());
			}
			if (memContext != null) {
				minListSizes = Math.min(minListSizes, memContext.getContextStatementCount());
			}

			double cardinality;

			if (minListSizes == Integer.MAX_VALUE) {
				// all wildcards
				cardinality = memStatementList.size();
			} else {
				cardinality = minListSizes;

				// List<Var> vars = getVariables(sp);
				// int constantVarCount = countConstantVars(vars);
				//
				// // Subtract 1 from var count as this was used for the list
				// size
				// double unboundVarFactor = (double)(vars.size() -
				// constantVarCount) / (vars.size() - 1);
				//
				// cardinality = Math.pow(cardinality, unboundVarFactor);
			}

			return cardinality;
		}

		protected Value getConstantValue(Var var) {
			if (var != null) {
				return var.getValue();
			}

			return null;
		}
	}
} // end inner class MemCardinalityCalculator
