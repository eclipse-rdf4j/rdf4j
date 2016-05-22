/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.spin.function.spif;

import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SPIF;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;
import org.eclipse.rdf4j.spin.function.AbstractSpinFunction;

public class Invoke extends AbstractSpinFunction implements Function {

	public Invoke() {
		super(SPIF.INVOKE_FUNCTION.stringValue());
	}

	@Override
	public Value evaluate(ValueFactory valueFactory, Value... args)
		throws ValueExprEvaluationException
	{
		if (args.length == 0) {
			throw new ValueExprEvaluationException("At least one argument is required");
		}
		if (!(args[0] instanceof URI)) {
			throw new ValueExprEvaluationException("The first argument must be a function URI");
		}
		URI func = (URI)args[0];
		Value[] funcArgs = new Value[args.length - 1];
		System.arraycopy(args, 1, funcArgs, 0, funcArgs.length);
		Function function = FunctionRegistry.getInstance().get(func.stringValue());
		return function.evaluate(valueFactory, funcArgs);
	}
}
