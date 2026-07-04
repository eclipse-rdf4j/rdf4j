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
package org.eclipse.rdf4j.query.dawg;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * @author Arjohn Kampman
 */
public class DAWGTestResultSetSchema {

	public static final String NAMESPACE = "http://www.w3.org/2001/sw/DataAccess/tests/result-set#";

	public static final IRI RESULTSET;

	public static final IRI RESULTVARIABLE;

	public static final IRI SOLUTION;

	public static final IRI BINDING;

	public static final IRI VALUE;

	public static final IRI VARIABLE;

	public static final IRI BOOLEAN;

	public static final Literal TRUE;

	public static final Literal FALSE;

	static {
		ValueFactory vf = SimpleValueFactory.getInstance();
		RESULTSET = vf.createIRI(NAMESPACE, "ResultSet");
		RESULTVARIABLE = vf.createIRI(NAMESPACE, "resultVariable");
		SOLUTION = vf.createIRI(NAMESPACE, "solution");
		BINDING = vf.createIRI(NAMESPACE, "binding");
		VALUE = vf.createIRI(NAMESPACE, "value");
		VARIABLE = vf.createIRI(NAMESPACE, "variable");
		BOOLEAN = vf.createIRI(NAMESPACE, "boolean");
		TRUE = vf.createLiteral(true);
		FALSE = vf.createLiteral(false);
	}
}
