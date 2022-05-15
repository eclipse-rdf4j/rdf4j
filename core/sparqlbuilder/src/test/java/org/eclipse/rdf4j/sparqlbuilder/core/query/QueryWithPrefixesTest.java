/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sparqlbuilder.core.query;

import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.datatype;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.lt;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.strlen;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.var;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.junit.Assert;
import org.junit.Test;

public class QueryWithPrefixesTest extends BaseExamples {
	@Test
	public void testSelectQuery1Prefix() {
		Variable x = var("x"), name = var("name");
		query = Queries
				.SELECT(name)
				.prefix(FOAF.NS)
				.where(x.has(FOAF.NAME, name));
		Assert.assertEquals(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "SELECT ?name\n"
						+ "WHERE { ?x foaf:name ?name . }\n",
				query.getQueryString());
	}

	@Test
	public void testSelectQuery2Prefixes() {
		Variable x = var("x"), name = var("name");
		query = Queries
				.SELECT(name)
				.prefix(FOAF.NS)
				.prefix(XSD.NS)
				.where(x.has(FOAF.NAME, name).filter(Expressions.equals(datatype(name), iri(XSD.STRING))));
		Assert.assertEquals(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
						+ "SELECT ?name\n"
						+ "WHERE { ?x foaf:name ?name .\n"
						+ "FILTER ( DATATYPE( ?name ) = xsd:string ) }\n",
				query.getQueryString());
	}

	@Test
	public void testInsertQuery1Prefix() {
		Variable x = var("x"), name = var("name");
		ModifyQuery modifyQuery = Queries
				.INSERT(x.has(FOAF.NAME, name))
				.prefix(FOAF.NS)
				.where(x.has(FOAF.SURNAME, name));
		Assert.assertEquals(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "INSERT { ?x foaf:name ?name . }\n"
						+ "WHERE { ?x foaf:surname ?name . }",
				modifyQuery.getQueryString());
	}

	@Test
	public void testInsertQuery2Prefixes() {
		Variable x = var("x"), name = var("name");
		ModifyQuery modifyQuery = Queries
				.INSERT(x.has(FOAF.NAME, name))
				.prefix(FOAF.NS)
				.prefix(XSD.NS)
				.where(x.has(FOAF.SURNAME, name).filter(Expressions.equals(datatype(name), iri(XSD.STRING))));
		Assert.assertEquals(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
						+ "INSERT { ?x foaf:name ?name . }\n"
						+ "WHERE { ?x foaf:surname ?name .\n"
						+ "FILTER ( DATATYPE( ?name ) = xsd:string ) }",
				modifyQuery.getQueryString());
	}

	@Test
	public void testSelectQueryWithPropertyPath2Prefixes() {
		Variable x = var("x"), name = var("name");
		query = Queries
				.SELECT(name)
				.prefix(FOAF.NS)
				.where(x.has(p -> p.pred(FOAF.ACCOUNT).then(FOAF.MBOX).build(), name));
		Assert.assertEquals(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "SELECT ?name\n"
						+ "WHERE { ?x foaf:account / foaf:mbox ?name . }\n",
				query.getQueryString());
	}

	@Test
	public void testSelectQuery1PrefixWithAngledBracket() {
		Variable x = var("x"), name = var("name");
		query = Queries
				.SELECT(name)
				.prefix(FOAF.NS)
				.where(x.has(FOAF.NAME, name)
						.filter(lt(strlen(name), 10)));
		Assert.assertEquals(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "SELECT ?name\n"
						+ "WHERE { ?x foaf:name ?name .\n"
						+ "FILTER ( STRLEN( ?name ) < 10 ) }\n",
				query.getQueryString());
	}
}
