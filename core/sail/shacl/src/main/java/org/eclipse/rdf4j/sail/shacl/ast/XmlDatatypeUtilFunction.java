/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * A custom SPARQL function that checks that a literal conforms to a given datatype by checking the datatype of the
 * literal and also checking if the literal is ill-typed if the datatype is a supported XSD datatype.
 */
public class XmlDatatypeUtilFunction implements Function {

	public String getURI() {
		return RSX.valueConformsToXsdDatatypeFunction.stringValue();
	}

	@Override
	public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Value evaluate(TripleSource tripleSource, Value... args) {
		if (args.length != 2) {
			throw new ValueExprEvaluationException(
					"valueConformsToXsdDatatypeFunction requires exactly 2 arguments, got " + args.length);
		}

		assert Arrays.stream(args).noneMatch(Objects::isNull);

		if (!(args[0].isLiteral())) {
			throw new ValueExprEvaluationException("invalid first argument (literal expected): " + args[0]);
		}

		if (!(args[1].isIRI())) {
			throw new ValueExprEvaluationException("invalid second argument (IRI expected): " + args[1]);
		}

		Literal literal = (Literal) args[0];
		CoreDatatype coreDatatype = CoreDatatype.from((IRI) args[1]);
		if (literal.getCoreDatatype() != coreDatatype) {
			return BooleanLiteral.FALSE;
		} else if (coreDatatype == CoreDatatype.NONE && !args[1].equals(literal.getDatatype())) {
			return BooleanLiteral.FALSE;
		}

		if (coreDatatype.isXSDDatatype()) {
			return BooleanLiteral.valueOf(XMLDatatypeUtil.isValidValue(literal.stringValue(), coreDatatype));
		}

		return BooleanLiteral.TRUE;
	}

}
