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
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;

/**
 * Constants for EARL primitives and for the EARL namespace.
 */
public class EARL {

	/**
	 * The EARL namespace: http://www.w3.org/ns/earl#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/earl#";

	/**
	 * The recommended prefix for the EARL namespace: "earl"
	 */
	public static final String PREFIX = "earl";

	public final static IRI ASSERTOR;

	public final static IRI ASSERTION;

	public final static IRI ASSERTEDBY;

	public final static IRI SUBJECT;

	public final static IRI TEST;

	public final static IRI TEST_SUBJECT;

	public final static IRI RESULT;

	public final static IRI MODE;

	public final static IRI TESTRESULT;

	public final static IRI OUTCOME;

	public final static IRI SOFTWARE;

	// Outcome values

	public final static IRI PASS;

	public final static IRI FAIL;

	public final static IRI CANNOTTELL;

	public final static IRI NOTAPPLICABLE;

	public final static IRI NOTTESTED;

	// Test modes

	public final static IRI MANUAL;

	public final static IRI AUTOMATIC;

	public final static IRI SEMIAUTOMATIC;

	public final static IRI NOTAVAILABLE;

	public final static IRI HEURISTIC;

	static {

		ASSERTOR = Vocabularies.createIRI(EARL.NAMESPACE, "Assertor");
		ASSERTION = Vocabularies.createIRI(EARL.NAMESPACE, "Assertion");
		ASSERTEDBY = Vocabularies.createIRI(EARL.NAMESPACE, "assertedBy");
		SUBJECT = Vocabularies.createIRI(EARL.NAMESPACE, "subject");
		TEST = Vocabularies.createIRI(EARL.NAMESPACE, "test");
		TEST_SUBJECT = Vocabularies.createIRI(EARL.NAMESPACE, "TestSubject");
		RESULT = Vocabularies.createIRI(EARL.NAMESPACE, "result");
		MODE = Vocabularies.createIRI(EARL.NAMESPACE, "mode");
		TESTRESULT = Vocabularies.createIRI(EARL.NAMESPACE, "TestResult");
		OUTCOME = Vocabularies.createIRI(EARL.NAMESPACE, "outcome");
		SOFTWARE = Vocabularies.createIRI(EARL.NAMESPACE, "Software");

		// Outcome values

		PASS = Vocabularies.createIRI(EARL.NAMESPACE, "pass");
		FAIL = Vocabularies.createIRI(EARL.NAMESPACE, "fail");
		CANNOTTELL = Vocabularies.createIRI(EARL.NAMESPACE, "cannotTell");
		NOTAPPLICABLE = Vocabularies.createIRI(EARL.NAMESPACE, "notApplicable");
		NOTTESTED = Vocabularies.createIRI(EARL.NAMESPACE, "notTested");

		// Test modes
		MANUAL = Vocabularies.createIRI(EARL.NAMESPACE, "manual");
		AUTOMATIC = Vocabularies.createIRI(EARL.NAMESPACE, "automatic");
		SEMIAUTOMATIC = Vocabularies.createIRI(EARL.NAMESPACE, "semiAutomatic");
		NOTAVAILABLE = Vocabularies.createIRI(EARL.NAMESPACE, "notAvailable");
		HEURISTIC = Vocabularies.createIRI(EARL.NAMESPACE, "heuristic");
	}
}
