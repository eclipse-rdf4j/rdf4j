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
package org.eclipse.rdf4j.spin.function.spif;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

abstract class AbstractStringReplacer implements Function {

	private final String uri;

	AbstractStringReplacer(String uri) {
		this.uri = uri;
	}

	@Override
	public String getURI() {
		return uri;
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
		String s = ((Literal) args[0]).getLabel();
		String regex = (args.length == 2) ? ((Literal) args[1]).getLabel() : ".";
		StringBuffer buf = new StringBuffer(s.length());
		Matcher matcher = Pattern.compile(regex).matcher(s);
		while (matcher.find()) {
			String g = matcher.group();
			matcher.appendReplacement(buf, transform(g));
		}
		matcher.appendTail(buf);
		return valueFactory.createLiteral(buf.toString());
	}

	protected abstract String transform(String s);
}
