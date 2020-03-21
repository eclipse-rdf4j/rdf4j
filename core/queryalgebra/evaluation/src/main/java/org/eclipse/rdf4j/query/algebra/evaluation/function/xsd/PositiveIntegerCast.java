/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.xsd;

import java.math.BigInteger;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * A {@link org.eclipse.rdf4j.query.algebra.evaluation.function.Function} that tries to cast its argument to an
 * <tt>xsd:positiveInteger</tt> .
 * 
 * @author Jeen Broekstra
 */
public class PositiveIntegerCast extends IntegerCastFunction {

	@Override
	protected IRI getXsdDatatype() {
		return XMLSchema.POSITIVE_INTEGER;
	}

	@Override
	protected boolean isValidForDatatype(String lexicalValue) {
		return XMLDatatypeUtil.isValidPositiveInteger(lexicalValue);
	}

	@Override
	protected Optional<Literal> createTypedLiteral(ValueFactory vf, BigInteger integerValue) {
		if (integerValue.compareTo(BigInteger.ZERO) > 0) {
			return Optional.of(vf.createLiteral(integerValue.toString(), getXsdDatatype()));
		}
		return Optional.empty();
	}

	@Override
	protected Optional<Literal> createTypedLiteral(ValueFactory vf, boolean booleanValue) {
		Literal result = null;
		if (booleanValue) {
			result = vf.createLiteral("1", getXsdDatatype());
		}
		return Optional.ofNullable(result);
	}
}
