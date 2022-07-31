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
import java.util.Map;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SPIN;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.util.TripleSources;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.spin.Argument;
import org.eclipse.rdf4j.spin.MalformedSpinException;
import org.eclipse.rdf4j.spin.SpinParser;

public class SpinFunctionParser implements FunctionParser {

	private final SpinParser parser;

	public SpinFunctionParser(SpinParser parser) {
		this.parser = parser;
	}

	@Override
	public Function parse(IRI funcUri, TripleSource store) throws RDF4JException {
		Value body = TripleSources.singleValue(funcUri, SPIN.BODY_PROPERTY, store);
		if (!(body instanceof Resource)) {
			return null;
		}
		ParsedQuery query = parser.parseQuery((Resource) body, store);
		if (query instanceof ParsedGraphQuery) {
			throw new MalformedSpinException("Function body must be an ASK or SELECT query");
		}

		Map<IRI, Argument> templateArgs = parser.parseArguments(funcUri, store);

		SpinFunction func = new SpinFunction(funcUri.stringValue());
		func.setParsedQuery(query);
		List<IRI> orderedArgs = SpinParser.orderArguments(templateArgs.keySet());
		for (IRI IRI : orderedArgs) {
			Argument arg = templateArgs.get(IRI);
			func.addArgument(arg);
		}

		return func;
	}
}
