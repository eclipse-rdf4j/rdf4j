/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.spin;

import org.eclipse.rdf4j.OpenRDFException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.model.vocabulary.SPIN;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.spin.SpinParser;
import org.eclipse.rdf4j.spin.function.AskFunction;
import org.eclipse.rdf4j.spin.function.Concat;
import org.eclipse.rdf4j.spin.function.EvalFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * QueryOptimizer that adds support for SPIN functions.
 */
public class SpinFunctionInterpreter implements QueryOptimizer {

	private static final Logger logger = LoggerFactory.getLogger(SpinFunctionInterpreter.class);

	private final TripleSource tripleSource;

	private final SpinParser parser;

	private final FunctionRegistry functionRegistry;

	public SpinFunctionInterpreter(SpinParser parser, TripleSource tripleSource,
			FunctionRegistry functionRegistry)
	{
		this.parser = parser;
		this.tripleSource = tripleSource;
		this.functionRegistry = functionRegistry;
		if (!(functionRegistry.get(FN.CONCAT.toString()).get() instanceof Concat)) {
			functionRegistry.add(new Concat());
		}
		if (!functionRegistry.has(SPIN.EVAL_FUNCTION.toString())) {
			functionRegistry.add(new EvalFunction(parser));
		}
		if (!functionRegistry.has(SPIN.ASK_FUNCTION.toString())) {
			functionRegistry.add(new AskFunction(parser));
		}
	}

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		try {
			tupleExpr.visit(new FunctionScanner());
		}
		catch (OpenRDFException e) {
			logger.warn("Failed to parse function");
		}
	}

	private class FunctionScanner extends AbstractQueryModelVisitor<OpenRDFException> {

		ValueFactory vf = tripleSource.getValueFactory();

		@Override
		public void meet(FunctionCall node)
			throws OpenRDFException
		{
			String name = node.getURI();
			if (!functionRegistry.has(name)) {
				IRI funcUri = vf.createIRI(name);
				Function f = parser.parseFunction(funcUri, tripleSource);
				functionRegistry.add(f);
			}
			super.meet(node);
		}
	}
}
