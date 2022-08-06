/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.examples.function;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * An example custom SPARQL function that detects palindromes that already exist in the database.
 *
 * @author Jeen Broekstra
 */
public class ExistingPalindromeFunction implements Function {

	// define a constant for the namespace of our custom function
	public static final String NAMESPACE = "http://example.org/custom-function/";

	/**
	 * return the URI 'http://example.org/custom-function/existingPalindrome' as a String
	 */
	@Override
	public String getURI() {
		return NAMESPACE + "existingPalindrome";
	}

	/**
	 * Executes the existingPalindrome function.
	 *
	 * @return A boolean literal representing true if the input argument is a palindrome and exists in the database,
	 *         false otherwise.
	 * @throws ValueExprEvaluationException if more than one argument is supplied or if the supplied argument is not a
	 *                                      literal.
	 */
	@Override
	public Value evaluate(TripleSource tripleSource, Value... args)
			throws ValueExprEvaluationException {
		// our palindrome function expects only a single argument, so throw an error
		// if there's more than one
		if (args.length != 1) {
			throw new ValueExprEvaluationException(
					"palindrome function requires" + "exactly 1 argument, got "
							+ args.length);
		}
		Value arg = args[0];
		// check if the argument is a literal, if not, we throw an error
		if (!(arg instanceof Literal)) {
			throw new ValueExprEvaluationException(
					"invalid argument (literal expected): " + arg);
		}

		// get the actual string value that we want to check for palindrome-ness.
		String label = ((Literal) arg).getLabel();
		// we invert our string
		String inverted = "";
		for (int i = label.length() - 1; i >= 0; i--) {
			inverted += label.charAt(i);
		}
		// a string is a palindrome if it is equal to its own inverse
		boolean palindrome = inverted.equalsIgnoreCase(label);

		// check if a triple with the rdfs:label predicate and this palindrome as its value already exists in the
		// database
		boolean existing = !QueryResults.asList(tripleSource.getStatements(null, RDFS.LABEL, (Literal) arg)).isEmpty();

		// return a new boolean literal that is true if the input argument is a palindrome and exists as a label
		return tripleSource.getValueFactory().createLiteral(palindrome && existing);
	}

	@Override
	public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		throw new UnsupportedOperationException();
	}
}
