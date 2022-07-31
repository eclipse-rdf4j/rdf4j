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

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SPIF;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.UnaryFunction;

public class UnCamelCase extends UnaryFunction {

	@Override
	public String getURI() {
		return SPIF.UN_CAMEL_CASE_FUNCTION.toString();
	}

	@Override
	protected Value evaluate(ValueFactory valueFactory, Value arg) throws ValueExprEvaluationException {
		if (!(arg instanceof Literal)) {
			throw new ValueExprEvaluationException("Argument must be a string");
		}
		String s = ((Literal) arg).getLabel();
		StringBuilder buf = new StringBuilder(s.length() + 10);
		char prev = '\0';
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (Character.isLowerCase(prev) && Character.isUpperCase(ch)) {
				buf.append(' ');
				buf.append(Character.toLowerCase(ch));
			} else if (ch == '_') {
				buf.append(' ');
			} else {
				buf.append(ch);
			}
			prev = ch;
		}
		return valueFactory.createLiteral(buf.toString());
	}
}
