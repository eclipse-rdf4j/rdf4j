/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function;

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
 * A {@link Function} that tries to cast its argument to an <tt>xsd:string</tt>.
 * 
 * @author Arjohn Kampman
 */
public class StringCast implements Function {

	public String getURI() {
		return XMLSchema.STRING.toString();
	}

	public Literal evaluate(ValueFactory valueFactory, Value... args)
		throws ValueExprEvaluationException
	{
		if (args.length != 1) {
			throw new ValueExprEvaluationException("xsd:string cast requires exactly 1 argument, got "
					+ args.length);
		}

		Value value = args[0];
		if (value instanceof IRI) {
			return valueFactory.createLiteral(value.toString(), XMLSchema.STRING);
		}
		else if (value instanceof Literal) {
			Literal literal = (Literal)value;
			IRI datatype = literal.getDatatype();

			if (QueryEvaluationUtil.isSimpleLiteral(literal)) {
				return valueFactory.createLiteral(literal.getLabel(), XMLSchema.STRING);
			}
			else if (!Literals.isLanguageLiteral(literal)) {
				if (datatype.equals(XMLSchema.STRING)) {
					return literal;
				}
				else if (XMLDatatypeUtil.isNumericDatatype(datatype) || datatype.equals(XMLSchema.BOOLEAN)
						|| datatype.equals(XMLSchema.DATETIME))
				{
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
					}
					else {
						return valueFactory.createLiteral(literal.getLabel(), XMLSchema.STRING);
					}
				}
				else {
					// for unknown datatypes, just use the lexical value.
					return valueFactory.createLiteral(literal.getLabel(), XMLSchema.STRING);
				}
			}
		}

		throw new ValueExprEvaluationException("Invalid argument for xsd:string cast: " + value);
	}
}
