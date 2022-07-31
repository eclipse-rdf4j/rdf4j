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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.AFN;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.UnaryFunction;

public class Localname extends UnaryFunction {

	@Override
	public String getURI() {
		return AFN.LOCALNAME.toString();
	}

	@Override
	protected Value evaluate(ValueFactory valueFactory, Value arg) throws ValueExprEvaluationException {
		if (arg instanceof IRI) {
			IRI uri = (IRI) arg;
			return valueFactory.createLiteral(uri.getLocalName());
		} else {
			throw new ValueExprEvaluationException("Not a URI");
		}
	}
}
