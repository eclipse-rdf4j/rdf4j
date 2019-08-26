/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

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
		ValueFactory factory = SimpleValueFactory.getInstance();
		ASSERTOR = factory.createIRI(EARL.NAMESPACE, "Assertor");
		ASSERTION = factory.createIRI(EARL.NAMESPACE, "Assertion");
		ASSERTEDBY = factory.createIRI(EARL.NAMESPACE, "assertedBy");
		SUBJECT = factory.createIRI(EARL.NAMESPACE, "subject");
		TEST = factory.createIRI(EARL.NAMESPACE, "test");
		TEST_SUBJECT = factory.createIRI(EARL.NAMESPACE, "TestSubject");
		RESULT = factory.createIRI(EARL.NAMESPACE, "result");
		MODE = factory.createIRI(EARL.NAMESPACE, "mode");
		TESTRESULT = factory.createIRI(EARL.NAMESPACE, "TestResult");
		OUTCOME = factory.createIRI(EARL.NAMESPACE, "outcome");
		SOFTWARE = factory.createIRI(EARL.NAMESPACE, "Software");

		// Outcome values

		PASS = factory.createIRI(EARL.NAMESPACE, "pass");
		FAIL = factory.createIRI(EARL.NAMESPACE, "fail");
		CANNOTTELL = factory.createIRI(EARL.NAMESPACE, "cannotTell");
		NOTAPPLICABLE = factory.createIRI(EARL.NAMESPACE, "notApplicable");
		NOTTESTED = factory.createIRI(EARL.NAMESPACE, "notTested");

		// Test modes
		MANUAL = factory.createIRI(EARL.NAMESPACE, "manual");
		AUTOMATIC = factory.createIRI(EARL.NAMESPACE, "automatic");
		SEMIAUTOMATIC = factory.createIRI(EARL.NAMESPACE, "semiAutomatic");
		NOTAVAILABLE = factory.createIRI(EARL.NAMESPACE, "notAvailable");
		HEURISTIC = factory.createIRI(EARL.NAMESPACE, "heuristic");
	}
}
