package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.jupiter.api.Test;

public class ArrayBindingBasedQueryEvaluationContextTest {
	@Test
	public void findAllVariablesGH4646() {
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL,
				"PREFIX  owl:  <http://www.w3.org/2002/07/owl#>\n"
						+ "PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>\n"
						+ "PREFIX  freebase: <http://rdf.freebase.com/ns/>\n"
						+ "PREFIX  skos: <http://www.w3.org/2004/02/skos/core#>\n"
						+ "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
						+ "PREFIX  dbpedia: <http://dbpedia.org/resource/>\n"
						+ "PREFIX  users: <http://schema.semantic-web.at/users/>\n"
						+ "PREFIX  tags: <http://www.holygoat.co.uk/owl/redwood/0.1/tags/>\n"
						+ "PREFIX  skos-xl: <http://www.w3.org/2008/05/skos-xl#>\n"
						+ "PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
						+ "PREFIX  dcterms: <http://purl.org/dc/terms/>\n"
						+ "PREFIX  ctag: <http://commontag.org/ns#>\n"
						+ "PREFIX  foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "PREFIX  dc:   <http://purl.org/dc/elements/1.1/>"
						+ "SELECT  (?p AS ?resource) (?violates AS ?property) (?c AS ?value)\n"
						+ "WHERE\n"
						+ "  {   { SELECT DISTINCT  ?c ?violates ?p\n"
						+ "        WHERE\n"
						+ "          { GRAPH <http://test.com/inportdata>\n"
						+ "              { ?c  ?p  ?x\n"
						+ "                FILTER ( ?p IN (skos:hasTopConcept, skos:narrower, skos:broader, skos:related, skos:member, skos:broaderTransitive, skos:narrowerTransitive) )\n"
						+ "                OPTIONAL\n"
						+ "                  { ?c  rdf:type  ?cType }\n"
						+ "              }\n"
						+ "            { SELECT  ?p ?domainType\n"
						+ "              WHERE\n"
						+ "                { GRAPH <tmp:validationengine/uni-schema>\n"
						+ "                    {   { ?p (rdfs:domain/(((owl:unionOf/(rdf:rest)*)/rdf:first))*)/^(rdfs:subClassOf)* ?domainType\n"
						+ "                          FILTER isIRI(?domainType)\n"
						+ "                        }\n"
						+ "                      UNION\n"
						+ "                        { ?p (rdfs:subPropertyOf)+ ?parentProperty .\n"
						+ "                          ?parentProperty (rdfs:domain/(((owl:unionOf/(rdf:rest)*)/rdf:first))*)/^(rdfs:subClassOf)* ?domainType\n"
						+ "                          FILTER isIRI(?domainType)\n"
						+ "                        }\n"
						+ "                    }\n"
						+ "                }\n"
						+ "            }\n"
						+ "            BIND(coalesce(sameTerm(?cType, ?domainType), false) AS ?typeMatch)\n"
						+ "            BIND(<urn:domainViolationBy> AS ?violates)\n"
						+ "          }\n"
						+ "        GROUP BY ?c ?p ?violates\n"
						+ "        HAVING ( MAX(?typeMatch) = false )\n"
						+ "      }\n"
						+ "    UNION\n"
						+ "      { SELECT DISTINCT  ?c ?violates ?p\n"
						+ "        WHERE\n"
						+ "          { GRAPH <http://test.com/inportdata>\n"
						+ "              { ?x  ?p  ?c\n"
						+ "                FILTER ( ?p IN (skos:hasTopConcept, skos:narrower, skos:broader, skos:related, skos:member, skos:broaderTransitive, skos:narrowerTransitive) )\n"
						+ "                OPTIONAL\n"
						+ "                  { ?c  rdf:type  ?cType }\n"
						+ "              }\n"
						+ "            { SELECT  ?p ?rangeType\n"
						+ "              WHERE\n"
						+ "                { GRAPH <tmp:validationengine/uni-schema>\n"
						+ "                    {   { ?p (rdfs:range/(((owl:unionOf/(rdf:rest)*)/rdf:first))*)/^(rdfs:subClassOf)* ?rangeType\n"
						+ "                          FILTER isIRI(?rangeType)\n"
						+ "                        }\n"
						+ "                      UNION\n"
						+ "                        { ?p (rdfs:subPropertyOf)+ ?parentProperty .\n"
						+ "                          ?parentProperty (rdfs:range/(((owl:unionOf/(rdf:rest)*)/rdf:first))*)/^(rdfs:subClassOf)* ?rangeType\n"
						+ "                          FILTER isIRI(?rangeType)\n"
						+ "                        }\n"
						+ "                    }\n"
						+ "                }\n"
						+ "            }\n"
						+ "            BIND(coalesce(sameTerm(?cType, ?rangeType), false) AS ?typeMatch)\n"
						+ "            BIND(<urn:rangeViolationBy> AS ?violates)\n"
						+ "          }\n"
						+ "        GROUP BY ?c ?p ?violates\n"
						+ "        HAVING ( MAX(?typeMatch) = false )\n"
						+ "      }\n"
						+ "  }",
				null);
		QueryRoot query = (QueryRoot) pq.getTupleExpr();
		String[] list = ArrayBindingBasedQueryEvaluationContext.findAllVariablesUsedInQuery(query);
		Set<String> vars = Arrays.stream(list).collect(Collectors.toSet());
		for (String x : List.of("resource", "property", "value", "c", "violates", "p", "x", "cType", "domainType",
				"parentProperty", "typeMatch", "rangeType")) {
			assertTrue(vars.contains(x), x + ": should be in the list");
		}
	}
}
