/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.spin.function.spif;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SPIF;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

public class EncodeURL implements Function {

	@Override
	public String getURI() {
		return SPIF.ENCODE_URL_FUNCTION.toString();
	}

	@Override
	public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length < 1 || args.length > 2) {
			throw new ValueExprEvaluationException("Incorrect number of arguments");
		}
		if (!(args[0] instanceof Literal)) {
			throw new ValueExprEvaluationException("First argument must be a string");
		}
		if (args.length == 2 && !(args[1] instanceof Literal)) {
			throw new ValueExprEvaluationException("Second argument must be a string");
		}
		Literal s = (Literal) args[0];

		try {
			Charset encoding = (args.length == 2) ? Charset.forName(((Literal) args[1]).getLabel())
					: StandardCharsets.UTF_8;
			return valueFactory.createLiteral(URLEncoder.encode(s.getLabel(), encoding));
		} catch (UnsupportedCharsetException e) {
			throw new ValueExprEvaluationException(e);
		}
	}
}
