/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.manifest;

import java.util.Map;

import junit.framework.Test;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.parser.sparql.manifest.SPARQL11ManifestTest;
import org.eclipse.rdf4j.query.parser.sparql.manifest.SPARQLUpdateConformanceTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.contextaware.ContextAwareRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;


/**
 * @author Jeen Broekstra
 */
public class W3CApprovedSPARQL11UpdateTest extends SPARQLUpdateConformanceTest {

	public W3CApprovedSPARQL11UpdateTest(String testURI, String name, String requestFile,
			IRI defaultGraphURI, Map<String, IRI> inputNamedGraphs, IRI resultDefaultGraphURI,
			Map<String, IRI> resultNamedGraphs)
	{
		super(testURI, name, requestFile, defaultGraphURI, inputNamedGraphs, resultDefaultGraphURI,
				resultNamedGraphs);
	}

	public static Test suite()
		throws Exception
	{
		return SPARQL11ManifestTest.suite(new Factory() {

			public W3CApprovedSPARQL11UpdateTest createSPARQLUpdateConformanceTest(String testURI,
					String name, String requestFile, IRI defaultGraphURI, Map<String, IRI> inputNamedGraphs,
					IRI resultDefaultGraphURI, Map<String, IRI> resultNamedGraphs)
			{
				return new W3CApprovedSPARQL11UpdateTest(testURI, name, requestFile, defaultGraphURI,
						inputNamedGraphs, resultDefaultGraphURI, resultNamedGraphs);
			}

		}, true, true, false);
	}

	@Override
	protected ContextAwareRepository newRepository()
		throws Exception
	{
		SailRepository repo = new SailRepository(new MemoryStore());

		return new ContextAwareRepository(repo);
	}

}

