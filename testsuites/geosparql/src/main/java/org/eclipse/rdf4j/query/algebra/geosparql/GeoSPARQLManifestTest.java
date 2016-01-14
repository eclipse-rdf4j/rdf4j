/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.geosparql;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import org.eclipse.rdf4j.query.parser.sparql.manifest.SPARQLQueryTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public abstract class GeoSPARQLManifestTest {

	public static Test suite(SPARQLQueryTest.Factory factory)
		throws Exception
	{
		TestSuite suite = new TestSuite(factory.getClass().getName());
		URL manifestUrl = GeoSPARQLManifestTest.class.getResource("/testcases-geosparql/manifest.txt");
		BufferedReader reader = new BufferedReader(new InputStreamReader(manifestUrl.openStream(), "UTF-8"));
		String line;
		while ((line = reader.readLine()) != null) {
			URL url = new URL(manifestUrl, line);
			suite.addTest(SPARQLQueryTest.suite(url.toString(), factory, false));
		}
		return suite;
	}
}
