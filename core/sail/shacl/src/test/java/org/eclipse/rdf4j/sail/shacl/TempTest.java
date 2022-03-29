/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Håvard Ottestad
 */
public class TempTest {

	@Test
	public void a() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shacl.trig");

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
//
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));

			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);

			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("yay"));
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("yay2"));

			connection.add(RDFS.SUBCLASSOF, RDFS.LABEL, connection.getValueFactory().createLiteral("b"));
			connection.add(RDFS.SUBCLASSOF, RDFS.LABEL, connection.getValueFactory().createLiteral("c"));

			connection.commit();

			connection.begin();

			connection.remove(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("b"));
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("c"));
			connection.remove(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.SUBCLASSOF, RDF.TYPE, RDFS.RESOURCE);

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}

	}

	@Test
	public void b() throws Exception {
		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shacl.trig");

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();
//
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));

//			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
//			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("yay"));
//
			connection.commit();

			connection.begin();

			connection.remove(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("b"));

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}
	}

	@Test
	public void maxCount() throws Exception {
		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclMax.trig");

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();
//			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("class1"));
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("class2"));
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("class3"));

			connection.commit();

			connection.begin();

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("b"));
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("c"));
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("d"));

			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);

			assertThrows(ShaclSailValidationException.class, () -> {
				try {
					connection.commit();
				} catch (RepositoryException e) {
					throw e.getCause();
				}
			});
		} finally {
			shaclRepository.shutDown();
		}
	}

	@Test
	public void minCount() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shacl.trig");

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();
//			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("b"));
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("c"));

			connection.commit();

			connection.begin();

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}
	}

	@Test
	public void leftOuterJoin() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shacl.trig");

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.CLASS);

			connection.commit();

			connection.begin();

			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.CLASS);

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("b"));

			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);

			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("yay"));
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("yay2"));

			connection.add(RDFS.SUBCLASSOF, RDFS.LABEL, connection.getValueFactory().createLiteral("b"));
			connection.add(RDFS.SUBCLASSOF, RDFS.LABEL, connection.getValueFactory().createLiteral("c"));

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}
	}

	@Test
	public void testUndefinedTargetClassValidatesAllSubjects3() throws Throwable {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclNoTargetClass.trig");

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();

			StringReader shaclRules = new StringReader(String.join("\n", "",
					"@prefix ex: <http://example.com/ns#> .",
					"@prefix sh: <http://www.w3.org/ns/shacl#> .",
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

					"ex:PersonShape",
					"	a sh:NodeShape  ;",
					// " sh:targetClass foaf:Person ;",
					"	sh:property ex:PersonShapeProperty .",

					"ex:PersonShapeProperty ",
					"	sh:path foaf:age ;",
					"	sh:datatype xsd:int ;",
					"  sh:maxCount 1 ;",
					"  sh:minCount 1 ."));

			connection.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			connection.begin();

			StringReader invalidSampleData = new StringReader(String.join("\n", "",
					"@prefix ex: <http://example.com/ns#> .",
					"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",

					"ex:peter a foaf:Person ;",
					"	foaf:age 20, \"30\"^^xsd:int  ."

			));
			connection.add(invalidSampleData, "", RDFFormat.TRIG);

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}
	}

	@Test
	@Disabled // this method is used to produce the log examples in the documentation
	public void doc() throws IOException {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		// Logger root = (Logger) LoggerFactory.getLogger(ShaclSail.class.getName());
		// root.setLevel(Level.INFO);

		shaclSail.setLogValidationPlans(false);
		shaclSail.setGlobalLogValidationExecution(false);
		shaclSail.setLogValidationViolations(false);
		shaclSail.setParallelValidation(false);

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin();

			StringReader shaclRules = new StringReader(String.join("\n", "",
					"@prefix ex: <http://example.com/ns#> .",
					"@prefix sh: <http://www.w3.org/ns/shacl#> .",
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

					"ex:PersonShape",
					"        a sh:NodeShape  ;",
					"        sh:targetClass ex:Person ;",
					"        sh:property [",
					"                sh:path ex:age ;",
					"                sh:datatype xsd:integer ;",
					"        ] ."

			));

			connection.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			add(connection, String.join("\n", "",
					"@prefix ex: <http://example.com/ns#> .",
					"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",

					"ex:pete a ex:Person ."

			));

			shaclSail.setLogValidationPlans(true);
			shaclSail.setGlobalLogValidationExecution(true);
			shaclSail.setLogValidationViolations(true);

			add(connection, String.join("\n", "",
					"@prefix ex: <http://example.com/ns#> .",
					"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",

					"ex:pete ex:age \"eighteen\" ."

			));

		} finally {
			sailRepository.shutDown();
		}
	}

	private void add(SailRepositoryConnection connection, String data) throws IOException {
		connection.begin();

		StringReader invalidSampleData = new StringReader(data);

		connection.add(invalidSampleData, "", RDFFormat.TRIG);
		connection.commit();
	}

}
