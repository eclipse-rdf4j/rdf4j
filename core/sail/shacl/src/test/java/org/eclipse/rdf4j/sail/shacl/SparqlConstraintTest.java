/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.io.StringReader;

import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SparqlConstraintTest {

	@Test
	public void testFailureBinding() throws IOException {

		SailRepository sailRepository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(new StringReader("" +
					"@prefix : <http://example.com/data/> .\n" +
					"@prefix ont: <http://example.com/ontology#> .\n" +
					"@prefix vocsh: <http://example.org/shape/> .\n" +
					"@prefix so: <http://www.ontotext.com/semantic-object/> .\n" +
					"@prefix affected: <http://www.ontotext.com/semantic-object/affected> .\n" +
					"@prefix res: <http://www.ontotext.com/semantic-object/result/> .\n" +
					"@prefix dct: <http://purl.org/dc/terms/> .\n" +
					"@prefix gn: <http://www.geonames.org/ontology#> .\n" +
					"@prefix owl: <http://www.w3.org/2002/07/owl#> .\n" +
					"@prefix puml: <http://plantuml.com/ontology#> .\n" +
					"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
					"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
					"@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n" +
					"@prefix void: <http://rdfs.org/ns/void#> .\n" +
					"@prefix wgs84: <http://www.w3.org/2003/01/geo/wgs84_pos#> .\n" +
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
					"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
					"@prefix dash: <http://datashapes.org/dash#> .\n" +
					"@prefix rsx: <http://rdf4j.org/shacl-extensions#> .\n" +
					"@prefix ec: <http://www.ontotext.com/connectors/entity-change#> .\n" +
					"@prefix ecinst: <http://www.ontotext.com/connectors/entity-change/instance#> .\n" +
					"@prefix rdf4j: <http://rdf4j.org/schema/rdf4j#> .\n" +
					"@prefix ex: <http://example.com/ns#> .\n" +
					"\n" +
					"rdf4j:SHACLShapeGraph {\n" +
					"\n" +
					"ex:\n" +
					"\tsh:declare [\n" +
					"\t\tsh:prefix \"ex\" ;\n" +
					"\t\tsh:namespace \"http://example.com/ns#\"^^xsd:anyURI ;\n" +
					"\t] ;\n" +
					"\tsh:declare [\n" +
					"\t\tsh:prefix \"schema\" ;\n" +
					"\t\tsh:namespace \"http://schema.org/\"^^xsd:anyURI ;\n" +
					"\t] .\n" +
					"\n" +
					"  ex:LanguageExampleShape\n" +
					"  \ta sh:NodeShape ;\n" +
					"  \tsh:targetClass ex:Country ;\n" +
					"  \tsh:sparql [\n" +
					"  \t\ta sh:SPARQLConstraint ;   # This triple is optional\n" +
					"  \t\tsh:message \"Values are literals with German language tag.\" ;\n" +
					"  \t\tsh:prefixes ex: ;\n" +
					"  \t\tsh:deactivated false ;\n" +
					"  \t\tsh:select \"\"\"\n" +
					"  \t\t\tSELECT $this (ex:germanLabel AS ?path) ?value ?failure\n" +
					"  \t\t\tWHERE {\n" +
					"  \t\t\t\t$this ex:germanLabel ?value .\n" +
					"  \t\t\t\tBIND(isIri(?value) as ?failure)\n" +
					"  \t\t\t}\n" +
					"  \t\t\t\"\"\" ;\n" +
					"  \t] .\n" +
					"}\n"), RDFFormat.TRIG);

			Update update = connection.prepareUpdate("PREFIX ex: <http://example.com/ns#>\n" +
					"PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
					"PREFIX sh: <http://www.w3.org/ns/shacl#>\n" +
					"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
					"\n" +
					"INSERT DATA {\n" +
					"ex:InvalidCountry a ex:Country .\n" +
					"ex:InvalidCountry ex:germanLabel ex:invalidValue .\n" +
					"}\n");

			// assert exception is thrown
			RepositoryException repositoryException = assertThrows(RepositoryException.class, update::execute);
			Throwable cause = repositoryException.getCause().getCause();
			Assertions.assertEquals(
					"org.eclipse.rdf4j.sail.shacl.ast.ShaclSparqlConstraintFailureException: The ?failure variable was true for <http://example.com/ns#InvalidCountry> in shape <http://example.com/ns#LanguageExampleShape> with result resultBindingSet: [this=http://example.com/ns#InvalidCountry;value=http://example.com/ns#invalidValue;failure=\"true\"^^<http://www.w3.org/2001/XMLSchema#boolean>;path=http://example.com/ns#germanLabel] and dataGraph: [] and query:PREFIX schema: <http://schema.org/> \n"
							+
							"PREFIX ex: <http://example.com/ns#> \n" +
							"\n" +
							"\n" +
							"\n" +
							"  \t\t\tSELECT $this (ex:germanLabel AS ?path) ?value ?failure\n" +
							"  \t\t\tWHERE {\n" +
							"  \t\t\t\t$this ex:germanLabel ?value .\n" +
							"  \t\t\t\tBIND(isIri(?value) as ?failure)\n" +
							"  \t\t\t}\n" +
							"  \t\t\t",
					cause.toString());

		}

	}

}
