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

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;

import com.google.common.base.Function;

public class KnownFunctionParser implements FunctionParser {

	private final FunctionRegistry functionRegistry;

	private final Function<IRI, String> wellKnownFunctions;

	public KnownFunctionParser(FunctionRegistry functionRegistry, Function<IRI, String> wellKnownFunctions) {
		this.functionRegistry = functionRegistry;
		this.wellKnownFunctions = wellKnownFunctions;
	}

	@Override
	public org.eclipse.rdf4j.query.algebra.evaluation.function.Function parse(IRI funcUri, TripleSource store)
			throws RDF4JException {
		{
			String name = null;
			if (wellKnownFunctions != null) {
				name = wellKnownFunctions.apply(funcUri);
			}
			if (name == null) {
				name = funcUri.stringValue();
			}
			return functionRegistry.get(name).orElse(null);
		}
	}
}
