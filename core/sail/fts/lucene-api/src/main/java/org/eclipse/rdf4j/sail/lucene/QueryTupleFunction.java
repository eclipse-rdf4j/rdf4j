/**
 * Copyright (c) 2015 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.sail.lucene;

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;

public class QueryTupleFunction implements TupleFunction {

	@Override
	public String getURI() {
		return LuceneSailSchema.SEARCH.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction#evaluate(org.eclipse.rdf4j.model.
	 * ValueFactory, org.eclipse.rdf4j.model.Value[])
	 */
	@Override
	public CloseableIteration<? extends List<? extends Value>, QueryEvaluationException> evaluate(
			ValueFactory valueFactory, Value... args)
		throws QueryEvaluationException
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO");
	}

}
