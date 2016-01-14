/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.ntriples;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.EarlReport;

/**
 * Class for generating EARL reports for N-Quads parser.
 * 
 * @author Peter Ansell
 */
public class NTriplesEarlReport {

	public static void main(String[] args)
		throws Exception
	{
		new EarlReport().generateReport(new NTriplesParserTest().createTestSuite(), EarlReport.ANSELL,
				SimpleValueFactory.getInstance().createIRI("http://www.w3.org/TR/n-triples/"));
	}

}
