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
package org.eclipse.rdf4j.query.algebra.evaluation.function.hash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.Test;

/**
 * @author jeen
 */
public abstract class HashFunctionTest {

	private HashFunction hashFunction;

	private String toHash;

	private String expectedDigest;

	private final ValueFactory f = SimpleValueFactory.getInstance();

	@Test
	public void testEvaluate() {
		try {
			Literal hash = getHashFunction().evaluate(f, f.createLiteral(getToHash()));

			assertNotNull(hash);
			assertEquals(XSD.STRING, hash.getDatatype());

			assertEquals(hash.getLabel(), getExpectedDigest());
		} catch (ValueExprEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate2() {
		try {
			Literal hash = getHashFunction().evaluate(f, f.createLiteral(getToHash(), XSD.STRING));

			assertNotNull(hash);
			assertEquals(XSD.STRING, hash.getDatatype());

			assertEquals(hash.getLabel(), getExpectedDigest());
		} catch (ValueExprEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate3() {
		try {
			getHashFunction().evaluate(f, f.createLiteral("4", XSD.INTEGER));

			fail("incompatible operand should have resulted in type error.");
		} catch (ValueExprEvaluationException e) {
			// do nothing, expected
		}
	}

	/**
	 * @param hashFunction The hashFunction to set.
	 */
	public void setHashFunction(HashFunction hashFunction) {
		this.hashFunction = hashFunction;
	}

	/**
	 * @return Returns the hashFunction.
	 */
	public HashFunction getHashFunction() {
		return hashFunction;
	}

	/**
	 * @param expectedDigest The expectedDigest to set.
	 */
	public void setExpectedDigest(String expectedDigest) {
		this.expectedDigest = expectedDigest;
	}

	/**
	 * @return Returns the expectedDigest.
	 */
	public String getExpectedDigest() {
		return expectedDigest;
	}

	/**
	 * @param toHash The toHash to set.
	 */
	public void setToHash(String toHash) {
		this.toHash = toHash;
	}

	/**
	 * @return Returns the toHash.
	 */
	public String getToHash() {
		return toHash;
	}
}
