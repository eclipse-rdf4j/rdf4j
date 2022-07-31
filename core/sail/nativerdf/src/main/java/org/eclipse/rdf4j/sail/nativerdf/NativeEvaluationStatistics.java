/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.nativerdf.model.NativeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arjohn Kampman
 * @author Enrico Minack
 */
class NativeEvaluationStatistics extends EvaluationStatistics {

	private static final Logger log = LoggerFactory.getLogger(NativeEvaluationStatistics.class);

	private final ValueStore valueStore;

	private final TripleStore tripleStore;

	public NativeEvaluationStatistics(ValueStore valueStore, TripleStore tripleStore) {
		this.valueStore = valueStore;
		this.tripleStore = tripleStore;
	}

	@Override
	protected CardinalityCalculator createCardinalityCalculator() {
		return new NativeCardinalityCalculator();
	}

	protected class NativeCardinalityCalculator extends CardinalityCalculator {

		@Override
		protected double getCardinality(StatementPattern sp) {
			try {
				Value subj = getConstantValue(sp.getSubjectVar());
				if (!(subj instanceof Resource)) {
					// can happen when a previous optimizer has inlined a comparison operator.
					// this can cause, for example, the subject variable to be equated to a literal value.
					// See SES-970
					subj = null;
				}
				Value pred = getConstantValue(sp.getPredicateVar());
				if (!(pred instanceof IRI)) {
					// can happen when a previous optimizer has inlined a comparison operator. See SES-970
					pred = null;
				}
				Value obj = getConstantValue(sp.getObjectVar());
				Value context = getConstantValue(sp.getContextVar());
				if (!(context instanceof Resource)) {
					// can happen when a previous optimizer has inlined a comparison operator. See SES-970
					context = null;
				}
				return cardinality((Resource) subj, (IRI) pred, obj, (Resource) context);
			} catch (IOException e) {
				log.error("Failed to estimate statement pattern cardinality, falling back to generic implementation",
						e);
				return super.getCardinality(sp);
			}
		}

		protected Value getConstantValue(Var var) {
			return (var != null) ? var.getValue() : null;
		}
	}

	private double cardinality(Resource subj, IRI pred, Value obj, Resource context) throws IOException {
		int subjID = NativeValue.UNKNOWN_ID;
		if (subj != null) {
			subjID = valueStore.getID(subj);
			if (subjID == NativeValue.UNKNOWN_ID) {
				return 0;
			}
		}

		int predID = NativeValue.UNKNOWN_ID;
		if (pred != null) {
			predID = valueStore.getID(pred);
			if (predID == NativeValue.UNKNOWN_ID) {
				return 0;
			}
		}

		int objID = NativeValue.UNKNOWN_ID;
		if (obj != null) {
			objID = valueStore.getID(obj);
			if (objID == NativeValue.UNKNOWN_ID) {
				return 0;
			}
		}

		int contextID = NativeValue.UNKNOWN_ID;
		if (context != null) {
			contextID = valueStore.getID(context);
			if (contextID == NativeValue.UNKNOWN_ID) {
				return 0;
			}
		}

		return tripleStore.cardinality(subjID, predID, objID, contextID);
	}
}
