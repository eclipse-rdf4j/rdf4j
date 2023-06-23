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
package org.eclipse.rdf4j.sail.base;

import java.util.HashSet;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.DistinctIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.RDFStarTripleSource;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.TripleSourceIterationWrapper;

/**
 * Implementation of the TripleSource interface using {@link SailDataset}
 */
@InternalUseOnly
public class SailDatasetTripleSource implements RDFStarTripleSource {

	private final ValueFactory vf;

	private final SailDataset dataset;

	public SailDatasetTripleSource(ValueFactory vf, SailDataset dataset) {
		this.vf = vf;
		this.dataset = dataset;
	}

	@Override
	public String toString() {
		return dataset.toString();
	}

	@Override
	public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(Resource subj, IRI pred,
			Value obj, Resource... contexts) throws QueryEvaluationException {
		CloseableIteration<? extends Statement, SailException> statements = null;
		try {
			statements = dataset.getStatements(subj, pred, obj, contexts);
			return new TripleSourceIterationWrapper<>(statements);
		} catch (Throwable t) {
			if (statements != null) {
				statements.close();
			}
			if (t instanceof SailException) {
				throw new QueryEvaluationException(t);
			}
			throw t;
		}
	}

	@Override
	public ValueFactory getValueFactory() {
		return vf;
	}

	@Override
	public CloseableIteration<? extends Triple, QueryEvaluationException> getRdfStarTriples(Resource subj, IRI pred,
			Value obj) throws QueryEvaluationException {
		CloseableIteration<? extends Triple, SailException> triples = null;
		TripleSourceIterationWrapper<? extends Triple> iterationWrapper = null;
		try {
			// In contrast to statement retrieval (which gets de-duplicated later on when handling things like
			// projections and conversions) we need to make sure we de-duplicate the RDF-star triples here.
			triples = dataset.getTriples(subj, pred, obj);
			iterationWrapper = new TripleSourceIterationWrapper<>(triples);
			// TODO: see if use of collection factory is possible here.
			return new DistinctIteration<>(iterationWrapper, new HashSet<>());
		} catch (Throwable t) {
			try {
				if (triples != null) {
					triples.close();
				}
			} finally {
				if (iterationWrapper != null) {
					iterationWrapper.close();
				}
			}

			if (t instanceof SailException) {
				throw new QueryEvaluationException(t);
			}
			throw t;
		}
	}
}
