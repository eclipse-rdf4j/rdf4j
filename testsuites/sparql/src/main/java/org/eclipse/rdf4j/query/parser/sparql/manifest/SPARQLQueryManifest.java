/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.manifest;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * Represents a W3C-style Manifest, consstiing of a list of test cases and optionally a list of sub-manifests.
 *
 * @author Jeen Broekstra
 *
 */
class SPARQLQueryManifest {
	private final List<Object[]> tests = new ArrayList<>();
	private final List<String> subManifests = new ArrayList<>();

	public SPARQLQueryManifest(String filename, List<String> excludedSubdirs) {
		this(filename, excludedSubdirs, true);
	}

	public SPARQLQueryManifest(String filename, List<String> excludedSubdirs, boolean approvedOnly) {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(new URL(filename), filename, RDFFormat.TURTLE);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			String manifestQuery = " PREFIX qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#> "
					+ "PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#> "
					+ "SELECT DISTINCT ?manifestFile "
					+ "WHERE { [] mf:include [ rdf:rest*/rdf:first ?manifestFile ] . }   ";

			try (TupleQueryResult manifestResults = connection
					.prepareTupleQuery(QueryLanguage.SPARQL, manifestQuery, filename)
					.evaluate()) {
				for (BindingSet bindingSet : manifestResults) {
					String subManifestFile = bindingSet.getValue("manifestFile").stringValue();
					if (SPARQLQueryComplianceTest.includeSubManifest(subManifestFile, excludedSubdirs)) {
						getSubManifests().add(subManifestFile);
					}
				}
			}

			StringBuilder query = new StringBuilder(512);
			query.append(" PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#> \n");
			query.append(" PREFIX dawgt: <http://www.w3.org/2001/sw/DataAccess/tests/test-dawg#> \n");
			query.append(" PREFIX qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#> \n");
			query.append(" PREFIX sd: <http://www.w3.org/ns/sparql-service-description#> \n");
			query.append(" PREFIX ent: <http://www.w3.org/ns/entailment/> \n");
			query.append(
					" SELECT DISTINCT ?testURI ?testName ?resultFile ?action ?queryFile ?defaultGraph ?ordered \n");
			query.append(" WHERE { [] rdf:first ?testURI . \n");
			if (approvedOnly) {
				query.append(" ?testURI dawgt:approval dawgt:Approved . \n");
			}
			query.append(" ?testURI mf:name ?testName; \n");
			query.append("          mf:result ?resultFile . \n");
			query.append(" OPTIONAL { ?testURI mf:checkOrder ?ordered } \n");
			query.append(" OPTIONAL { ?testURI  mf:requires ?requirement } \n");
			query.append(" ?testURI mf:action ?action. \n");
			query.append(" ?action qt:query ?queryFile . \n");
			query.append(" OPTIONAL { ?action qt:data ?defaultGraph } \n");
			query.append(" OPTIONAL { ?action sd:entailmentRegime ?regime } \n");
			// skip tests involving CSV result files, these are not query tests
			query.append(" FILTER(!STRENDS(STR(?resultFile), \"csv\")) \n");
			// skip tests involving entailment regimes
			query.append(" FILTER(!BOUND(?regime)) \n");
			// skip test involving basic federation, these are tested separately.
			query.append(" FILTER (!BOUND(?requirement) || (?requirement != mf:BasicFederation)) \n");
			query.append(" }\n");

			try (TupleQueryResult result = connection.prepareTupleQuery(query.toString()).evaluate()) {

				query.setLength(0);
				query.append(" PREFIX qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#> \n");
				query.append(" SELECT ?graph \n");
				query.append(" WHERE { ?action qt:graphData ?graph } \n");
				TupleQuery namedGraphsQuery = connection.prepareTupleQuery(query.toString());

				for (BindingSet bs : result) {
					// FIXME I'm sure there's a neater way to do this
					String testName = bs.getValue("testName").stringValue();
					String displayName = filename.substring(0, filename.lastIndexOf('/'));
					displayName = displayName.substring(displayName.lastIndexOf('/') + 1, displayName.length())
							+ ": " + testName;

					IRI defaultGraphURI = (IRI) bs.getValue("defaultGraph");
					Value action = bs.getValue("action");
					Value ordered = bs.getValue("ordered");

					SimpleDataset dataset = null;

					// Query named graphs
					namedGraphsQuery.setBinding("action", action);
					try (TupleQueryResult namedGraphs = namedGraphsQuery.evaluate()) {
						if (defaultGraphURI != null || namedGraphs.hasNext()) {
							dataset = new SimpleDataset();
							if (defaultGraphURI != null) {
								dataset.addDefaultGraph(defaultGraphURI);
							}
							while (namedGraphs.hasNext()) {
								BindingSet graphBindings = namedGraphs.next();
								IRI namedGraphURI = (IRI) graphBindings.getValue("graph");
								dataset.addNamedGraph(namedGraphURI);
							}
						}
					}

					getTests().add(new Object[] {
							displayName,
							bs.getValue("testURI").stringValue(),
							testName,
							bs.getValue("queryFile").stringValue(),
							bs.getValue("resultFile").stringValue(),
							dataset,
							Literals.getBooleanValue(ordered, false) });
				}
			}

		}

	}

	/**
	 * @return the tests
	 */
	public List<Object[]> getTests() {
		return tests;
	}

	/**
	 * @return the subManifests
	 */
	public List<String> getSubManifests() {
		return subManifests;
	}

}