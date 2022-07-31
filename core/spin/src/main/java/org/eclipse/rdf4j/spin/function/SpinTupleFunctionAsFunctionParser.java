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
package org.eclipse.rdf4j.spin.function;

import java.util.List;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SPIN;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;
import org.eclipse.rdf4j.query.algebra.evaluation.util.TripleSources;
import org.eclipse.rdf4j.spin.SpinParser;

public class SpinTupleFunctionAsFunctionParser implements FunctionParser {

	private final SpinParser parser;

	public SpinTupleFunctionAsFunctionParser(SpinParser parser) {
		this.parser = parser;
	}

	@Override
	public Function parse(IRI funcUri, TripleSource store) throws RDF4JException {
		Statement magicPropStmt = TripleSources.single(funcUri, RDF.TYPE, SPIN.MAGIC_PROPERTY_CLASS, store);
		if (magicPropStmt == null) {
			return null;
		}

		Value body = TripleSources.singleValue(funcUri, SPIN.BODY_PROPERTY, store);
		if (!(body instanceof Resource)) {
			return null;
		}
		final TupleFunction tupleFunc = parser.parseMagicProperty(funcUri, store);
		return new TransientFunction() {

			@Override
			public String getURI() {
				return tupleFunc.getURI();
			}

			@Override
			public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
				try {
					try (CloseableIteration<? extends List<? extends Value>, QueryEvaluationException> iter = tupleFunc
							.evaluate(valueFactory, args)) {
						if (iter.hasNext()) {
							return iter.next().get(0);
						} else {
							return null;
						}
					}
				} catch (QueryEvaluationException e) {
					throw new ValueExprEvaluationException(e);
				}
			}
		};
	}
}
