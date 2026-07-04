/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.readonly.sparql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class SparqlQueryEvaluatorDefaultTest {

	private static final IRI CTX1 = Values.iri("http://example.com/ctx1");

	private static final IRI CTX2 = Values.iri("http://example.com/ctx2");

	private static final IRI TYP1 = Values.iri("http://example.com/typ1");

	private static final IRI TYP2 = Values.iri("http://example.com/typ2");

	@Test
	public void queryWithoutContext() throws MalformedQueryException, IllegalStateException, IOException {
		Repository repo = new SailRepository(new MemoryStore());
		try (RepositoryConnection con = repo.getConnection()) {
			con.add(getTestModel1(), CTX1);
			con.add(getTestModel2(), CTX2);
			con.getStatements(null, null, null).forEach(System.out::println);
		}

		EvaluateResult evaluateResult = new EvaluateResultDefault(new ByteArrayOutputStream());
		SparqlQueryEvaluator sparqlQueryEvaluator = new SparqlQueryEvaluatorDefault();

		String queryString = "SELECT ?subject ?predicate ?object WHERE { ?subject ?predicate ?object} ";
		String[] namedGraphUris = null;// new String[] {};

		String[] defaultGraphUri = null;// new String[] {};

		sparqlQueryEvaluator.evaluate(evaluateResult, repo, queryString, null,
				defaultGraphUri, namedGraphUris);

		ArrayNode bindingArray = asArrayNode(evaluateResult);
		assertEquals(4, bindingArray.size());
	}

	@Test
	public void queryWithDefaultGraphUri() throws MalformedQueryException, IllegalStateException, IOException {
		Repository repo = new SailRepository(new MemoryStore());
		try (RepositoryConnection con = repo.getConnection()) {
			con.add(getTestModel1(), CTX1);
			con.add(getTestModel2(), CTX1);
			con.getStatements(null, null, null).forEach(System.out::println);
		}

		EvaluateResult evaluateResult = new EvaluateResultDefault(new ByteArrayOutputStream());
		SparqlQueryEvaluator sparqlQueryEvaluator = new SparqlQueryEvaluatorDefault();

		String queryString = "SELECT ?subject ?predicate ?object WHERE { ?subject ?predicate ?object} ";
		String[] namedGraphUris = new String[] {};
		String[] defaultGraphUri = new String[] { CTX1.stringValue() };

		sparqlQueryEvaluator.evaluate(evaluateResult, repo, queryString, null,
				defaultGraphUri, namedGraphUris);

		ArrayNode bindingArray = asArrayNode(evaluateResult);
		assertEquals(4, bindingArray.size());
	}

	@Test
	public void queryWithDefaultGraphUriAndNamedGraphUris()
			throws MalformedQueryException, IllegalStateException, IOException {
		Repository repo = new SailRepository(new MemoryStore());
		try (RepositoryConnection con = repo.getConnection()) {
			con.add(getTestModel1(), CTX1);
			con.add(getTestModel2(), CTX2);
			con.getStatements(null, null, null).forEach(System.out::println);
		}

		EvaluateResult evaluateResult = new EvaluateResultDefault(new ByteArrayOutputStream());
		SparqlQueryEvaluator sparqlQueryEvaluator = new SparqlQueryEvaluatorDefault();

		String queryString = "SELECT ?subject ?predicate ?object WHERE { ?subject ?predicate ?object} ";
		String[] namedGraphUris = new String[] { CTX2.stringValue() };
		String[] defaultGraphUri = new String[] { CTX1.stringValue() };

		sparqlQueryEvaluator.evaluate(evaluateResult, repo, queryString, null,
				defaultGraphUri, namedGraphUris);

		ArrayNode bindingArray = asArrayNode(evaluateResult);
		assertEquals(2, bindingArray.size());
	}

	@Test
	public void queryWithNamedGraphUris() throws MalformedQueryException, IllegalStateException, IOException {
		Repository repo = new SailRepository(new MemoryStore());
		try (RepositoryConnection con = repo.getConnection()) {
			con.add(getTestModel1(), CTX1);
			con.add(getTestModel2(), CTX2);
			con.getStatements(null, null, null).forEach(System.out::println);
		}

		EvaluateResult evaluateResult = new EvaluateResultDefault(new ByteArrayOutputStream());
		SparqlQueryEvaluator sparqlQueryEvaluator = new SparqlQueryEvaluatorDefault();

		String queryString = "SELECT ?subject ?predicate ?object WHERE { ?subject ?predicate ?object} ";
		String[] namedGraphUris = new String[] {};
		String[] defaultGraphUri = new String[] { CTX1.stringValue(), CTX2.stringValue() };

		sparqlQueryEvaluator.evaluate(evaluateResult, repo, queryString, null,
				defaultGraphUri, namedGraphUris);

		ArrayNode bindingArray = asArrayNode(evaluateResult);
		assertEquals(4, bindingArray.size());
	}

	private Model getTestModel1() {
		Literal obj1_1 = Values.literal("testValue_user1_obj1_1");
		Literal obj1_2 = Values.literal("testValue_user1_obj1_2");
		IRI obj1 = Values.iri("http://example.com/user1/object1");
		Model model = new ModelBuilder()
				.subject(obj1)
				.add(TYP1, obj1_1)
				.add(TYP2, obj1_2)
				.build();
		return model;
	}

	private Model getTestModel2() {
		Literal obj2_1 = Values.literal("testValue_user1_obj2_1");
		Literal obj2_2 = Values.literal("testValue_user1_obj2_2");
		IRI obj2 = Values.iri("http://example.com/user1/object2");

		Model model = new ModelBuilder()
				.subject(obj2)
				.add(TYP1, obj2_1)
				.add(TYP2, obj2_2)
				.build();
		return model;
	}

	private ArrayNode asArrayNode(EvaluateResult evaluateResult)
			throws IOException, JsonProcessingException, JsonMappingException {
		ByteArrayOutputStream stream = (ByteArrayOutputStream) evaluateResult.getOutputstream();
		String evaluateResultString = new String(stream.toByteArray());
		System.out.println(evaluateResultString);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(evaluateResultString);
		ArrayNode bindingArray = (ArrayNode) root.get("results").get("bindings");
		return bindingArray;
	}
}
