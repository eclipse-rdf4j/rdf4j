/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.hash;

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
private final String HEX = "0123456789abcdef";
	
	/**
	 * Calculate hash value, represented as hexadecimal string.
	 * 
	 * @param text text
	 * @param algorithm name of the hash algorithm
	 * @return hexadecimal string
	 * @throws NoSuchAlgorithmException 
	 */
	protected String hash(String text, String algorithm)
		throws NoSuchAlgorithmException
	{
		byte[] hash = MessageDigest.getInstance(algorithm).digest(text.getBytes());

		// convert to hexadecimal representation, with leading zeros
		char[] hex = new char[hash.length * 2];
		for (int i = 0, j = 0; i < hex.length; ) {
			hex[i++] = HEX.charAt((hash[j] & 0xF0) >>> 4);
			hex[i++] = HEX.charAt(hash[j++] & 0x0F);
		}
		return new String(hex);
	}

	@Override
	public abstract Literal evaluate(ValueFactory valueFactory, Value... args)
		throws ValueExprEvaluationException;
}
