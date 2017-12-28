/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.spin.function.spif;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SPIF;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.UnaryFunction;

public class Trim extends UnaryFunction {

	@Override
	public String getURI() {
		return SPIF.TRIM_FUNCTION.toString();
	}

	@Override
	protected Value evaluate(ValueFactory valueFactory, Value arg)
		throws ValueExprEvaluationException
	{
		if (!(arg instanceof Literal)) {
			throw new ValueExprEvaluationException("Argument must be a literal");
		}
		return valueFactory.createLiteral(arg.stringValue().trim());
	}
}
