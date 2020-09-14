/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtle;

import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.junit.Test;

/**
 * @author Arjohn Kampman
 */
public class TurtleWriterTest extends RDFWriterTest {

	private IRI uri1;

	private IRI uri2;

	private String exNs;

	public TurtleWriterTest() {
		super(new TurtleWriterFactory(), new TurtleParserFactory());

		exNs = "http://example.org/";
		uri1 = vf.createIRI(exNs, "uri1");
		uri2 = vf.createIRI(exNs, "uri2");
	}

	@Override
	protected void setupWriterConfig(WriterConfig config) {
		config.set(BasicWriterSettings.PRETTY_PRINT, false);
	}

	@Test
	public void testBlankNodeInlining1() throws Exception {
		Model expected = Rio.parse(
				new StringReader(
						String.join("\n", "",
								"@prefix ex: <http://example.com/ns#> .",
								"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .",
								"@prefix sh: <http://www.w3.org/ns/shacl#> .",
								"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
								"",
								"_:bn1 a sh:ValidationReport;",
								"  sh:result _:bn2 .",
								"",
								"_:bn2 a sh:ValidationResult;",
								"  sh:detail _:bn3 .",
								"",
								"_:bn3 a sh:ValidationResult;",
								"  sh:sourceShape _:bn4 .",
								"",
								"_:bn4 a sh:NodeShape;",
								"  sh:datatype xsd:string .",
								"",
								"_:bn3 sh:value \"123\" .",
								"",
								"_:bn2 sh:focusNode ex:validPerson1;",
								"  sh:sourceShape _:bn5 .",
								"",
								"_:bn5 a sh:PropertyShape;",
								"  sh:not _:bn4;",
								"  sh:path ex:age .",
								"",
								"_:bn2 sh:value \"123\" ."

						)
				), "", RDFFormat.TURTLE);

		StringWriter stringWriter = new StringWriter();

		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		Rio.write(expected, stringWriter, RDFFormat.TURTLE, config);

//		System.out.println(stringWriter.toString());

		Model actual = Rio.parse(new StringReader(stringWriter.toString()), "", RDFFormat.TURTLE);

		assertTrue(Models.isomorphic(expected, actual));

	}

	@Test
	public void testBlankNodeInlining2() throws Exception {
		Model expected = Rio.parse(
				new StringReader(
						String.join("\n", "",
								"_:b1 <http://www.w3.org/ns/shacl#focusNode> <http://example.com/ns#validPerson1>, _:b3;",
								"		<http://www.w3.org/ns/shacl#value> _:b3;",
								"  	<http://www.w3.org/ns/shacl#sourceShape> [ a <http://www.w3.org/ns/shacl#PropertyShape>; a [ a [] ] ] .",
								"[] a [a []]."

						)
				), "", RDFFormat.TURTLE);

		StringWriter stringWriter = new StringWriter();
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		Rio.write(expected, stringWriter, RDFFormat.TURTLE, config);

//		System.out.println(stringWriter.toString());

		Model actual = Rio.parse(new StringReader(stringWriter.toString()), "", RDFFormat.TURTLE);

		assertTrue(Models.isomorphic(expected, actual));

	}

	/**
	 * Test that blank node subjects are processed in correct order even when not supplied in order.
	 *
	 */
	@Test
	public void testBlanknodeInlining_SubjectOrder() throws Exception {

		String data = "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"@prefix rep: <http://www.openrdf.org/config/repository#> .\n" +
				"@prefix sr: <http://www.openrdf.org/config/repository/sail#> .\n" +
				"@prefix sail: <http://www.openrdf.org/config/sail#> .\n" +
				"@prefix sb: <http://www.openrdf.org/config/sail/base#> .\n" +
				"@prefix ms: <http://www.openrdf.org/config/sail/memory#> .\n" +
				"\n" +
				"_:node1eemcmeprx2 rep:repositoryType \"openrdf:SailRepository\";\n" +
				"  sr:sailImpl _:node1eemcmeprx3 .\n" +
				"\n" +
				"_:node1eemcmeprx1 a rep:Repository;\n" +
				"  rep:repositoryID \"test-strict\";\n" +
				"  rep:repositoryImpl _:node1eemcmeprx2 .\n" +
				"\n" +
				"_:node1eemcmeprx3 sail:sailType \"openrdf:MemoryStore\";\n" +
				"  sb:evaluationStrategyFactory \"org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory\" .\n";

		Model expected = Rio.parse(new StringReader(data), "", RDFFormat.TURTLE);

		StringWriter stringWriter = new StringWriter();
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		Rio.write(expected, stringWriter, RDFFormat.TURTLE, config);

//		System.out.println(stringWriter.toString());

		Model actual = Rio.parse(new StringReader(stringWriter.toString()), "", RDFFormat.TURTLE);
		assertTrue(Models.isomorphic(expected, actual));

	}

