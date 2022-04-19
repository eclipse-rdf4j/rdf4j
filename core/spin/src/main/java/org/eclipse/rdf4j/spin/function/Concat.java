/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.spin.function;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * Extended version of concat for SPIN.
 */
public class Concat implements Function {

	@Override
	public String getURI() {
		return FN.CONCAT.toString();
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length == 0) {
			throw new ValueExprEvaluationException("CONCAT requires at least 1 argument, got " + args.length);
		}

		StringBuilder concatBuilder = new StringBuilder();
		String languageTag = null;

		boolean useLanguageTag = true;
		boolean useDatatype = true;

		for (Value arg : args) {
			if (arg instanceof Literal) {
				Literal lit = (Literal) arg;

				// verify that every literal argument has the same language tag. If
				// not, the operator result should not use a language tag.
				if (useLanguageTag && Literals.isLanguageLiteral(lit)) {
					if (languageTag == null) {
						languageTag = lit.getLanguage().get();
					} else if (!languageTag.equals(lit.getLanguage().get())) {
						languageTag = null;
						useLanguageTag = false;
					}
				} else {
					useLanguageTag = false;
				}

				// check datatype: concat only expects plain, language-tagged or
				// string-typed literals. If all arguments are of type xsd:string,
				// the result also should be,
				// otherwise the result will not have a datatype.
				if (lit.getDatatype() == null) {
					useDatatype = false;
				}

				concatBuilder.append(lit.getLabel());
			} else {
				throw new ValueExprEvaluationException("unexpected argument type for concat operator: " + arg);
			}
		}

		Literal result;

		if (useDatatype) {
			result = valueFactory.createLiteral(concatBuilder.toString(), XSD.STRING);
		} else if (useLanguageTag) {
			result = valueFactory.createLiteral(concatBuilder.toString(), languageTag);
		} else {
			result = valueFactory.createLiteral(concatBuilder.toString());
		}

		return result;

	}

}
