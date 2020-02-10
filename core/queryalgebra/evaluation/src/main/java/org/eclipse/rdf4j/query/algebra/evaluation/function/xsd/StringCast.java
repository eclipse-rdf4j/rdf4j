/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.xsd;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;

/**
 * A {@link org.eclipse.rdf4j.query.algebra.evaluation.function.Function} that tries to cast its argument to an
 * <tt>xsd:string</tt>.
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
			if (QueryEvaluationUtil.isSimpleLiteral(literal)) {
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
			return valueFactory.createLiteral(value.toString(), XMLSchema.STRING);
		} else if (value instanceof Literal) {
			Literal literal = (Literal) value;
			IRI datatype = literal.getDatatype();

			if (QueryEvaluationUtil.isSimpleLiteral(literal)) {
				return valueFactory.createLiteral(literal.getLabel(), XMLSchema.STRING);
			} else if (!Literals.isLanguageLiteral(literal)) {
				if (XMLDatatypeUtil.isNumericDatatype(datatype) || datatype.equals(XMLSchema.BOOLEAN)
						|| datatype.equals(XMLSchema.DATETIME) || datatype.equals(XMLSchema.DATETIMESTAMP)) {
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
						return valueFactory.createLiteral(normalizedValue, XMLSchema.STRING);
					} else {
						return valueFactory.createLiteral(literal.getLabel(), XMLSchema.STRING);
					}
				} else {
					// for unknown datatypes, just use the lexical value.
					return valueFactory.createLiteral(literal.getLabel(), XMLSchema.STRING);
				}
			}
		}

		throw typeError(value, null);
	}

	@Override
	protected IRI getXsdDatatype() {
		return XMLSchema.STRING;
	}

	@Override
	protected boolean isValidForDatatype(String lexicalValue) {
		return true;
	}
}
