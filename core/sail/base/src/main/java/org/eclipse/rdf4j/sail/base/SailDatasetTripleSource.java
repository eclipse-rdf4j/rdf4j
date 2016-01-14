/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ExceptionConvertingIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.sail.SailException;

/**
 * Implementation of the TripleSource interface using {@link SailDataset}
 */
class SailDatasetTripleSource implements TripleSource {

	private final ValueFactory vf;

	private final SailDataset dataset;

	public SailDatasetTripleSource(ValueFactory vf, SailDataset dataset) {
		this.vf = vf;
		this.dataset = dataset;
	}

	public String toString() {
		return dataset.toString();
	}

	public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(Resource subj,
			IRI pred, Value obj, Resource... contexts)
		throws QueryEvaluationException
	{
		try {
			return new Eval(dataset.getStatements(subj, pred, obj, contexts));
		}
		catch (SailException e) {
			throw new QueryEvaluationException(e);
		}
	}

	public ValueFactory getValueFactory() {
		return vf;
	}

	public static class Eval extends ExceptionConvertingIteration<Statement, QueryEvaluationException> {

		public Eval(Iteration<? extends Statement, ? extends Exception> iter) {
			super(iter);
		}

		protected QueryEvaluationException convert(Exception e) {
			return new QueryEvaluationException(e);
		}

	}
}
