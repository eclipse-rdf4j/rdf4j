/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.eclipse.rdf4j.queryrender.sparql.SPARQLQueryRenderer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SPARQLQueryRenderTest {
	private static String base;
	private static String lineSeparator;
	private static SPARQLParser parser;
	private static SPARQLQueryRenderer renderer;

	@BeforeAll
	public static void beforeAll() {
		base = "http://example.org/base/";
		lineSeparator = System.lineSeparator();
		parser = new SPARQLParser();
		renderer = new SPARQLQueryRenderer();
	}

	@AfterAll
	public static void afterAll() {
		parser = null;
		renderer = null;
	}

	@Test
	public void renderArbitraryLengthPathTest() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select ?s ?o").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  ?s <http://www.w3.org/2000/01/rdf-schema#subClassOf+> ?o.").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?s ?o").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  ?s <http://www.w3.org/2000/01/rdf-schema#subClassOf+> ?o.").append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	@Test
	public void renderBindTest1() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select ?b").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(1 as ?b).").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?b").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer> as ?b).").append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	@Test
	public void renderBindTest2() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select ?b").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  ?s ?p ?o.").append(lineSeparator);
		sb.append("  bind(?s as ?b).").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?b").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  ?s ?p ?o.").append(lineSeparator);
		sb.append("  bind(?s as ?b).").append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	@Test
	public void renderBindTest3() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select ?b1 ?b2").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(1 as ?b1).").append(lineSeparator);
		sb.append("  bind(2 as ?b2).").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?b1 ?b2").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer> as ?b1).");
		sb.append(lineSeparator);
		sb.append("  bind(\"\"\"2\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer> as ?b2).");
		sb.append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	@Test
	public void renderBindTest4() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select ?b1 ?b2").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(1 as ?b1).").append(lineSeparator);
		sb.append("  bind(<http://www.example.org/MyFunction>(2, ?b1) as ?b2).").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?b1 ?b2").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer> as ?b1).");
		sb.append(lineSeparator);
		sb.append("  bind(<http://www.example.org/MyFunction>");
		sb.append("(\"\"\"2\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>, ?b1) as ?b2).");
		sb.append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	@Test
	public void renderBindTest5() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select ?b1 ?b2").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(1 as ?b1).").append(lineSeparator);
		sb.append("  bind(concat(\"numberStr: \", str(?b1)) as ?b2).").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?b1 ?b2").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer> as ?b1).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(concat(\"\"\"numberStr: \"\"\"^^<http://www.w3.org/2001/XMLSchema#string>,  str(?b1)) as ?b2).");
		sb.append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	@Test
	public void renderFunctionalFormsTest() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select ?b1").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(if(1 = 1, \":)\", \":(\") as ?b1).").append(lineSeparator);
		sb.append("  bind(if(0 || 0, \":)\", \":(\") as ?b2).").append(lineSeparator);
		sb.append("  bind(if(1 && 1, \":)\", \":(\") as ?b3).").append(lineSeparator);
		sb.append("  bind(bound(?b1) as ?b4).").append(lineSeparator);
		// TODO: COALESCE
		// sb.append(" bind(coalesce(?b1, 3) as ?b5).").append(lineSeparator);
		sb.append("  bind(sameTerm(?b1, ?b3) as ?b8).").append(lineSeparator);
		// TODO: (NOT) IN
		// sb.append(" bind(1 in(1, 2, 3) as ?b9).").append(lineSeparator);
		// sb.append(" bind(1 not in(1, 2, 3) as ?b10).").append(lineSeparator);
		// TODO: (NOT) EXISTS
		// sb.append(" filter exists { ?s ?p ?o. }").append(lineSeparator);
		// sb.append(" filter not exists { ?s ?p 1. }").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?b1").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append(
				"  bind(if((\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer> = \"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>), \"\"\":)\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\":(\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b1).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(if( (\"\"\"0\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer> || \"\"\"0\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>), \"\"\":)\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\":(\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b2).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(if( (\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer> && \"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>), \"\"\":)\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\":(\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b3).");
		sb.append(lineSeparator);
		sb.append("  bind( bound(?b1) as ?b4).").append(lineSeparator);
		// TODO: COALESCE
		// sb.append(" bind( coalesce(?b1,
		// \"\"\"3\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>) as ?b5).")
		// .append(lineSeparator);
		sb.append("  bind( sameTerm(?b1, ?b3) as ?b8).").append(lineSeparator);

		// TODO: (NOT) IN
		// sb.append(
		// " bind(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>
		// in(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>,
		// \"\"\"2\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>,
		// \"\"\"3\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>) as ?b9).")
		// .append(lineSeparator);
		// sb.append(
		// " bind(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer> not
		// in(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>,
		// \"\"\"2\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>,
		// \"\"\"3\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>) as ?b10). ")
		// .append(lineSeparator);
		// TODO: (NOT)EXISTS
		// sb.append(" filter exists { ?s ?p ?o. }").append(lineSeparator);
		// sb.append(" filter !exists { ?s ?p
		// \"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>. }")
		// .append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	@Test
	public void renderConstruct() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("construct  {").append(lineSeparator);
		sb.append("  ?s ?p ?o.").append(lineSeparator);
		sb.append("}").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  ?s ?p ?o.").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();
		executeRenderTest(query, query);
	}

	@Test
	public void renderFunctionsOnRdfTermsTest() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select ?b2").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append(" bind(isIRI(<http://www.w3.org/2001/XMLSchema#integer>) as ?b1).").append(lineSeparator);
		sb.append("  bind(isURI(<http://www.w3.org/2001/XMLSchema#integer>) as ?b2).").append(lineSeparator);
		sb.append("  bind(isBlank(?b2) as ?b3).").append(lineSeparator);
		sb.append("  bind(isLiteral(1) as ?b4).").append(lineSeparator);
		sb.append("  bind(isNumeric(1) as ?b5).").append(lineSeparator);
		sb.append("  bind(str(1) as ?b6).").append(lineSeparator);
		sb.append("  bind(lang(\"Roberto\"@es) as ?b7).").append(lineSeparator);
		sb.append("  bind(datatype(1) as ?b8).").append(lineSeparator);
		sb.append("  bind(iri(\"abc\") as ?b9).").append(lineSeparator);
		sb.append("  bind(uri(\"abc\") as ?b10).").append(lineSeparator);
		// TODO:
		// sb.append(" bind(bnode() as ?b11).").append(lineSeparator);
		sb.append("  bind(strdt(\"123\", <http://www.w3.org/2001/XMLSchema#integer>) as ?b12).").append(lineSeparator);
		sb.append("	 bind(strlang(\"abc\", \"en\") as ?b13).").append(lineSeparator);
		sb.append("  bind(uuid() as ?b14).").append(lineSeparator);
		sb.append("  bind(struuid() as ?b15).").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?b2").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind( isURI(<http://www.w3.org/2001/XMLSchema#integer>) as ?b1).").append(lineSeparator);
		sb.append("  bind( isURI(<http://www.w3.org/2001/XMLSchema#integer>) as ?b2).").append(lineSeparator);
		sb.append("  bind( isBlank(?b2) as ?b3).").append(lineSeparator);
		sb.append("  bind( isLiteral(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>) as ?b4).")
				.append(lineSeparator);
		sb.append("  bind( isNumeric(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>) as ?b5).")
				.append(lineSeparator);
		sb.append("  bind( str(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>) as ?b6).")
				.append(lineSeparator);
		sb.append("  bind( lang(\"\"\"Roberto\"\"\"@es) as ?b7).").append(lineSeparator);
		sb.append("  bind( datatype(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>) as ?b8).")
				.append(lineSeparator);
		sb.append("  bind( IRI(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b9).")
				.append(lineSeparator);
		sb.append("  bind( IRI(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b10).")
				.append(lineSeparator);
		// TODO:
		// sb.append(" bind(bnode() as ?b11).").append(lineSeparator);
		sb.append(
				"  bind(strdt(\"\"\"123\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, <http://www.w3.org/2001/XMLSchema#integer>) as ?b12).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(strlang(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"en\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b13).")
				.append(lineSeparator);
		sb.append("  bind(uuid() as ?b14).").append(lineSeparator);
		sb.append("  bind(struuid() as ?b15).").append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	@Test
	public void renderFunctionsOnStringsTest() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select ?b1").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#string-length>(\"abc\") as ?b1).");
		sb.append(lineSeparator);
		sb.append("  bind(strlen(\"abc\") as ?b2).").append(lineSeparator);
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#substring>(\"abc\", \"abc\") as ?b3).");
		sb.append(lineSeparator);
		sb.append("  bind(substr(\"abc\", \"abc\") as ?b4).").append(lineSeparator);
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#upper-case>(\"abc\") as ?b5).").append(lineSeparator);
		sb.append("  bind(ucase(\"abc\") as ?b6).").append(lineSeparator);
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#lower-case>(\"abc\") as ?b7).");
		sb.append(lineSeparator);
		sb.append("  bind(lcase(\"abc\") as ?b8).").append(lineSeparator);
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#starts-with>(\"abc\", \"abc\") as ?b9).");
		sb.append(lineSeparator);
		sb.append("  bind(strstarts(\"abc\", \"abc\") as ?b10).").append(lineSeparator);
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#ends-with>(\"abc\", \"abc\") as ?b11).");
		sb.append(lineSeparator);
		sb.append("  bind(strends(\"abc\", \"abc\") as ?b12).").append(lineSeparator);
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#contains>(\"abc\", \"abc\") as ?b13).");
		sb.append(lineSeparator);
		sb.append("  bind(contains(\"abc\", \"abc\") as ?b14).").append(lineSeparator);
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#substring-before>(\"abc\", \"abc\") as ?b15).");
		sb.append(lineSeparator);
		sb.append("  bind(strbefore(\"abc\", \"abc\") as ?b16).").append(lineSeparator);
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#substring-after>(\"abc\", \"abc\") as ?b17).");
		sb.append(lineSeparator);
		sb.append("  bind(strafter(\"abc\", \"abc\") as ?b18).").append(lineSeparator);
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#encode-for-uri>(\"abc\") as ?b19).");
		sb.append(lineSeparator);
		sb.append("  bind(encode_for_uri(\"abc\") as ?b20).").append(lineSeparator);
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#concat>(\"abc\", \"abc\", \"abc\") as ?b21).");
		sb.append(lineSeparator);
		sb.append("  bind(concat(\"abc\", \"abc\", \"abc\") as ?b22).").append(lineSeparator);
		sb.append("  bind(langmatches(lang(\"abc\"), \"abc\") as ?b23).").append(lineSeparator);
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#matches>(\"abc\", \"abc\") as ?b24).");
		sb.append(lineSeparator);
		sb.append("  bind(regex(\"abc\", \"abc\") as ?b25).").append(lineSeparator);
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#replace>(\"abc\", \"abc\", \"abc\") as ?b26).");
		sb.append(lineSeparator);
		sb.append("  bind(replace(\"abc\", \"abc\", \"abc\") as ?b27).").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?b1").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(strlen(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b1).");
		sb.append(lineSeparator);
		sb.append("  bind(strlen(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b2).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(substr(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b3).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(substr(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b4).");
		sb.append(lineSeparator);
		sb.append("  bind(ucase(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b5).");
		sb.append(lineSeparator);
		sb.append("  bind(ucase(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b6).");
		sb.append(lineSeparator);
		sb.append("  bind(lcase(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b7).");
		sb.append(lineSeparator);
		sb.append("  bind(lcase(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b8).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(strstarts(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b9).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(strstarts(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b10).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(strends(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b11).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(strends(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b12).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(contains(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b13).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(contains(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b14).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(strbefore(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b15).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(strbefore(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b16).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(strafter(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b17).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(strafter(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b18).");
		sb.append(lineSeparator);
		sb.append("  bind(encode_for_uri(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b19).");
		sb.append(lineSeparator);
		sb.append("  bind(encode_for_uri(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b20).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(concat(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b21).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(concat(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b22).");
		sb.append(lineSeparator);
		sb.append(
				"  bind( langMatches( lang(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>), \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b23).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(regex(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b24).");
		sb.append(lineSeparator);
		sb.append(
				"  bind( regex(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b25).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(replace(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b26).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(replace(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>, \"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b27).");
		sb.append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	@Test
	public void renderFunctionsOnNumericsTest() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select ?b1").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#numeric-abs>(1) as ?b1).").append(lineSeparator);
		sb.append("  bind(abs(1) as ?b2).").append(lineSeparator);
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#numeric-round>(1.9) as ?b3).").append(lineSeparator);
		sb.append("  bind(round(1.9) as ?b4).").append(lineSeparator);
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#numeric-ceil>(1.5) as ?b5).").append(lineSeparator);
		sb.append("  bind(ceil(1.5 ) as ?b6).").append(lineSeparator);
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#numeric-floor>(1.1) as ?b7).").append(lineSeparator);
		sb.append("  bind(floor(1.1) as ?b8).").append(lineSeparator);
		sb.append("  bind(rand() as ?b9).").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?b1").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(abs(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>) as ?b1).");
		sb.append(lineSeparator);
		sb.append("  bind(abs(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>) as ?b2).");
		sb.append(lineSeparator);
		sb.append("  bind(round(\"\"\"1.9\"\"\"^^<http://www.w3.org/2001/XMLSchema#decimal>) as ?b3).");
		sb.append(lineSeparator);
		sb.append("  bind(round(\"\"\"1.9\"\"\"^^<http://www.w3.org/2001/XMLSchema#decimal>) as ?b4).");
		sb.append(lineSeparator);
		sb.append("  bind(ceil(\"\"\"1.5\"\"\"^^<http://www.w3.org/2001/XMLSchema#decimal>) as ?b5).");
		sb.append(lineSeparator);
		sb.append("  bind(ceil(\"\"\"1.5\"\"\"^^<http://www.w3.org/2001/XMLSchema#decimal>) as ?b6).");
		sb.append(lineSeparator);
		sb.append("  bind(floor(\"\"\"1.1\"\"\"^^<http://www.w3.org/2001/XMLSchema#decimal>) as ?b7).");
		sb.append(lineSeparator);
		sb.append("  bind(floor(\"\"\"1.1\"\"\"^^<http://www.w3.org/2001/XMLSchema#decimal>) as ?b8).");
		sb.append(lineSeparator);
		sb.append("  bind(rand() as ?b9).").append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	@Test
	public void renderFunctionsOnDatesAndTimesTest() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("prefix xsd: <http://www.w3.org/2001/XMLSchema#>").append(lineSeparator);
		sb.append("select ?b1").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(now() as ?b1).").append(lineSeparator);
		sb.append(
				"  bind(<http://www.w3.org/2005/xpath-functions#year-from-dateTime>(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) as ?b2).");
		sb.append(lineSeparator);
		sb.append("  bind(year(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) as ?b3).").append(lineSeparator);
		sb.append(
				"  bind(<http://www.w3.org/2005/xpath-functions#month-from-dateTime>(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) as ?b4).");
		sb.append(lineSeparator);
		sb.append("  bind(month(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) as ?b5).").append(lineSeparator);
		sb.append(
				"  bind(<http://www.w3.org/2005/xpath-functions#day-from-dateTime>(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) as ?b6).");
		sb.append(lineSeparator);
		sb.append("  bind(day(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) as ?b7).").append(lineSeparator);
		sb.append(
				"  bind(<http://www.w3.org/2005/xpath-functions#hours-from-dateTime>(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) as ?b8).");
		sb.append(lineSeparator);
		sb.append("  bind(hours(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) as ?b9).").append(lineSeparator);
		sb.append(
				"  bind(<http://www.w3.org/2005/xpath-functions#minutes-from-dateTime>(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) as ?b10).");
		sb.append(lineSeparator);
		sb.append("  bind(minutes(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) as ?b11).").append(lineSeparator);
		sb.append(
				"  bind(<http://www.w3.org/2005/xpath-functions#seconds-from-dateTime>(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) as ?b12).");
		sb.append(lineSeparator);
		sb.append("  bind(seconds(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) as ?b13).").append(lineSeparator);
		sb.append(
				"  bind(<http://www.w3.org/2005/xpath-functions#timezone-from-dateTime>(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) as ?b14).");
		sb.append(lineSeparator);
		sb.append("  bind(timezone(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) as ?b15).").append(lineSeparator);
		sb.append("  bind(tz(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) as ?b16).").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?b1").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(now() as ?b1).").append(lineSeparator);
		sb.append(
				"  bind(year(\"\"\"2011-01-10T14:45:13.815-05:00\"\"\"^^<http://www.w3.org/2001/XMLSchema#dateTime>) as ?b2).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(year(\"\"\"2011-01-10T14:45:13.815-05:00\"\"\"^^<http://www.w3.org/2001/XMLSchema#dateTime>) as ?b3).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(month(\"\"\"2011-01-10T14:45:13.815-05:00\"\"\"^^<http://www.w3.org/2001/XMLSchema#dateTime>) as ?b4).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(month(\"\"\"2011-01-10T14:45:13.815-05:00\"\"\"^^<http://www.w3.org/2001/XMLSchema#dateTime>) as ?b5).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(day(\"\"\"2011-01-10T14:45:13.815-05:00\"\"\"^^<http://www.w3.org/2001/XMLSchema#dateTime>) as ?b6).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(day(\"\"\"2011-01-10T14:45:13.815-05:00\"\"\"^^<http://www.w3.org/2001/XMLSchema#dateTime>) as ?b7).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(hours(\"\"\"2011-01-10T14:45:13.815-05:00\"\"\"^^<http://www.w3.org/2001/XMLSchema#dateTime>) as ?b8).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(hours(\"\"\"2011-01-10T14:45:13.815-05:00\"\"\"^^<http://www.w3.org/2001/XMLSchema#dateTime>) as ?b9).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(minutes(\"\"\"2011-01-10T14:45:13.815-05:00\"\"\"^^<http://www.w3.org/2001/XMLSchema#dateTime>) as ?b10).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(minutes(\"\"\"2011-01-10T14:45:13.815-05:00\"\"\"^^<http://www.w3.org/2001/XMLSchema#dateTime>) as ?b11).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(seconds(\"\"\"2011-01-10T14:45:13.815-05:00\"\"\"^^<http://www.w3.org/2001/XMLSchema#dateTime>) as ?b12).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(seconds(\"\"\"2011-01-10T14:45:13.815-05:00\"\"\"^^<http://www.w3.org/2001/XMLSchema#dateTime>) as ?b13).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(timezone(\"\"\"2011-01-10T14:45:13.815-05:00\"\"\"^^<http://www.w3.org/2001/XMLSchema#dateTime>) as ?b14).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(timezone(\"\"\"2011-01-10T14:45:13.815-05:00\"\"\"^^<http://www.w3.org/2001/XMLSchema#dateTime>) as ?b15).");
		sb.append(lineSeparator);
		sb.append(
				"  bind(tz(\"\"\"2011-01-10T14:45:13.815-05:00\"\"\"^^<http://www.w3.org/2001/XMLSchema#dateTime>) as ?b16).");
		sb.append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	@Test
	public void renderHashFunctionsTest() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select ?b1").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(md5(\"abc\") as ?b1).").append(lineSeparator);
		sb.append("  bind(sha1(\"abc\") as ?b2).").append(lineSeparator);
		sb.append("  bind(sha256(\"abc\") as ?b3).").append(lineSeparator);
		sb.append("  bind(sha384(\"abc\") as ?b4).").append(lineSeparator);
		sb.append("  bind(sha512(\"abc\") as ?b5).").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?b1").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(md5(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b1).");
		sb.append(lineSeparator);
		sb.append("  bind(sha1(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b2).");
		sb.append(lineSeparator);
		sb.append("  bind(sha256(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b3).");
		sb.append(lineSeparator);
		sb.append("  bind(sha384(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b4).");
		sb.append(lineSeparator);
		sb.append("  bind(sha512(\"\"\"abc\"\"\"^^<http://www.w3.org/2001/XMLSchema#string>) as ?b5).");
		sb.append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	public void executeRenderTest(String query, String expected) throws Exception {
		ParsedQuery pq = parser.parseQuery(query, base);
		String actual = renderer.render(pq);

		assertEquals(expected, actual);
	}
}
