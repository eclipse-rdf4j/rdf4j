/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.s3.storage.Catalog;

/**
 * Evaluation statistics for the S3 SAIL. Uses catalog-level file statistics (row counts and min/max ranges) to estimate
 * statement pattern cardinality.
 */
class S3EvaluationStatistics extends EvaluationStatistics {

	private final S3ValueStore valueStore;
	private final Catalog catalog;

	S3EvaluationStatistics(S3ValueStore valueStore, Catalog catalog) {
		this.valueStore = valueStore;
		this.catalog = catalog;
	}

	@Override
	protected CardinalityCalculator createCardinalityCalculator() {
		return new S3CardinalityCalculator();
	}

	protected class S3CardinalityCalculator extends CardinalityCalculator {

		@Override
		protected double getCardinality(StatementPattern sp) {
			Value subj = getConstantValue(sp.getSubjectVar());
			if (subj != null && !(subj instanceof Resource)) {
				subj = null;
			}
			Value pred = getConstantValue(sp.getPredicateVar());
			if (pred != null && !(pred instanceof IRI)) {
				pred = null;
			}
			Value obj = getConstantValue(sp.getObjectVar());
			Value context = getConstantValue(sp.getContextVar());
			if (context != null && !(context instanceof Resource)) {
				context = null;
			}
			return estimateCardinality((Resource) subj, (IRI) pred, obj, (Resource) context);
		}

		private Value getConstantValue(Var var) {
			return (var != null) ? var.getValue() : null;
		}
	}

	private double estimateCardinality(Resource subj, IRI pred, Value obj, Resource context) {
		long subjID = S3ValueStore.UNKNOWN_ID;
		if (subj != null) {
			subjID = valueStore.getId(subj);
			if (subjID == S3ValueStore.UNKNOWN_ID) {
				return 0;
			}
		}

		long predID = S3ValueStore.UNKNOWN_ID;
		if (pred != null) {
			predID = valueStore.getId(pred);
			if (predID == S3ValueStore.UNKNOWN_ID) {
				return 0;
			}
		}

		long objID = S3ValueStore.UNKNOWN_ID;
		if (obj != null) {
			objID = valueStore.getId(obj);
			if (objID == S3ValueStore.UNKNOWN_ID) {
				return 0;
			}
		}

		long contextID = S3ValueStore.UNKNOWN_ID;
		if (context != null) {
			contextID = valueStore.getId(context);
			if (contextID == S3ValueStore.UNKNOWN_ID) {
				return 0;
			}
		}

		if (catalog == null) {
			return 1000;
		}

		// Sum row counts from files whose stats allow matching the pattern,
		// then divide by number of sort orders since each triple is stored 3 times
		List<Catalog.ParquetFileInfo> files = catalog.getFiles();
		long totalMatchingRows = 0;
		for (Catalog.ParquetFileInfo file : files) {
			if (file.mayContain(subjID, predID, objID, contextID)) {
				totalMatchingRows += file.getRowCount();
			}
		}

		int numSortOrders = 3;
		return (double) totalMatchingRows / numSortOrders;
	}
}
