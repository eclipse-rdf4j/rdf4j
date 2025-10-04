/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.query.explanation.GenericPlanNode;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class QueryPlanRetrievalTest {

	public static final String MAIN_QUERY = String.join("\n", "",
			"{",
			"    {",
			"        OPTIONAL {",
			"            ?d ?e ?f",
			"        }",
			"    } ",
			"    ?a a ?c, ?d. ",
			"    FILTER(?c != ?d && ?c != \"<\") ",
			"    OPTIONAL{",
			"        ?d ?e ?f",
			"    } ",
			"}");

	public static final String TUPLE_QUERY = "SELECT ?a WHERE " + MAIN_QUERY;
	public static final String ASK_QUERY = "ASK WHERE " + MAIN_QUERY;
	public static final String CONSTRUCT_QUERY = "CONSTRUCT {?a a ?c, ?d} WHERE " + MAIN_QUERY;

	public static final String SUB_QUERY = "select ?a where {{select ?a where {?a a ?type}} {SELECT ?a WHERE "
			+ MAIN_QUERY + "}}";

	public static final String SUB_QUERY2 = "PREFIX epo: <http://data.europa.eu/a4g/ontology#>\n" +
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX legal: <https://www.w3.org/ns/legal#>\n" +
			"PREFIX dcterms: <http://purl.org/dc/terms#>\n" +
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
			"PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
			"\n" +
			"SELECT DISTINCT ?countryID ?year (COUNT(DISTINCT ?lot) AS ?amountLots) (SUM(if(?bidders = 1, 1, 0)) AS ?numSingleBidders) WHERE {\n"
			+
			"\n" +
			"        ?proc a epo:Procedure .\n" +
			"        ?proc epo:hasProcedureType ?p .\n" +
			"        ?stat epo:concernsSubmissionsForLot ?lot .\n" +
			"        ?stat a epo:SubmissionStatisticalInformation .\n" +
			"        ?stat epo:hasReceivedTenders ?bidders .\n" +
			"        ?resultnotice a epo:ResultNotice .\n" +
			"        ?resultnotice epo:hasDispatchDate ?ddate .\n" +
			"        ?proc epo:hasProcurementScopeDividedIntoLot ?lot .\n" +
			"        ?resultnotice epo:refersToRole ?buyerrole .\n" +
			"      	  ?resultnotice epo:refersToProcedure ?proc .\n" +
			"\n" +
			"      \tFILTER ( ?p != <http://publications.europa.eu/resource/authority/procurement-procedure-type/neg-wo-call>)\n"
			+
			"\t\tBIND(year(xsd:dateTime(?ddate)) AS ?year) .\n" +
			"\n" +
			"\n" +
			"        {\n" +
			"          SELECT DISTINCT ?buyerrole ?countryID  WHERE {\n" +
			"            ?org epo:hasBuyerType ?buytype .\n" +
			"            FILTER (?buytype != <http://publications.europa.eu/resource/authority/buyer-legal-type/eu-int-org> )\n"
			+
			"\n" +
			"            ?buyerrole epo:playedBy ?org .\n" +
			"            ?org legal:registeredAddress ?orgaddress .\n" +
			"            ?orgaddress epo:hasCountryCode ?countrycode  .\n" +
			"            ?countrycode dc:identifier ?countryID .\n" +
			"\n" +
			"           }\n" +
			"        }\n" +
			"} GROUP BY ?countryID ?year";

	public static final String CONSTRUCT = "PREFIX epo: <http://data.europa.eu/a4g/ontology#>\n" +
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX legal: <https://www.w3.org/ns/legal#>\n" +
			"PREFIX dcterms: <http://purl.org/dc/terms#>\n" +
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
			"PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
			"\n" +
			"CONSTRUCT {" +
			"\n" +
			"        ?proc a epo:Procedure .\n" +
			"        ?proc epo:hasProcedureType ?p .\n" +
			"        ?stat epo:concernsSubmissionsForLot ?lot .\n" +
			"        ?stat a epo:SubmissionStatisticalInformation .\n" +
			"        ?stat epo:hasReceivedTenders ?bidders .\n" +
			"        ?resultnotice a epo:ResultNotice .\n" +
			"        ?resultnotice epo:hasDispatchDate ?ddate .\n" +
			"        ?proc epo:hasProcurementScopeDividedIntoLot ?lot .\n" +
			"        ?resultnotice epo:refersToRole ?buyerrole .\n" +
			"      	  ?resultnotice epo:refersToProcedure ?proc .\n" +
			"}" +
			" WHERE {\n"
			+
			"\n" +
			"        ?proc a epo:Procedure .\n" +
			"        ?proc epo:hasProcedureType ?p .\n" +
			"        ?stat epo:concernsSubmissionsForLot ?lot .\n" +
			"        ?stat a epo:SubmissionStatisticalInformation .\n" +
			"        ?stat epo:hasReceivedTenders ?bidders .\n" +
			"        ?resultnotice a epo:ResultNotice .\n" +
			"        ?resultnotice epo:hasDispatchDate ?ddate .\n" +
			"        ?proc epo:hasProcurementScopeDividedIntoLot ?lot .\n" +
			"        ?resultnotice epo:refersToRole ?buyerrole .\n" +
			"      	  ?resultnotice epo:refersToProcedure ?proc .\n" +
			"\n" +
			"      \tFILTER ( ?p != <http://publications.europa.eu/resource/authority/procurement-procedure-type/neg-wo-call>)\n"
			+
			"\t\tBIND(year(xsd:dateTime(?ddate)) AS ?year) .\n" +
			"\n" +
			"\n" +
			"        {\n" +
			"          SELECT DISTINCT ?buyerrole ?countryID  WHERE {\n" +
			"            ?org epo:hasBuyerType ?buytype .\n" +
			"            FILTER (?buytype != <http://publications.europa.eu/resource/authority/buyer-legal-type/eu-int-org> )\n"
			+
			"\n" +
			"            ?buyerrole epo:playedBy ?org .\n" +
			"            ?org legal:registeredAddress ?orgaddress .\n" +
			"            ?orgaddress epo:hasCountryCode ?countrycode  .\n" +
			"            ?countrycode dc:identifier ?countryID .\n" +
			"\n" +
			"           }\n" +
			"        }\n" +
			"} GROUP BY ?countryID ?year";

	public static final String UNION_QUERY = "select ?a where {?a a ?type. {?a ?b ?c, ?c2. {?c2 a ?type1}UNION{?c2 a ?type2}} UNION {?type ?d ?c}}";

	ValueFactory vf = SimpleValueFactory.getInstance();

	private void addData(SailRepository sailRepository) {
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDF.PROPERTY, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(vf.createBNode("01"), FOAF.KNOWS, vf.createBNode("02"));
			connection.add(vf.createBNode("03"), FOAF.KNOWS, vf.createBNode("04"));
			connection.add(vf.createBNode("05"), FOAF.KNOWS, vf.createBNode("06"));
			connection.add(vf.createBNode("07"), FOAF.KNOWS, vf.createBNode("08"));
			connection.add(vf.createBNode("09"), FOAF.KNOWS, vf.createBNode("10"));
			connection.add(vf.createBNode("11"), FOAF.KNOWS, vf.createBNode("12"));
			connection.add(vf.createBNode("13"), FOAF.KNOWS, vf.createBNode("14"));
			connection.add(vf.createBNode("15"), FOAF.KNOWS, vf.createBNode("16"));
		}
	}

	@Test
	public void testFilterDontMergeAcrossSubqueryOptimizedPlanRetrieval() throws Exception {
		String sparql = "SELECT * WHERE {?s ?p ?o .  {?o ?p2 ?o2. FILTER(?o > ?o2) FILTER(?o2 != ?o)}  {?o ?p3 ?o3. FILTER(?o > ?o3) FILTER(?o != ?o3  || ?o = ?o3)} FILTER(?s > ?o)}";
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(sparql);
			String actual = query.explain(Explanation.Level.Optimized).toString();
			assertThat(actual).isEqualToNormalizingNewlines(
					"Join (JoinIterator)\n" +
							"╠══ Filter [left]\n" +
							"║  ├── Compare (>)\n" +
							"║  │     Var (name=s)\n" +
							"║  │     Var (name=o)\n" +
							"║  └── StatementPattern (costEstimate=5.67, resultSizeEstimate=12)\n" +
							"║        s: Var (name=s)\n" +
							"║        p: Var (name=p)\n" +
							"║        o: Var (name=o)\n" +
							"╚══ Join (HashJoinIteration) [right]\n" +
							"   ├── Filter (new scope) [left]\n" +
							"   │  ╠══ And\n" +
							"   │  ║  ├── Compare (>)\n" +
							"   │  ║  │     Var (name=o)\n" +
							"   │  ║  │     Var (name=o2)\n" +
							"   │  ║  └── Compare (!=)\n" +
							"   │  ║        Var (name=o2)\n" +
							"   │  ║        Var (name=o)\n" +
							"   │  ╚══ StatementPattern (resultSizeEstimate=12)\n" +
							"   │        s: Var (name=o)\n" +
							"   │        p: Var (name=p2)\n" +
							"   │        o: Var (name=o2)\n" +
							"   └── Filter (new scope) [right]\n" +
							"      ╠══ And\n" +
							"      ║  ├── Compare (>)\n" +
							"      ║  │     Var (name=o)\n" +
							"      ║  │     Var (name=o3)\n" +
							"      ║  └── Or\n" +
							"      ║     ╠══ Compare (!=)\n" +
							"      ║     ║     Var (name=o)\n" +
							"      ║     ║     Var (name=o3)\n" +
							"      ║     ╚══ Compare (=)\n" +
							"      ║           Var (name=o)\n" +
							"      ║           Var (name=o3)\n" +
							"      ╚══ StatementPattern (resultSizeEstimate=12)\n" +
							"            s: Var (name=o)\n" +
							"            p: Var (name=p3)\n" +
							"            o: Var (name=o3)\n");
		}
		sailRepository.shutDown();
	}

	@Test
	public void testSpecificFilterScopeScenario() throws Exception {
		String sparql = "PREFIX ex: <http://example.com/>\n" +
				"\n" +
				"SELECT ?s ?p ?o ?o2 ?g WHERE {\n" +
				"  {                                      # scope‑0\n" +
				"    {                                    # scope‑1  (UNION A)\n" +
				"      ?s ex:p ?o .                       #   pattern A1\n" +
				"      ?s ex:q ?o2 .\n" +
				"      FILTER (?o > 1 && ?o2 < 5 && BOUND(?s) && !BOUND(?g))\n" +
				"    }\n" +
				"    UNION {                              # scope‑1  (UNION B)\n" +
				"      GRAPH ?g { ?s ex:r ?o }            #   GRAPH introduces scope‑2\n" +
				"      FILTER (?o != 42 && ?g != ex:Bad)\n" +
				"    }\n" +
				"  }\n" +
				"  OPTIONAL {                             # scope‑0 → scope‑3\n" +
				"    BIND (EXISTS { ?s ex:p2 ?x } AS ?hasX)\n" +
				"    FILTER (?hasX && STRLEN(STR(?x)) > 3)\n" +
				"  }\n" +
				"  FILTER (?o2 IN (1,2,3,4,5))            # top‑level filter\n" +
				"}";
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(sparql);
			String actual = query.explain(Explanation.Level.Optimized).toString();
			assertThat(actual).isEqualToNormalizingNewlines("Projection\n" +
					"╠══ ProjectionElemList\n" +
					"║     ProjectionElem \"s\"\n" +
					"║     ProjectionElem \"p\"\n" +
					"║     ProjectionElem \"o\"\n" +
					"║     ProjectionElem \"o2\"\n" +
					"║     ProjectionElem \"g\"\n" +
					"╚══ LeftJoin (LeftJoinIterator)\n" +
					"      Compare (>)\n" +
					"      ╠══ FunctionCall (http://www.w3.org/2005/xpath-functions#string-length)\n" +
					"      ║     Str\n" +
					"      ║        Var (name=x)\n" +
					"      ╚══ ValueConstant (value=\"3\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n" +
					"      Union\n" +
					"      ╠══ Filter\n" +
					"      ║  ├── Not\n" +
					"      ║  │     Bound\n" +
					"      ║  │        Var (name=g)\n" +
					"      ║  └── Join (JoinIterator)\n" +
					"      ║     ╠══ Filter (new scope) [left]\n" +
					"      ║     ║  ├── And\n" +
					"      ║     ║  │  ╠══ Bound\n" +
					"      ║     ║  │  ║     Var (name=s)\n" +
					"      ║     ║  │  ╚══ Compare (>)\n" +
					"      ║     ║  │        Var (name=o)\n" +
					"      ║     ║  │        ValueConstant (value=\"1\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n"
					+
					"      ║     ║  └── StatementPattern (costEstimate=2.50, resultSizeEstimate=0)\n" +
					"      ║     ║        s: Var (name=s)\n" +
					"      ║     ║        p: Var (name=_const_c03ab50c_uri, value=http://example.com/p, anonymous)\n" +
					"      ║     ║        o: Var (name=o)\n" +
					"      ║     ╚══ Filter [right]\n" +
					"      ║        ├── And\n" +
					"      ║        │  ╠══ ListMemberOperator\n" +
					"      ║        │  ║     Var (name=o2)\n" +
					"      ║        │  ║     ValueConstant (value=\"1\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n"
					+
					"      ║        │  ║     ValueConstant (value=\"2\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n"
					+
					"      ║        │  ║     ValueConstant (value=\"3\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n"
					+
					"      ║        │  ║     ValueConstant (value=\"4\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n"
					+
					"      ║        │  ║     ValueConstant (value=\"5\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n"
					+
					"      ║        │  ╚══ Compare (<)\n" +
					"      ║        │        Var (name=o2)\n" +
					"      ║        │        ValueConstant (value=\"5\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n"
					+
					"      ║        └── StatementPattern (costEstimate=2.24, resultSizeEstimate=0)\n" +
					"      ║              s: Var (name=s)\n" +
					"      ║              p: Var (name=_const_c03ab50d_uri, value=http://example.com/q, anonymous)\n" +
					"      ║              o: Var (name=o2)\n" +
					"      ╚══ Filter\n" +
					"         ├── And\n" +
					"         │  ╠══ And\n" +
					"         │  ║  ├── Compare (!=)\n" +
					"         │  ║  │     Var (name=g)\n" +
					"         │  ║  │     ValueConstant (value=http://example.com/Bad)\n" +
					"         │  ║  └── Compare (!=)\n" +
					"         │  ║        Var (name=o)\n" +
					"         │  ║        ValueConstant (value=\"42\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n" +
					"         │  ╚══ ListMemberOperator\n" +
					"         │        Var (name=o2)\n" +
					"         │        ValueConstant (value=\"1\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n" +
					"         │        ValueConstant (value=\"2\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n" +
					"         │        ValueConstant (value=\"3\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n" +
					"         │        ValueConstant (value=\"4\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n" +
					"         │        ValueConstant (value=\"5\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n" +
					"         └── StatementPattern FROM NAMED CONTEXT (resultSizeEstimate=0)\n" +
					"               s: Var (name=s)\n" +
					"               p: Var (name=_const_c03ab50e_uri, value=http://example.com/r, anonymous)\n" +
					"               o: Var (name=o)\n" +
					"               c: Var (name=g)\n" +
					"      Filter\n" +
					"      ╠══ Var (name=hasX)\n" +
					"      ╚══ Extension\n" +
					"         ├── SingletonSet\n" +
					"         └── ExtensionElem (hasX)\n" +
					"               Exists\n" +
					"                  StatementPattern (resultSizeEstimate=0)\n" +
					"                     s: Var (name=s)\n" +
					"                     p: Var (name=_const_471beca6_uri, value=http://example.com/p2, anonymous)\n" +
					"                     o: Var (name=x)\n");
		}
		sailRepository.shutDown();
	}

	@Test
	public void multipleScopesAndFilters() throws Exception {
		String sparql = "PREFIX : <http://example.com/>\n" +
				"\n" +
				"SELECT ?s ?o ?score ?lvl WHERE {\n" +
				"\n" +
				"  VALUES ?s { :A :B :C }\n" +
				"\n" +
				"  ?s :prop ?o .\n" +
				"\n" +
				"  FILTER (BOUND(?s))                                    # F‑0‑1\n" +
				"  FILTER (?o         != :Forbidden)                     # F‑0‑2\n" +
				"  {FILTER (LCASE(STR(?o)) != \"bad\")                      # F‑0‑3\n" +
				"  FILTER (NOT EXISTS { ?s :deprecated true }) }         # F‑0‑4\n" +
				"\n" +
				"\n" +

				"  {\n" +
				"    ?s :score ?score .\n" +
				"\n" +
				"    FILTER (?score  > 10)                               # F‑1‑1\n" +
				"    {FILTER (?score  < 100)}                              # F‑1‑2\n" +
				"    FILTER ((?score - 2) != 0)                           # F‑1‑3\n" +
				"\n" +
				"\n" +

				"    OPTIONAL {\n" +
				"      ?s         :reviewedBy ?reviewer .\n" +
				"      ?reviewer  :level      ?lvl     .\n" +
				"\n" +
				"      FILTER (?lvl  >= 3)                               # F‑2‑1\n" +
				"      FILTER (?lvl  <= 8)                               # F‑2‑2\n" +
				"      FILTER (REGEX(STR(?reviewer),\n" +
				"                    \"^http://example\\\\.com/user\"))      # F‑2‑3\n" +
				"    }\n" +
				"  }\n" +
				"}";

		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(sparql);
			String actual = query.explain(Explanation.Level.Optimized).toString();
			assertThat(actual).isEqualToNormalizingNewlines("Projection\n" +
					"╠══ ProjectionElemList\n" +
					"║     ProjectionElem \"s\"\n" +
					"║     ProjectionElem \"o\"\n" +
					"║     ProjectionElem \"score\"\n" +
					"║     ProjectionElem \"lvl\"\n" +
					"╚══ Join (JoinIterator)\n" +
					"   ├── Filter [left]\n" +
					"   │  ╠══ Bound\n" +
					"   │  ║     Var (name=s)\n" +
					"   │  ╚══ BindingSetAssignment ([[s=http://example.com/A], [s=http://example.com/B], [s=http://example.com/C]]) (costEstimate=0, resultSizeEstimate=1.00)\n"
					+
					"   └── Join (JoinIterator) [right]\n" +
					"      ╠══ Filter (new scope) [left]\n" +
					"      ║  ├── And\n" +
					"      ║  │  ╠══ Compare (!=)\n" +
					"      ║  │  ║  ├── FunctionCall (http://www.w3.org/2005/xpath-functions#lower-case)\n" +
					"      ║  │  ║  │     Str\n" +
					"      ║  │  ║  │        Var (name=o)\n" +
					"      ║  │  ║  └── ValueConstant (value=\"bad\")\n" +
					"      ║  │  ╚══ Not\n" +
					"      ║  │        Exists\n" +
					"      ║  │           StatementPattern (resultSizeEstimate=0)\n" +
					"      ║  │              s: Var (name=s)\n" +
					"      ║  │              p: Var (name=_const_52097_uri, value=http://example.com/deprecated, anonymous)\n"
					+
					"      ║  │              o: Var (name=_const_36758e_lit_eeeee601, value=\"true\"^^<http://www.w3.org/2001/XMLSchema#boolean>, anonymous)\n"
					+
					"      ║  └── SingletonSet\n" +
					"      ╚══ Join (JoinIterator) [right]\n" +
					"         ├── Filter [left]\n" +
					"         │  ╠══ Compare (!=)\n" +
					"         │  ║     Var (name=o)\n" +
					"         │  ║     ValueConstant (value=http://example.com/Forbidden)\n" +
					"         │  ╚══ StatementPattern (costEstimate=2.24, resultSizeEstimate=0)\n" +
					"         │        s: Var (name=s)\n" +
					"         │        p: Var (name=_const_efd45947_uri, value=http://example.com/prop, anonymous)\n" +
					"         │        o: Var (name=o)\n" +
					"         └── LeftJoin [right]\n" +
					"            ╠══ Join (HashJoinIteration) [left]\n" +
					"            ║  ├── Filter (new scope) [left]\n" +
					"            ║  │  ╠══ And\n" +
					"            ║  │  ║  ├── Compare (>)\n" +
					"            ║  │  ║  │     Var (name=score)\n" +
					"            ║  │  ║  │     ValueConstant (value=\"10\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n"
					+
					"            ║  │  ║  └── Compare (!=)\n" +
					"            ║  │  ║     ╠══ MathExpr (-)\n" +
					"            ║  │  ║     ║     Var (name=score)\n" +
					"            ║  │  ║     ║     ValueConstant (value=\"2\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n"
					+
					"            ║  │  ║     ╚══ ValueConstant (value=\"0\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n"
					+
					"            ║  │  ╚══ StatementPattern (costEstimate=2.24, resultSizeEstimate=0)\n" +
					"            ║  │        s: Var (name=s)\n" +
					"            ║  │        p: Var (name=_const_ada452e_uri, value=http://example.com/score, anonymous)\n"
					+
					"            ║  │        o: Var (name=score)\n" +
					"            ║  └── Filter (new scope) (costEstimate=6.00, resultSizeEstimate=1.00) [right]\n" +
					"            ║     ╠══ Compare (<)\n" +
					"            ║     ║     Var (name=score)\n" +
					"            ║     ║     ValueConstant (value=\"100\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n"
					+
					"            ║     ╚══ SingletonSet\n" +
					"            ╚══ Join (JoinIterator) [right]\n" +
					"               ├── Filter [left]\n" +
					"               │  ╠══ Regex\n" +
					"               │  ║  ├── Str\n" +
					"               │  ║  │     Var (name=reviewer)\n" +
					"               │  ║  └── ValueConstant (value=\"^http://example\\.com/user\")\n" +
					"               │  ╚══ StatementPattern (costEstimate=1.12, resultSizeEstimate=0)\n" +
					"               │        s: Var (name=s)\n" +
					"               │        p: Var (name=_const_f053af92_uri, value=http://example.com/reviewedBy, anonymous)\n"
					+
					"               │        o: Var (name=reviewer)\n" +
					"               └── Filter [right]\n" +
					"                  ╠══ And\n" +
					"                  ║  ├── Compare (>=)\n" +
					"                  ║  │     Var (name=lvl)\n" +
					"                  ║  │     ValueConstant (value=\"3\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n"
					+
					"                  ║  └── Compare (<=)\n" +
					"                  ║        Var (name=lvl)\n" +
					"                  ║        ValueConstant (value=\"8\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n"
					+
					"                  ╚══ StatementPattern (costEstimate=2.24, resultSizeEstimate=0)\n" +
					"                        s: Var (name=reviewer)\n" +
					"                        p: Var (name=_const_a78a220_uri, value=http://example.com/level, anonymous)\n"
					+
					"                        o: Var (name=lvl)\n");
		}
		sailRepository.shutDown();
	}

	@Test
	public void multipleScopesAndFilters2() throws Exception {
		String sparql = "PREFIX : <http://example/>\n" +
				"\n" +
				"SELECT ?s ?o WHERE {\n" +
				"  VALUES ?s { :S }                                 # scope‑0  (outermost)\n" +
				"\n" +
				"  {                                                # scope‑1  (new group graph pattern)\n" +
				"    {          " +
				"      FILTER (?o != ?s)          # ▸ Filter‑B  (scope‑1)\n" +

				"      FILTER (NOT EXISTS {?o ?s ?c})          # ▸ Filter‑B  (scope‑1)\n" +
				"      BIND(?s as ?o)\n" +
//				"		FILTER (?o = :O)                         # ▸ Filter‑A  (scope‑2)\n" +

				"    }\n" +
				"\n" +

				"  }\n" +
				"}";
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(sparql);
			String actual = query.explain(Explanation.Level.Unoptimized).toString();
			assertThat(actual).isEqualToNormalizingNewlines("Projection\n" +
					"╠══ ProjectionElemList\n" +
					"║     ProjectionElem \"s\"\n" +
					"║     ProjectionElem \"o\"\n" +
					"╚══ Join\n" +
					"   ├── BindingSetAssignment ([[s=http://example/S]]) [left]\n" +
					"   └── Filter (new scope) [right]\n" +
					"      ╠══ Not\n" +
					"      ║     Exists\n" +
					"      ║        StatementPattern\n" +
					"      ║           s: Var (name=o)\n" +
					"      ║           p: Var (name=s)\n" +
					"      ║           o: Var (name=c)\n" +
					"      ╚══ Filter\n" +
					"         ├── Compare (!=)\n" +
					"         │     Var (name=o)\n" +
					"         │     Var (name=s)\n" +
					"         └── Extension\n" +
					"            ╠══ SingletonSet\n" +
					"            ╚══ ExtensionElem (o)\n" +
					"                  Var (name=s)\n");
		}

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(sparql);
			String actual = query.explain(Explanation.Level.Optimized).toString();
			assertThat(actual).isEqualToNormalizingNewlines("Projection\n" +
					"╠══ ProjectionElemList\n" +
					"║     ProjectionElem \"s\"\n" +
					"║     ProjectionElem \"o\"\n" +
					"╚══ Join (JoinIterator)\n" +
					"   ├── Filter (new scope) [left]\n" +
					"   │  ╠══ And\n" +
					"   │  ║  ├── Compare (!=)\n" +
					"   │  ║  │     Var (name=o)\n" +
					"   │  ║  │     Var (name=s)\n" +
					"   │  ║  └── Not\n" +
					"   │  ║        Exists\n" +
					"   │  ║           StatementPattern (resultSizeEstimate=12)\n" +
					"   │  ║              s: Var (name=o)\n" +
					"   │  ║              p: Var (name=s)\n" +
					"   │  ║              o: Var (name=c)\n" +
					"   │  ╚══ Extension\n" +
					"   │     ├── SingletonSet\n" +
					"   │     └── ExtensionElem (o)\n" +
					"   │           Var (name=s)\n" +
					"   └── BindingSetAssignment ([[s=http://example/S]]) (costEstimate=6.00, resultSizeEstimate=1.00) [right]\n");
		}
		sailRepository.shutDown();
	}

	@Test
	public void testTupleQuery() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(TUPLE_QUERY);

			String actual = query.explain(Explanation.Level.Unoptimized).toString();
			String expected = "Projection\n" +
					"╠══ ProjectionElemList\n" +
					"║     ProjectionElem \"a\"\n" +
					"╚══ Filter\n" +
					"   ├── And\n" +
					"   │  ╠══ Compare (!=)\n" +
					"   │  ║     Var (name=c)\n" +
					"   │  ║     Var (name=d)\n" +
					"   │  ╚══ Compare (!=)\n" +
					"   │        Var (name=c)\n" +
					"   │        ValueConstant (value=\"<\")\n" +
					"   └── LeftJoin\n" +
					"      ╠══ Join [left]\n" +
					"      ║  ├── LeftJoin (new scope) [left]\n" +
					"      ║  │  ╠══ SingletonSet [left]\n" +
					"      ║  │  ╚══ StatementPattern [right]\n" +
					"      ║  │        s: Var (name=d)\n" +
					"      ║  │        p: Var (name=e)\n" +
					"      ║  │        o: Var (name=f)\n" +
					"      ║  └── Join [right]\n" +
					"      ║     ╠══ StatementPattern [left]\n" +
					"      ║     ║     s: Var (name=a)\n" +
					"      ║     ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"      ║     ║     o: Var (name=c)\n" +
					"      ║     ╚══ StatementPattern [right]\n" +
					"      ║           s: Var (name=a)\n" +
					"      ║           p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"      ║           o: Var (name=d)\n" +
					"      ╚══ StatementPattern [right]\n" +
					"            s: Var (name=d)\n" +
					"            p: Var (name=e)\n" +
					"            o: Var (name=f)\n";

			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();
	}

	@Test
	public void testTupleQueryOptimized() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			TupleQuery query = connection.prepareTupleQuery(TUPLE_QUERY);
			String actual = query.explain(Explanation.Level.Optimized).toString();
			String expected = "Projection\n" +
					"╠══ ProjectionElemList\n" +
					"║     ProjectionElem \"a\"\n" +
					"╚══ LeftJoin (LeftJoinIterator)\n" +
					"   ├── Join (JoinIterator) [left]\n" +
					"   │  ╠══ StatementPattern (costEstimate=3.00, resultSizeEstimate=4.00) [left]\n" +
					"   │  ║     s: Var (name=a)\n" +
					"   │  ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"   │  ║     o: Var (name=d)\n" +
					"   │  ╚══ Filter [right]\n" +
					"   │     ├── Compare (!=)\n" +
					"   │     │     Var (name=c)\n" +
					"   │     │     Var (name=d)\n" +
					"   │     └── Join (HashJoinIteration)\n" +
					"   │        ╠══ Filter [left]\n" +
					"   │        ║  ├── Compare (!=)\n" +
					"   │        ║  │     Var (name=c)\n" +
					"   │        ║  │     ValueConstant (value=\"<\")\n" +
					"   │        ║  └── StatementPattern (costEstimate=3.00, resultSizeEstimate=4.00)\n" +
					"   │        ║        s: Var (name=a)\n" +
					"   │        ║        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"   │        ║        o: Var (name=c)\n" +
					"   │        ╚══ LeftJoin (new scope) (costEstimate=6.61, resultSizeEstimate=12) [right]\n" +
					"   │           ├── SingletonSet [left]\n" +
					"   │           └── StatementPattern (resultSizeEstimate=12) [right]\n" +
					"   │                 s: Var (name=d)\n" +
					"   │                 p: Var (name=e)\n" +
					"   │                 o: Var (name=f)\n" +
					"   └── StatementPattern (resultSizeEstimate=12) [right]\n" +
					"         s: Var (name=d)\n" +
					"         p: Var (name=e)\n" +
					"         o: Var (name=f)\n";
			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

	}

	@Test
	@Disabled
	public void testTupleQueryTimed() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			TupleQuery query = connection.prepareTupleQuery(TUPLE_QUERY);

			GenericPlanNode genericPlanNode = query.explain(Explanation.Level.Timed).toGenericPlanNode();

			GenericPlanNode leftJoin = genericPlanNode.getPlans().get(1);
			GenericPlanNode filterNode = genericPlanNode.getPlans().get(1).getPlans().get(0).getPlans().get(1);

			assertEquals("LeftJoin", leftJoin.getType());
			assertEquals("Filter", filterNode.getType());

			assertTrue(filterNode.getSelfTimeActual() > leftJoin.getSelfTimeActual());
			assertTrue(filterNode.getSelfTimeActual() < leftJoin.getTotalTimeActual());

			assertThat(genericPlanNode.toString()).contains("selfTimeActual");
			assertThat(genericPlanNode.toString()).contains("totalTimeActual");

		}
		sailRepository.shutDown();

	}

	@Test
	public void testTupleQueryExecuted() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(TUPLE_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toString();
			String expected = "Projection (resultSizeActual=2)\n" +
					"╠══ ProjectionElemList\n" +
					"║     ProjectionElem \"a\"\n" +
					"╚══ LeftJoin (LeftJoinIterator) (resultSizeActual=2)\n" +
					"   ├── Join (JoinIterator) (resultSizeActual=2) [left]\n" +
					"   │  ╠══ StatementPattern (costEstimate=3.00, resultSizeEstimate=4.00, resultSizeActual=4) [left]\n"
					+
					"   │  ║     s: Var (name=a)\n" +
					"   │  ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"   │  ║     o: Var (name=d)\n" +
					"   │  ╚══ Filter (resultSizeActual=2) [right]\n" +
					"   │     ├── Compare (!=)\n" +
					"   │     │     Var (name=c)\n" +
					"   │     │     Var (name=d)\n" +
					"   │     └── Join (HashJoinIteration) (resultSizeActual=6)\n" +
					"   │        ╠══ Filter (resultSizeActual=6) [left]\n" +
					"   │        ║  ├── Compare (!=)\n" +
					"   │        ║  │     Var (name=c)\n" +
					"   │        ║  │     ValueConstant (value=\"<\")\n" +
					"   │        ║  └── StatementPattern (costEstimate=3.00, resultSizeEstimate=4.00, resultSizeActual=6)\n"
					+
					"   │        ║        s: Var (name=a)\n" +
					"   │        ║        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"   │        ║        o: Var (name=c)\n" +
					"   │        ╚══ LeftJoin (new scope) (BadlyDesignedLeftJoinIterator) (costEstimate=6.61, resultSizeEstimate=12, resultSizeActual=4) [right]\n"
					+
					"   │           ├── SingletonSet (resultSizeActual=4) [left]\n" +
					"   │           └── StatementPattern (resultSizeEstimate=12, resultSizeActual=48) [right]\n" +
					"   │                 s: Var (name=d)\n" +
					"   │                 p: Var (name=e)\n" +
					"   │                 o: Var (name=f)\n" +
					"   └── StatementPattern (resultSizeEstimate=12, resultSizeActual=2) [right]\n" +
					"         s: Var (name=d)\n" +
					"         p: Var (name=e)\n" +
					"         o: Var (name=f)\n";
			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

	}

	@Test
	public void testGenericPlanNode() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(TUPLE_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toGenericPlanNode().toString();
			String expected = "Projection (resultSizeActual=2)\n" +
					"╠══ ProjectionElemList\n" +
					"║     ProjectionElem \"a\"\n" +
					"╚══ LeftJoin (LeftJoinIterator) (resultSizeActual=2)\n" +
					"   ├── Join (JoinIterator) (resultSizeActual=2) [left]\n" +
					"   │  ╠══ StatementPattern (costEstimate=3.00, resultSizeEstimate=4.00, resultSizeActual=4) [left]\n"
					+
					"   │  ║     s: Var (name=a)\n" +
					"   │  ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"   │  ║     o: Var (name=d)\n" +
					"   │  ╚══ Filter (resultSizeActual=2) [right]\n" +
					"   │     ├── Compare (!=)\n" +
					"   │     │     Var (name=c)\n" +
					"   │     │     Var (name=d)\n" +
					"   │     └── Join (HashJoinIteration) (resultSizeActual=6)\n" +
					"   │        ╠══ Filter (resultSizeActual=6) [left]\n" +
					"   │        ║  ├── Compare (!=)\n" +
					"   │        ║  │     Var (name=c)\n" +
					"   │        ║  │     ValueConstant (value=\"<\")\n" +
					"   │        ║  └── StatementPattern (costEstimate=3.00, resultSizeEstimate=4.00, resultSizeActual=6)\n"
					+
					"   │        ║        s: Var (name=a)\n" +
					"   │        ║        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"   │        ║        o: Var (name=c)\n" +
					"   │        ╚══ LeftJoin (new scope) (BadlyDesignedLeftJoinIterator) (costEstimate=6.61, resultSizeEstimate=12, resultSizeActual=4) [right]\n"
					+
					"   │           ├── SingletonSet (resultSizeActual=4) [left]\n" +
					"   │           └── StatementPattern (resultSizeEstimate=12, resultSizeActual=48) [right]\n" +
					"   │                 s: Var (name=d)\n" +
					"   │                 p: Var (name=e)\n" +
					"   │                 o: Var (name=f)\n" +
					"   └── StatementPattern (resultSizeEstimate=12, resultSizeActual=2) [right]\n" +
					"         s: Var (name=d)\n" +
					"         p: Var (name=e)\n" +
					"         o: Var (name=f)\n";
			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

	}

	@Test
	public void testJsonPlanNode() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(TUPLE_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toJson();
			String expected = "{\n" +
					"  \"type\" : \"Projection\",\n" +
					"  \"resultSizeActual\" : 2,\n" +
					"  \"plans\" : [ {\n" +
					"    \"type\" : \"ProjectionElemList\",\n" +
					"    \"plans\" : [ {\n" +
					"      \"type\" : \"ProjectionElem \\\"a\\\"\"\n" +
					"    } ]\n" +
					"  }, {\n" +
					"    \"type\" : \"LeftJoin\",\n" +
					"    \"resultSizeActual\" : 2,\n" +
					"    \"algorithm\" : \"LeftJoinIterator\",\n" +
					"    \"plans\" : [ {\n" +
					"      \"type\" : \"Join\",\n" +
					"      \"resultSizeActual\" : 2,\n" +
					"      \"algorithm\" : \"JoinIterator\",\n" +
					"      \"plans\" : [ {\n" +
					"        \"type\" : \"StatementPattern\",\n" +
					"        \"costEstimate\" : 3.0,\n" +
					"        \"resultSizeEstimate\" : 4.0,\n" +
					"        \"resultSizeActual\" : 4,\n" +
					"        \"plans\" : [ {\n" +
					"          \"type\" : \"Var (name=a)\"\n" +
					"        }, {\n" +
					"          \"type\" : \"Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\"\n"
					+
					"        }, {\n" +
					"          \"type\" : \"Var (name=d)\"\n" +
					"        } ]\n" +
					"      }, {\n" +
					"        \"type\" : \"Filter\",\n" +
					"        \"resultSizeActual\" : 2,\n" +
					"        \"plans\" : [ {\n" +
					"          \"type\" : \"Compare (!=)\",\n" +
					"          \"plans\" : [ {\n" +
					"            \"type\" : \"Var (name=c)\"\n" +
					"          }, {\n" +
					"            \"type\" : \"Var (name=d)\"\n" +
					"          } ]\n" +
					"        }, {\n" +
					"          \"type\" : \"Join\",\n" +
					"          \"resultSizeActual\" : 6,\n" +
					"          \"algorithm\" : \"HashJoinIteration\",\n" +
					"          \"plans\" : [ {\n" +
					"            \"type\" : \"Filter\",\n" +
					"            \"resultSizeActual\" : 6,\n" +
					"            \"plans\" : [ {\n" +
					"              \"type\" : \"Compare (!=)\",\n" +
					"              \"plans\" : [ {\n" +
					"                \"type\" : \"Var (name=c)\"\n" +
					"              }, {\n" +
					"                \"type\" : \"ValueConstant (value=\\\"<\\\")\"\n" +
					"              } ]\n" +
					"            }, {\n" +
					"              \"type\" : \"StatementPattern\",\n" +
					"              \"costEstimate\" : 3.0,\n" +
					"              \"resultSizeEstimate\" : 4.0,\n" +
					"              \"resultSizeActual\" : 6,\n" +
					"              \"plans\" : [ {\n" +
					"                \"type\" : \"Var (name=a)\"\n" +
					"              }, {\n" +
					"                \"type\" : \"Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\"\n"
					+
					"              }, {\n" +
					"                \"type\" : \"Var (name=c)\"\n" +
					"              } ]\n" +
					"            } ]\n" +
					"          }, {\n" +
					"            \"type\" : \"LeftJoin\",\n" +
					"            \"costEstimate\" : 6.611489018457944,\n" +
					"            \"resultSizeEstimate\" : 12.0,\n" +
					"            \"resultSizeActual\" : 4,\n" +
					"            \"newScope\" : true,\n" +
					"            \"algorithm\" : \"BadlyDesignedLeftJoinIterator\",\n" +
					"            \"plans\" : [ {\n" +
					"              \"type\" : \"SingletonSet\",\n" +
					"              \"resultSizeActual\" : 4\n" +
					"            }, {\n" +
					"              \"type\" : \"StatementPattern\",\n" +
					"              \"resultSizeEstimate\" : 12.0,\n" +
					"              \"resultSizeActual\" : 48,\n" +
					"              \"plans\" : [ {\n" +
					"                \"type\" : \"Var (name=d)\"\n" +
					"              }, {\n" +
					"                \"type\" : \"Var (name=e)\"\n" +
					"              }, {\n" +
					"                \"type\" : \"Var (name=f)\"\n" +
					"              } ]\n" +
					"            } ]\n" +
					"          } ]\n" +
					"        } ]\n" +
					"      } ]\n" +
					"    }, {\n" +
					"      \"type\" : \"StatementPattern\",\n" +
					"      \"resultSizeEstimate\" : 12.0,\n" +
					"      \"resultSizeActual\" : 2,\n" +
					"      \"plans\" : [ {\n" +
					"        \"type\" : \"Var (name=d)\"\n" +
					"      }, {\n" +
					"        \"type\" : \"Var (name=e)\"\n" +
					"      }, {\n" +
					"        \"type\" : \"Var (name=f)\"\n" +
					"      } ]\n" +
					"    } ]\n" +
					"  } ]\n" +
					"}";
			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

	}

	@Test
	public void testAskQuery() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareBooleanQuery(ASK_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toString();
			String expected = "Slice (limit=1) (resultSizeActual=1)\n" +
					"   LeftJoin (LeftJoinIterator) (resultSizeActual=1)\n" +
					"   ├── Join (JoinIterator) (resultSizeActual=1) [left]\n" +
					"   │  ╠══ StatementPattern (costEstimate=3.00, resultSizeEstimate=4.00, resultSizeActual=3) [left]\n"
					+
					"   │  ║     s: Var (name=a)\n" +
					"   │  ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"   │  ║     o: Var (name=d)\n" +
					"   │  ╚══ Filter (resultSizeActual=1) [right]\n" +
					"   │     ├── Compare (!=)\n" +
					"   │     │     Var (name=c)\n" +
					"   │     │     Var (name=d)\n" +
					"   │     └── Join (HashJoinIteration) (resultSizeActual=4)\n" +
					"   │        ╠══ Filter (resultSizeActual=4) [left]\n" +
					"   │        ║  ├── Compare (!=)\n" +
					"   │        ║  │     Var (name=c)\n" +
					"   │        ║  │     ValueConstant (value=\"<\")\n" +
					"   │        ║  └── StatementPattern (costEstimate=3.00, resultSizeEstimate=4.00, resultSizeActual=4)\n"
					+
					"   │        ║        s: Var (name=a)\n" +
					"   │        ║        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"   │        ║        o: Var (name=c)\n" +
					"   │        ╚══ LeftJoin (new scope) (BadlyDesignedLeftJoinIterator) (costEstimate=6.61, resultSizeEstimate=12, resultSizeActual=3) [right]\n"
					+
					"   │           ├── SingletonSet (resultSizeActual=3) [left]\n" +
					"   │           └── StatementPattern (resultSizeEstimate=12, resultSizeActual=36) [right]\n" +
					"   │                 s: Var (name=d)\n" +
					"   │                 p: Var (name=e)\n" +
					"   │                 o: Var (name=f)\n" +
					"   └── StatementPattern (resultSizeEstimate=12, resultSizeActual=1) [right]\n" +
					"         s: Var (name=d)\n" +
					"         p: Var (name=e)\n" +
					"         o: Var (name=f)\n";
			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

	}

	@Test
	public void testConstructQuery() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareGraphQuery(CONSTRUCT_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toString();

			String expected = "Reduced (resultSizeActual=3)\n" +
					"   MultiProjection (resultSizeActual=4)\n" +
					"      ProjectionElemList\n" +
					"         ProjectionElem \"a\" AS \"subject\"\n" +
					"         ProjectionElem \"_const_f5e5585a_uri\" AS \"predicate\"\n" +
					"         ProjectionElem \"c\" AS \"object\"\n" +
					"      ProjectionElemList\n" +
					"         ProjectionElem \"a\" AS \"subject\"\n" +
					"         ProjectionElem \"_const_f5e5585a_uri\" AS \"predicate\"\n" +
					"         ProjectionElem \"d\" AS \"object\"\n" +
					"      Extension (resultSizeActual=2)\n" +
					"      ╠══ LeftJoin (LeftJoinIterator) (resultSizeActual=2)\n" +
					"      ║  ├── Join (JoinIterator) (resultSizeActual=2) [left]\n" +
					"      ║  │  ╠══ StatementPattern (costEstimate=3.00, resultSizeEstimate=4.00, resultSizeActual=4) [left]\n"
					+
					"      ║  │  ║     s: Var (name=a)\n" +
					"      ║  │  ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"      ║  │  ║     o: Var (name=d)\n" +
					"      ║  │  ╚══ Filter (resultSizeActual=2) [right]\n" +
					"      ║  │     ├── Compare (!=)\n" +
					"      ║  │     │     Var (name=c)\n" +
					"      ║  │     │     Var (name=d)\n" +
					"      ║  │     └── Join (HashJoinIteration) (resultSizeActual=6)\n" +
					"      ║  │        ╠══ Filter (resultSizeActual=6) [left]\n" +
					"      ║  │        ║  ├── Compare (!=)\n" +
					"      ║  │        ║  │     Var (name=c)\n" +
					"      ║  │        ║  │     ValueConstant (value=\"<\")\n" +
					"      ║  │        ║  └── StatementPattern (costEstimate=3.00, resultSizeEstimate=4.00, resultSizeActual=6)\n"
					+
					"      ║  │        ║        s: Var (name=a)\n" +
					"      ║  │        ║        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"      ║  │        ║        o: Var (name=c)\n" +
					"      ║  │        ╚══ LeftJoin (new scope) (BadlyDesignedLeftJoinIterator) (costEstimate=6.61, resultSizeEstimate=12, resultSizeActual=4) [right]\n"
					+
					"      ║  │           ├── SingletonSet (resultSizeActual=4) [left]\n" +
					"      ║  │           └── StatementPattern (resultSizeEstimate=12, resultSizeActual=48) [right]\n" +
					"      ║  │                 s: Var (name=d)\n" +
					"      ║  │                 p: Var (name=e)\n" +
					"      ║  │                 o: Var (name=f)\n" +
					"      ║  └── StatementPattern (resultSizeEstimate=12, resultSizeActual=2) [right]\n" +
					"      ║        s: Var (name=d)\n" +
					"      ║        p: Var (name=e)\n" +
					"      ║        o: Var (name=f)\n" +
					"      ╚══ ExtensionElem (_const_f5e5585a_uri)\n" +
					"            ValueConstant (value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type)\n";

			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

	}

	@Disabled // slow test used for debugging
	@Test
	public void bigDataset() throws IOException {
		SailRepository repository = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(QueryPlanRetrievalTest.class.getClassLoader()
					.getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl"), "", RDFFormat.TURTLE);
			connection.commit();
		}

		String query1 = IOUtils.toString(
				QueryPlanRetrievalTest.class.getClassLoader().getResourceAsStream("benchmarkFiles/query1.qr"),
				StandardCharsets.UTF_8);

		try (SailRepositoryConnection connection = repository.getConnection()) {
			String s = connection.prepareTupleQuery(query1).explain(Explanation.Level.Timed).toString();
		}

		repository.shutDown();

	}

	@Test
	public void testSubQuery() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(SUB_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toString();
			String expected = "Projection (resultSizeActual=4)\n" +
					"╠══ ProjectionElemList\n" +
					"║     ProjectionElem \"a\"\n" +
					"╚══ Join (HashJoinIteration) (resultSizeActual=4)\n" +
					"   ├── Projection (new scope) (resultSizeActual=4) [left]\n" +
					"   │  ╠══ ProjectionElemList\n" +
					"   │  ║     ProjectionElem \"a\"\n" +
					"   │  ╚══ StatementPattern (resultSizeEstimate=4.00, resultSizeActual=4)\n" +
					"   │        s: Var (name=a)\n" +
					"   │        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"   │        o: Var (name=type)\n" +
					"   └── Projection (new scope) (resultSizeActual=2) [right]\n" +
					"      ╠══ ProjectionElemList\n" +
					"      ║     ProjectionElem \"a\"\n" +
					"      ╚══ LeftJoin (LeftJoinIterator) (resultSizeActual=2)\n" +
					"         ├── Join (JoinIterator) (resultSizeActual=2) [left]\n" +
					"         │  ╠══ StatementPattern (costEstimate=3.00, resultSizeEstimate=4.00, resultSizeActual=4) [left]\n"
					+
					"         │  ║     s: Var (name=a)\n" +
					"         │  ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"         │  ║     o: Var (name=d)\n" +
					"         │  ╚══ Filter (resultSizeActual=2) [right]\n" +
					"         │     ├── Compare (!=)\n" +
					"         │     │     Var (name=c)\n" +
					"         │     │     Var (name=d)\n" +
					"         │     └── Join (HashJoinIteration) (resultSizeActual=6)\n" +
					"         │        ╠══ Filter (resultSizeActual=6) [left]\n" +
					"         │        ║  ├── Compare (!=)\n" +
					"         │        ║  │     Var (name=c)\n" +
					"         │        ║  │     ValueConstant (value=\"<\")\n" +
					"         │        ║  └── StatementPattern (costEstimate=3.00, resultSizeEstimate=4.00, resultSizeActual=6)\n"
					+
					"         │        ║        s: Var (name=a)\n" +
					"         │        ║        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"         │        ║        o: Var (name=c)\n" +
					"         │        ╚══ LeftJoin (new scope) (BadlyDesignedLeftJoinIterator) (costEstimate=6.61, resultSizeEstimate=12, resultSizeActual=4) [right]\n"
					+
					"         │           ├── SingletonSet (resultSizeActual=4) [left]\n" +
					"         │           └── StatementPattern (resultSizeEstimate=12, resultSizeActual=48) [right]\n" +
					"         │                 s: Var (name=d)\n" +
					"         │                 p: Var (name=e)\n" +
					"         │                 o: Var (name=f)\n" +
					"         └── StatementPattern (resultSizeEstimate=12, resultSizeActual=2) [right]\n" +
					"               s: Var (name=d)\n" +
					"               p: Var (name=e)\n" +
					"               o: Var (name=f)\n";

			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}

		sailRepository.shutDown();

	}

	@Test
	public void testSubQuery2() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(SUB_QUERY2);

			String actual = query.explain(Explanation.Level.Optimized).toString();
			String expected = "Distinct\n" +
					"   Projection\n" +
					"   ├── ProjectionElemList\n" +
					"   │     ProjectionElem \"countryID\"\n" +
					"   │     ProjectionElem \"year\"\n" +
					"   │     ProjectionElem \"amountLots\"\n" +
					"   │     ProjectionElem \"numSingleBidders\"\n" +
					"   └── Extension\n" +
					"         Group (countryID, year)\n" +
					"            Join (HashJoinIteration)\n" +
					"            ╠══ Extension [left]\n" +
					"            ║  ├── Join (JoinIterator)\n" +
					"            ║  │  ╠══ StatementPattern (costEstimate=0.71, resultSizeEstimate=0) [left]\n" +
					"            ║  │  ║     s: Var (name=resultnotice)\n" +
					"            ║  │  ║     p: Var (name=_const_183bd06d_uri, value=http://data.europa.eu/a4g/ontology#refersToProcedure, anonymous)\n"
					+
					"            ║  │  ║     o: Var (name=proc)\n" +
					"            ║  │  ╚══ Join (JoinIterator) [right]\n" +
					"            ║  │     ├── StatementPattern (costEstimate=1.00, resultSizeEstimate=0) [left]\n" +
					"            ║  │     │     s: Var (name=proc)\n" +
					"            ║  │     │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"            ║  │     │     o: Var (name=_const_be18ee7b_uri, value=http://data.europa.eu/a4g/ontology#Procedure, anonymous)\n"
					+
					"            ║  │     └── Join (JoinIterator) [right]\n" +
					"            ║  │        ╠══ StatementPattern (costEstimate=1.00, resultSizeEstimate=0) [left]\n" +
					"            ║  │        ║     s: Var (name=resultnotice)\n" +
					"            ║  │        ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"            ║  │        ║     o: Var (name=_const_77e914ad_uri, value=http://data.europa.eu/a4g/ontology#ResultNotice, anonymous)\n"
					+
					"            ║  │        ╚══ Join (JoinIterator) [right]\n" +
					"            ║  │           ├── StatementPattern (costEstimate=1.12, resultSizeEstimate=0) [left]\n"
					+
					"            ║  │           │     s: Var (name=proc)\n" +
					"            ║  │           │     p: Var (name=_const_9c3f1eec_uri, value=http://data.europa.eu/a4g/ontology#hasProcurementScopeDividedIntoLot, anonymous)\n"
					+
					"            ║  │           │     o: Var (name=lot)\n" +
					"            ║  │           └── Join (JoinIterator) [right]\n" +
					"            ║  │              ╠══ StatementPattern (costEstimate=0.75, resultSizeEstimate=0) [left]\n"
					+
					"            ║  │              ║     s: Var (name=stat)\n" +
					"            ║  │              ║     p: Var (name=_const_25686184_uri, value=http://data.europa.eu/a4g/ontology#concernsSubmissionsForLot, anonymous)\n"
					+
					"            ║  │              ║     o: Var (name=lot)\n" +
					"            ║  │              ╚══ Join (JoinIterator) [right]\n" +
					"            ║  │                 ├── StatementPattern (costEstimate=1.00, resultSizeEstimate=0) [left]\n"
					+
					"            ║  │                 │     s: Var (name=stat)\n" +
					"            ║  │                 │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"            ║  │                 │     o: Var (name=_const_ea79e75_uri, value=http://data.europa.eu/a4g/ontology#SubmissionStatisticalInformation, anonymous)\n"
					+
					"            ║  │                 └── Join (JoinIterator) [right]\n" +
					"            ║  │                    ╠══ Filter [left]\n" +
					"            ║  │                    ║  ├── Compare (!=)\n" +
					"            ║  │                    ║  │     Var (name=p)\n" +
					"            ║  │                    ║  │     ValueConstant (value=http://publications.europa.eu/resource/authority/procurement-procedure-type/neg-wo-call)\n"
					+
					"            ║  │                    ║  └── StatementPattern (costEstimate=2.24, resultSizeEstimate=0)\n"
					+
					"            ║  │                    ║        s: Var (name=proc)\n" +
					"            ║  │                    ║        p: Var (name=_const_9c756f6b_uri, value=http://data.europa.eu/a4g/ontology#hasProcedureType, anonymous)\n"
					+
					"            ║  │                    ║        o: Var (name=p)\n" +
					"            ║  │                    ╚══ Join (JoinIterator) [right]\n" +
					"            ║  │                       ├── StatementPattern (costEstimate=2.24, resultSizeEstimate=0) [left]\n"
					+
					"            ║  │                       │     s: Var (name=stat)\n" +
					"            ║  │                       │     p: Var (name=_const_98c73a3c_uri, value=http://data.europa.eu/a4g/ontology#hasReceivedTenders, anonymous)\n"
					+
					"            ║  │                       │     o: Var (name=bidders)\n" +
					"            ║  │                       └── Join (JoinIterator) [right]\n" +
					"            ║  │                          ╠══ StatementPattern (costEstimate=2.24, resultSizeEstimate=0) [left]\n"
					+
					"            ║  │                          ║     s: Var (name=resultnotice)\n" +
					"            ║  │                          ║     p: Var (name=_const_1b0b00ca_uri, value=http://data.europa.eu/a4g/ontology#hasDispatchDate, anonymous)\n"
					+
					"            ║  │                          ║     o: Var (name=ddate)\n" +
					"            ║  │                          ╚══ StatementPattern (costEstimate=2.24, resultSizeEstimate=0) [right]\n"
					+
					"            ║  │                                s: Var (name=resultnotice)\n" +
					"            ║  │                                p: Var (name=_const_6aa9a9c_uri, value=http://data.europa.eu/a4g/ontology#refersToRole, anonymous)\n"
					+
					"            ║  │                                o: Var (name=buyerrole)\n" +
					"            ║  └── ExtensionElem (year)\n" +
					"            ║        FunctionCall (http://www.w3.org/2005/xpath-functions#year-from-dateTime)\n" +
					"            ║           FunctionCall (http://www.w3.org/2001/XMLSchema#dateTime)\n" +
					"            ║              Var (name=ddate)\n" +
					"            ╚══ Distinct (new scope) [right]\n" +
					"                  Projection\n" +
					"                  ╠══ ProjectionElemList\n" +
					"                  ║     ProjectionElem \"buyerrole\"\n" +
					"                  ║     ProjectionElem \"countryID\"\n" +
					"                  ╚══ Join (JoinIterator)\n" +
					"                     ├── StatementPattern (costEstimate=1.25, resultSizeEstimate=0) [left]\n" +
					"                     │     s: Var (name=org)\n" +
					"                     │     p: Var (name=_const_beb18915_uri, value=https://www.w3.org/ns/legal#registeredAddress, anonymous)\n"
					+
					"                     │     o: Var (name=orgaddress)\n" +
					"                     └── Join (JoinIterator) [right]\n" +
					"                        ╠══ StatementPattern (costEstimate=1.12, resultSizeEstimate=0) [left]\n" +
					"                        ║     s: Var (name=orgaddress)\n" +
					"                        ║     p: Var (name=_const_2f7de0e1_uri, value=http://data.europa.eu/a4g/ontology#hasCountryCode, anonymous)\n"
					+
					"                        ║     o: Var (name=countrycode)\n" +
					"                        ╚══ Join (JoinIterator) [right]\n" +
					"                           ├── Filter [left]\n" +
					"                           │  ╠══ Compare (!=)\n" +
					"                           │  ║     Var (name=buytype)\n" +
					"                           │  ║     ValueConstant (value=http://publications.europa.eu/resource/authority/buyer-legal-type/eu-int-org)\n"
					+
					"                           │  ╚══ StatementPattern (costEstimate=2.24, resultSizeEstimate=0)\n" +
					"                           │        s: Var (name=org)\n" +
					"                           │        p: Var (name=_const_1abd8d4b_uri, value=http://data.europa.eu/a4g/ontology#hasBuyerType, anonymous)\n"
					+
					"                           │        o: Var (name=buytype)\n" +
					"                           └── Join (JoinIterator) [right]\n" +
					"                              ╠══ StatementPattern (costEstimate=2.24, resultSizeEstimate=0) [left]\n"
					+
					"                              ║     s: Var (name=buyerrole)\n" +
					"                              ║     p: Var (name=_const_beb855c2_uri, value=http://data.europa.eu/a4g/ontology#playedBy, anonymous)\n"
					+
					"                              ║     o: Var (name=org)\n" +
					"                              ╚══ StatementPattern (costEstimate=2.24, resultSizeEstimate=0) [right]\n"
					+
					"                                    s: Var (name=countrycode)\n" +
					"                                    p: Var (name=_const_a825a5f4_uri, value=http://purl.org/dc/elements/1.1/identifier, anonymous)\n"
					+
					"                                    o: Var (name=countryID)\n" +
					"            GroupElem (amountLots)\n" +
					"               Count (Distinct)\n" +
					"                  Var (name=lot)\n" +
					"            GroupElem (numSingleBidders)\n" +
					"               Sum\n" +
					"                  If\n" +
					"                     Compare (=)\n" +
					"                        Var (name=bidders)\n" +
					"                        ValueConstant (value=\"1\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n"
					+
					"                     ValueConstant (value=\"1\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n" +
					"                     ValueConstant (value=\"0\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n" +
					"         ExtensionElem (amountLots)\n" +
					"            Count (Distinct)\n" +
					"               Var (name=lot)\n" +
					"         ExtensionElem (numSingleBidders)\n" +
					"            Sum\n" +
					"               If\n" +
					"                  Compare (=)\n" +
					"                     Var (name=bidders)\n" +
					"                     ValueConstant (value=\"1\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n" +
					"                  ValueConstant (value=\"1\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n" +
					"                  ValueConstant (value=\"0\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n";

			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}

		sailRepository.shutDown();

	}

	@Test
	public void testUnionQuery() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(UNION_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toString();
			String expected = "Projection (resultSizeActual=24)\n" +
					"╠══ ProjectionElemList\n" +
					"║     ProjectionElem \"a\"\n" +
					"╚══ Join (JoinIterator) (resultSizeActual=24)\n" +
					"   ├── StatementPattern (costEstimate=3.00, resultSizeEstimate=4.00, resultSizeActual=4) [left]\n"
					+
					"   │     s: Var (name=a)\n" +
					"   │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"   │     o: Var (name=type)\n" +
					"   └── Union (resultSizeActual=24) [right]\n" +
					"      ╠══ Join (JoinIterator) (resultSizeActual=20)\n" +
					"      ║  ├── StatementPattern (costEstimate=2.20, resultSizeEstimate=12, resultSizeActual=6) [left]\n"
					+
					"      ║  │     s: Var (name=a)\n" +
					"      ║  │     p: Var (name=b)\n" +
					"      ║  │     o: Var (name=c2)\n" +
					"      ║  └── Join (JoinIterator) (resultSizeActual=20) [right]\n" +
					"      ║     ╠══ StatementPattern (costEstimate=2.57, resultSizeEstimate=12, resultSizeActual=10) [left]\n"
					+
					"      ║     ║     s: Var (name=a)\n" +
					"      ║     ║     p: Var (name=b)\n" +
					"      ║     ║     o: Var (name=c)\n" +
					"      ║     ╚══ Union (resultSizeActual=20) [right]\n" +
					"      ║        ├── StatementPattern (new scope) (costEstimate=3.00, resultSizeEstimate=4.00, resultSizeActual=10)\n"
					+
					"      ║        │     s: Var (name=c2)\n" +
					"      ║        │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"      ║        │     o: Var (name=type1)\n" +
					"      ║        └── StatementPattern (new scope) (costEstimate=3.00, resultSizeEstimate=4.00, resultSizeActual=10)\n"
					+
					"      ║              s: Var (name=c2)\n" +
					"      ║              p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"      ║              o: Var (name=type2)\n" +
					"      ╚══ StatementPattern (new scope) (costEstimate=6.61, resultSizeEstimate=12, resultSizeActual=4)\n"
					+
					"            s: Var (name=type)\n" +
					"            p: Var (name=d)\n" +
					"            o: Var (name=c)\n";

			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}

		sailRepository.shutDown();

	}

	@Test
	public void testTimeout() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			for (int i = 0; i < 1000; i++) {
				connection.add(vf.createBNode(i + ""), RDF.TYPE, vf.createBNode((i + 1) + ""));
				connection.add(vf.createBNode(i + ""), RDF.TYPE, vf.createBNode((i - 1) + ""));

				connection.add(vf.createBNode(i + ""), RDF.TYPE, vf.createBNode((i + 2) + ""));
				connection.add(vf.createBNode(i + ""), RDF.TYPE, vf.createBNode((i - 2) + ""));
			}
			connection.commit();
		}

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(String.join("\n", "",
					"select * where {",
					"	?a (a|^a)* ?type.   ",
					"	FILTER NOT EXISTS{?a (a|^a)* ?type} ",
					"	FILTER NOT EXISTS{?a (a|^a)* ?type} ",
					"	FILTER NOT EXISTS{?a (a|^a)* ?type} ",
					"	FILTER NOT EXISTS{?a (a|^a)* ?type}",
					"	FILTER NOT EXISTS{?a (a|^a)* ?type}",
					"	FILTER NOT EXISTS{?a (a|^a)* ?type}",
					"	FILTER NOT EXISTS{?a (a|^a)* ?type}",
					"	FILTER NOT EXISTS{?a (a|^a)* ?type}",
					"}"));

			query.setMaxExecutionTime(1);

			String actual = query.explain(Explanation.Level.Timed).toString();
			assertThat(actual).contains("Timed out");

		}
		sailRepository.shutDown();

	}

	@Test
	public void testDot() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(TUPLE_QUERY);

			Explanation explain = query.explain(Explanation.Level.Optimized);
			String actual = explain.toDot();
			actual = actual.replaceAll("UUID_\\w+", "UUID");

			String expected = "digraph Explanation {\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Projection</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>ProjectionElemList</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID -> UUID [label=\"\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>ProjectionElem &quot;a&quot;</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>LeftJoin</U></td></tr> <tr><td>Algorithm</td><td>LeftJoinIterator</td></tr></table>> shape=plaintext];\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Join</U></td></tr> <tr><td>Algorithm</td><td>JoinIterator</td></tr></table>> shape=plaintext];\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>StatementPattern</U></td></tr> <tr><td>Cost estimate</td><td>3.00</td></tr> <tr><td>Result size estimate</td><td>4.00</td></tr></table>> shape=plaintext];\n"
					+
					"   UUID -> UUID [label=\"index 0\"] ;\n" +
					"   UUID -> UUID [label=\"index 1\"] ;\n" +
					"   UUID -> UUID [label=\"index 2\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=a)</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=d)</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Filter</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Compare (!=)</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=c)</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=d)</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Join</U></td></tr> <tr><td>Algorithm</td><td>HashJoinIteration</td></tr></table>> shape=plaintext];\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Filter</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Compare (!=)</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=c)</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>ValueConstant (value=&quot;&lt;&quot;)</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>StatementPattern</U></td></tr> <tr><td>Cost estimate</td><td>3.00</td></tr> <tr><td>Result size estimate</td><td>4.00</td></tr></table>> shape=plaintext];\n"
					+
					"   UUID -> UUID [label=\"index 0\"] ;\n" +
					"   UUID -> UUID [label=\"index 1\"] ;\n" +
					"   UUID -> UUID [label=\"index 2\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=a)</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=c)</U></td></tr></table>> shape=plaintext];\n"
					+
					"   subgraph cluster_UUID {\n" +
					"   color=grey\n" +
					"UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>LeftJoin</U></td></tr> <tr><td><B>New scope</B></td><td><B>true</B></td></tr> <tr><td>Cost estimate</td><td>6.61</td></tr> <tr><td>Result size estimate</td><td>12</td></tr></table>> shape=plaintext];\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>SingletonSet</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>StatementPattern</U></td></tr> <tr><td>Result size estimate</td><td>12</td></tr></table>> shape=plaintext];\n"
					+
					"   UUID -> UUID [label=\"index 0\"] ;\n" +
					"   UUID -> UUID [label=\"index 1\"] ;\n" +
					"   UUID -> UUID [label=\"index 2\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=d)</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=e)</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=f)</U></td></tr></table>> shape=plaintext];\n"
					+
					"\n" +
					"}\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>StatementPattern</U></td></tr> <tr><td>Result size estimate</td><td>12</td></tr></table>> shape=plaintext];\n"
					+
					"   UUID -> UUID [label=\"index 0\"] ;\n" +
					"   UUID -> UUID [label=\"index 1\"] ;\n" +
					"   UUID -> UUID [label=\"index 2\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=d)</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=e)</U></td></tr></table>> shape=plaintext];\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=f)</U></td></tr></table>> shape=plaintext];\n"
					+
					"\n" +
					"}\n";
			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

	}

	@Test
	public void testDotTimed() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(SUB_QUERY);

			Explanation explain = query.explain(Explanation.Level.Timed);
			String actual = explain.toDot();
			actual = actual.replaceAll("UUID_\\w+", "UUID");

			assertThat(actual).startsWith("digraph Explanation {");
			assertThat(actual).contains(
					"[label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"");
			assertThat(actual).contains("Total time actual</td><td BGCOLOR=");
			assertThat(actual).contains("Self time actual</td><td BGCOLOR=\"");
			assertThat(actual).contains("ms</td>");
			assertThat(actual).contains("<U>Projection</U>");
			assertThat(actual).contains("<U>ProjectionElemList</U>");
			assertThat(actual).contains("<U>Join</U>");

		}
		sailRepository.shutDown();

	}

	@Test
	public void testWildcard() {

		String expected = "StatementPattern (resultSizeEstimate=12)\n" +
				"   s: Var (name=a)\n" +
				"   p: Var (name=b)\n" +
				"   o: Var (name=c)\n";
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			TupleQuery query = connection.prepareTupleQuery("select * where {?a ?b ?c.}");
			String actual = query.explain(Explanation.Level.Optimized).toString();

			assertThat(actual).isEqualToNormalizingNewlines(expected);
		}
		sailRepository.shutDown();

	}

	@Test
	public void testArbitraryLengthPath() {

		String expected = "Projection\n" +
				"╠══ ProjectionElemList\n" +
				"║     ProjectionElem \"a\"\n" +
				"║     ProjectionElem \"b\"\n" +
				"║     ProjectionElem \"c\"\n" +
				"║     ProjectionElem \"d\"\n" +
				"╚══ Join (JoinIterator)\n" +
				"   ├── StatementPattern (costEstimate=8.50, resultSizeEstimate=12) [left]\n" +
				"   │     s: Var (name=a)\n" +
				"   │     p: Var (name=b)\n" +
				"   │     o: Var (name=c)\n" +
				"   └── ArbitraryLengthPath (costEstimate=5.39, resultSizeEstimate=24) [right]\n" +
				"         Var (name=c)\n" +
				"         StatementPattern (resultSizeEstimate=0)\n" +
				"            s: Var (name=c)\n" +
				"            p: Var (name=_const_f804988f_uri, value=http://a, anonymous)\n" +
				"            o: Var (name=d)\n" +
				"         Var (name=d)\n";
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			TupleQuery query = connection.prepareTupleQuery("select * where {?a ?b ?c. ?c <http://a>* ?d}");
			String actual = query.explain(Explanation.Level.Optimized).toString();

			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

	}

	@Test
	public void constructQueryTest() {

		String expected = "Reduced\n" +
				"   MultiProjection\n" +
				"      ProjectionElemList\n" +
				"         ProjectionElem \"proc\" AS \"subject\"\n" +
				"         ProjectionElem \"_const_f5e5585a_uri\" AS \"predicate\"\n" +
				"         ProjectionElem \"_const_be18ee7b_uri\" AS \"object\"\n" +
				"      ProjectionElemList\n" +
				"         ProjectionElem \"proc\" AS \"subject\"\n" +
				"         ProjectionElem \"_const_9c756f6b_uri\" AS \"predicate\"\n" +
				"         ProjectionElem \"p\" AS \"object\"\n" +
				"      ProjectionElemList\n" +
				"         ProjectionElem \"stat\" AS \"subject\"\n" +
				"         ProjectionElem \"_const_25686184_uri\" AS \"predicate\"\n" +
				"         ProjectionElem \"lot\" AS \"object\"\n" +
				"      ProjectionElemList\n" +
				"         ProjectionElem \"stat\" AS \"subject\"\n" +
				"         ProjectionElem \"_const_f5e5585a_uri\" AS \"predicate\"\n" +
				"         ProjectionElem \"_const_ea79e75_uri\" AS \"object\"\n" +
				"      ProjectionElemList\n" +
				"         ProjectionElem \"stat\" AS \"subject\"\n" +
				"         ProjectionElem \"_const_98c73a3c_uri\" AS \"predicate\"\n" +
				"         ProjectionElem \"bidders\" AS \"object\"\n" +
				"      ProjectionElemList\n" +
				"         ProjectionElem \"resultnotice\" AS \"subject\"\n" +
				"         ProjectionElem \"_const_f5e5585a_uri\" AS \"predicate\"\n" +
				"         ProjectionElem \"_const_77e914ad_uri\" AS \"object\"\n" +
				"      ProjectionElemList\n" +
				"         ProjectionElem \"resultnotice\" AS \"subject\"\n" +
				"         ProjectionElem \"_const_1b0b00ca_uri\" AS \"predicate\"\n" +
				"         ProjectionElem \"ddate\" AS \"object\"\n" +
				"      ProjectionElemList\n" +
				"         ProjectionElem \"proc\" AS \"subject\"\n" +
				"         ProjectionElem \"_const_9c3f1eec_uri\" AS \"predicate\"\n" +
				"         ProjectionElem \"lot\" AS \"object\"\n" +
				"      ProjectionElemList\n" +
				"         ProjectionElem \"resultnotice\" AS \"subject\"\n" +
				"         ProjectionElem \"_const_6aa9a9c_uri\" AS \"predicate\"\n" +
				"         ProjectionElem \"buyerrole\" AS \"object\"\n" +
				"      ProjectionElemList\n" +
				"         ProjectionElem \"resultnotice\" AS \"subject\"\n" +
				"         ProjectionElem \"_const_183bd06d_uri\" AS \"predicate\"\n" +
				"         ProjectionElem \"proc\" AS \"object\"\n" +
				"      Extension\n" +
				"         Group (countryID, year)\n" +
				"            Join (HashJoinIteration)\n" +
				"            ╠══ Extension [left]\n" +
				"            ║  ├── Join (JoinIterator)\n" +
				"            ║  │  ╠══ StatementPattern (costEstimate=0.71, resultSizeEstimate=0) [left]\n" +
				"            ║  │  ║     s: Var (name=resultnotice)\n" +
				"            ║  │  ║     p: Var (name=_const_183bd06d_uri, value=http://data.europa.eu/a4g/ontology#refersToProcedure, anonymous)\n"
				+
				"            ║  │  ║     o: Var (name=proc)\n" +
				"            ║  │  ╚══ Join (JoinIterator) [right]\n" +
				"            ║  │     ├── StatementPattern (costEstimate=1.00, resultSizeEstimate=0) [left]\n" +
				"            ║  │     │     s: Var (name=proc)\n" +
				"            ║  │     │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
				+
				"            ║  │     │     o: Var (name=_const_be18ee7b_uri, value=http://data.europa.eu/a4g/ontology#Procedure, anonymous)\n"
				+
				"            ║  │     └── Join (JoinIterator) [right]\n" +
				"            ║  │        ╠══ StatementPattern (costEstimate=1.00, resultSizeEstimate=0) [left]\n" +
				"            ║  │        ║     s: Var (name=resultnotice)\n" +
				"            ║  │        ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
				+
				"            ║  │        ║     o: Var (name=_const_77e914ad_uri, value=http://data.europa.eu/a4g/ontology#ResultNotice, anonymous)\n"
				+
				"            ║  │        ╚══ Join (JoinIterator) [right]\n" +
				"            ║  │           ├── StatementPattern (costEstimate=1.12, resultSizeEstimate=0) [left]\n" +
				"            ║  │           │     s: Var (name=proc)\n" +
				"            ║  │           │     p: Var (name=_const_9c3f1eec_uri, value=http://data.europa.eu/a4g/ontology#hasProcurementScopeDividedIntoLot, anonymous)\n"
				+
				"            ║  │           │     o: Var (name=lot)\n" +
				"            ║  │           └── Join (JoinIterator) [right]\n" +
				"            ║  │              ╠══ StatementPattern (costEstimate=0.75, resultSizeEstimate=0) [left]\n"
				+
				"            ║  │              ║     s: Var (name=stat)\n" +
				"            ║  │              ║     p: Var (name=_const_25686184_uri, value=http://data.europa.eu/a4g/ontology#concernsSubmissionsForLot, anonymous)\n"
				+
				"            ║  │              ║     o: Var (name=lot)\n" +
				"            ║  │              ╚══ Join (JoinIterator) [right]\n" +
				"            ║  │                 ├── StatementPattern (costEstimate=1.00, resultSizeEstimate=0) [left]\n"
				+
				"            ║  │                 │     s: Var (name=stat)\n" +
				"            ║  │                 │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
				+
				"            ║  │                 │     o: Var (name=_const_ea79e75_uri, value=http://data.europa.eu/a4g/ontology#SubmissionStatisticalInformation, anonymous)\n"
				+
				"            ║  │                 └── Join (JoinIterator) [right]\n" +
				"            ║  │                    ╠══ Filter [left]\n" +
				"            ║  │                    ║  ├── Compare (!=)\n" +
				"            ║  │                    ║  │     Var (name=p)\n" +
				"            ║  │                    ║  │     ValueConstant (value=http://publications.europa.eu/resource/authority/procurement-procedure-type/neg-wo-call)\n"
				+
				"            ║  │                    ║  └── StatementPattern (costEstimate=2.24, resultSizeEstimate=0)\n"
				+
				"            ║  │                    ║        s: Var (name=proc)\n" +
				"            ║  │                    ║        p: Var (name=_const_9c756f6b_uri, value=http://data.europa.eu/a4g/ontology#hasProcedureType, anonymous)\n"
				+
				"            ║  │                    ║        o: Var (name=p)\n" +
				"            ║  │                    ╚══ Join (JoinIterator) [right]\n" +
				"            ║  │                       ├── StatementPattern (costEstimate=2.24, resultSizeEstimate=0) [left]\n"
				+
				"            ║  │                       │     s: Var (name=stat)\n" +
				"            ║  │                       │     p: Var (name=_const_98c73a3c_uri, value=http://data.europa.eu/a4g/ontology#hasReceivedTenders, anonymous)\n"
				+
				"            ║  │                       │     o: Var (name=bidders)\n" +
				"            ║  │                       └── Join (JoinIterator) [right]\n" +
				"            ║  │                          ╠══ StatementPattern (costEstimate=2.24, resultSizeEstimate=0) [left]\n"
				+
				"            ║  │                          ║     s: Var (name=resultnotice)\n" +
				"            ║  │                          ║     p: Var (name=_const_1b0b00ca_uri, value=http://data.europa.eu/a4g/ontology#hasDispatchDate, anonymous)\n"
				+
				"            ║  │                          ║     o: Var (name=ddate)\n" +
				"            ║  │                          ╚══ StatementPattern (costEstimate=2.24, resultSizeEstimate=0) [right]\n"
				+
				"            ║  │                                s: Var (name=resultnotice)\n" +
				"            ║  │                                p: Var (name=_const_6aa9a9c_uri, value=http://data.europa.eu/a4g/ontology#refersToRole, anonymous)\n"
				+
				"            ║  │                                o: Var (name=buyerrole)\n" +
				"            ║  └── ExtensionElem (year)\n" +
				"            ║        FunctionCall (http://www.w3.org/2005/xpath-functions#year-from-dateTime)\n" +
				"            ║           FunctionCall (http://www.w3.org/2001/XMLSchema#dateTime)\n" +
				"            ║              Var (name=ddate)\n" +
				"            ╚══ Distinct (new scope) [right]\n" +
				"                  Projection\n" +
				"                  ╠══ ProjectionElemList\n" +
				"                  ║     ProjectionElem \"buyerrole\"\n" +
				"                  ║     ProjectionElem \"countryID\"\n" +
				"                  ╚══ Join (JoinIterator)\n" +
				"                     ├── StatementPattern (costEstimate=1.25, resultSizeEstimate=0) [left]\n" +
				"                     │     s: Var (name=org)\n" +
				"                     │     p: Var (name=_const_beb18915_uri, value=https://www.w3.org/ns/legal#registeredAddress, anonymous)\n"
				+
				"                     │     o: Var (name=orgaddress)\n" +
				"                     └── Join (JoinIterator) [right]\n" +
				"                        ╠══ StatementPattern (costEstimate=1.12, resultSizeEstimate=0) [left]\n" +
				"                        ║     s: Var (name=orgaddress)\n" +
				"                        ║     p: Var (name=_const_2f7de0e1_uri, value=http://data.europa.eu/a4g/ontology#hasCountryCode, anonymous)\n"
				+
				"                        ║     o: Var (name=countrycode)\n" +
				"                        ╚══ Join (JoinIterator) [right]\n" +
				"                           ├── Filter [left]\n" +
				"                           │  ╠══ Compare (!=)\n" +
				"                           │  ║     Var (name=buytype)\n" +
				"                           │  ║     ValueConstant (value=http://publications.europa.eu/resource/authority/buyer-legal-type/eu-int-org)\n"
				+
				"                           │  ╚══ StatementPattern (costEstimate=2.24, resultSizeEstimate=0)\n" +
				"                           │        s: Var (name=org)\n" +
				"                           │        p: Var (name=_const_1abd8d4b_uri, value=http://data.europa.eu/a4g/ontology#hasBuyerType, anonymous)\n"
				+
				"                           │        o: Var (name=buytype)\n" +
				"                           └── Join (JoinIterator) [right]\n" +
				"                              ╠══ StatementPattern (costEstimate=2.24, resultSizeEstimate=0) [left]\n"
				+
				"                              ║     s: Var (name=buyerrole)\n" +
				"                              ║     p: Var (name=_const_beb855c2_uri, value=http://data.europa.eu/a4g/ontology#playedBy, anonymous)\n"
				+
				"                              ║     o: Var (name=org)\n" +
				"                              ╚══ StatementPattern (costEstimate=2.24, resultSizeEstimate=0) [right]\n"
				+
				"                                    s: Var (name=countrycode)\n" +
				"                                    p: Var (name=_const_a825a5f4_uri, value=http://purl.org/dc/elements/1.1/identifier, anonymous)\n"
				+
				"                                    o: Var (name=countryID)\n" +
				"         ExtensionElem (_const_f5e5585a_uri)\n" +
				"            ValueConstant (value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type)\n" +
				"         ExtensionElem (_const_be18ee7b_uri)\n" +
				"            ValueConstant (value=http://data.europa.eu/a4g/ontology#Procedure)\n" +
				"         ExtensionElem (_const_9c756f6b_uri)\n" +
				"            ValueConstant (value=http://data.europa.eu/a4g/ontology#hasProcedureType)\n" +
				"         ExtensionElem (_const_25686184_uri)\n" +
				"            ValueConstant (value=http://data.europa.eu/a4g/ontology#concernsSubmissionsForLot)\n" +
				"         ExtensionElem (_const_ea79e75_uri)\n" +
				"            ValueConstant (value=http://data.europa.eu/a4g/ontology#SubmissionStatisticalInformation)\n"
				+
				"         ExtensionElem (_const_98c73a3c_uri)\n" +
				"            ValueConstant (value=http://data.europa.eu/a4g/ontology#hasReceivedTenders)\n" +
				"         ExtensionElem (_const_77e914ad_uri)\n" +
				"            ValueConstant (value=http://data.europa.eu/a4g/ontology#ResultNotice)\n" +
				"         ExtensionElem (_const_1b0b00ca_uri)\n" +
				"            ValueConstant (value=http://data.europa.eu/a4g/ontology#hasDispatchDate)\n" +
				"         ExtensionElem (_const_9c3f1eec_uri)\n" +
				"            ValueConstant (value=http://data.europa.eu/a4g/ontology#hasProcurementScopeDividedIntoLot)\n"
				+
				"         ExtensionElem (_const_6aa9a9c_uri)\n" +
				"            ValueConstant (value=http://data.europa.eu/a4g/ontology#refersToRole)\n" +
				"         ExtensionElem (_const_183bd06d_uri)\n" +
				"            ValueConstant (value=http://data.europa.eu/a4g/ontology#refersToProcedure)\n";
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			GraphQuery query = connection.prepareGraphQuery(CONSTRUCT);
			String actual = query.explain(Explanation.Level.Optimized).toString();

			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

	}

	@Test
	public void testHaving() {

		String expected = "Order (resultSizeActual=4)\n" +
				"   OrderElem (ASC)\n" +
				"      Var (name=nbTerm)\n" +
				"   OrderElem (ASC)\n" +
				"      Var (name=nameSjb1)\n" +
				"   OrderElem (ASC)\n" +
				"      Var (name=idTerm3)\n" +
				"   Projection (resultSizeActual=4)\n" +
				"   ├── ProjectionElemList\n" +
				"   │     ProjectionElem \"nameSjb1\"\n" +
				"   │     ProjectionElem \"idCN1\"\n" +
				"   │     ProjectionElem \"nbTerm\"\n" +
				"   │     ProjectionElem \"idTerm3\"\n" +
				"   └── Join (JoinIterator) (resultSizeActual=4)\n" +
				"      ╠══ Projection (new scope) (resultSizeActual=2) [left]\n" +
				"      ║  ├── ProjectionElemList\n" +
				"      ║  │     ProjectionElem \"nameSjb1\"\n" +
				"      ║  │     ProjectionElem \"idCN1\"\n" +
				"      ║  │     ProjectionElem \"nbTerm\"\n" +
				"      ║  └── Extension (resultSizeActual=2)\n" +
				"      ║     ╠══ Extension (resultSizeActual=2)\n" +
				"      ║     ║     Filter (resultSizeActual=2)\n" +
				"      ║     ║     ╠══ Compare (<)\n" +
				"      ║     ║     ║     Var (name=nbTerm)\n" +
				"      ║     ║     ║     ValueConstant (value=\"3\"^^<http://www.w3.org/2001/XMLSchema#integer>)\n" +
				"      ║     ║     ╚══ Group (nameSjb1, idCN1) (resultSizeActual=4)\n" +
				"      ║     ║        ├── LeftJoin (LeftJoinIterator) (resultSizeActual=11)\n" +
				"      ║     ║        │  ╠══ Join (JoinIterator) (resultSizeActual=11) [left]\n" +
				"      ║     ║        │  ║  ├── StatementPattern (costEstimate=54, resultSizeEstimate=4.00, resultSizeActual=4) [left]\n"
				+
				"      ║     ║        │  ║  │     s: Var (name=idTerm1)\n" +
				"      ║     ║        │  ║  │     p: Var (name=_const_c6e40399_uri, value=http://iec.ch/TC57/2013/CIM-schema-cim16#Terminal.ConductingEquipment, anonymous)\n"
				+
				"      ║     ║        │  ║  │     o: Var (name=idSjb1)\n" +
				"      ║     ║        │  ║  └── Join (JoinIterator) (resultSizeActual=11) [right]\n" +
				"      ║     ║        │  ║     ╠══ StatementPattern (costEstimate=1.00, resultSizeEstimate=4.00, resultSizeActual=4) [left]\n"
				+
				"      ║     ║        │  ║     ║     s: Var (name=idSjb1)\n" +
				"      ║     ║        │  ║     ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
				+
				"      ║     ║        │  ║     ║     o: Var (name=_const_6965b017_uri, value=http://iec.ch/TC57/2013/CIM-schema-cim16#BusbarSection, anonymous)\n"
				+
				"      ║     ║        │  ║     ╚══ Join (JoinIterator) (resultSizeActual=11) [right]\n" +
				"      ║     ║        │  ║        ├── StatementPattern (costEstimate=2.12, resultSizeEstimate=13, resultSizeActual=4) [left]\n"
				+
				"      ║     ║        │  ║        │     s: Var (name=idTerm1)\n" +
				"      ║     ║        │  ║        │     p: Var (name=_const_4395d870_uri, value=http://iec.ch/TC57/2013/CIM-schema-cim16#Terminal.ConnectivityNode, anonymous)\n"
				+
				"      ║     ║        │  ║        │     o: Var (name=idCN1)\n" +
				"      ║     ║        │  ║        └── StatementPattern (costEstimate=4.24, resultSizeEstimate=13, resultSizeActual=11) [right]\n"
				+
				"      ║     ║        │  ║              s: Var (name=idTermOfCN)\n" +
				"      ║     ║        │  ║              p: Var (name=_const_4395d870_uri, value=http://iec.ch/TC57/2013/CIM-schema-cim16#Terminal.ConnectivityNode, anonymous)\n"
				+
				"      ║     ║        │  ║              o: Var (name=idCN1)\n" +
				"      ║     ║        │  ╚══ StatementPattern (resultSizeEstimate=4.00, resultSizeActual=11) [right]\n"
				+
				"      ║     ║        │        s: Var (name=idSjb1)\n" +
				"      ║     ║        │        p: Var (name=_const_857da984_uri, value=http://iec.ch/TC57/2013/CIM-schema-cim16#IdentifiedObject.name, anonymous)\n"
				+
				"      ║     ║        │        o: Var (name=nameSjb1)\n" +
				"      ║     ║        └── GroupElem (nbTerm)\n" +
				"      ║     ║              Count\n" +
				"      ║     ║                 Var (name=idTermOfCN)\n" +
				"      ║     ╚══ ExtensionElem (nbTerm)\n" +
				"      ║           Count\n" +
				"      ║              Var (name=idTermOfCN)\n" +
				"      ╚══ StatementPattern (costEstimate=18, resultSizeEstimate=13, resultSizeActual=4) [right]\n" +
				"            s: Var (name=idTerm3)\n" +
				"            p: Var (name=_const_4395d870_uri, value=http://iec.ch/TC57/2013/CIM-schema-cim16#Terminal.ConnectivityNode, anonymous)\n"
				+
				"            o: Var (name=idCN1)\n";
		SailRepository sailRepository = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(new StringReader("@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
					"@prefix cim: <http://iec.ch/TC57/2013/CIM-schema-cim16#> .\n" +
					"@prefix : <http://example.org/> .\n" +
					"\n" +
					"# Busbar Sections\n" +
					":BusbarSection1 rdf:type cim:BusbarSection ;\n" +
					"    cim:IdentifiedObject.name \"Busbar1\" .\n" +
					"\n" +
					":BusbarSection2 rdf:type cim:BusbarSection ;\n" +
					"    cim:IdentifiedObject.name \"Busbar2\" .\n" +
					"\n" +
					":BusbarSection3 rdf:type cim:BusbarSection ;\n" +
					"    cim:IdentifiedObject.name \"Busbar3\" .\n" +
					"\n" +
					":BusbarSection4 rdf:type cim:BusbarSection ;\n" +
					"    cim:IdentifiedObject.name \"Busbar4\" .\n" +
					"\n" +
					"# Connectivity Nodes\n" +
					":ConnectivityNode1 a cim:ConnectivityNode .\n" +
					":ConnectivityNode2 a cim:ConnectivityNode .\n" +
					":ConnectivityNode3 a cim:ConnectivityNode .\n" +
					":ConnectivityNode4 a cim:ConnectivityNode .\n" +
					":ConnectivityNode5 a cim:ConnectivityNode .\n" +
					":ConnectivityNode6 a cim:ConnectivityNode .\n" +
					"\n" +
					"# Terminals connected to ConnectivityNode1 (3 terminals)\n" +
					":Terminal1 cim:Terminal.ConductingEquipment :BusbarSection1 ;\n" +
					"    cim:Terminal.ConnectivityNode :ConnectivityNode1 .\n" +
					"\n" +
					":Terminal2 cim:Terminal.ConnectivityNode :ConnectivityNode1 .\n" +
					":Terminal3 cim:Terminal.ConnectivityNode :ConnectivityNode1 .\n" +
					"\n" +
					"# Terminals connected to ConnectivityNode2 (2 terminals)\n" +
					":Terminal4 cim:Terminal.ConductingEquipment :BusbarSection2 ;\n" +
					"    cim:Terminal.ConnectivityNode :ConnectivityNode2 .\n" +
					"\n" +
					":Terminal5 cim:Terminal.ConnectivityNode :ConnectivityNode2 .\n" +
					"\n" +
					"# Terminal connected to ConnectivityNode3 (1 terminal)\n" +
					":Terminal6 cim:Terminal.ConnectivityNode :ConnectivityNode3 .\n" +
					"\n" +
					"# Terminals connected to ConnectivityNode4 (4 terminals)\n" +
					":Terminal7 cim:Terminal.ConductingEquipment :BusbarSection3 ;\n" +
					"    cim:Terminal.ConnectivityNode :ConnectivityNode4 .\n" +
					"\n" +
					":Terminal8 cim:Terminal.ConnectivityNode :ConnectivityNode4 .\n" +
					":Terminal9 cim:Terminal.ConnectivityNode :ConnectivityNode4 .\n" +
					":Terminal10 cim:Terminal.ConnectivityNode :ConnectivityNode4 .\n" +
					"\n" +
					"# Terminals connected to ConnectivityNode5 (2 terminals)\n" +
					":Terminal11 cim:Terminal.ConductingEquipment :BusbarSection4 ;\n" +
					"    cim:Terminal.ConnectivityNode :ConnectivityNode5 .\n" +
					"\n" +
					":Terminal12 cim:Terminal.ConnectivityNode :ConnectivityNode5 .\n" +
					"\n" +
					"# Terminal connected to ConnectivityNode6 (1 terminal)\n" +
					":Terminal13 cim:Terminal.ConnectivityNode :ConnectivityNode6 ."), "", RDFFormat.TURTLE);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			TupleQuery query = connection.prepareTupleQuery(
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
							"PREFIX cim: <http://iec.ch/TC57/2013/CIM-schema-cim16#>\n" +
							"" +
							"select ?nameSjb1 ?idCN1 ?nbTerm ?idTerm3\n" +
							"where {\n" +
							"    {\n" +
							"        select ?nameSjb1 ?idCN1 (count(?idTermOfCN) as ?nbTerm)\n" +
							"        where {\n" +
							"            ?idSjb1 rdf:type cim:BusbarSection .\n" +
							"            ?idTerm1 cim:Terminal.ConductingEquipment ?idSjb1 .\n" +
							"            ?idTerm1 cim:Terminal.ConnectivityNode ?idCN1 .\n" +
							"            ?idTermOfCN cim:Terminal.ConnectivityNode ?idCN1\n" +
							"            OPTIONAL { ?idSjb1 cim:IdentifiedObject.name ?nameSjb1 . }\n" +
							"        }\n" +
							"        group by ?nameSjb1 ?idCN1\n" +
							"        having (?nbTerm < 3)\n" +
							"    }\n" +
							"    ?idTerm3 cim:Terminal.ConnectivityNode ?idCN1\n" +
							"}\n" +
							"order by ?nbTerm ?nameSjb1 ?idTerm3");
			String actual = query.explain(Explanation.Level.Executed).toString();

			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

	}

}
