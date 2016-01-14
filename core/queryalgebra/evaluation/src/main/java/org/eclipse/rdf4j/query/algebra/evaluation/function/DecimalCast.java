/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function;

import java.math.BigDecimal;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;

/**
 * A {@link Function} that tries to cast its argument to an <tt>xsd:decimal</tt>.
 * 
 * @author Arjohn Kampman
 */
public class DecimalCast implements Function {

	public String getURI() {
		return XMLSchema.DECIMAL.toString();
	}

	public Literal evaluate(ValueFactory valueFactory, Value... args)
		throws ValueExprEvaluationException
	{
		if (args.length != 1) {
			throw new ValueExprEvaluationException("xsd:decimal cast requires exactly 1 argument, got "
					+ args.length);
		}

		if (args[0] instanceof Literal) {
			Literal literal = (Literal)args[0];
			IRI datatype = literal.getDatatype();

			if (QueryEvaluationUtil.isStringLiteral(literal)) {
				String decimalValue = XMLDatatypeUtil.collapseWhiteSpace(literal.getLabel());
				if (XMLDatatypeUtil.isValidDecimal(decimalValue)) {
					return valueFactory.createLiteral(decimalValue, XMLSchema.DECIMAL);
				}
			}
			else if (datatype != null) {
				if (datatype.equals(XMLSchema.DECIMAL)) {
					return literal;
				}
				else if (XMLDatatypeUtil.isNumericDatatype(datatype)) {
					// FIXME: floats and doubles must be processed separately, see
					// http://www.w3.org/TR/xpath-functions/#casting-from-primitive-to-primitive
					try {
						BigDecimal decimalValue = literal.decimalValue();
						return valueFactory.createLiteral(decimalValue.toPlainString(), XMLSchema.DECIMAL);
					}
					catch (NumberFormatException e) {
						throw new ValueExprEvaluationException(e.getMessage(), e);
					}
				}
				else if (datatype.equals(XMLSchema.BOOLEAN)) {
					try {
						return valueFactory.createLiteral(literal.booleanValue() ? "1.0" : "0.0", XMLSchema.DECIMAL);
					}
					catch (IllegalArgumentException e) {
						throw new ValueExprEvaluationException(e.getMessage(), e);
					}
				}
			}
		}

		throw new ValueExprEvaluationException("Invalid argument for xsd:decimal cast: " + args[0]);
	}
}
