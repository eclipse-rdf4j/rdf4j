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
 * A {@link Function} that tries to cast its argument to an <tt>xsd:integer</tt> .
 * 
 * @author Jeen Broekstra
 */
// TODO the Service Registry currently still uses the (deprecated) IntegerCast function in the parent package. This needs to be updated in the next 
// minor release (we're avoiding this in the patch for compatibility reasons)
public class IntegerCast extends IntegerDatatypeCast {

	@Override
	protected IRI getIntegerDatatype() {
		return XMLSchema.INTEGER;
	}

	@Override
	protected Optional<Literal> createTypedLiteral(ValueFactory vf, BigInteger integerValue)
		throws ArithmeticException
	{
		return Optional.of(vf.createLiteral(integerValue.toString(), getIntegerDatatype()));
	}

	@Override
	protected boolean isValidForDatatype(String lexicalValue) {
		return XMLDatatypeUtil.isValidInt(lexicalValue);
	}

}
