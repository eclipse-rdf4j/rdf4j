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
package org.eclipse.rdf4j.rio.turtle;

import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.TurtleWriterSettings;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Arjohn Kampman
 */
public class TurtleWriterTest extends AbstractTurtleWriterTest {

	private final IRI uri1;

	private final IRI uri2;

	private final String exNs;

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

		Model actual = Rio.parse(new StringReader(stringWriter.toString()), "", RDFFormat.TURTLE);
		assertTrue(Models.isomorphic(expected, actual));
	}

	@Test
	@Ignore
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

		Model expected = Rio.parse(new StringReader(data), "", RDFFormat.TURTLE);

		StringWriter stringWriter = new StringWriter();
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		Rio.write(expected, stringWriter, RDFFormat.TURTLE, config);

		Model actual = Rio.parse(new StringReader(stringWriter.toString()), "", RDFFormat.TURTLE);
		assertTrue(Models.isomorphic(expected, actual));
	}

	@Test
	public void testBNodeValuesInList() throws Exception {
		String data = "" +
				"@prefix ex:    <http://example.com/ns#> .\n" +
				"\n" +
				"ex:a  ex:list   (_:b0 _:b0) .";

		Model expected = Rio.parse(new StringReader(data), "", RDFFormat.TURTLE);

		StringWriter stringWriter = new StringWriter();
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		Rio.write(expected, stringWriter, RDFFormat.TURTLE, config);

		Model actual = Rio.parse(new StringReader(stringWriter.toString()), "", RDFFormat.TURTLE);
		assertTrue(Models.isomorphic(expected, actual));
	}

	@Test
	public void testBNodeValuesInList2() throws Exception {
		String data = "" +
				"@prefix ex:    <http://example.com/ns#> .\n" +
				"\n" +
				"ex:a  ex:list   (_:b0 _:b1) .";

		Model expected = Rio.parse(new StringReader(data), "", RDFFormat.TURTLE);

		StringWriter stringWriter = new StringWriter();
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		Rio.write(expected, stringWriter, RDFFormat.TURTLE, config);
		Model actual = Rio.parse(new StringReader(stringWriter.toString()), "", RDFFormat.TURTLE);
		assertTrue(Models.isomorphic(expected, actual));
	}

	@Test
	public void testBNodeValuesInList3() throws Exception {
		String data = "" +
				"@prefix ex:    <http://example.com/ns#> .\n" +
				"\n" +
				"ex:a  ex:list   (_:b0 _:b1) .\n" +
				"_:b1 ex:foo ex:bar.\n";

		Model expected = Rio.parse(new StringReader(data), "", RDFFormat.TURTLE);

		StringWriter stringWriter = new StringWriter();
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		Rio.write(expected, stringWriter, RDFFormat.TURTLE, config);
		Model actual = Rio.parse(new StringReader(stringWriter.toString()), "", RDFFormat.TURTLE);
		assertTrue(Models.isomorphic(expected, actual));
	}

	@Test
	public void testInvalidList_nonListPredicate() throws Exception {
		String data = "@prefix ex:    <http://example.com/ns#> .\n" +
				"@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
				"\n" +
				"ex:ex1    ex:list  [ rdf:first  [  ] ;\n" +
				"                             rdf:rest   rdf:nil ;\n" +
				"                             ex:abc   ex:def\n" +
				"                           ]\n" +
				"                   .\n" +
				"";

		Model expected = Rio.parse(new StringReader(data), "", RDFFormat.TURTLE);

		StringWriter stringWriter = new StringWriter();
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		Rio.write(expected, stringWriter, RDFFormat.TURTLE, config);

		Model actual = Rio.parse(new StringReader(stringWriter.toString()), "", RDFFormat.TURTLE);
		assertTrue(Models.isomorphic(expected, actual));
	}

	@Test
	public void testInvalidList_multipleRdfRestPredicates() throws Exception {
		String data = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
				"\n" +
				"_:node1ei5i7ns4x11752 a sh:ValidationReport;\n" +
				"  sh:conforms false;\n" +
				"  <http://rdf4j.org/schema/rdf4j#truncated> false;\n" +
				"  sh:result _:5f9908e9-caee-4059-aa52-69227a27ac20, _:3a37be3d-7af5-4b74-b7cf-b6358196bf0e,\n" +
				"    _:ba3bb72d-eb40-4aec-987a-bf2a3d925f06, _:6c86bbd3-a6ea-4b4f-8881-d479cff3aff5, _:ff167b12-c1b4-4341-bf63-187bed54e1ed,\n"
				+
				"    _:c3c6878c-7a69-41c8-b65b-50ab02da9519, _:f07f8ae4-1137-454c-ac6a-f385631b592e, _:a7b959ba-a7b4-4922-9974-9f363fdaccdf .\n"
				+
				"\n" +
				"_:5f9908e9-caee-4059-aa52-69227a27ac20 a sh:ValidationResult;\n" +
				"  sh:focusNode ex:validPerson1;\n" +
				"  sh:value ex:steve;\n" +
				"  sh:resultPath ex:knows;\n" +
				"  sh:sourceConstraintComponent sh:NotConstraintComponent;\n" +
				"  sh:resultSeverity sh:Violation;\n" +
				"  sh:sourceShape _:node1ei5i7sdex1 .\n" +
				"\n" +
				"_:node1ei5i7sdex1 a sh:PropertyShape;\n" +
				"  sh:path ex:knows;\n" +
				"  sh:not _:node1ei5i7sdex2 .\n" +
				"\n" +
				"_:node1ei5i7sdex2 a sh:NodeShape;\n" +
				"  sh:and _:node1ei5i7sdex3 .\n" +
				"\n" +
				"_:node1ei5i7sdex3 rdf:first _:node1ei5i7sdex4;\n" +
				"  rdf:rest _:node1ei5i7ns4x11754, _:node1ei5i7ns4x11756, _:node1ei5i7ns4x11758, _:node1ei5i7ns4x11760,\n"
				+
				"    _:node1ei5i7ns4x11763, _:node1ei5i7ns4x11765, _:node1ei5i7ns4x11767, _:node1ei5i7ns4x11769 .\n" +
				"\n" +
				"_:node1ei5i7ns4x11754 rdf:first _:node1ei5i7sdex7;\n" +
				"  rdf:rest rdf:nil .\n" +
				"\n" +
				"_:node1ei5i7sdex4 a sh:NodeShape;\n" +
				"  sh:in _:node1ei5i7sdex5 .\n" +
				"\n" +
				"_:node1ei5i7sdex5 rdf:first ex:steve;\n" +
				"  rdf:rest rdf:nil .\n" +
				"\n" +
				"_:node1ei5i7sdex7 a sh:NodeShape;\n" +
				"  sh:in _:node1ei5i7sdex8 .\n" +
				"\n" +
				"_:node1ei5i7sdex8 rdf:first ex:pete;\n" +
				"  rdf:rest _:node1ei5i7ns4x11755, _:node1ei5i7ns4x11757, _:node1ei5i7ns4x11759, _:node1ei5i7ns4x11761,\n"
				+
				"    _:node1ei5i7ns4x11764, _:node1ei5i7ns4x11766, _:node1ei5i7ns4x11768, _:node1ei5i7ns4x11770 .\n" +
				"\n" +
				"_:node1ei5i7ns4x11755 rdf:first ex:steve;\n" +
				"  rdf:rest rdf:nil .\n" +
				"\n" +
				"_:3a37be3d-7af5-4b74-b7cf-b6358196bf0e a sh:ValidationResult;\n" +
				"  sh:focusNode ex:validPerson1;\n" +
				"  sh:value ex:steve;\n" +
				"  sh:resultPath ex:knows;\n" +
				"  sh:sourceConstraintComponent sh:NotConstraintComponent;\n" +
				"  sh:resultSeverity sh:Violation;\n" +
				"  sh:sourceShape _:node1ei5i7sdex1 .\n" +
				"\n" +
				"_:node1ei5i7ns4x11756 rdf:first _:node1ei5i7sdex7;\n" +
				"  rdf:rest rdf:nil .\n" +
				"\n" +
				"_:node1ei5i7ns4x11757 rdf:first ex:steve;\n" +
				"  rdf:rest rdf:nil .\n" +
				"\n" +
				"_:ba3bb72d-eb40-4aec-987a-bf2a3d925f06 a sh:ValidationResult;\n" +
				"  sh:focusNode ex:validPerson1;\n" +
				"  sh:value ex:steve;\n" +
				"  sh:resultPath ex:knows;\n" +
				"  sh:sourceConstraintComponent sh:NotConstraintComponent;\n" +
				"  sh:resultSeverity sh:Violation;\n" +
				"  sh:sourceShape _:node1ei5i7sdex1 .\n" +
				"\n" +
				"_:node1ei5i7ns4x11758 rdf:first _:node1ei5i7sdex7;\n" +
				"  rdf:rest rdf:nil .\n" +
				"\n" +
				"_:node1ei5i7ns4x11759 rdf:first ex:steve;\n" +
				"  rdf:rest rdf:nil .\n" +
				"\n" +
				"_:6c86bbd3-a6ea-4b4f-8881-d479cff3aff5 a sh:ValidationResult;\n" +
				"  sh:focusNode ex:validPerson1;\n" +
				"  sh:value ex:steve;\n" +
				"  sh:resultPath ex:knows;\n" +
				"  sh:sourceConstraintComponent sh:NotConstraintComponent;\n" +
				"  sh:resultSeverity sh:Violation;\n" +
				"  sh:sourceShape _:node1ei5i7sdex1 .\n" +
				"\n" +
				"_:node1ei5i7ns4x11760 rdf:first _:node1ei5i7sdex7;\n" +
				"  rdf:rest rdf:nil .\n" +
				"\n" +
				"_:node1ei5i7ns4x11761 rdf:first ex:steve;\n" +
				"  rdf:rest rdf:nil .\n" +
				"\n" +
				"_:ff167b12-c1b4-4341-bf63-187bed54e1ed a sh:ValidationResult;\n" +
				"  sh:focusNode ex:validPerson1;\n" +
				"  sh:value ex:steve;\n" +
				"  sh:resultPath ex:knows;\n" +
				"  sh:sourceConstraintComponent sh:NotConstraintComponent;\n" +
				"  sh:resultSeverity sh:Violation;\n" +
				"  sh:sourceShape _:node1ei5i7sdex1 .\n" +
				"\n" +
				"_:node1ei5i7ns4x11763 rdf:first _:node1ei5i7sdex7;\n" +
				"  rdf:rest rdf:nil .\n" +
				"\n" +
				"_:node1ei5i7ns4x11764 rdf:first ex:steve;\n" +
				"  rdf:rest rdf:nil .\n" +
				"\n" +
				"_:c3c6878c-7a69-41c8-b65b-50ab02da9519 a sh:ValidationResult;\n" +
				"  sh:focusNode ex:validPerson1;\n" +
				"  sh:value ex:steve;\n" +
				"  sh:resultPath ex:knows;\n" +
				"  sh:sourceConstraintComponent sh:NotConstraintComponent;\n" +
				"  sh:resultSeverity sh:Violation;\n" +
				"  sh:sourceShape _:node1ei5i7sdex1 .\n" +
				"\n" +
				"_:node1ei5i7ns4x11765 rdf:first _:node1ei5i7sdex7;\n" +
				"  rdf:rest rdf:nil .\n" +
				"\n" +
				"_:node1ei5i7ns4x11766 rdf:first ex:steve;\n" +
				"  rdf:rest rdf:nil .\n" +
				"\n" +
				"_:f07f8ae4-1137-454c-ac6a-f385631b592e a sh:ValidationResult;\n" +
				"  sh:focusNode ex:validPerson1;\n" +
				"  sh:value ex:steve;\n" +
				"  sh:resultPath ex:knows;\n" +
				"  sh:sourceConstraintComponent sh:NotConstraintComponent;\n" +
				"  sh:resultSeverity sh:Violation;\n" +
				"  sh:sourceShape _:node1ei5i7sdex1 .\n" +
				"\n" +
				"_:node1ei5i7ns4x11767 rdf:first _:node1ei5i7sdex7;\n" +
				"  rdf:rest rdf:nil .\n" +
				"\n" +
				"_:node1ei5i7ns4x11768 rdf:first ex:steve;\n" +
				"  rdf:rest rdf:nil .\n" +
				"\n" +
				"_:a7b959ba-a7b4-4922-9974-9f363fdaccdf a sh:ValidationResult;\n" +
				"  sh:focusNode ex:validPerson1;\n" +
				"  sh:value ex:steve;\n" +
				"  sh:resultPath ex:knows;\n" +
				"  sh:sourceConstraintComponent sh:NotConstraintComponent;\n" +
				"  sh:resultSeverity sh:Violation;\n" +
				"  sh:sourceShape _:node1ei5i7sdex1 .\n" +
				"\n" +
				"_:node1ei5i7ns4x11769 rdf:first _:node1ei5i7sdex7;\n" +
				"  rdf:rest rdf:nil .\n" +
				"\n" +
				"_:node1ei5i7ns4x11770 rdf:first ex:steve;\n" +
				"  rdf:rest rdf:nil .\n";

		Model expected = Rio.parse(new StringReader(data), "", RDFFormat.TURTLE);

		StringWriter stringWriter = new StringWriter();
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		Rio.write(expected, stringWriter, RDFFormat.TURTLE, config);

		Model actual = Rio.parse(new StringReader(stringWriter.toString()), "", RDFFormat.TURTLE);
		assertTrue(Models.isomorphic(expected, actual));
	}

	@Test
	public void testBlankNodeInlining_directCircularReference() throws Exception {
		String data = "@prefix dc: <http://purl.org/dc/terms/> .\n" +
				"@prefix ns0: <http://www.w3.org/ns/earl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"<http://example.org/DISPLAY_NAME>\n" +
				"  a <http://www.w3.org/ns/earl#TestCriterion> ;\n" +
				"  dc:hasPart _:genid3 .\n" +
				"\n" +
				"_:genid3\n" +
				"  a ns0:TestCase ;\n" +
				"  ns0:assertions [\n" +
				"    a ns0:Assertion ;\n" +
				"    ns0:test _:genid3 \n" + // direct circular reference between two blank nodes
				"  ] .";

		Model expected = Rio.parse(new StringReader(data), "", RDFFormat.TURTLE);

//		System.out.println("### EXPECTED ###");
//		System.out.println(data);
//		System.out.println("#################\n");
//
		StringWriter stringWriter = new StringWriter();
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		Rio.write(expected, stringWriter, RDFFormat.TURTLE, config);

//		System.out.println("### ACTUAL ###");
//		System.out.println(stringWriter.toString());
//		System.out.println("#################\n");
//
		Model actual = Rio.parse(new StringReader(stringWriter.toString()), "", RDFFormat.TURTLE);
		assertTrue(Models.isomorphic(expected, actual));
	}

	@Test
	public void testBlankNodeInlining_indirectCircularReference() throws Exception {
		String data = "@prefix dc: <http://purl.org/dc/terms/> .\n" +
				"@prefix ns0: <http://www.w3.org/ns/earl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"<http://example.org/DISPLAY_NAME>\n" +
				"  a <http://www.w3.org/ns/earl#TestCriterion> ;\n" +
				"  dc:hasPart _:genid3 .\n" +
				"\n" +
				"_:genid3\n" +
				"  a ns0:TestCase ;\n" +
				"  ns0:assertions [\n" +
				"    a ns0:Assertion ;\n" +
				"    ns0:test [ ns0:reference _:genid3 ] \n" + // indirect blank node cycle
				"  ] .";

		Model expected = Rio.parse(new StringReader(data), "", RDFFormat.TURTLE);

//		System.out.println("### EXPECTED ###");
//		System.out.println(data);
//		System.out.println("#################\n");

		StringWriter stringWriter = new StringWriter();
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		Rio.write(expected, stringWriter, RDFFormat.TURTLE, config);

//		System.out.println("### ACTUAL ###");
//		System.out.println(stringWriter.toString());
//		System.out.println("#################\n");

		Model actual = Rio.parse(new StringReader(stringWriter.toString()), "", RDFFormat.TURTLE);
		assertTrue(Models.isomorphic(expected, actual));
	}

	@Test
	public void testBlankNodeInlining_indirectCircularReferenceWithIRI() throws Exception {
		String data = "@prefix dc: <http://purl.org/dc/terms/> .\n" +
				"@prefix ns0: <http://www.w3.org/ns/earl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"<http://example.org/DISPLAY_NAME>\n" +
				"  a <http://www.w3.org/ns/earl#TestCriterion> ;\n" +
				"  dc:hasPart _:genid3 .\n" +
				"\n" +
				"_:genid3\n" +
				"  a ns0:TestCase ;\n" +
				"  ns0:assertions [\n" +
				"    a ns0:Assertion ;\n" +
				"    ns0:test ns0:testSubject \n" +
				"  ] ." +
				" ns0:testSubject ns0:reference _:genid3 ."; // blank node cycle broken with an IRI subject

		Model expected = Rio.parse(new StringReader(data), "", RDFFormat.TURTLE);

//		System.out.println("### EXPECTED ###");
//		System.out.println(data);
//		System.out.println("#################\n");

		StringWriter stringWriter = new StringWriter();
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		Rio.write(expected, stringWriter, RDFFormat.TURTLE, config);

//		System.out.println("### ACTUAL ###");
//		System.out.println(stringWriter.toString());
//		System.out.println("#################\n");
//
		Model actual = Rio.parse(new StringReader(stringWriter.toString()), "", RDFFormat.TURTLE);
		assertTrue(Models.isomorphic(expected, actual));
	}

	@Test
	public void testIgnoreAbbreviateNumbers() throws Exception {
		StringWriter sw = new StringWriter();

		WriterConfig config = new WriterConfig();
		// abbreviate numbers should be ignored when pretty print is false
		config.set(BasicWriterSettings.PRETTY_PRINT, false)
				.set(TurtleWriterSettings.ABBREVIATE_NUMBERS, true);

		Rio.write(getAbbrevTestModel(), sw, RDFFormat.TURTLE, config);

		String result = sw.toString();
		assertTrue(result.contains("\"1234567.89\"^^<http://www.w3.org/2001/XMLSchema#double>"));
		assertTrue(result.contains("\"-2\"^^<http://www.w3.org/2001/XMLSchema#integer>"));
		assertTrue(result.contains("\"55.66\"^^<http://www.w3.org/2001/XMLSchema#decimal>"));
	}
}
