/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import static org.eclipse.rdf4j.model.base.AbstractIRI.createIRI;

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

		ASSERTOR = createIRI(EARL.NAMESPACE, "Assertor");
		ASSERTION = createIRI(EARL.NAMESPACE, "Assertion");
		ASSERTEDBY = createIRI(EARL.NAMESPACE, "assertedBy");
		SUBJECT = createIRI(EARL.NAMESPACE, "subject");
		TEST = createIRI(EARL.NAMESPACE, "test");
		TEST_SUBJECT = createIRI(EARL.NAMESPACE, "TestSubject");
		RESULT = createIRI(EARL.NAMESPACE, "result");
		MODE = createIRI(EARL.NAMESPACE, "mode");
		TESTRESULT = createIRI(EARL.NAMESPACE, "TestResult");
		OUTCOME = createIRI(EARL.NAMESPACE, "outcome");
		SOFTWARE = createIRI(EARL.NAMESPACE, "Software");

		// Outcome values

		PASS = createIRI(EARL.NAMESPACE, "pass");
		FAIL = createIRI(EARL.NAMESPACE, "fail");
		CANNOTTELL = createIRI(EARL.NAMESPACE, "cannotTell");
		NOTAPPLICABLE = createIRI(EARL.NAMESPACE, "notApplicable");
		NOTTESTED = createIRI(EARL.NAMESPACE, "notTested");

		// Test modes
		MANUAL = createIRI(EARL.NAMESPACE, "manual");
		AUTOMATIC = createIRI(EARL.NAMESPACE, "automatic");
		SEMIAUTOMATIC = createIRI(EARL.NAMESPACE, "semiAutomatic");
		NOTAVAILABLE = createIRI(EARL.NAMESPACE, "notAvailable");
		HEURISTIC = createIRI(EARL.NAMESPACE, "heuristic");
	}
}
