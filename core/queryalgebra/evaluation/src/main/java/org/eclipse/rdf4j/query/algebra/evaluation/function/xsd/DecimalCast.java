/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.xsd;

import java.math.BigDecimal;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * A {@link Function} that tries to cast its argument to an <tt>xsd:decimal</tt>.
 * 
 * @author Arjohn Kampman
 * @author Jeen Broekstra
 */
public class DecimalCast extends CastFunction {

	@Override
	protected Literal convert(ValueFactory valueFactory, Value value) throws ValueExprEvaluationException {
		if (value instanceof Literal) {
			Literal literal = (Literal) value;
			IRI datatype = literal.getDatatype();

			if (XMLDatatypeUtil.isNumericDatatype(datatype)) {
				// FIXME: floats and doubles must be processed separately, see
				// http://www.w3.org/TR/xpath-functions/#casting-from-primitive-to-primitive
				try {
					BigDecimal decimalValue = literal.decimalValue();
					return valueFactory.createLiteral(decimalValue.toPlainString(), XMLSchema.DECIMAL);
				} catch (NumberFormatException e) {
					throw typeError(literal, e);
				}
			} else if (datatype.equals(XMLSchema.BOOLEAN)) {
				try {
					return valueFactory.createLiteral(literal.booleanValue() ? "1.0" : "0.0", XMLSchema.DECIMAL);
				} catch (IllegalArgumentException e) {
					throw typeError(literal, e);
				}
			}
		}

		throw typeError(value, null);
	}

	@Override
	protected IRI getXsdDatatype() {
		return XMLSchema.DECIMAL;
	}

	@Override
	protected boolean isValidForDatatype(String lexicalValue) {
		return XMLDatatypeUtil.isValidDecimal(lexicalValue);
	}
}
