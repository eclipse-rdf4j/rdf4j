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

import java.math.BigDecimal;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * A {@link org.eclipse.rdf4j.query.algebra.evaluation.function.Function} that tries to cast its argument to an
 * <var>xsd:decimal</var>.
 *
 * @author Arjohn Kampman
 * @author Jeen Broekstra
 */
public class DecimalCast extends CastFunction {

	@Override
	protected Literal convert(ValueFactory valueFactory, Value value) throws ValueExprEvaluationException {
		if (value instanceof Literal) {
			Literal literal = (Literal) value;
			CoreDatatype.XSD datatype = literal.getCoreDatatype().asXSDDatatypeOrNull();

			if (datatype != null && datatype.isNumericDatatype()) {
				// FIXME: floats and doubles must be processed separately, see
				// http://www.w3.org/TR/xpath-functions/#casting-from-primitive-to-primitive
				try {
					BigDecimal decimalValue = literal.decimalValue();
					return valueFactory.createLiteral(decimalValue.toPlainString(), CoreDatatype.XSD.DECIMAL);
				} catch (NumberFormatException e) {
					throw typeError(literal, e);
				}
			} else if (datatype == CoreDatatype.XSD.BOOLEAN) {
				try {
					return valueFactory.createLiteral(literal.booleanValue() ? "1.0" : "0.0", CoreDatatype.XSD.DECIMAL);
				} catch (IllegalArgumentException e) {
					throw typeError(literal, e);
				}
			}
		}

		throw typeError(value, null);
	}

	@Override
	protected CoreDatatype.XSD getCoreXsdDatatype() {
		return CoreDatatype.XSD.DECIMAL;
	}

	@Override
	protected boolean isValidForDatatype(String lexicalValue) {
		return XMLDatatypeUtil.isValidDecimal(lexicalValue);
	}
}