	@Test
	public void testNoBuffering() throws Exception {
		String data = "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"@prefix rep: <http://www.openrdf.org/config/repository#> .\n" +
				"@prefix sr: <http://www.openrdf.org/config/repository/sail#> .\n" +
				"@prefix sail: <http://www.openrdf.org/config/sail#> .\n" +
				"@prefix sb: <http://www.openrdf.org/config/sail/base#> .\n" +
				"@prefix ms: <http://www.openrdf.org/config/sail/memory#> .\n" +
				"\n" +
				"_:node1eemcmeprx2 rep:repositoryType \"openrdf:SailRepository\";\n" +
				"  sr:sailImpl _:node1eemcmeprx3 .\n" +
				"\n" +
				"_:node1eemcmeprx1 a rep:Repository;\n" +
				"  rep:repositoryID \"test-strict\";\n" +
				"  rep:repositoryImpl _:node1eemcmeprx2 .\n" +
				"\n" +
				"_:node1eemcmeprx3 sail:sailType \"openrdf:MemoryStore\";\n" +
				"  sb:evaluationStrategyFactory \"org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory\" .\n";

		Model expected = Rio.parse(new StringReader(data), "", RDFFormat.TURTLE);

		StringWriter stringWriter = new StringWriter();
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.INLINE_BLANK_NODES, false);
		config.set(BasicWriterSettings.PRETTY_PRINT, false);
		Rio.write(expected, stringWriter, RDFFormat.TURTLE, config);

//		System.out.println(stringWriter.toString());

