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
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;

/**
 * A {@link Function} that tries to cast its argument to an <tt>xsd:float</tt>.
 * 
 * @author Arjohn Kampman
 */
public class FloatCast implements Function {

	public String getURI() {
		return XMLSchema.FLOAT.toString();
	}

	public Literal evaluate(ValueFactory valueFactory, Value... args)
		throws ValueExprEvaluationException
	{
		if (args.length != 1) {
			throw new ValueExprEvaluationException("xsd:float cast requires exactly 1 argument, got "
					+ args.length);
		}

		if (args[0] instanceof Literal) {
			Literal literal = (Literal)args[0];
			IRI datatype = literal.getDatatype();

			if (QueryEvaluationUtil.isStringLiteral(literal)) {
				String floatValue = XMLDatatypeUtil.collapseWhiteSpace(literal.getLabel());
				if (XMLDatatypeUtil.isValidFloat(floatValue)) {
					return valueFactory.createLiteral(floatValue, XMLSchema.FLOAT);
				}
			}
			else if (datatype != null) {
				if (datatype.equals(XMLSchema.FLOAT)) {
					return literal;
				}
				else if (XMLDatatypeUtil.isNumericDatatype(datatype)) {
					// FIXME: doubles must be processed separately, see
					// http://www.w3.org/TR/xpath-functions/#casting-from-primitive-to-primitive
					try {
						float floatValue = literal.floatValue();
						return valueFactory.createLiteral(floatValue);
					}
					catch (NumberFormatException e) {
						throw new ValueExprEvaluationException(e.getMessage(), e);
					}
				}
				else if (datatype.equals(XMLSchema.BOOLEAN)) {
					try {
						return valueFactory.createLiteral(literal.booleanValue() ? 1f : 0f);
					}
					catch (IllegalArgumentException e) {
						throw new ValueExprEvaluationException(e.getMessage(), e);
					}
				}
			}
		}

		throw new ValueExprEvaluationException("Invalid argument for xsd:float cast: " + args[0]);
	}
}
