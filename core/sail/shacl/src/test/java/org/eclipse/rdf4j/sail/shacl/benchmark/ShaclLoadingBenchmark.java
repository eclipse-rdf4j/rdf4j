/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import java.io.StringReader;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 20)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G", "-XX:+UseSerialGC" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=15s,duration=120s,filename=recording.jfr,settings=ProfilingAggressive.jfc", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ShaclLoadingBenchmark {
	{
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}

	@Setup(Level.Iteration)
	public void setUp() throws InterruptedException {
		System.gc();
		Thread.sleep(100);
		((Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName()))
				.setLevel(ch.qos.logback.classic.Level.ERROR);
		((Logger) LoggerFactory.getLogger(ShaclSail.class.getName())).setLevel(ch.qos.logback.classic.Level.ERROR);
	}

	@Benchmark
	public void testAddingDataAndAddingShapes() throws Exception {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));
		repository.init();
		((ShaclSail) repository.getSail()).setParallelValidation(true);
		((ShaclSail) repository.getSail()).setCacheSelectNodes(true);

		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.SNAPSHOT);

			for (int i = 0; i < BenchmarkConfigs.NUMBER_OF_TRANSACTIONS; i++) {
				StringReader data = new StringReader(String.join("\n", "",
						"@prefix ex: <http://example.com/ns#> .",
						"@prefix sh: <http://www.w3.org/ns/shacl#> .",
						"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
						"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " ."

				));

				connection.add(data, "", RDFFormat.TURTLE);

			}
			connection.commit();

			for (int i = 0; i < BenchmarkConfigs.NUMBER_OF_TRANSACTIONS; i++) {
				connection.begin(IsolationLevels.SNAPSHOT);
				StringReader shaclRules = new StringReader(String.join("\n", "",
						"@prefix ex: <http://example.com/ns#> .",
						"@prefix sh: <http://www.w3.org/ns/shacl#> .",
						"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
						"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

						"[]",
						"        a sh:NodeShape  ;",
						"        sh:targetClass ex:Person ;",
						"        sh:property [",
						"                sh:path ex:age" + i + " ;",
						"                sh:datatype xsd:integer ;",
						"        ] ."));

				connection.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
				connection.commit();

			}
		}

		repository.shutDown();

	}

	@Benchmark
	public void loadComplexShapes() throws Exception {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (SailRepositoryConnection connection = repository.getConnection()) {

			StringReader shaclRules1 = new StringReader(String.join("\n", "",
					"@prefix sh: <http://www.w3.org/ns/shacl#>.",
					"@prefix schema: <http://schema.org/> .",
					"@prefix spdx:  <http://spdx.org/rdf/terms#> .",
					"@prefix dct:   <http://purl.org/dc/terms/> .",
					"@prefix adms:  <http://www.w3.org/ns/adms#> .",
					"@prefix owl:   <http://www.w3.org/2002/07/owl#> .",
					"@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix skos:  <http://www.w3.org/2004/02/skos/core#> .",
					"@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .",
					"@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .",
					"@prefix dcat:  <http://www.w3.org/ns/dcat#> .",
					"@prefix foaf:  <http://xmlns.com/foaf/0.1/> .",
					"[]",
					"                   a              sh:NodeShape ;",
					"                   sh:targetClass  dcat:Catalog ;",
					"                   sh:property",
					"                   [",
					"                           sh:minCount   1 ;",
					"                           sh:path  dct:description ;",
					"                           sh:nodeKind sh:Literal;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:minCount   1 ;",
					"                           sh:path  dct:title ;",
					"                           sh:nodeKind sh:Literal;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:maxCount   1 ;",
					"                           sh:path  dct:issued ;",
					"                           sh:or ([sh:datatype xsd:date;] [sh:datatype xsd:dateTime;]) ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:minCount   1 ;",
					"                           sh:path  dct:issued ;",
					"                           # sh:severity   sh:Warning",
					"                   ],",
					"                    [",
					"                           sh:maxCount   1 ;",
					"                           sh:path  dct:modified ;",
					"                           sh:or ([sh:datatype xsd:date;] [sh:datatype xsd:dateTime;]) ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                    [",
					"                           sh:minCount   1 ;",
					"                           sh:path  dct:modified ;",
					"                           # sh:severity   sh:Warning",
					"                   ],",
					"                   [",
					"                           sh:maxCount   1 ;",
					"                           sh:path  dct:publisher ;",
					"                           sh:class foaf:Agent ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:minCount   1 ;",
					"                           sh:path  dcat:dataset ;",
					"                           sh:class dcat:Dataset ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:path  dcat:themeTaxonomy ;",
					"                           sh:class skos:ConceptScheme ;",
					"                           # sh:severity   sh:Violation",
					"                   ]      ,",
					"",
					"                   [",
					"                           sh:path  dct:hasPart ;",
					"                           sh:class dcat:Catalog ;",
					"                           # sh:severity   sh:Violation",
					"                   ]  ,",
					"                   [",
					"                           sh:path  dct:isPartOf ;",
					"                           sh:maxCount 1;",
					"                           sh:class dcat:Catalog ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:path  dct:spatial ;",
					"                           sh:class dct:Location ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                            sh:maxCount 1;",
					"                           sh:path  foaf:homepage ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                            sh:minCount 1;",
					"                           sh:path  foaf:homepage ;",
					"                           # sh:severity   sh:Warning",
					"                   ],",
					"                   [",
					"                           sh:path  dcat:record ;",
					"                           sh:class dcat:CatalogRecord ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:path  dct:license ;",
					"                           sh:maxCount 1;",
					"                           # sh:severity   sh:Violation",
					"                   ] ,",
					"                   [",
					"                           sh:path  dct:rights ;",
					"                           sh:maxCount 1;",
					"                           sh:class dct:RightsStatement ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:path  dct:language ;",
					"                           sh:minCount 1 ;",
					"                           # sh:severity   sh:Warning",
					"                   ]",
					"                    ."

			));
			StringReader shaclRules2 = new StringReader(String.join("\n", "",

					"@prefix sh: <http://www.w3.org/ns/shacl#>.",
					"@prefix schema: <http://schema.org/> .",
					"@prefix spdx:  <http://spdx.org/rdf/terms#> .",
					"@prefix dct:   <http://purl.org/dc/terms/> .",
					"@prefix adms:  <http://www.w3.org/ns/adms#> .",
					"@prefix owl:   <http://www.w3.org/2002/07/owl#> .",
					"@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix skos:  <http://www.w3.org/2004/02/skos/core#> .",
					"@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .",
					"@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .",
					"@prefix dcat:  <http://www.w3.org/ns/dcat#> .",
					"@prefix foaf:  <http://xmlns.com/foaf/0.1/> .",
					"[]",
					"                   a              sh:NodeShape ;",
					"                   sh:targetClass  foaf:Agent ;",
					"                   sh:property",
					"                   [",
					"                           sh:minCount   1 ;",
					"                           sh:path  foaf:name ;",
					"                           sh:nodeKind sh:Literal ;",
					"                           sh:minLength 1;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   skos:Concept ;",
					"                           sh:path  dct:type ;",
					"                           sh:maxCount 1;",
					"                           # sh:severity   sh:Violation",
					"                   ] ,",
					"                   [",
					"                           sh:path  dct:type ;",
					"                           sh:minCount 1;",
					"                           # sh:severity   sh:Warning",
					"                   ]",
					"                   ."

			));
			StringReader shaclRules3 = new StringReader(String.join("\n", "",

					"@prefix sh: <http://www.w3.org/ns/shacl#>.",
					"@prefix schema: <http://schema.org/> .",
					"@prefix spdx:  <http://spdx.org/rdf/terms#> .",
					"@prefix dct:   <http://purl.org/dc/terms/> .",
					"@prefix adms:  <http://www.w3.org/ns/adms#> .",
					"@prefix owl:   <http://www.w3.org/2002/07/owl#> .",
					"@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix skos:  <http://www.w3.org/2004/02/skos/core#> .",
					"@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .",
					"@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .",
					"@prefix dcat:  <http://www.w3.org/ns/dcat#> .",
					"@prefix foaf:  <http://xmlns.com/foaf/0.1/> .",
					"[]",
					"                   a              sh:NodeShape ;",
					"                   sh:targetClass  dcat:CatalogRecord ;",
					"                   sh:property",
					"                   [",
					"                           sh:minCount   1 ;",
					"                           sh:path  dct:modified ;",
					"                           sh:or ([sh:datatype xsd:date;] [sh:datatype xsd:dateTime;]) ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:maxCount   1 ;",
					"                           sh:path  dct:issued ;",
					"                           sh:or ([sh:datatype xsd:date;] [sh:datatype xsd:dateTime;]) ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:minCount   1 ;",
					"                           sh:path  dct:issued ;",
					"                           # sh:severity   sh:Warning",
					"                   ] ,",
					"                   [",
					"                           sh:minCount   1 ;",
					"                           sh:maxCount   1 ;",
					"                           sh:path  foaf:primaryTopic ;",
					"                           sh:class dcat:Dataset ;",
					"                           # sh:severity   sh:Violation",
					"                   ] ,",
					"                   [",
					"                           sh:path  dct:source ;",
					"                           sh:class dcat:CatalogRecord ;",
					"                           # sh:severity   sh:Violation",
					"                   ] ,",
					"                   [",
					"                           sh:path  adms:status ;",
					"                           sh:class skos:Concept ;",
					"                           # sh:severity   sh:Violation",
					"                   ] ,",
					"                   [",
					"                           sh:path  adms:status ;",
					"                           sh:class skos:Concept ;",
					"                           sh:minCount 1;",
					"                           # sh:severity   sh:Warning",
					"                   ],",
					"                   [",
					"                           sh:path  dct:conformsTo ;",
					"                           sh:nodeKind sh:BlankNodeOrIRI ;",
					"                           sh:maxCount 1;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:path  dct:description ;",
					"                           sh:minLength 10;",
					"                           sh:nodeKind sh:Literal;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:path  dct:title ;",
					"                           sh:nodeKind sh:Literal;",
					"                           # sh:severity   sh:Violation",
					"                   ]."

			));
			StringReader shaclRules4 = new StringReader(String.join("\n", "",

					"@prefix sh: <http://www.w3.org/ns/shacl#>.",
					"@prefix schema: <http://schema.org/> .",
					"@prefix spdx:  <http://spdx.org/rdf/terms#> .",
					"@prefix dct:   <http://purl.org/dc/terms/> .",
					"@prefix adms:  <http://www.w3.org/ns/adms#> .",
					"@prefix owl:   <http://www.w3.org/2002/07/owl#> .",
					"@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix skos:  <http://www.w3.org/2004/02/skos/core#> .",
					"@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .",
					"@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .",
					"@prefix dcat:  <http://www.w3.org/ns/dcat#> .",
					"@prefix foaf:  <http://xmlns.com/foaf/0.1/> .",
					"[]",
					"                   a              sh:NodeShape ;",
					"                   sh:targetClass  dcat:Dataset ;",
					"                   sh:property",
					"                   [",
					"                           sh:minCount   1 ;",
					"                           sh:minLength 10;",
					"                           sh:path  dct:description ;",
					"                           sh:nodeKind sh:Literal;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:minCount   1 ;",
					"                           sh:minLength 1;",
					"                           sh:maxLength 100;",
					"                           sh:path  dct:title ;",
					"                           sh:nodeKind sh:Literal;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:minCount   1 ;",
					"                           sh:minLength 1;",
					"",
					"                           sh:path  dct:identifier ;",
					"                           sh:nodeKind sh:Literal;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:path  adms:versionNotes ;",
					"                           sh:nodeKind sh:Literal;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:maxCount   1 ;",
					"                           sh:minLength 1;",
					"                           sh:path  owl:versionInfo ;",
					"                           sh:nodeKind sh:Literal;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:maxCount   1 ;",
					"                           sh:path  dct:issued ;",
					"                           sh:or ([sh:datatype xsd:date;] [sh:datatype xsd:dateTime;]) ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:maxCount   1 ;",
					"                           sh:path  dct:modified ;",
					"                           sh:or ([sh:datatype xsd:date;] [sh:datatype xsd:dateTime;]) ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   dcat:Dataset ;",
					"                           sh:path  dct:hasVersion ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   dcat:Dataset ;",
					"                           sh:path  dct:versionOf ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   dcat:Dataset ;",
					"                           sh:path  dct:hasPart ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   dcat:Dataset ;",
					"                           sh:path  dct:isPartOf ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   dct:Location ;",
					"                           sh:path  dct:spatial ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   dcat:Dataset ;",
					"                           sh:path  dct:isRequiredBy ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   dcat:Dataset ;",
					"                           sh:path  dct:requires ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   foaf:Document ;",
					"                           sh:path  dct:landingPage ;",
					"                           sh:maxCount 1;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   foaf:Document ;",
					"                           sh:path  dct:page ;",
					"                           sh:maxCount 1;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   dct:Frequency ;",
					"                           sh:path  dct:accurialPeriodicity ;",
					"                           sh:maxCount 1;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   adms:Identifier ;",
					"                           sh:path  adms:identifier ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   vcard:Kind ;",
					"                           sh:path  dcat:contactPoint ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"",
					"                   [",
					"                           sh:path  dct:temporal ;",
					"                           sh:class dct:PeriodeOfTime ;",
					"                           # sh:severity   sh:Violation",
					"                   ] ,",
					"                   [",
					"                           sh:path  dct:provenance ;",
					"                           sh:maxCount 1;",
					"                           sh:class dct:ProvenanceStatement ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:path  dct:accessRights ;",
					"                           sh:maxCount 1;",
					"                           sh:class dct:RightsStatement ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   dcat:Dataset ;",
					"                           sh:path  dct:references ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   dcat:Dataset ;",
					"                           sh:path  dct:isReferencedBy ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   dct:Standard ;",
					"                           sh:path  dct:conformsTo ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:path  dcat:keyword ;",
					"                           sh:nodeKind sh:Literal;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:minCount 1;",
					"                           sh:path  dcat:keyword ;",
					"                           # sh:severity   sh:Warning",
					"                   ],",
					"                   [",
					"                           sh:class dcat:Dataset;",
					"                           sh:path  dct:source ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class dcat:Dataset;",
					"                           sh:path  dct:replaces ;",
					"                           # sh:severity   sh:Violation",
					"                   ] ,",
					"                   [",
					"                           sh:class dcat:Dataset;",
					"                           sh:path  dct:isReplacedBy ;",
					"                           # sh:severity   sh:Violation",
					"                   ]   ,",
					"                   [",
					"                           sh:class dcat:Distribution;",
					"                           sh:path  dcat:distribution ;",
					"                           # sh:severity   sh:Violation",
					"                   ]   ,",
					"                   [",
					"                           sh:minCount 1;",
					"                           sh:path  dcat:distribution ;",
					"                           # sh:severity   sh:Warning",
					"                   ]    ,",
					"                   [",
					"                           sh:class dcat:Distribution;",
					"                           sh:path  adms:sample ;",
					"                           # sh:severity   sh:Violation",
					"                   ]   ,",
					"                   [",
					"                           sh:nodeKind sh:BlankNodeOrIRI;",
					"                           sh:path  dct:relation ;",
					"                           # sh:severity   sh:Violation",
					"                   ]   ,",
					"                   [",
					"                           sh:nodeKind sh:BlankNodeOrIRI;",
					"                           sh:path  dct:creator ;",
					"                           # sh:severity   sh:Violation",
					"                   ]   ,",
					"                  #[",
					"                  #        sh:class skos:Concept;",
					"                  #        sh:path  dcatno:accessRightsComment ;",
					"                  #        # sh:severity   sh:Violation",
					"                  #]   ,",
					"                   [",
					"                           sh:class skos:Concept;",
					"                           sh:path  dcat:theme ;",
					"                           # sh:severity   sh:Violation",
					"                   ]   ,",
					"                   [",
					"                           sh:class skos:Concept;",
					"                           sh:maxCount 1;",
					"                           sh:path  dct:type ;",
					"                           # sh:severity   sh:Violation",
					"                   ]   ,",
					"                   [",
					"                           sh:path  dct:subject ;",
					"                           sh:minCount 1 ;",
					"                           # sh:severity   sh:Warning",
					"                   ]",
					"                   ."

			));
			StringReader shaclRules5 = new StringReader(String.join("\n", "",

					"@prefix sh: <http://www.w3.org/ns/shacl#>.",
					"@prefix schema: <http://schema.org/> .",
					"@prefix spdx:  <http://spdx.org/rdf/terms#> .",
					"@prefix dct:   <http://purl.org/dc/terms/> .",
					"@prefix adms:  <http://www.w3.org/ns/adms#> .",
					"@prefix owl:   <http://www.w3.org/2002/07/owl#> .",
					"@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix skos:  <http://www.w3.org/2004/02/skos/core#> .",
					"@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .",
					"@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .",
					"@prefix dcat:  <http://www.w3.org/ns/dcat#> .",
					"@prefix foaf:  <http://xmlns.com/foaf/0.1/> .",
					"[]",
					"                  a              sh:NodeShape ;",
					"                  sh:targetClass  adms:Identifier ;",
					"                  sh:property",
					"                  [",
					"                          sh:minCount   1 ;",
					"                          sh:maxCount   1 ;",
					"                          sh:path  skos:notation ;",
					"                          sh:nodeKind sh:Literal;",
					"                          # sh:severity   sh:Violation",
					"                  ]."

			));
			StringReader shaclRules6 = new StringReader(String.join("\n", "",

					"@prefix sh: <http://www.w3.org/ns/shacl#>.",
					"@prefix schema: <http://schema.org/> .",
					"@prefix spdx:  <http://spdx.org/rdf/terms#> .",
					"@prefix dct:   <http://purl.org/dc/terms/> .",
					"@prefix adms:  <http://www.w3.org/ns/adms#> .",
					"@prefix owl:   <http://www.w3.org/2002/07/owl#> .",
					"@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix skos:  <http://www.w3.org/2004/02/skos/core#> .",
					"@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .",
					"@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .",
					"@prefix dcat:  <http://www.w3.org/ns/dcat#> .",
					"@prefix foaf:  <http://xmlns.com/foaf/0.1/> .",
					"[]",
					"                  a              sh:NodeShape ;",
					"                  sh:targetClass  dct:PeriodOfTime ;",
					"                  sh:property",
					"                  [",
					"                          sh:maxCount   1 ;",
					"                          sh:path schema:endDate ;",
					"                          sh:or ([sh:datatype xsd:date;] [sh:datatype xsd:dateTime;]) ;",
					"                          # sh:severity   sh:Violation",
					"                  ],",
					"                  [",
					"                          sh:maxCount   1 ;",
					"                          sh:path schema:startDate ;",
					"                          sh:or ([sh:datatype xsd:date;] [sh:datatype xsd:dateTime;]) ;",
					"                          # sh:severity   sh:Violation",
					"                  ]."

			));
			StringReader shaclRules7 = new StringReader(String.join("\n", "",

					"@prefix sh: <http://www.w3.org/ns/shacl#>.",
					"@prefix schema: <http://schema.org/> .",
					"@prefix spdx:  <http://spdx.org/rdf/terms#> .",
					"@prefix dct:   <http://purl.org/dc/terms/> .",
					"@prefix adms:  <http://www.w3.org/ns/adms#> .",
					"@prefix owl:   <http://www.w3.org/2002/07/owl#> .",
					"@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix skos:  <http://www.w3.org/2004/02/skos/core#> .",
					"@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .",
					"@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .",
					"@prefix dcat:  <http://www.w3.org/ns/dcat#> .",
					"@prefix foaf:  <http://xmlns.com/foaf/0.1/> .",
					"[]",
					"                   a              sh:NodeShape ;",
					"                   sh:targetClass  dcat:Distribution ;",
					"                                       sh:or (",
					"                                               [ sh:path  dcat:accessURL ; sh:minCount 1 ]",
					"                                               [ sh:path  dcat:downloadURL ; sh:minCount 1 ]",
					"                                                ) ;",
					"                   sh:property",
					"                   [",
					"                           sh:class   dct:Standard ;",
					"                           sh:path  dct:conformsTo ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   dct:RightsStatement ;",
					"                           sh:path  dct:rights ;",
					"                           sh:maxCount 1;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   dct:Standard ;",
					"                           sh:path  dct:conformsTo ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   foaf:Document ;",
					"                           sh:path  foaf:page ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   spdx:Checksum ;",
					"                           sh:path  spdx:checksum ;",
					"                           sh:maxCount 1;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"",
					"                           sh:path  dct:license ;",
					"                           sh:maxCount 1;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:class   skos:Concept ;",
					"                           sh:path  adms:status ;",
					"                           sh:maxCount 1;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                                sh:path  dcat:accessURL ;",
					"                           sh:nodeKind sh:IRI ;",
					"",
					"                           # sh:severity   sh:Violation",
					"",
					"",
					"                   ],",
					"                   [",
					"                           sh:nodeKind sh:IRI ;",
					"                           sh:path  dcat:downloadURL ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:path  dct:byteSize ;",
					"                           sh:datatype xsd:decimal;",
					"                           sh:maxCount 1 ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:path  dct:issued ;",
					"                           sh:or ([sh:datatype xsd:date;] [sh:datatype xsd:dateTime;]) ;",
					"                           sh:maxCount 1 ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:path  dct:modified ;",
					"                           sh:or ([sh:datatype xsd:date;] [sh:datatype xsd:dateTime;]) ;",
					"                           sh:maxCount 1 ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:path  dct:title ;",
					"                           sh:nodeKind sh:Literal;",
					"                           sh:maxCount 1 ;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:path  dct:description ;",
					"                           sh:nodeKind sh:Literal;",
					"                           # sh:severity   sh:Violation",
					"                   ],",
					"                   [",
					"                           sh:path  dct:description ;",
					"                           sh:minCount 1 ;",
					"                           # sh:severity   sh:Warning",
					"                   ]."

			));
			StringReader shaclRules8 = new StringReader(String.join("\n", "",

					"@prefix sh: <http://www.w3.org/ns/shacl#>.",
					"@prefix schema: <http://schema.org/> .",
					"@prefix spdx:  <http://spdx.org/rdf/terms#> .",
					"@prefix dct:   <http://purl.org/dc/terms/> .",
					"@prefix adms:  <http://www.w3.org/ns/adms#> .",
					"@prefix owl:   <http://www.w3.org/2002/07/owl#> .",
					"@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix skos:  <http://www.w3.org/2004/02/skos/core#> .",
					"@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .",
					"@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .",
					"@prefix dcat:  <http://www.w3.org/ns/dcat#> .",
					"@prefix foaf:  <http://xmlns.com/foaf/0.1/> .",
					"[]",
					"                  a              sh:NodeShape ;",
					"                  sh:targetClass  spdx:Checksum ;",
					"                  sh:property",
					"                  [",
					"                          sh:maxCount   1 ;",
					"                          sh:minCount   1 ;",
					"                          sh:path spdx:algorithm ;",
					"                          # sh:in (spdx:checksumAlgorithm_md5 spdx:checksumAlgorithm_sha1 spdx:checksumAlgorithm_sha256) ;",
					"                          # sh:severity   sh:Violation",
					"                  ],",
					"                  [",
					"                          sh:maxCount   1 ;",
					"                          sh:minCount   1 ;",
					"                          sh:path spdx:checksumValue ;",
					"                          sh:datatype xsd:hexBinary ;",
					"                          # sh:severity   sh:Violation",
					"                  ]."

			));
			StringReader shaclRules9 = new StringReader(String.join("\n", "",

					"@prefix sh: <http://www.w3.org/ns/shacl#>.",
					"@prefix schema: <http://schema.org/> .",
					"@prefix spdx:  <http://spdx.org/rdf/terms#> .",
					"@prefix dct:   <http://purl.org/dc/terms/> .",
					"@prefix adms:  <http://www.w3.org/ns/adms#> .",
					"@prefix owl:   <http://www.w3.org/2002/07/owl#> .",
					"@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix skos:  <http://www.w3.org/2004/02/skos/core#> .",
					"@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .",
					"@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .",
					"@prefix dcat:  <http://www.w3.org/ns/dcat#> .",
					"@prefix foaf:  <http://xmlns.com/foaf/0.1/> .",
					"[]",
					"                  a              sh:NodeShape ;",
					"                  sh:targetClass  dct:LicenseDocument ;",
					"                  sh:property",
					"                  [",
					"                          sh:maxCount   1 ;",
					"                          sh:path dct:type ;",
					"                          sh:class skos:Concept ;",
					"                          # sh:severity   sh:Violation",
					"                  ] ,",
					"                  [",
					"                          sh:minCount   1 ;",
					"                          sh:path dct:type ;",
					"                          # sh:severity   sh:Warning",
					"                  ]   ."

			));
			StringReader shaclRules10 = new StringReader(String.join("\n", "",

					"@prefix sh: <http://www.w3.org/ns/shacl#>.",
					"@prefix schema: <http://schema.org/> .",
					"@prefix spdx:  <http://spdx.org/rdf/terms#> .",
					"@prefix dct:   <http://purl.org/dc/terms/> .",
					"@prefix adms:  <http://www.w3.org/ns/adms#> .",
					"@prefix owl:   <http://www.w3.org/2002/07/owl#> .",
					"@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix skos:  <http://www.w3.org/2004/02/skos/core#> .",
					"@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .",
					"@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .",
					"@prefix dcat:  <http://www.w3.org/ns/dcat#> .",
					"@prefix foaf:  <http://xmlns.com/foaf/0.1/> .",
					"[]",
					"                  a              sh:NodeShape ;",
					"                  sh:targetClass  skos:Concept ;",
					"                  sh:property",
					"                  [",
					"                          sh:minCount   1 ;",
					"                          sh:path skos:prefLabel ;",
					"                          sh:nodeKind sh:Literal;",
					"                          # sh:severity   sh:Violation",
					"                  ]    ,",
					"                  [",
					"                          sh:uniqueLang   true ;",
					"                          sh:path skos:prefLabel ;",
					"                          # sh:severity   sh:Violation",
					"                  ]    ,",
					"                  [",
					"                          sh:path skos:inScheme ;",
					"                          sh:class skos:ConceptScheme ;",
					"                          # sh:severity   sh:Violation",
					"                  ]   ,",
					"                  [",
					"                          sh:minCount   1 ;",
					"                          sh:path skos:inScheme ;",
					"                          # sh:severity   sh:Warning",
					"                  ]   ."

			));
			StringReader shaclRules11 = new StringReader(String.join("\n", "",

					"@prefix sh: <http://www.w3.org/ns/shacl#>.",
					"@prefix schema: <http://schema.org/> .",
					"@prefix spdx:  <http://spdx.org/rdf/terms#> .",
					"@prefix dct:   <http://purl.org/dc/terms/> .",
					"@prefix adms:  <http://www.w3.org/ns/adms#> .",
					"@prefix owl:   <http://www.w3.org/2002/07/owl#> .",
					"@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix skos:  <http://www.w3.org/2004/02/skos/core#> .",
					"@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .",
					"@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .",
					"@prefix dcat:  <http://www.w3.org/ns/dcat#> .",
					"@prefix foaf:  <http://xmlns.com/foaf/0.1/> .",
					"[]",
					"                  a              sh:NodeShape ;",
					"                  sh:targetClass  skos:ConceptScheme ;",
					"                  sh:property",
					"                  [",
					"                          sh:path dct:title ;",
					"                          sh:nodeKind sh:Literal;",
					"                          # sh:severity   sh:Violation",
					"                  ] ."));

			connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(shaclRules1, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(shaclRules2, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(shaclRules3, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(shaclRules4, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(shaclRules5, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(shaclRules6, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(shaclRules7, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(shaclRules8, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(shaclRules9, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(shaclRules10, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(shaclRules11, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();
		}

		repository.shutDown();

	}

}
