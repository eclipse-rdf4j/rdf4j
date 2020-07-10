package org.eclipse.rdf4j.repository.http;

import java.io.IOException;
import java.io.StringReader;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.junit.Test;

public class Temp {

	@Test
	public void test() throws IOException {

		HTTPRepository httpRepository = new HTTPRepository("http://localhost:8080/rdf4j-server", "1");

		System.out.println();

		try (RepositoryConnection connection = httpRepository.getConnection()) {
			connection.setIsolationLevel(IsolationLevels.NONE);
			connection.begin();
			connection.remove((Resource) null, null, null);
			connection.remove((Resource) null, null, null, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			connection.begin();

			connection.add(new StringReader("@base <http://example.com/ns> .\n" +
					"@prefix ex: <http://example.com/ns#> .\n" +
					"@prefix owl: <http://www.w3.org/2002/07/owl#> .\n" +
					"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
					"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
					"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
					"\n" +
					"ex:PersonShape\n" +
					"\ta sh:NodeShape  ;\n" +
					"\tsh:targetClass rdfs:Resource ;\n" +
					"\tsh:property ex:PersonShapeProperty  .\n" +
					"\n" +
					"\n" +
					"ex:PersonShapeProperty\n" +
					"        sh:path rdfs:label ;\n" +
					"        sh:minCount 1 ."), "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			connection.begin(IsolationLevels.SNAPSHOT, ShaclSail.Settings.Validation.Disabled);
			connection.add(RDF.SUBJECT, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

		}

		httpRepository.shutDown();

	}

}
