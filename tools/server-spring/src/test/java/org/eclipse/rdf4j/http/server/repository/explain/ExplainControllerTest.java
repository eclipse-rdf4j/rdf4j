/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository.explain;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

class ExplainControllerTest {
	private static final String VALID_QUERY = "SELECT * WHERE { ?s ?p ?o }";
	private static final String REPO_ID = "test-repo";

	private ExplainController controller;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;

	@BeforeEach
	void setUp() {
		controller = new ExplainController();

		request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setRequestURI("/repositories/" + REPO_ID + "/explain");
		request.setAttribute("repositoryID", REPO_ID);
		request.setAttribute("repository", new SailRepository(new MemoryStore()));
		response = new MockHttpServletResponse();
	}

	@Test
	void testSuccessfulExplain() throws Exception {
		final String requestBody = "{\"query\":\"" + VALID_QUERY + "\",\"level\":\"Timed\"}";
		request.setContent(requestBody.getBytes());
		request.setContentType("application/json");

		final ModelAndView result = controller.handleRequestInternal(request, response);

		assertEquals(HttpStatus.OK.value(), response.getStatus());
		assertNotNull(result);
		assertTrue(result.getModel().containsKey("explanation"));
		assertNotNull(result.getModel().get("explanation"));
		assertTrue(result.getModel().get("explanation").toString().contains("resultSizeEstimate=0"));
		assertTrue(result.getModel().get("explanation").toString().contains("resultSizeActual=0"));
	}

	@Test
	void testNullRepositoryExplain() throws Exception {
		request.setAttribute("repository", null);
		final String requestBody = "{\"query\":\"" + VALID_QUERY + "\",\"level\":\"Timed\"}";
		request.setContent(requestBody.getBytes());
		request.setContentType("application/json");

		final ModelAndView result = controller.handleRequestInternal(request, response);

		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
		assertNotNull(result);
		assertTrue(result.getModel().containsKey("message"));
		assertTrue(result.getModel().get("message").toString().startsWith("Repository is null"));
	}

	@Test
	void testMalformedQueryExplain() throws Exception {
		final String invalidQuery = "SELECT * BROKEN SYNTAX { ?s ?p }";
		final String requestBody = "{\"query\":\"" + invalidQuery + "\",\"level\":\"Timed\"}";
		request.setContent(requestBody.getBytes());
		request.setContentType("application/json");

		final ModelAndView result = controller.handleRequestInternal(request, response);

		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
		assertNotNull(result);
		assertTrue(result.getModel().containsKey("message"));
		System.out.println(result.getModel());
		assertTrue(result.getModel().get("message").toString().startsWith("Malformed query:"));
	}

	@Test
	void testEmptyQueryExplain() throws Exception {
		final String requestBody = "{\"query\":\"\",\"level\":\"Timed\"}";
		request.setContent(requestBody.getBytes());
		request.setContentType("application/json");

		final ModelAndView result = controller.handleRequestInternal(request, response);

		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
		assertNotNull(result);
		assertTrue(result.getModel().containsKey("message"));
		assertTrue(result.getModel().get("message").toString().startsWith("Malformed query:"));
		assertTrue(result.getModel().get("message").toString().contains("Was expecting one of:"));
	}

	@Test
	void testInvalidJsonRequestBody() throws Exception {
		final String requestBody = "{\"query\":\"" + VALID_QUERY + "\", level\":\"TIMED\"}"; // Missing quote before
																								// level
		request.setContent(requestBody.getBytes());
		request.setContentType("application/json");

		final ModelAndView result = controller.handleRequestInternal(request, response);

		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
		assertNotNull(result);
		assertTrue(result.getModel().containsKey("message"));
		assertTrue(result.getModel().get("message").toString().startsWith("Invalid request:"));
	}

	@Test
	void testMissingLevelParameter() throws Exception {
		final String requestBody = "{\"query\":\"" + VALID_QUERY + "\"}";
		request.setContent(requestBody.getBytes());
		request.setContentType("application/json");

		final ModelAndView result = controller.handleRequestInternal(request, response);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
		assertNotNull(result);
		assertTrue(result.getModel().containsKey("message"));
		assertTrue(result.getModel().get("message").toString().contains("Explain error:"));
	}

	@Test
	void testMissingQueryParameter() throws Exception {
		final String requestBody = "{\"level\":\"TIMED\"}";
		request.setContent(requestBody.getBytes());
		request.setContentType("application/json");

		final ModelAndView result = controller.handleRequestInternal(request, response);

		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
		assertNotNull(result);
		assertTrue(result.getModel().containsKey("message"));
		assertTrue(result.getModel().get("message").toString().contains("Invalid request:"));
	}

	@Test
	void testAllExplanationLevels() throws Exception {
		for (Explanation.Level level : Explanation.Level.values()) {
			response = new MockHttpServletResponse();

			final String requestBody = "{\"query\":\"" + VALID_QUERY + "\",\"level\":\"" + level.name() + "\"}";
			request.setContent(requestBody.getBytes());
			request.setContentType("application/json");

			final ModelAndView result = controller.handleRequestInternal(request, response);

			assertEquals(HttpStatus.OK.value(), response.getStatus(),
					"Failed with explanation level: " + level.name());
			assertNotNull(result);
			assertTrue(result.getModel().containsKey("explanation"));
			assertNotNull(result.getModel().get("explanation"));
		}
	}

	@Test
	void testInvalidExplanationLevel() throws Exception {
		final String requestBody = "{\"query\":\"" + VALID_QUERY + "\",\"level\":\"INVALID_LEVEL\"}";
		request.setContent(requestBody.getBytes());
		request.setContentType("application/json");

		// Act
		final ModelAndView result = controller.handleRequestInternal(request, response);

		// Assert
		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
		assertNotNull(result);
		assertTrue(result.getModel().containsKey("message"));
		assertTrue(result.getModel().get("message").toString().contains("Invalid request:"));
	}
}