		Model actual = Rio.parse(new StringReader(stringWriter.toString()), "", RDFFormat.TURTLE);
		assertTrue(Models.isomorphic(expected, actual));
	}

	@Test
	public void anotherBnodeTest() throws Exception {
		String data = "@prefix ex:    <http://example.com/ns#> .\n" +
				"@prefix sh:    <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
				"@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n" +
				"@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n" +
				"@prefix foaf:  <http://xmlns.com/foaf/0.1/> .\n" +
				"\n" +
				"_:b0    a        sh:PropertyShape ;\n" +
				"        sh:not   [ a       sh:NodeShape ;\n" +
				"                   sh:and  [ rdf:first  [ a       sh:NodeShape ;\n" +
				"                                          sh:in   ( ex:steve )\n" +
				"                                        ] ;\n" +
				"                             rdf:rest   ( _:b1\n" +
				"                                        ) ;\n" +
				"                             rdf:rest   ( _:b1\n" +
				"                                        ) ;\n" +
				"                             rdf:rest   ( _:b1\n" +
				"                                        ) ;\n" +
				"                             rdf:rest   ( _:b1\n" +
				"                                        ) ;\n" +
				"                             rdf:rest   ( _:b1\n" +
				"                                        ) ;\n" +
				"                             rdf:rest   ( _:b1\n" +
				"                                        ) ;\n" +
				"                             rdf:rest   ( _:b1\n" +
				"                                        ) ;\n" +
				"                             rdf:rest   ( _:b1\n" +
				"                                        )\n" +
				"                           ]\n" +
				"                 ] ;\n" +
				"        sh:path  ex:knows .\n" +
				"\n" +
				"_:b1    a       sh:NodeShape ;\n" +
				"        sh:in   [ rdf:first  ex:pete ;\n" +
				"                  rdf:rest   ( ex:steve ) ;\n" +
				"                  rdf:rest   ( ex:steve ) ;\n" +
				"                  rdf:rest   ( ex:steve ) ;\n" +
				"                  rdf:rest   ( ex:steve ) ;\n" +
				"                  rdf:rest   ( ex:steve ) ;\n" +
				"                  rdf:rest   ( ex:steve ) ;\n" +
				"                  rdf:rest   ( ex:steve ) ;\n" +
				"                  rdf:rest   ( ex:steve )\n" +
				"                ] .\n" +
				"\n" +
				"[ a            sh:ValidationReport ;\n" +
				"  <http://rdf4j.org/schema/rdf4j#truncated>\n" +
				"          false ;\n" +
				"  sh:conforms  false ;\n" +
				"  sh:result    [ a                             sh:ValidationResult ;\n" +
				"                 sh:focusNode                  ex:validPerson1 ;\n" +
				"                 sh:resultPath                 ex:knows ;\n" +
				"                 sh:resultSeverity             sh:Violation ;\n" +
				"                 sh:sourceConstraintComponent  sh:NotConstraintComponent ;\n" +
				"                 sh:sourceShape                _:b0 ;\n" +
				"                 sh:value                      ex:steve\n" +
				"               ] ;\n" +
				"  sh:result    [ a                             sh:ValidationResult ;\n" +
				"                 sh:focusNode                  ex:validPerson1 ;\n" +
				"                 sh:resultPath                 ex:knows ;\n" +
				"                 sh:resultSeverity             sh:Violation ;\n" +
				"                 sh:sourceConstraintComponent  sh:NotConstraintComponent ;\n" +
				"                 sh:sourceShape                _:b0 ;\n" +
				"                 sh:value                      ex:steve\n" +
				"               ] ;\n" +
				"  sh:result    [ a                             sh:ValidationResult ;\n" +
				"                 sh:focusNode                  ex:validPerson1 ;\n" +
				"                 sh:resultPath                 ex:knows ;\n" +
				"                 sh:resultSeverity             sh:Violation ;\n" +
				"                 sh:sourceConstraintComponent  sh:NotConstraintComponent ;\n" +
				"                 sh:sourceShape                _:b0 ;\n" +
				"                 sh:value                      ex:steve\n" +
				"               ] ;\n" +
				"  sh:result    [ a                             sh:ValidationResult ;\n" +
				"                 sh:focusNode                  ex:validPerson1 ;\n" +
				"                 sh:resultPath                 ex:knows ;\n" +
				"                 sh:resultSeverity             sh:Violation ;\n" +
				"                 sh:sourceConstraintComponent  sh:NotConstraintComponent ;\n" +
				"                 sh:sourceShape                _:b0 ;\n" +
				"                 sh:value                      ex:steve\n" +
				"               ] ;\n" +
				"  sh:result    [ a                             sh:ValidationResult ;\n" +
				"                 sh:focusNode                  ex:validPerson1 ;\n" +
				"                 sh:resultPath                 ex:knows ;\n" +
				"                 sh:resultSeverity             sh:Violation ;\n" +
				"                 sh:sourceConstraintComponent  sh:NotConstraintComponent ;\n" +
				"                 sh:sourceShape                _:b0 ;\n" +
				"                 sh:value                      ex:steve\n" +
				"               ] ;\n" +
				"  sh:result    [ a                             sh:ValidationResult ;\n" +
				"                 sh:focusNode                  ex:validPerson1 ;\n" +
				"                 sh:resultPath                 ex:knows ;\n" +
				"                 sh:resultSeverity             sh:Violation ;\n" +
				"                 sh:sourceConstraintComponent  sh:NotConstraintComponent ;\n" +
				"                 sh:sourceShape                _:b0 ;\n" +
				"                 sh:value                      ex:steve\n" +
				"               ] ;\n" +
				"  sh:result    [ a                             sh:ValidationResult ;\n" +
				"                 sh:focusNode                  ex:validPerson1 ;\n" +
				"                 sh:resultPath                 ex:knows ;\n" +
				"                 sh:resultSeverity             sh:Violation ;\n" +
				"                 sh:sourceConstraintComponent  sh:NotConstraintComponent ;\n" +
				"                 sh:sourceShape                _:b0 ;\n" +
				"                 sh:value                      ex:steve\n" +
				"               ] ;\n" +
				"  sh:result    [ a                             sh:ValidationResult ;\n" +
				"                 sh:focusNode                  ex:validPerson1 ;\n" +
				"                 sh:resultPath                 ex:knows ;\n" +
				"                 sh:resultSeverity             sh:Violation ;\n" +
				"                 sh:sourceConstraintComponent  sh:NotConstraintComponent ;\n" +
				"                 sh:sourceShape                _:b0 ;\n" +
				"                 sh:value                      ex:steve\n" +
				"               ]\n" +
				"] .\n";

		System.out.println("### EXPECTED ###");
		System.out.println(data);
		System.out.println("#################\n");

		Model expected = Rio.parse(new StringReader(data), "", RDFFormat.TURTLE);

		StringWriter stringWriter = new StringWriter();
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		Rio.write(expected, stringWriter, RDFFormat.TURTLE, config);

		System.out.println("### ACTUAL ###");
		System.out.println(stringWriter.toString());
		System.out.println("#################\n");

		Model actual = Rio.parse(new StringReader(stringWriter.toString()), "", RDFFormat.TURTLE);
		assertTrue(Models.isomorphic(expected, actual));
	}

	@Test
	public void anotherBnodeTestReduced() throws Exception {
		String data = "" +
				"@prefix ex:    <http://example.com/ns#> .\n" +
				"@prefix sh:    <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
				"@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n" +
				"@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n" +
				"@prefix foaf:  <http://xmlns.com/foaf/0.1/> .\n" +
				"\n" +
				"ex:a  ex:list   (_:b0 _:b0) .";

		System.out.println("### EXPECTED ###");
		System.out.println(data);
		System.out.println("#################\n");

		Model expected = Rio.parse(new StringReader(data), "", RDFFormat.TURTLE);

		StringWriter stringWriter = new StringWriter();
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		Rio.write(expected, stringWriter, RDFFormat.TURTLE, config);

		System.out.println("### ACTUAL ###");
		System.out.println(stringWriter.toString());
		System.out.println("#################\n");

		Model actual = Rio.parse(new StringReader(stringWriter.toString()), "", RDFFormat.TURTLE);
		assertTrue(Models.isomorphic(expected, actual));
	}
}
