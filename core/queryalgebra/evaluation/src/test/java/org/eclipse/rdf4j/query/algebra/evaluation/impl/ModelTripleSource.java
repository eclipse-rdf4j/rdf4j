package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;

public final class ModelTripleSource implements TripleSource {
	private final Model m;
	private final ValueFactory vf;

	public ModelTripleSource(Model m, ValueFactory vf) {
		this.m = m;
		this.vf = vf;
	}

	@Override
	public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(Resource subj,
			IRI pred, Value obj, Resource... contexts) throws QueryEvaluationException {
		return new CloseableIteratorIteration<>(m.getStatements(subj, pred, obj, contexts).iterator());
	}

	@Override
	public ValueFactory getValueFactory() {
		return vf;
	}
}