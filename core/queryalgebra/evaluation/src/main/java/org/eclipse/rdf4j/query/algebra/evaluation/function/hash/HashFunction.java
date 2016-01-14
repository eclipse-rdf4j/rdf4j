/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.hash;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * Abstract hash function
 * 
 * @author jeen
 */
public abstract class HashFunction implements Function {

	protected String hash(String text, String algorithm)
		throws NoSuchAlgorithmException
	{
		byte[] hash = MessageDigest.getInstance(algorithm).digest(text.getBytes());
		BigInteger bi = new BigInteger(1, hash);
		String result = bi.toString(16);
		if (result.length() % 2 != 0) {
			return "0" + result;
		}
		return result;
	}

	public abstract Literal evaluate(ValueFactory valueFactory, Value... args)
		throws ValueExprEvaluationException;

}
