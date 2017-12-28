/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.spin.function.spif;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.text.StrSubstitutor;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SPIF;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

public class BuildURI implements Function {

	@Override
	public String getURI() {
		return SPIF.BUILD_URI_FUNCTION.toString();
	}

	@Override
	public Value evaluate(ValueFactory valueFactory, Value... args)
		throws ValueExprEvaluationException
	{
		if (args.length < 1) {
			throw new ValueExprEvaluationException("Incorrect number of arguments");
		}
		if (!(args[0] instanceof Literal)) {
			throw new ValueExprEvaluationException("First argument must be a string");
		}
		Literal s = (Literal)args[0];
		String tmpl = s.getLabel();
		Map<String, String> mappings = new HashMap<String, String>(args.length);
		for (int i = 1; i < args.length; i++) {
			mappings.put(Integer.toString(i), args[i].stringValue());
		}
		String newValue = StrSubstitutor.replace(tmpl, mappings, "{?", "}");
		if (tmpl.charAt(0) == '<' && tmpl.charAt(tmpl.length() - 1) == '>') {
			return valueFactory.createURI(newValue.substring(1, newValue.length() - 1));
		}
		throw new ValueExprEvaluationException("Invalid URI template: " + tmpl);
	}
}
