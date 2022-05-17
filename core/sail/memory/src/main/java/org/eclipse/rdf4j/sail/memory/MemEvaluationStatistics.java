/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

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

			if (subj == null && pred == null && obj == null && context == null) {
				return memStatementList.size();
			} else {
				return minStatementCount(subj, pred, obj, context);
			}

		}

		private int minStatementCount(Value subj, Value pred, Value obj, Value context) {
			int minListSizes = Integer.MAX_VALUE;

			if (subj != null) {
				MemResource memSubj = valueFactory.getMemResource((Resource) subj);
				if (memSubj != null) {
					minListSizes = memSubj.getSubjectStatementCount();
					if (minListSizes == 0) {
						return 0;
					}
				} else {
					// couldn't find the value in the value factory, which means that there are no statements with that
					// value
					return 0;
				}
			}

			if (pred != null) {
				MemIRI memPred = valueFactory.getMemURI((IRI) pred);
				if (memPred != null) {
					minListSizes = Math.min(minListSizes, memPred.getPredicateStatementCount());
					if (minListSizes == 0) {
						return 0;
					}
				} else {
					// couldn't find the value in the value factory, which means that there are no statements with that
					// value
					return 0;
				}
			}

			if (obj != null) {
				MemValue memObj = valueFactory.getMemValue(obj);
				if (memObj != null) {
					minListSizes = Math.min(minListSizes, memObj.getObjectStatementCount());
					if (minListSizes == 0) {
						return 0;
					}
				} else {
					// couldn't find the value in the value factory, which means that there are no statements with that
					// value
					return 0;
				}
			}

			if (context != null) {
				MemResource memContext = valueFactory.getMemResource((Resource) context);
				if (memContext != null) {
					minListSizes = Math.min(minListSizes, memContext.getContextStatementCount());
				} else {
					// couldn't find the value in the value factory, which means that there are no statements with that
					// value
					return 0;
				}
			}

			assert minListSizes != Integer.MAX_VALUE : "minListSizes should have been updated before this point";

			return minListSizes;
		}

		protected Value getConstantValue(Var var) {
			if (var != null) {
				return var.getValue();
			}

			return null;
		}
	}

}
