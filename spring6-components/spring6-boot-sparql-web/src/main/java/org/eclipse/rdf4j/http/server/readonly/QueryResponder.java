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
package org.eclipse.rdf4j.http.server.readonly;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

import java.io.IOException;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.http.server.readonly.sparql.SparqlQueryEvaluator;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.repository.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Experimental
@RestController
public class QueryResponder {

	private static final String[] ALL_GRAPHS = {};

	/**
	 * The repository that is being served.
	 */
	@Autowired
	private final Repository repository;

	@Autowired
	private SparqlQueryEvaluator sparqlQueryEvaluator;

	public QueryResponder(Repository repository) {
		this.repository = repository;
	}

	@PostMapping(value = "/sparql", consumes = APPLICATION_FORM_URLENCODED_VALUE)
	public void sparqlPostURLencoded(
			@RequestParam(value = "default-graph-uri", required = false) String defaultGraphUri,
			@RequestParam(value = "named-graph-uri", required = false) String namedGraphUri,
			@RequestParam(value = "query") String query, @RequestHeader(ACCEPT) String acceptHeader,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			EvaluateResultHttpResponse result = new EvaluateResultHttpResponse(response);
			sparqlQueryEvaluator.evaluate(result, repository, query, acceptHeader, toArray(defaultGraphUri),
					toArray(namedGraphUri));
		} catch (MalformedQueryException | IllegalStateException | IOException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	@GetMapping("/sparql")
	public void sparqlGet(@RequestParam(value = "default-graph-uri", required = false) String defaultGraphUri,
			@RequestParam(value = "named-graph-uri", required = false) String namedGraphUri,
			@RequestParam(value = "query") String query, @RequestHeader(ACCEPT) String acceptHeader,
			HttpServletRequest request, HttpServletResponse response) throws IOException {

		try {
			EvaluateResultHttpResponse result = new EvaluateResultHttpResponse(response);
			sparqlQueryEvaluator.evaluate(result, repository, query, acceptHeader, toArray(defaultGraphUri),
					toArray(namedGraphUri));
		} catch (MalformedQueryException | IllegalStateException | IOException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		}
	}

	private String[] toArray(String namedGraphUri) {
		if (namedGraphUri != null) {
			return new String[] { namedGraphUri };
		}
		return ALL_GRAPHS;
	}
}
