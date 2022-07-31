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
package org.eclipse.rdf4j.query.algebra.evaluation.function.xsd;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;

/**
 * A {@link org.eclipse.rdf4j.query.algebra.evaluation.function.Function} that tries to cast its argument to an
 * <var>xsd:string</var>.
 *
 * @author Arjohn Kampman
 * @author Jeen Broekstra
 */
public class StringCast extends CastFunction {

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 1) {
			throw new ValueExprEvaluationException(
					getXsdName() + " cast requires exactly 1 argument, got " + args.length);
		}

		if (args[0] instanceof Literal) {
			Literal literal = (Literal) args[0];
			IRI datatype = literal.getDatatype();

			// we override because unlike most other cast functions, xsd:string should not accept a language-tagged
			// string literal.
			if (QueryEvaluationUtility.isSimpleLiteral(literal)) {
				String lexicalValue = XMLDatatypeUtil.collapseWhiteSpace(literal.getLabel());
				if (isValidForDatatype(lexicalValue)) {
					return valueFactory.createLiteral(lexicalValue, getXsdDatatype());
				}
			} else if (datatype != null) {
				if (datatype.equals(getXsdDatatype())) {
					return literal;
				}
			}
			return convert(valueFactory, literal);
		} else {
			return convert(valueFactory, args[0]);
		}
	}

	@Override
	protected Literal convert(ValueFactory valueFactory, Value value) throws ValueExprEvaluationException {
		if (value instanceof IRI) {
			return valueFactory.createLiteral(value.toString(), XSD.STRING);
		} else if (value instanceof Literal) {
			Literal literal = (Literal) value;
			IRI datatype = literal.getDatatype();

			if (QueryEvaluationUtility.isSimpleLiteral(literal)) {
				return valueFactory.createLiteral(literal.getLabel(), XSD.STRING);
			} else if (!Literals.isLanguageLiteral(literal)) {
				if (XMLDatatypeUtil.isNumericDatatype(datatype) || datatype.equals(XSD.BOOLEAN)
						|| datatype.equals(XSD.DATETIME) || datatype.equals(XSD.DATETIMESTAMP)) {
					// FIXME Slightly simplified wrt the spec, we just always use the
					// canonical value of the
					// source literal as the target lexical value. This is not 100%
					// compliant with handling of
					// some date-related datatypes.
					//
					// See
					// http://www.w3.org/TR/xpath-functions/#casting-from-primitive-to-primitive
					if (XMLDatatypeUtil.isValidValue(literal.getLabel(), datatype)) {
						String normalizedValue = XMLDatatypeUtil.normalize(literal.getLabel(), datatype);
						return valueFactory.createLiteral(normalizedValue, XSD.STRING);
					} else {
						return valueFactory.createLiteral(literal.getLabel(), XSD.STRING);
					}
				} else {
					// for unknown datatypes, just use the lexical value.
					return valueFactory.createLiteral(literal.getLabel(), XSD.STRING);
				}
			}
		}

		throw typeError(value, null);
	}

	@Override
	protected IRI getXsdDatatype() {
		return XSD.STRING;
	}

	@Override
	protected boolean isValidForDatatype(String lexicalValue) {
		return true;
	}
}
